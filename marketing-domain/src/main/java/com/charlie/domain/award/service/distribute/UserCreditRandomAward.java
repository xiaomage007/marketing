package com.charlie.domain.award.service.distribute;

import com.charlie.domain.award.model.entity.DistributeAwardEntity;
import com.charlie.domain.award.repository.IAwardRepository;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @description: 用户积分奖品，支持 award_config 透传，满足黑名单积分奖励。
 * @author: Charlie
 * @date: 2026/9/15 9:38
 */
@Component("user_credit_random")
public class UserCreditRandomAward implements IDistributeAward {

    @Resource
    private IAwardRepository repository;

    @Override
    public void giveOutPrizes(DistributeAwardEntity distributeAwardEntity) {

    }

}
