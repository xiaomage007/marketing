package com.charlie.infrastructure.persistent.dao;

import com.charlie.infrastructure.persistent.po.RaffleActivityCount;
import org.apache.ibatis.annotations.Mapper;

/**
 * @description: 抽奖活动次数配置表Dao
 * @author: Charlie
 * @date: 2026/8/13 10:03
 */
@Mapper
public interface IRaffleActivityCountDao {

    RaffleActivityCount queryRaffleActivityCountByActivityCountId(Long activityCountId);

}
