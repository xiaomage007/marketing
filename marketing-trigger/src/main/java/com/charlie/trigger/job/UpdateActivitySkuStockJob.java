package com.charlie.trigger.job;

import com.charlie.domain.activity.model.valobj.ActivitySkuStockKeyVO;
import com.charlie.domain.activity.service.ISkuStock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 定时任务 - 异步把活动 SKU 库存扣减结果落库到 MySQL
 * <p>
 * <b>所在链路:</b> 活动抽奖命中某个 sku 后,Redis 原子扣减库存(防超卖)→ 每笔扣减事件投递一条到 Redis 延迟队列 →
 * <b>本 Job 每 5 秒 poll 一条事件</b> → 把「1 次扣减」同步写回 {@code raffle_activity_sku.stock_count_surplus}。
 * <p>
 * <b>核心价值:</b> 高频抽奖场景下,DB 写会成为瓶颈。本 Job 把「即时防超卖」和「最终落库」解耦:
 * <ul>
 *   <li>Redis: 抽奖时原子扣减,毫秒级返回,保证不超卖</li>
 *   <li>MySQL: 由本 Job 异步、串行刷盘,最终一致即可</li>
 * </ul>
 * <p>
 * <b>具体事例(为什么需要延迟队列 + 定时刷盘):</b>
 * <pre>
 *   假设某秒杀活动 sku=901L,初始库存 100,Redis 中 surplus=100。
 *
 *   T0 时刻,50 个用户同时对 sku=901L 发起抽奖:
 *     - Redis DECR 在毫秒级内把 surplus 从 100 扣到 50(防超卖 OK)
 *     - 每笔抽奖各自投递一条独立事件到延迟队列(默认 3s 延迟),不是合并成 1 条
 *       → 此时延迟队列里有 50 条 [sku=901, activityId=100001] 消息
 *     - 若直接每扣一次就 UPDATE MySQL,瞬间 50 笔并发写 → DB 被打爆
 *
 *   T0 + 3s:延迟队列把 50 条独立消息依次投递到 BlockingQueue(仍是 50 条,没有合并)
 *   T0 + 5s:本 Job 触发,takeQueueValue() 取出第 1 条 sku=901L
 *           updateActivitySkuStock(901L) → UPDATE raffle_activity_sku SET stock_count_surplus = surplus - 1
 *   T0 + 10s:取出第 2 条 → 再 -1
 *   T0 + 15s:取出第 3 条 → 再 -1
 *   ...
 *   T0 + 150s:取出第 50 条 → 再 -1
 *
 *   结论:
 *     - 总 DB 写入次数 = 50 次(等于实际抽奖笔数,没有合并/聚合)
 *     - 但瞬时并发从「T0 时刻 50 笔并发」被削平为「5s / 笔」的串行节奏
 *     - DB 瞬时 QPS:50/s(瞬时打爆) → 0.2/s(每 5s 一笔)
 *
 *   T0 + 6s 库存耗尽(Redis surplus 降到 0)→ ActivitySkuStockZeroCustomer 收到 MQ →
 *     clearActivitySkuStock(901L) 把 sku=901 的库存直接置 0(强一致覆写)
 *     clearQueueValue() 清空 BlockingQueue 中剩余的 49 笔待落库事件(避免后续被 -1 覆盖回非 0)
 * </pre>
 * <p>
 * <b>触发节奏:</b> cron {@code "0/5 * * * * ?"} 每 5 秒一次,每次只处理队列中<b>一个</b>事件。
 * <p>
 *
 * @author Charlie
 */
@Slf4j
@Component
public class UpdateActivitySkuStockJob {

    /**
     * 领域服务抽象,屏蔽底层 Redis 延迟队列 + DAO 细节;
     * 通过 {@code @Resource} 注入的是 Spring 容器中的领域服务 Bean(实现 {@link ISkuStock})。
     */
    @Resource
    private ISkuStock skuStock;

    /**
     * 每 5 秒从延迟队列取一条活动 SKU 库存扣减事件,落库到 MySQL。
     * <p>
     * 流程:
     * <pre>
     *   poll 队列 → 队列为空直接返回 → 非空则按 sku 把「1 次扣减」刷盘到 raffle_activity_sku 表
     * </pre>
     * <p>
     * <b>具体事例:</b>
     * <pre>
     *   假设 T0 + 3s 时 BlockingQueue 中堆了 3 条事件:
     *     [sku=901, activityId=100001]
     *     [sku=901, activityId=100001]
     *     [sku=902, activityId=100001]
     *
     *   T0 + 5s   execute() 触发,takeQueueValue() 取出 [901, 100001]
     *             updateActivitySkuStock(901) → UPDATE raffle_activity_sku SET stock_count_surplus = surplus - 1
     *
     *   T0 + 10s  execute() 触发,取出 [901, 100001] → 再次 -1
     *   T0 + 15s  execute() 触发,取出 [902, 100001] → 刷 sku=902 的库存
     *
     *   注意:每条事件单独触发一次「surplus - 1」,不会合并。
     * </pre>
     * <p>
     * <b>为什么 try-catch 整个方法体?</b>
     * Spring {@code @Scheduled} 默认在方法抛出未捕获异常时会中断后续调度(具体取决于 TaskScheduler 实现)。
     * 这里包一层是「最坏情况兜底」,确保即便落库失败,定时任务本身也继续跑——失败的扣减由后续对账任务补偿。
     */
    @Scheduled(cron = "0/5 * * * * ?")
    public void execute() {
        try {
            log.info("定时任务，更新活动sku库存【延迟队列获取，降低对数据库的更新频次，不要产生竞争】");
            // 非阻塞 poll:队列空立即返回 null,不浪费线程
            ActivitySkuStockKeyVO activitySkuStockKeyVO = skuStock.takeQueueValue();
            if (null == activitySkuStockKeyVO) return;
            log.info("定时任务，更新活动sku库存 sku:{} activityId:{}", activitySkuStockKeyVO.getSku(), activitySkuStockKeyVO.getActivityId());
            // 注意:事件已在 takeQueueValue 中从队列取出,此处失败将丢消息(见类级注释「已知风险」)
            skuStock.updateActivitySkuStock(activitySkuStockKeyVO.getSku());
        } catch (Exception e) {
            log.error("定时任务，更新活动sku库存失败", e);
        }
    }

}