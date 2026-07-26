package com.charlie.domain.strategy.service.rule.tree.factory.engine;

import com.charlie.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;

/**
 * @ClassName: IDecisionTreeEngine
 * @Description: 规则树组合接口
 * @Author: Charlie
 * @Date: 2026/7/26 17:32
 * @Version: 1.0
 */
public interface IDecisionTreeEngine {

    DefaultTreeFactory.StrategyAwardData process(String userId, Long strategyId, Integer awardId);

}