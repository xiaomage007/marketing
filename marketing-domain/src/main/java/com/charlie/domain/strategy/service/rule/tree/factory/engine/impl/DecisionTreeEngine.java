package com.charlie.domain.strategy.service.rule.tree.factory.engine.impl;

import com.charlie.domain.strategy.model.valobj.RuleLimitTypeVO;
import com.charlie.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import com.charlie.domain.strategy.model.valobj.RuleTreeNodeLineVO;
import com.charlie.domain.strategy.model.valobj.RuleTreeNodeVO;
import com.charlie.domain.strategy.model.valobj.RuleTreeVO;
import com.charlie.domain.strategy.service.rule.tree.ILogicTreeNode;
import com.charlie.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import com.charlie.domain.strategy.service.rule.tree.factory.engine.IDecisionTreeEngine;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * @ClassName: DecisionTreeEngine
 * @Description: 决策树引擎
 * @Author: Charlie
 * @Date: 2026/7/26 17:34
 * @Version: 1.0
 */
@Slf4j
public class DecisionTreeEngine implements IDecisionTreeEngine {

    private final Map<String, ILogicTreeNode> logicTreeNodeGroup;

    private final RuleTreeVO ruleTreeVO;

    public DecisionTreeEngine(Map<String, ILogicTreeNode> logicTreeNodeGroup, RuleTreeVO ruleTreeVO) {
        this.logicTreeNodeGroup = logicTreeNodeGroup;
        this.ruleTreeVO = ruleTreeVO;
    }

    /**
     * 决策树引擎入口：从根节点开始迭代执行，沿节点出边跳转，直到走到叶子节点（无后续节点）后返回最终决策结果。
     * <p>
     * 执行流程：
     * <pre>
     *   根节点 ruleKey -> ILogicTreeNode.logic() -> 得到 RuleLogicCheckTypeVO(ALLOW/TAKE_OVER)
     *        -> nextNode() 按 code 匹配出边 -> 跳到下一个节点 -> 循环直至 nextNode == null
     * </pre>
     * <p>
     * <b>awardData 采纳规则（关键）</b>：仅当节点返回 <b>TAKE_OVER 且携带非空 awardData</b> 时，引擎才采纳本次 awardData 并<b>立即 break</b>
     * （"锁奖"语义——节点已经决定最终奖品，引擎不再 walk 下游，包括兜底节点）。
     * <ul>
     *   <li>TAKE_OVER + awardData 非空 → 锁奖，立刻返回，不再走 nextNode（避免被下游兜底覆盖）</li>
     *   <li>TAKE_OVER + awardData 为空 → 继续 walk（通常由下游兜底节点覆盖）</li>
     *   <li>ALLOW（无论是否携带 awardData）→ 不采纳 awardData，继续走 nextNode</li>
     * </ul>
     *
     * @param userId     用户ID（透传给决策节点用于业务判断，例如黑名单校验、积分权重等）
     * @param strategyId 策略ID（透传给决策节点）
     * @param awardId    上游已选出的奖品ID（透传给决策节点，部分规则会基于此修改最终奖品）
     * @return 最后一次锁奖产出的 {@link DefaultTreeFactory.StrategyAwardVO}，含 awardId 与 awardRuleValue
     */
    @Override
    public DefaultTreeFactory.StrategyAwardVO process(String userId, Long strategyId, Integer awardId) {
        DefaultTreeFactory.StrategyAwardVO strategyAwardVO = null;

        // 1. 获取规则树基础信息：根节点 key + 节点映射表（key=规则节点 key，value=节点详情）
        String nextNode = ruleTreeVO.getTreeRootRuleNode();
        Map<String, RuleTreeNodeVO> treeNodeMap = ruleTreeVO.getTreeNodeMap();

        // 2. 定位起始节点「根节点记录了第一个要执行的规则 key」
        RuleTreeNodeVO ruleTreeNode = treeNodeMap.get(nextNode);

        // 3. 沿决策树迭代：当前节点 -> 计算 -> 选下一节点，直到无后续节点
        while (null != nextNode) {
            // 3.1 根据 ruleKey 从节点组中取出对应的决策节点实现（ILogicTreeNode）
            ILogicTreeNode logicTreeNode = logicTreeNodeGroup.get(ruleTreeNode.getRuleKey());
            String ruleValue = ruleTreeNode.getRuleValue();
            // 3.2 决策节点执行业务逻辑，得到动作实体（校验类型 + 抽奖数据）
            //     ruleLogicCheckTypeVO.getCode() 取值："0000"=ALLOW 放行，"0001"=TAKE_OVER 接管
            DefaultTreeFactory.TreeActionEntity logicEntity = logicTreeNode.logic(userId, strategyId, awardId, ruleValue);
            RuleLogicCheckTypeVO ruleLogicCheckTypeVO = logicEntity.getRuleLogicCheckType();
            log.info("决策树引擎【{}】treeId:{} node:{} code:{}", ruleTreeVO.getTreeName(), ruleTreeVO.getTreeId(), nextNode, ruleLogicCheckTypeVO.getCode());

            // 3.3 锁奖短路：节点 TAKE_OVER 且携带非空 awardData，表示"已决定最终奖品"
            //     ——采纳本次 awardData 并立即退出循环，不再 walk 下游节点（包括兜底），
            //     避免"扣减成功→rule_stock 已决定奖→再被 rule_luck_award 覆盖"这类串台。
            boolean lockAward = RuleLogicCheckTypeVO.TAKE_OVER.equals(ruleLogicCheckTypeVO)
                    && logicEntity.getStrategyAwardVO() != null;
            if (lockAward) {
                strategyAwardVO = logicEntity.getStrategyAwardVO();
                break;
            }

            // 3.4 非锁奖路径：按当前节点的决策结果 code，从出边列表中选出下一个节点；无出边则返回 null 结束迭代
            //     ALLOW 路径即便节点塞了 awardData 也不采纳——保留上游 awardId 不变，继续走下游
            nextNode = nextNode(ruleLogicCheckTypeVO.getCode(), ruleTreeNode.getTreeNodeLineVOList());
            ruleTreeNode = treeNodeMap.get(nextNode);
        }

        // 4. 返回最后一次锁奖产出的奖品数据（含 awardId 与 awardRuleValue）
        return strategyAwardVO;
    }

    /**
     * 根据当前节点的决策结果，从其出边列表中匹配下一个要执行的节点 key。
     * <p>
     * 匹配规则：按列表顺序逐条用 {@link #decisionLogic} 判断，命中第一条即返回（短路），
     * 因此出边的配置顺序即匹配优先级。
     *
     * @param matterValue        当前节点的决策结果 code，取自 {@link RuleLogicCheckTypeVO#getCode()}
     *                           （"0000"=放行 ALLOW，"0001"=接管 TAKE_OVER）
     * @param treeNodeLineVOList 当前节点的出边集合，每条线描述「from -> to + 限定条件」
     * @return 下一个节点 key；若当前节点无出边（叶子节点），返回 null 结束迭代
     * @throws RuntimeException 有出边但无一匹配，说明规则树配置异常，直接中断
     */
    public String nextNode(String matterValue, List<RuleTreeNodeLineVO> treeNodeLineVOList) {
        // 无出边：当前节点为叶子节点，结束决策树迭代
        if (null == treeNodeLineVOList || treeNodeLineVOList.isEmpty()) return null;
        // 顺序匹配第一条满足限定条件的出边，命中即返回
        for (RuleTreeNodeLineVO nodeLine : treeNodeLineVOList) {
            if (decisionLogic(matterValue, nodeLine)) {
                return nodeLine.getRuleNodeTo();
            }
        }
        // 有出边但都不匹配：规则树配置不完整，直接抛异常暴露问题
        throw new RuntimeException("决策树引擎，nextNode 计算失败，未找到可执行节点！");
    }

    /**
     * 出边限定条件匹配。将当前节点决策结果（code 字符串）按 {@link RuleLimitTypeVO} 与出边的 ruleLimitValue 做比较。
     * <p>
     * 当前仅实现 EQUAL 等值匹配（决策树场景下结果只有 ALLOW/TAKE_OVER 两种 code，等值即可覆盖）；
     * GT/LT/GE/LE/ENUM 预留扩展位，未匹配返回 false。
     *
     * @param matterValue 当前节点决策结果 code（"0000" 或 "0001"）
     * @param nodeLine    待匹配的出边
     * @return true 表示该出边被选中
     */
    public boolean decisionLogic(String matterValue, RuleTreeNodeLineVO nodeLine) {
        switch (nodeLine.getRuleLimitType()) {
            case EQUAL:
                // 等值比较：决策结果 code == 出边限定值 code
                return matterValue.equals(nodeLine.getRuleLimitValue().getCode());
            // 以下规则暂时不需要实现
            case GT:
            case LT:
            case GE:
            case LE:
            default:
                return false;
        }
    }

}