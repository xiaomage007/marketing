package com.charlie.domain.strategy.service;

import com.charlie.domain.strategy.model.entity.StrategyAwardEntity;

import java.util.List;

/**
 * @description: 策略奖品接口
 * @author: Charlie
 * @date: 2026/8/5 7:33
 */
public interface IRaffleAward {

    /**
     * 根据策略ID查询抽奖奖品列表配置
     *
     * @param strategyId 策略ID
     * @return 奖品列表
     */
    List<StrategyAwardEntity> queryRaffleStrategyAwardList(Long strategyId);

    /**
     * 根据策略ID查询抽奖奖品列表配置
     *
     * @param activityId 策略ID
     * @return 奖品列表
     */
    List<StrategyAwardEntity> queryRaffleStrategyAwardListByActivityId(Long activityId);
}
