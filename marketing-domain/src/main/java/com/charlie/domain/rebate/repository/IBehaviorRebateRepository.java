package com.charlie.domain.rebate.repository;

import com.charlie.domain.rebate.model.aggregate.BehaviorRebateAggregate;
import com.charlie.domain.rebate.model.entity.BehaviorRebateOrderEntity;
import com.charlie.domain.rebate.model.valobj.BehaviorTypeVO;
import com.charlie.domain.rebate.model.valobj.DailyBehaviorRebateVO;

import java.util.List;

/**
 * @description: 行为返利服务仓储接口
 * @author: Charlie
 * @date: 2026/9/7 10:49
 */
public interface IBehaviorRebateRepository {

    List<DailyBehaviorRebateVO> queryDailyBehaviorRebateConfig(BehaviorTypeVO behaviorTypeVO);

    void saveUserRebateRecord(String userId, List<BehaviorRebateAggregate> behaviorRebateAggregates);

    List<BehaviorRebateOrderEntity> queryOrderByOutBusinessNo(String userId, String outBusinessNo);
}
