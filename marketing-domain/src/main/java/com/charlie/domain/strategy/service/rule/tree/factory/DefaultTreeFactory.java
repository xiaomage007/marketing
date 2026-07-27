package com.charlie.domain.strategy.service.rule.tree.factory;

import com.charlie.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import com.charlie.domain.strategy.model.valobj.RuleTreeVO;
import com.charlie.domain.strategy.service.rule.tree.ILogicTreeNode;
import com.charlie.domain.strategy.service.rule.tree.factory.engine.IDecisionTreeEngine;
import com.charlie.domain.strategy.service.rule.tree.factory.engine.impl.DecisionTreeEngine;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @ClassName: DefaultTreeFactory
 * @Description: 规则树工厂。
 *               <p>职责：
 *               <ol>
 *                 <li>容器：通过 Spring 注入 {@code Map<String, ILogicTreeNode>}，
 *                     key 是 bean 名（即规则 code，如 {@code rule_lock}），value 是节点实现。
 *                     该 map 用于在创建引擎时按 ruleKey 取节点。</li>
 *                 <li>载体：内部静态类 {@link TreeActionEntity}/{@link StrategyAwardData}
 *                     作为节点输出与最终决策结果的传输对象。</li>
 *               </ol>
 *               <p>注：当前尚未提供 {@code openLogicTree(ruleTreeVO)} 之类的方法把引擎实例化出来，
 *               后续接入抽奖流程时补一个工厂方法即可，{@link com.charlie.domain.strategy.service.rule.tree.factory.engine.impl.DecisionTreeEngine}
 *               的构造器已支持接收该 map 与 {@link RuleTreeVO}。
 * @Author: Charlie
 * @Date: 2026/7/26 16:54
 * @Version: 1.0
 */
@Service
public class DefaultTreeFactory {

    private final Map<String, ILogicTreeNode> logicTreeNodeGroup;

    public DefaultTreeFactory(Map<String, ILogicTreeNode> logicTreeNodeGroup) {
        this.logicTreeNodeGroup = logicTreeNodeGroup;
    }

    public IDecisionTreeEngine openLogicTree(RuleTreeVO ruleTreeVO) {
        return new DecisionTreeEngine(logicTreeNodeGroup, ruleTreeVO);
    }

    /**
     * 决策树动作实体：单个节点执行后产出的结果，由 {@link com.charlie.domain.strategy.service.rule.tree.ILogicTreeNode#logic} 返回。
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TreeActionEntity {
        /** 节点校验类型：ALLOW 放行 / TAKE_OVER 接管，决定决策树下一步走向 */
        private RuleLogicCheckTypeVO ruleLogicCheckType;
        /** 节点产出的奖品数据；非叶子节点可不填，最终返回值以最后一次填充的为准 */
        private StrategyAwardData strategyAwardData;
    }

    /**
     * 抽奖奖品数据：决策树最终返回给上游的奖品结果。
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StrategyAwardData {
        /** 抽奖奖品ID - 内部流转使用 */
        private Integer awardId;
        /** 抽奖奖品规则 */
        private String awardRuleValue;
    }


}