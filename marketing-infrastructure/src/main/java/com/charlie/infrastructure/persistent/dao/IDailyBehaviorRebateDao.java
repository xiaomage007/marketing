package com.charlie.infrastructure.persistent.dao;

import com.charlie.infrastructure.persistent.po.DailyBehaviorRebate;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @description: 日常行为返利活动配置
 * @author: Charlie
 * @date: 2026/9/7 9:43
 */
@Mapper
public interface IDailyBehaviorRebateDao {

    List<DailyBehaviorRebate> queryDailyBehaviorRebateByBehaviorType(String behaviorType);

}
