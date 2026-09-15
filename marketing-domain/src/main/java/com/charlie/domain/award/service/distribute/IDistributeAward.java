package com.charlie.domain.award.service.distribute;

import com.charlie.domain.award.model.entity.DistributeAwardEntity;

/**
 * @description: 分发奖品接口
 * @author: Charlie
 * @date: 2026/9/15 9:35
 */
public interface IDistributeAward {

    void giveOutPrizes(DistributeAwardEntity distributeAwardEntity);

}
