package com.charlie.domain.strategy.service.rule.tree.impl;

import com.charlie.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import com.charlie.domain.strategy.service.rule.tree.ILogicTreeNode;
import com.charlie.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @ClassName: RuleLockLogicTreeNode
 * @Description: 次数锁节点。
 *               <p>语义：判断用户参与抽奖的次数是否达到解锁门槛（例如「抽满 N 次才放行高价值奖品」）。
 *               <p>当前为占位实现：直接返回 ALLOW，未读取次数与阈值。后续接入时需注入仓储，
 *               按用户累计次数与 ruleValue 中的阈值比较，达到则 ALLOW，否则 TAKE_OVER 走兜底分支。
 * @Author: Charlie
 * @Date: 2026/7/26 17:35
 * @Version: 1.0
 */
@Slf4j
@Component("rule_lock")
public class RuleLockLogicTreeNode implements ILogicTreeNode {

    @Override
    public DefaultTreeFactory.TreeActionEntity logic(String userId, Long strategyId, Integer awardId) {
        return DefaultTreeFactory.TreeActionEntity.builder()
                .ruleLogicCheckType(RuleLogicCheckTypeVO.ALLOW)
                .build();
    }
}