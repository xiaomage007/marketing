package com.charlie.domain.activity.repository;

import com.charlie.domain.activity.model.aggregate.CreateOrderAggregate;
import com.charlie.domain.activity.model.entity.ActivityCountEntity;
import com.charlie.domain.activity.model.entity.ActivityEntity;
import com.charlie.domain.activity.model.entity.ActivitySkuEntity;

import java.util.Date;

/**
 * @description: 活动仓储接口
 * @author: Charlie
 * @date: 2026/8/15 16:47
 */
public interface IActivityRepository {

    ActivitySkuEntity queryActivitySku(Long sku);

    ActivityEntity queryRaffleActivityByActivityId(Long activityId);

    ActivityCountEntity queryRaffleActivityCountByActivityCountId(Long activityCountId);

    void doSaveOrder(CreateOrderAggregate createOrderAggregate);

    void cacheActivitySkuStockCount(String cacheKey, Integer stockCount);

    boolean subtractionActivitySkuStock(Long sku, String cacheKey, Date endDateTime);
}
