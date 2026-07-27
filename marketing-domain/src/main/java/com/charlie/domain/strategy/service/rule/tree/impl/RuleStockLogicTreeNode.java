package com.charlie.domain.strategy.service.rule.tree.impl;

import com.charlie.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import com.charlie.domain.strategy.service.rule.tree.ILogicTreeNode;
import com.charlie.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @ClassName: RuleStockLogicTreeNode
 * @Description: 库存扣减节点。
 *               <p>语义：对当前 awardId 执行 Redis 库存扣减（DECR/Guarda），扣减成功放行；
 *               库存不足则 TAKE_OVER 跳兜底。
 *               <p>当前为占位实现：直接返回 TAKE_OVER 但未填充 strategyAwardData，
 *               实际接入时需注入仓储完成扣减并按结果设置校验类型。
 * @Author: Charlie
 * @Date: 2026/7/26 17:39
 * @Version: 1.0
 */
@Slf4j
@Component("rule_stock")
public class RuleStockLogicTreeNode implements ILogicTreeNode {
    @Override
    public DefaultTreeFactory.TreeActionEntity logic(String userId, Long strategyId, Integer awardId) {
        // TODO: 调用仓储扣减 awardId 库存，按结果返回 ALLOW/TAKE_OVER；当前占位直接接管
        return DefaultTreeFactory.TreeActionEntity.builder()
                .ruleLogicCheckType(RuleLogicCheckTypeVO.TAKE_OVER)
                .build();
    }
}