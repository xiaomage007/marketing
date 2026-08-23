package com.charlie.domain.activity.service.rule.impl;

import com.charlie.domain.activity.model.entity.ActivityCountEntity;
import com.charlie.domain.activity.model.entity.ActivityEntity;
import com.charlie.domain.activity.model.entity.ActivitySkuEntity;
import com.charlie.domain.activity.model.valobj.ActivitySkuStockKeyVO;
import com.charlie.domain.activity.repository.IActivityRepository;
import com.charlie.domain.activity.service.armory.IActivityDispatch;
import com.charlie.domain.activity.service.rule.AbstractActionChain;
import com.charlie.types.enums.ResponseCode;
import com.charlie.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @description: 商品库存规则节点
 * @author: Charlie
 * @date: 2026/8/18 9:07
 */
@Slf4j
@Component("activity_sku_stock_action")
public class ActivitySkuStockActionChain extends AbstractActionChain {

    @Resource
    private IActivityDispatch activityDispatch;
    @Resource
    private IActivityRepository activityRepository;

    @Override
    public boolean action(ActivitySkuEntity activitySkuEntity, ActivityEntity activityEntity, ActivityCountEntity activityCountEntity) {
        log.info("活动责任链-商品库存处理【有效期、状态、库存(sku)】开始。sku:{} activityId:{}", activitySkuEntity.getSku(), activityEntity.getActivityId());
        boolean status = activityDispatch.subtractionActivitySkuStock(activitySkuEntity.getSku(), activityEntity.getEndDateTime());
        if (status) {
            log.info("活动责任链-商品库存处理【有效期、状态、库存(sku)】成功。sku:{} activityId:{}", activitySkuEntity.getSku(), activityEntity.getActivityId());
            activityRepository.activitySkuStockConsumeSendQueue(ActivitySkuStockKeyVO.builder()
                    .sku(activitySkuEntity.getSku())
                    .activityId(activityEntity.getActivityId())
                    .build()
            );
            return true;
        }
        throw new AppException(ResponseCode.ACTIVITY_SKU_STOCK_ERROR.getCode(), ResponseCode.ACTIVITY_SKU_STOCK_ERROR.getInfo());

    }
}
