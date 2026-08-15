package com.charlie.domain.activity.service;

import com.charlie.domain.activity.repository.IActivityRepository;

/**
 * @description: 抽奖活动服务
 * @author: Charlie
 * @date: 2026/8/15 16:50
 */
public class RaffleActivityService extends AbstractRaffleActivity{

    public RaffleActivityService(IActivityRepository activityRepository) {
        super(activityRepository);
    }

}
