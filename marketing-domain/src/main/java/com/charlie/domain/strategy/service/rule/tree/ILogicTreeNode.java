package com.charlie.domain.strategy.service.rule.tree;

import com.charlie.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import com.charlie.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;

/**
 * 决策树节点接口。每个实现类对应一种规则（如次数锁、库存扣减、兜底奖励），
 *               由 Spring 以 bean 名注册，被 {@link com.charlie.domain.strategy.service.rule.tree.factory.DefaultTreeFactory}
 *               收集为 {@code Map<String, ILogicTreeNode>}，key=bean 名（即规则 code）。
 * @author Charlie
 */
public interface ILogicTreeNode {

    /**
     * 节点业务逻辑：根据入参判断当前规则是否接管抽奖流程，并产出（或修改）奖品数据。
     * <p>
     * 返回值通过 {@link DefaultTreeFactory.TreeActionEntity#getRuleLogicCheckType()} 表达两种语义：
     * <ul>
     *   <li>{@link RuleLogicCheckTypeVO#ALLOW}（"0000"）：放行，决策树按出边跳到下一个节点；
     *       <b>即便本节点设置了 {@code strategyAwardVO}，引擎也不会采纳</b>，awardId 保持上游传入的值不变</li>
     *   <li>{@link RuleLogicCheckTypeVO#TAKE_OVER}（"0001"）：接管。<b>如果同时设置了非空的 {@code strategyAwardVO}</b>，
     *       引擎认为「节点已锁奖」会<b>立即采纳 awardData 并终止决策树</b>；如果未设置 awardData，
     *       引擎继续按出边 walk，由下游节点（如 {@code rule_luck_award} 兜底）覆盖最终奖品</li>
     * </ul>
     *
     * @param userId     用户ID
     * @param strategyId 策略ID
     * @param awardId    上游已选出的奖品ID，节点可读取或改写
     * @param ruleValue  节点的规则值（如门槛值、积分区间等），由 {@code rule_tree_node.rule_value} 透传
     * @return 决策动作实体，包含校验类型与（可选）奖品数据
     */
    DefaultTreeFactory.TreeActionEntity logic(String userId, Long strategyId, Integer awardId, String ruleValue);

}