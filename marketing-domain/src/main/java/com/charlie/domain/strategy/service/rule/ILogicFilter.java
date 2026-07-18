package com.charlie.domain.strategy.service.rule;

import com.charlie.domain.strategy.model.entity.RuleActionEntity;
import com.charlie.domain.strategy.model.entity.RuleMatterEntity;

/**
 * @description: 抽奖规则过滤接口
 * @author: Charlie
 * @date: 2026/7/18 9:35
 */
public interface ILogicFilter<T extends RuleActionEntity.RaffleEntity> {

    RuleActionEntity<T> filter(RuleMatterEntity ruleMatterEntity);

}
