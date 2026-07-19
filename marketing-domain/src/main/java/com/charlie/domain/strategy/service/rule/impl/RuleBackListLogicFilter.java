package com.charlie.domain.strategy.service.rule.impl;

import com.charlie.domain.strategy.model.entity.RuleActionEntity;
import com.charlie.domain.strategy.model.entity.RuleMatterEntity;
import com.charlie.domain.strategy.repository.IStrategyRepository;
import com.charlie.domain.strategy.service.rule.ILogicFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @ClassName: RuleBackListLogicFilter
 * @Description:
 * @Author: Charlie
 * @Date: 2026/7/19 10:28
 * @Version: 1.0
 */
@Component
@Slf4j
public class RuleBackListLogicFilter  implements ILogicFilter<RuleActionEntity.RaffleBeforeEntity> {

    @Resource
    private IStrategyRepository repository;

    @Override
    public RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> filter(RuleMatterEntity ruleMatterEntity) {

        return null;
    }

}