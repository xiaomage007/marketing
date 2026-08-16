package com.charlie.domain.activity.service;

import com.charlie.domain.activity.repository.IActivityRepository;
import org.springframework.stereotype.Service;

/**
 * @description: 抽奖活动服务
 * @author: Charlie
 * @date: 2026/8/15 16:50
 */
@Service
public class RaffleActivityService extends AbstractRaffleActivity{

    public RaffleActivityService(IActivityRepository activityRepository) {
        super(activityRepository);
    }

}
