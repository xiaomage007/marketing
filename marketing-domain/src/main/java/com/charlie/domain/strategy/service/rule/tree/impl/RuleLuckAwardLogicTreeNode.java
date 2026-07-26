package com.charlie.domain.strategy.service.rule.tree.impl;

import com.charlie.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import com.charlie.domain.strategy.service.rule.tree.ILogicTreeNode;
import com.charlie.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @ClassName: RuleLuckAwardLogicTreeNode
 * @Description: 兜底奖励节点
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
                .strategyAwardData(DefaultTreeFactory.StrategyAwardData.builder()
                        .awardId(101)
                        .awardRuleValue("1,100")
                        .build())
                .build();
    }

}