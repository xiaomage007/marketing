package com.charlie.domain.activity.service;

import com.charlie.domain.activity.model.entity.ActivityOrderEntity;
import com.charlie.domain.activity.model.entity.ActivityShopCartEntity;

/**
 * @description: 抽奖活动订单接口
 * @author: Charlie
 * @date: 2026/8/15 16:47
 */
public interface IRaffleOrder {

    /**
     * 以sku创建抽奖活动订单，获得参与抽奖资格（可消耗的次数）
     *
     * @param activityShopCartEntity 活动sku实体，通过sku领取活动。
     * @return 活动参与记录实体
     */
    ActivityOrderEntity createRaffleActivityOrder(ActivityShopCartEntity activityShopCartEntity);

}
