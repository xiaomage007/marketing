package com.charlie.domain.rebate.service;

import com.charlie.domain.rebate.model.entity.BehaviorEntity;

import java.util.List;

/**
 * @description: 行为返利服务接口
 * @author: Charlie
 * @date: 2026/9/7 9:46
 */
public interface IBehaviorRebateService {

    /**
     * 创建行为动作的入账订单
     *
     * @param behaviorEntity 行为实体对象
     * @return 订单id
     */
    List<String> createOrder(BehaviorEntity behaviorEntity);

}
