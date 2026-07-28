package com.charlie.domain.strategy.service.rule.tree.impl;

import com.charlie.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import com.charlie.domain.strategy.service.rule.tree.ILogicTreeNode;
import com.charlie.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @ClassName: RuleLuckAwardLogicTreeNode
 * @Description: 兜底奖励节点。
 *               <p>语义：当上游规则（如次数锁、库存）TAKE_OVER 接管后，决策树跳到此节点，
 *               覆盖最终奖品为「兜底奖品」，避免用户拿不到任何奖励。
 *               <p>当前为占位实现：固定返回 awardId=101、awardRuleValue="1,100"。后续应从
 *               strategy_rule 读取实际配置。
 * @Author: Charlie
 * @Date: 2026/7/26 17:38
 * @Version: 1.0
 */
@Slf4j
@Component("rule_luck_award")
public class RuleLuckAwardLogicTreeNode implements ILogicTreeNode {

    @Override
    public DefaultTreeFactory.TreeActionEntity logic(String userId, Long strategyId, Integer awardId) {
        return DefaultTreeFactory.TreeActionEntity.builder()
                .ruleLogicCheckType(RuleLogicCheckTypeVO.TAKE_OVER)
                .strategyAwardVO(DefaultTreeFactory.StrategyAwardVO.builder()
                        .awardId(101)
                        .awardRuleValue("1,100")
                        .build())
                .build();
    }

}