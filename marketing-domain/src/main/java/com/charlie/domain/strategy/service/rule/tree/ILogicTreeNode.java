package com.charlie.domain.strategy.service.rule.tree;

import com.charlie.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import com.charlie.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;

/**
 * @ClassName: ILogicTreeNode
 * @Description: 决策树节点接口。每个实现类对应一种规则（如次数锁、库存扣减、兜底奖励），
 *               由 Spring 以 bean 名注册，被 {@link com.charlie.domain.strategy.service.rule.tree.factory.DefaultTreeFactory}
 *               收集为 {@code Map<String, ILogicTreeNode>}，key=bean 名（即规则 code）。
 * @Author: Charlie
 * @Date: 2026/7/26 16:25
 * @Version: 1.0
 */
public interface ILogicTreeNode {

    /**
     * 节点业务逻辑：根据入参判断当前规则是否接管抽奖流程，并产出（或修改）奖品数据。
     * <p>
     * 返回值通过 {@link DefaultTreeFactory.TreeActionEntity#getRuleLogicCheckType()} 表达两种语义：
     * <ul>
     *   <li>{@link RuleLogicCheckTypeVO#ALLOW}（"0000"）：放行，决策树按出边跳到下一个节点</li>
     *   <li>{@link RuleLogicCheckTypeVO#TAKE_OVER}（"0001"）：接管，决策树按出边跳到下一个节点，
     *       通常下游会接兜底奖励节点，由兜底节点覆盖最终奖品</li>
     * </ul>
     *
     * @param userId     用户ID
     * @param strategyId 策略ID
     * @param awardId    上游已选出的奖品ID，节点可读取或改写
     * @return 决策动作实体，包含校验类型与（可选）奖品数据
     */
    DefaultTreeFactory.TreeActionEntity logic(String userId, Long strategyId, Integer awardId);

}