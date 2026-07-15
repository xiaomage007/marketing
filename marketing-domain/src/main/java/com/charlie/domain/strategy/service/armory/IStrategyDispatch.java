package com.charlie.domain.strategy.service.armory;

/**
 * @description: 策略抽奖调度
 * @author: Charlie
 * @date: 2026/7/15 7:51
 */
public interface IStrategyDispatch {

    /**
     * 获取抽奖策略装配的随机结果
     *
     * @param strategyId 策略ID
     * @return 抽奖结果
     */
    Integer getRandomAwardId(Long strategyId);

    Integer getRandomAwardId(Long strategyId, String ruleWeightValue);

}
