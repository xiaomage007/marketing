package com.charlie.domain.strategy.service.rule.tree.factory.engine;

import com.charlie.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;

/**
 * @ClassName: IDecisionTreeEngine
 * @Description: 决策树引擎接口。约定一次规则树执行的入口方法，
 * 具体实现见 {@link com.charlie.domain.strategy.service.rule.tree.factory.engine.impl.DecisionTreeEngine}。
 * @Author: Charlie
 * @Date: 2026/7/26 17:32
 * @Version: 1.0
 */
public interface IDecisionTreeEngine {

    /**
     * 沿决策树从根节点迭代执行至叶子节点，返回最终决策产出的奖品数据。
     *
     * @param userId     用户ID
     * @param strategyId 策略ID
     * @param awardId    上游已选出的奖品ID
     * @return 最后一次决策产出的 {@link DefaultTreeFactory.StrategyAwardVO}
     */
    DefaultTreeFactory.StrategyAwardVO process(String userId, Long strategyId, Integer awardId);

}