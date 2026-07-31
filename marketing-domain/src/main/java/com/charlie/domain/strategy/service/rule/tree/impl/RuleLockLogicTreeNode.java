package com.charlie.domain.strategy.service.rule.tree.impl;

import com.charlie.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import com.charlie.domain.strategy.service.rule.tree.ILogicTreeNode;
import com.charlie.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 决策树节点 - 抽奖次数门槛锁（rule_lock）
 * <p>
 * 业务语义：限制用户在达到「累计抽奖次数门槛」之前不能命中某些奖品，常见于留存类活动——
 * 例如「抽满 10 次才能抽到一等奖」「注册新用户需先完成 5 次抽奖才解锁大额奖品」。
 * <p>
 * 决策树中的位置：通常作为出边 <b>rule_lock</b> 的目的节点，配置方式是在
 * {@code strategy_rule.rule_value} 中写入门槛整数（如 {@code "10"}）。
 * <p>
 * 行为：
 * <ul>
 *   <li>用户累计抽奖次数 {@code >=} 阈值：返回 {@link RuleLogicCheckTypeVO#ALLOW}，决策树继续走下一节点</li>
 *   <li>用户累计抽奖次数 {@code <} 阈值：返回 {@link RuleLogicCheckTypeVO#TAKE_OVER}，
 *       通常会跳到 {@code RuleLuckAwardLogicTreeNode} 由兜底奖品接管</li>
 * </ul>
 * <p>
 * <b>当前为占位实现</b>：{@link #userRaffleCount} 写死为 {@code 10L}，未对接仓储查真实累计次数；
 * 后续需替换为「从 Redis/DB 读取 userId 的累计抽奖次数」。
 *
 * @author Charlie
 */
@Slf4j
@Component("rule_lock")
public class RuleLockLogicTreeNode implements ILogicTreeNode {

    /**
     * 用户累计抽奖次数的占位值。
     * <p>
     * TODO: 替换为从 Redis（或 DB）按 userId 实时查询的累计抽奖次数；
     * 当前硬编码为 10L 仅用于本地调试和单测。
     */
    private Long userRaffleCount = 10L;

    @Override
    public DefaultTreeFactory.TreeActionEntity logic(String userId, Long strategyId, Integer awardId, String ruleValue) {
        log.info("规则过滤-次数锁 userId:{} strategyId:{} awardId:{}", userId, strategyId, awardId);

        // ruleValue 形如 "10"，解析为门槛值；解析失败直接抛错，避免规则配置错误被静默吞掉
        long raffleCount = 0L;
        try {
            raffleCount = Long.parseLong(ruleValue);
        } catch (Exception e) {
            throw new RuntimeException("规则过滤-次数锁异常 ruleValue: " + ruleValue + " 配置不正确");
        }

        // 达到门槛 → 放行，让决策树继续向下寻找其他规则节点
        if (userRaffleCount >= raffleCount) {
            return DefaultTreeFactory.TreeActionEntity.builder()
                    .ruleLogicCheckType(RuleLogicCheckTypeVO.ALLOW)
                    .build();
        }

        // 未达门槛 → 接管，由后续兜底节点覆盖最终奖品（避免用户拿不到任何奖励）
        return DefaultTreeFactory.TreeActionEntity.builder()
                .ruleLogicCheckType(RuleLogicCheckTypeVO.TAKE_OVER)
                .build();
    }
}