package com.charlie.domain.activity.service;

import com.charlie.domain.activity.model.valobj.ActivitySkuStockKeyVO;

/**
 * @description: 活动sku库存处理接口
 * @author: Charlie
 * @date: 2026/8/23 15:19
 */
public interface IRaffleActivitySkuStockService {

    ActivitySkuStockKeyVO takeQueueValue();

    void updateActivitySkuStock(Long sku);

    void clearActivitySkuStock(Long sku);

    void clearQueueValue();
}
