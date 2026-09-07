package com.charlie.domain.strategy.service.rule.tree.impl;

import com.charlie.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import com.charlie.domain.strategy.model.valobj.StrategyAwardStockKeyVO;
import com.charlie.domain.strategy.repository.IStrategyRepository;
import com.charlie.domain.strategy.service.armory.IStrategyDispatch;
import com.charlie.domain.strategy.service.rule.tree.ILogicTreeNode;
import com.charlie.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 决策树节点 - 库存扣减（rule_stock）
 * <p>
 * 业务语义：对当前命中的 {@code awardId} 在 Redis 中执行<b>原子库存扣减</b>。
 * <ul>
 *   <li><b>扣减成功</b> → TAKE_OVER + 透传 awardId，引擎检测到「TAKE_OVER 且 awardData 非空」会<b>立即锁奖结束</b>，
 *       不再 walk 下游（避免被 {@code rule_luck_award} 覆盖）</li>
 *   <li><b>库存不足</b> → TAKE_OVER + <b>不携带</b> awardData，引擎继续走下一节点，
 *       由 {@code rule_luck_award} 兜底节点覆盖 awardId 为兜底奖品</li>
 * </ul>
 * <p>
 * 决策树中的位置：通常是某奖品节点的「出边 rule_stock」的目的节点，即<b>命中真正想发的奖品后必经的库存闸口</b>。
 * <p>
 * 关键设计点：
 * <ul>
 *   <li><b>扣减即锁奖</b>：扣减成功后 TAKE_OVER 携带 awardId，引擎短路掉下游路径，保证「一次抽奖、一次扣减、一个奖品」原子闭环</li>
 *   <li><b>异步落库</b>：扣减成功后只把事件投到延迟队列，{@code UpdateAwardStockJob} 异步把 Redis 扣减量合并写回 MySQL，
 *       避免抽奖高峰时 DB 成为瓶颈（Redis 抗写 + DB 异步 batch）</li>
 *   <li><b>不超卖兜底</b>：{@link IStrategyDispatch#subtractionAwardStock} 内部已用 Redis Lua 保证原子扣减，
 *       返回 false 即库存不足，绝对不会出现「扣成负数」</li>
 * </ul>
 *
 * @author Charlie
 */
@Slf4j
@Component("rule_stock")
public class RuleStockLogicTreeNode implements ILogicTreeNode {

    @Resource
    private IStrategyDispatch strategyDispatch;
    @Resource
    private IStrategyRepository strategyRepository;

    @Override
    public DefaultTreeFactory.TreeActionEntity logic(String userId, Long strategyId, Integer awardId, String ruleValue, Date endDateTime) {
        log.info("规则过滤-库存扣减 userId:{} strategyId:{} awardId:{}", userId, strategyId, awardId);

        // Redis 原子扣减（Lua 脚本保证「读-减-写」三步原子，不超卖）
        Boolean status = strategyDispatch.subtractionAwardStock(strategyId, awardId, endDateTime);
        if (status) {
            log.info("规则过滤-库存扣减-成功 userId:{} strategyId:{} awardId:{}", userId, strategyId, awardId);

            // 异步落库：投递「扣减事件」到延迟队列，由 trigger 层的 UpdateAwardStockJob 消费后合并写入 MySQL
            // 延迟 3 秒是为了给同批次多次扣减一个窗口期，消费者可以批量合并写，减少 DB 写压力
            strategyRepository.awardStockConsumeSendQueue(StrategyAwardStockKeyVO.builder()
                    .strategyId(strategyId)
                    .awardId(awardId)
                    .build());

            // 扣减成功 → TAKE_OVER + 透传 awardId；引擎检测到「TAKE_OVER 且 awardData 非空」立即锁奖结束
            return DefaultTreeFactory.TreeActionEntity.builder()
                    .ruleLogicCheckType(RuleLogicCheckTypeVO.TAKE_OVER)
                    .strategyAwardVO(DefaultTreeFactory.StrategyAwardVO.builder()
                            .awardId(awardId)
                            .awardRuleValue(ruleValue).build())
                    .build();
        }
        // 库存不足 → TAKE_OVER + 不携带 awardData；引擎继续 walk，下游 rule_luck_award 会接管并覆盖 awardId 为兜底奖品
        log.warn("规则过滤-库存扣减-告警，库存不足。userId:{} strategyId:{} awardId:{}", userId, strategyId, awardId);
        return DefaultTreeFactory.TreeActionEntity.builder()
                .ruleLogicCheckType(RuleLogicCheckTypeVO.TAKE_OVER)
                .build();
    }
}