package com.charlie.domain.strategy.service.rule.tree.impl;

import com.charlie.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import com.charlie.domain.strategy.service.rule.tree.ILogicTreeNode;
import com.charlie.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import com.charlie.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 决策树节点 - 兜底奖励（rule_luck_award）
 * <p>
 * 业务语义：当上游规则（如次数锁未达标、库存扣减失败等）<b>TAKE_OVER 接管但未携带 awardData</b> 时，
 * 决策树按出边跳到此节点，把最终奖品<b>覆盖</b>为「兜底奖品」，避免用户抽不到任何东西。
 * <p>
 * 典型场景：「未达抽奖次数门槛」/「库存耗尽」等情况下，让用户拿到一个保底奖励（如积分、安慰奖）。
 * <p>
 * <b>触发条件</b>：上游节点 TAKE_OVER 且 {@code strategyAwardVO == null}（参考 {@link com.charlie.domain.strategy.service.rule.tree.factory.engine.impl.DecisionTreeEngine}
 * 的「锁奖短路」逻辑）。如果上游节点 TAKE_OVER 且携带了 awardData，会被引擎立即采纳并终止决策树——本节点不会被调用。
 * <p>
 * 决策树中的位置：通常是某条 TAKE_OVER 出边的目的节点（即终点之一）。
 * <p>
 * {@code ruleValue} 格式约定：{@code "<兜底奖品ID>:<兜底奖品的规则值>"}，用 {@link Constants#COLON}（冒号）分隔，
 * 例如 {@code "101:1,100"} 表示「奖品 ID=101，其规则值 1,100 透传给下游」。
 *
 * @author Charlie
 */
@Slf4j
@Component("rule_luck_award")
public class RuleLuckAwardLogicTreeNode implements ILogicTreeNode {

    @Override
    public DefaultTreeFactory.TreeActionEntity logic(String userId, Long strategyId, Integer awardId, String ruleValue) {
        log.info("规则过滤-兜底奖品 userId:{} strategyId:{} awardId:{} ruleValue:{}", userId, strategyId, awardId, ruleValue);
        // 按 COLON 拆分出「奖品ID」与「奖品规则值」两段；规则值缺失也允许（视为空字符串透传）
        String[] split = ruleValue.split(Constants.COLON);
        if (split.length == 0) {
            // split 不可能为 length=0（"abc".split 至少返回 ["abc"]），此处实际不可达，保留仅为防御
            log.error("规则过滤-兜底奖品，兜底奖品未配置告警 userId:{} strategyId:{} awardId:{}", userId, strategyId, awardId);
            throw new RuntimeException("兜底奖品未配置 " + ruleValue);
        }
        Integer luckAwardId = Integer.valueOf(split[0]);
        // 第二段可选：缺省时给空串，避免下游 NPE
        String awardRuleValue = split.length > 1 ? split[1] : "";
        log.info("规则过滤-兜底奖品 userId:{} strategyId:{} awardId:{} awardRuleValue:{}", userId, strategyId, luckAwardId, awardRuleValue);
        // 兜底节点一定 TAKE_OVER——上游已经接管，本节点负责把奖品「盖掉」成兜底奖品
        return DefaultTreeFactory.TreeActionEntity.builder()
                .ruleLogicCheckType(RuleLogicCheckTypeVO.TAKE_OVER)
                .strategyAwardVO(DefaultTreeFactory.StrategyAwardVO.builder()
                        .awardId(luckAwardId)
                        .awardRuleValue(awardRuleValue)
                        .build())
                .build();
    }

}