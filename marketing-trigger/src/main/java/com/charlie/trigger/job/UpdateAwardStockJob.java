package com.charlie.trigger.job;

import com.charlie.domain.strategy.model.valobj.StrategyAwardStockKeyVO;
import com.charlie.domain.strategy.service.IRaffleStock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 定时任务 - 异步把 Redis 库存扣减结果落库到 MySQL
 * <p>
 * <b>所在链路:</b> 抽奖命中奖品后,Redis 原子扣减库存(防超卖)→ 扣减事件投递到延迟队列 →
 * <b>本 Job 周期 poll 队列</b> → 把扣减量同步写回 {@code strategy_award.award_count_surplus}。
 * <p>
 * <b>核心价值:</b> 高频抽奖场景下,DB 写会成为瓶颈。本 Job 把「即时防超卖」和「最终落库」解耦:
 * <ul>
 *   <li>Redis: 抽奖时原子扣减,毫秒级返回,保证不超卖</li>
 *   <li>MySQL: 由本 Job 异步刷盘,最终一致即可</li>
 * </ul>
 * <p>
 * <b>触发节奏:</b> cron {@code "0/5 * * * * ?"} 每 5 秒一次,每次只处理队列中<b>一个</b>事件。
 * <p>
 * <b>已知风险(待优化):</b>
 * <ol>
 *   <li><b>单条刷盘</b>:每次只刷 1 条,DB QPS 仍然偏高。可改为「批量聚合一段时间窗口内的所有事件,单次 UPDATE」</li>
 *   <li><b>丢消息风险</b>:{@link IRaffleStock#takeQueueValue()} 用 {@code poll()} 已经把事件从队列取走,
 *       如果 {@link IRaffleStock#updateStrategyAwardStock} 抛异常且没有重试/补偿,事件会被静默丢弃——
 *       当前实现仅靠日志告警,生产环境建议接入「失败重试 + 死信队列」或「定时对账」</li>
 * </ol>
 *
 * @author Charlie
 */
@Slf4j
@Component
public class UpdateAwardStockJob {

    /**
     * 领域服务抽象,屏蔽底层 Redis 队列 + DAO 细节;
     * 通过 {@code @Resource} 注入的是 Spring 容器中的 {@code DefaultRaffleStrategy} Bean。
     */
    @Resource
    private IRaffleStock raffleStock;

    /**
     * 每 5 秒从延迟队列取一条库存扣减事件,落库到 MySQL。
     * <p>
     * 流程:
     * <pre>
     *   poll 队列 → 队列为空直接返回 → 非空则按 strategyId + awardId 把「1 次扣减」刷盘
     * </pre>
     * <p>
     * <b>为什么 try-catch 整个方法体?</b>
     * Spring {@code @Scheduled} 默认在方法抛出未捕获异常时会中断后续调度(具体取决于 TaskScheduler 实现)。
     * 这里包一层是「最坏情况兜底」,确保即便落库失败,定时任务本身也继续跑——失败的扣减由后续对账任务补偿。
     */
    @Scheduled(cron = "0/5 * * * * ?")
    public void exec() {
        try {
            log.info("定时任务，更新奖品消耗库存【延迟队列获取，降低对数据库的更新频次，不要产生竞争】");
            // 非阻塞 poll:队列空立即返回 null,不浪费线程
            StrategyAwardStockKeyVO strategyAwardStockKeyVO = raffleStock.takeQueueValue();
            if (null == strategyAwardStockKeyVO) return;
            log.info("定时任务，更新奖品消耗库存 strategyId:{} awardId:{}", strategyAwardStockKeyVO.getStrategyId(), strategyAwardStockKeyVO.getAwardId());
            // 注意:事件已在 takeQueueValue 中从队列取出,此处失败将丢消息(见类级注释「已知风险」)
            raffleStock.updateStrategyAwardStock(strategyAwardStockKeyVO.getStrategyId(), strategyAwardStockKeyVO.getAwardId());
        } catch (Exception e) {
            log.error("定时任务，更新奖品消耗库存失败", e);
        }
    }
}
