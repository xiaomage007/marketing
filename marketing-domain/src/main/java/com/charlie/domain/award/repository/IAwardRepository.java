package com.charlie.domain.award.repository;

import com.charlie.domain.award.model.aggregate.UserAwardRecordAggregate;

/**
 * @description: 奖品仓储服务
 * @author: Charlie
 * @date: 2026/8/30 7:26
 */
public interface IAwardRepository {

    /**
     * 保存中奖记录
     * @param userAwardRecordAggregate
     */
    void saveUserAwardRecord(UserAwardRecordAggregate userAwardRecordAggregate);

}
