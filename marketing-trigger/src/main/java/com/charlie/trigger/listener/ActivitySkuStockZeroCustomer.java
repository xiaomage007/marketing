package com.charlie.trigger.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.charlie.domain.activity.service.IRaffleActivitySkuStockService;
import com.charlie.types.event.BaseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * MQ 消费者 - 活动 SKU 库存清零后的最终一致性收尾
 * <p>
 * <b>所在链路:</b>
 * <pre>
 *   抽奖命中 sku → Redis DECR → surplus == 0 →
 *   {@code ActivityRepository.subtractionActivitySkuStock} 发布 MQ 消息 →
 *   <b>本消费者</b>收到 → 把 MySQL 的 {@code raffle_activity_sku.stock_count_surplus} 强制置 0 +
 *   清空 {@code UpdateActivitySkuStockJob} 还在排队的 BlockingQueue。
 * </pre>
 * <p>
 * <b>核心价值:</b> Redis 与 MySQL 是「最终一致」关系。本类是「库存耗尽」时的<b>强一致收尾</b>,
 * 防止 Job 的「延迟 -1」把已被 Redis 标记为 0 的库存又扣成负数。
 * <p>
 * <b>消息体格式(由 {@code ActivitySkuStockZeroMessageEvent.buildEventMessage} 生成):</b>
 * <pre>
 *   {
 *     "id": "88012345678",                // 11 位随机数字,消息 ID
 *     "timestamp": "2026-08-23 16:30:05", // 事件生成时间
 *     "data": 901                          // 业务数据:被清零的 sku
 *   }
 * </pre>
 * <p>
 * <b>具体事例(与 UpdateActivitySkuStockJob 衔接):</b>
 * <pre>
 *   假设某秒杀活动 sku=901L,初始库存 100,Redis 中 surplus=100,DB 中 stock_count_surplus=100。
 *
 *   T0 时刻,50 个用户同时对 sku=901L 发起抽奖:
 *     - Redis DECR 1 → 99(DECR 2 → 98 ... DECR 50 → 50)
 *     - 每笔 DECR 都投递一条独立事件到延迟队列,共 50 条 [sku=901, activityId=100001]
 *
 *   T0 + 3s :延迟队列把 50 条消息投递到 BlockingQueue
 *   T0 + 5s :UpdateActivitySkuStockJob 取出第 1 条 → DB 写 surplus = 99(剩 49 笔排队)
 *   T0 + 6s :用户对 sku=901L 发起第 51 次抽奖 → DECR surplus:1 → 0
 *             → ActivityRepository.subtractionActivitySkuStock 检测到 surplus == 0 →
 *               发布消息到队列 "activity_sku_stock_zero"(默认交换机,路由键=队列名):
 *               {"id":"88012345678","timestamp":"...","data":901}
 *             → return false(告诉上游「库存已空,本笔拒绝」)
 *
 *   T0 + 6.05s :本消费者 listener() 触发:
 *               1) JSON.parseObject → EventMessage<Long>{id, timestamp, data=901}
 *               2) clearActivitySkuStock(901):
 *                    UPDATE raffle_activity_sku
 *                    SET stock_count_surplus = 0, update_time = now()
 *                    WHERE sku = 901
 *                  → DB 库存与 Redis 一致(都是 0)
 *               3) clearQueueValue():
 *                    destinationQueue.clear()
 *                  → BlockingQueue 里还排着的 49 笔 [901] 全部清空
 *                  → 避免 UpdateActivitySkuStockJob 在 49 * 5s = 245s 内把 DB 一路扣成负数
 *
 *   最终一致性窗口:
 *     T0 + 5s   DB surplus = 99(Job 已写)
 *     T0 + 6.05s DB surplus = 0(本消费者强一致覆写)
 *     → 中间 1.05s 内 DB 是「中间值 99」,但已经接近终态;后续 Job 即使再写也只在 49 笔扣完前是 0
 * </pre>
 * <p>
 * <b>两步顺序的设计:</b>
 * <ul>
 *   <li><b>先 clearActivitySkuStock</b>:DB 强制置 0 是「终态覆写」,即使后续 Job 又刷一条 -1,
 *       因为 Job 的 SQL 带 {@code WHERE stock_count_surplus > 0} 保护(surplus 已为 0 时不扣),
 *       最终 DB 仍是 0。</li>
 *   <li><b>后 clearQueueValue</b>:清空 BlockingQueue 中残留的扣减事件,避免 Job 在 5s 节奏下一路把 DB 扣成负数。</li>
 * </ul>
 * <p>
 * <b>已知风险(待优化):</b>
 * <ol>
 *   <li><b>MQ 丢失风险</b>:若本消费者没收到消息(网络丢/服务宕),Redis 已是 0 但 DB 还是中间值。
 *       需要「定时对账」Job 兜底:扫描 {@code stock_count_surplus > 0 AND Redis surplus = 0} 的 sku 并强制 DB 置 0。</li>
 *   <li><b>重复消费风险</b>:MQ 至少一次投递语义下,本类可能被重复触发。
 *       {@code clearActivitySkuStock} 是幂等的(SET 0 SET 0 还是 0);
 *       但 {@code clearQueueValue} 会清掉 BlockingQueue 中<b>新进来</b>的扣减消息——理论上不会发生,
 *       因为本消费触发时 Redis surplus 已经为 0,上游 DECR 会被分支③回填为 0 后 return false,不会投延迟队列。</li>
 *   <li><b>中间状态不一致</b>:DB 写 0 之前,UpdateActivitySkuStockJob 仍可能拿到一条 [901] 事件做 -1,
 *       因为 Job 的 SQL 是「surplus &gt; 0 才扣」,且此时 DB surplus 还是中间值(如 99),会被扣成 98。
 *       紧接着本消费者写 0 → 最终正确。
 *       <b>副作用</b>:DB surplus 在写 0 之前会闪一下 98;但因为整批事件马上就要被清掉,实际影响可忽略。</li>
 * </ol>
 *
 * @author Charlie
 */
@Slf4j
@Component
public class ActivitySkuStockZeroCustomer {


    /**
     * 领域服务抽象,屏蔽底层 Redis 队列 + DAO 细节;
     * 通过 {@code @Resource} 注入的是 Spring 容器中的领域服务 Bean(实现 {@link IRaffleActivitySkuStockService})。
     */
    @Resource
    private IRaffleActivitySkuStockService skuStock;

    /**
     * MQ 消息处理入口。{@code queues = "..."} 表示「直接监听已声明的队列」,不再由本注解自动声明;
     * 队列/交换机/binding 的拓扑定义在 {@code application*.yml} 的 {@code rabbitmq.topology} 配置段,
     * 由 RabbitMqConfig(marketing-infrastructure 模块)读取并构建 Declarables,启动时由 RabbitAdmin 自动 declare。
     * <p>
     * SpEL {@code #{@rabbitMqTopologyProperties.queues['activity-sku-stock-zero'].name}}:
     * 引用配置绑定 Bean,按逻辑 key 取队列的 broker 物理名,与配置文件同源。
     * <p>
     * 流程:
     * <pre>
     *   收到 JSON 消息 → 反序列化为 EventMessage&lt;Long&gt; → 取 data(sku) →
     *   clearActivitySkuStock(sku) 把 DB 库存置 0 →
     *   clearQueueValue() 清空 BlockingQueue 残留事件
     * </pre>
     * <p>
     * <b>具体事例:</b>
     * <pre>
     *   收到消息:
     *     {"id":"88012345678","timestamp":"2026-08-23 16:30:05","data":901}
     *
     *   listener(String message) 执行:
     *     1) JSON.parseObject → EventMessage<Long> eventMessage
     *        → eventMessage.getData() = 901L
     *     2) skuStock.clearActivitySkuStock(901L)
     *        → UPDATE raffle_activity_sku SET stock_count_surplus = 0 WHERE sku = 901
     *     3) skuStock.clearQueueValue()
     *        → destinationQueue.clear()(BlockingQueue 里若还有 49 笔 [901],全部清空)
     * </pre>
     * <p>
     * <b>为什么 try-catch 后要 rethrow?</b>
     * 失败时不能简单 {@code log + return}——必须抛出去让 Spring AMQP 走「消息拒绝」逻辑,
     * 触发死信队列 / 重试机制;否则静默吞异常会导致「Redis 0、DB 99」的脏数据永远不会被对账发现。
     */
    @RabbitListener(queues = "#{@rabbitMqTopologyProperties.queues['activity-sku-stock-zero'].name}")
    public void listener(String message) {

        try {
            log.info("监听活动sku库存消耗为0消息 message: {}", message);
            BaseEvent.EventMessage<Long> eventMessage = JSON.parseObject(message, new TypeReference<BaseEvent.EventMessage<Long>>() {
            }.getType());
            Long sku = eventMessage.getData();
            skuStock.clearActivitySkuStock(sku);
            skuStock.clearQueueValue();
        } catch (Exception e) {
            log.error("监听活动sku库存消耗为0消息，消费失败 message: {}", message);
            throw e;
        }

    }

}