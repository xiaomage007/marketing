package com.charlie.infrastructure.persistent.dao;

import com.charlie.infrastructure.persistent.po.RaffleActivity;
import org.apache.ibatis.annotations.Mapper;

/**
 * @description: 抽奖活动表Dao
 * @author: Charlie
 * @date: 2026/8/13 10:04
 */
@Mapper
public interface IRaffleActivityDao {

    RaffleActivity queryRaffleActivityByActivityId(Long activityId);

}
