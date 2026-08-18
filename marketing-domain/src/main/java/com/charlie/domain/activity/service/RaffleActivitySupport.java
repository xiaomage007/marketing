package com.charlie.domain.activity.service;

import com.charlie.domain.activity.model.entity.ActivityCountEntity;
import com.charlie.domain.activity.model.entity.ActivityEntity;
import com.charlie.domain.activity.model.entity.ActivitySkuEntity;
import com.charlie.domain.activity.repository.IActivityRepository;
import com.charlie.domain.activity.service.rule.factory.DefaultActivityChainFactory;

/**
 * @description: 抽奖活动的支撑类
 * @author: Charlie
 * @date: 2026/8/18 8:52
 */
public class RaffleActivitySupport {

    protected IActivityRepository activityRepository;

    protected DefaultActivityChainFactory actionChainFactory;

    public RaffleActivitySupport(IActivityRepository activityRepository, DefaultActivityChainFactory actionChainFactory) {
        this.activityRepository = activityRepository;
        this.actionChainFactory = actionChainFactory;
    }

    public ActivitySkuEntity queryActivitySku(Long sku) {
        return activityRepository.queryActivitySku(sku);
    }

    public ActivityEntity queryRaffleActivityByActivityId(Long activityId) {
        return activityRepository.queryRaffleActivityByActivityId(activityId);
    }

    public ActivityCountEntity queryRaffleActivityCountByActivityCountId(Long activityCountId) {
        return activityRepository.queryRaffleActivityCountByActivityCountId(activityCountId);
    }

}
