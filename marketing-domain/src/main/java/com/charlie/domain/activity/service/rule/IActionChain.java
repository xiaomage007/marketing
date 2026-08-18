package com.charlie.domain.activity.service.rule;

import com.charlie.domain.activity.model.entity.ActivityCountEntity;
import com.charlie.domain.activity.model.entity.ActivityEntity;
import com.charlie.domain.activity.model.entity.ActivitySkuEntity;

/**
 * @description: 下单规则过滤接口
 * @author: Charlie
 * @date: 2026/8/18 8:59
 */
public interface IActionChain extends IActionChainArmory {

    boolean action(ActivitySkuEntity activitySkuEntity, ActivityEntity activityEntity, ActivityCountEntity activityCountEntity);

}
