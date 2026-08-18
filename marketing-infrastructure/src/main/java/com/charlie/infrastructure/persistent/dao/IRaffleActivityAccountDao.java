package com.charlie.infrastructure.persistent.dao;

import com.charlie.infrastructure.persistent.po.RaffleActivityAccount;
import org.apache.ibatis.annotations.Mapper;

/**
 * @description: 抽奖活动账户表
 * @author: Charlie
 * @date: 2026/8/13 10:00
 */
@Mapper
public interface IRaffleActivityAccountDao {

    int updateAccountQuota(RaffleActivityAccount raffleActivityAccount);

    void insert(RaffleActivityAccount raffleActivityAccount);

}
