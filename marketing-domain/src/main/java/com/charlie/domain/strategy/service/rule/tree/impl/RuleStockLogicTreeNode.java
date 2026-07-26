package com.charlie.domain.strategy.service.rule.tree.impl;

import com.charlie.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import com.charlie.domain.strategy.service.rule.tree.ILogicTreeNode;
import com.charlie.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @ClassName: RuleStockLogicTreeNode
 * @Description: 库存扣减节点
 * @Author: Charlie
 * @Date: 2026/7/26 17:39
 * @Version: 1.0
 */
@Slf4j
@Component("rule_stock")
public class RuleStockLogicTreeNode implements ILogicTreeNode {
    @Override
    public DefaultTreeFactory.TreeActionEntity logic(String userId, Long strategyId, Integer awardId) {
        return DefaultTreeFactory.TreeActionEntity.builder()
                .ruleLogicCheckType(RuleLogicCheckTypeVO.TAKE_OVER)
                .build();
    }
}