package com.charlie.domain.award.service;

import com.charlie.domain.award.model.entity.DistributeAwardEntity;
import com.charlie.domain.award.model.entity.UserAwardRecordEntity;

/**
 * @description: 奖品服务接口
 * @author: Charlie
 * @date: 2026/8/30 7:23
 */
public interface IAwardService {

    void saveUserAwardRecord(UserAwardRecordEntity userAwardRecordEntity);

    /**
     * 配送发货奖品
     */
    void distributeAward(DistributeAwardEntity distributeAwardEntity);

}
