package com.charlie.domain.activity.service.quota;

import com.charlie.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import com.charlie.domain.activity.model.entity.*;
import com.charlie.domain.activity.model.valobj.ActivitySkuStockKeyVO;
import com.charlie.domain.activity.model.valobj.OrderStateVO;
import com.charlie.domain.activity.repository.IActivityRepository;
import com.charlie.domain.activity.service.IRaffleActivitySkuStockService;
import com.charlie.domain.activity.service.quota.rule.factory.DefaultActivityChainFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * @description: 抽奖活动服务
 * @author: Charlie
 * @date: 2026/8/15 16:50
 */
@Service
public class RaffleActivityAccountQuotaService extends AbstractRaffleActivityAccountQuota implements IRaffleActivitySkuStockService {

    public RaffleActivityAccountQuotaService(IActivityRepository activityRepository, DefaultActivityChainFactory actionChainFactory) {
        super(activityRepository, actionChainFactory);
    }

    @Override
    protected CreateQuotaOrderAggregate buildOrderAggregate(SkuRechargeEntity skuRechargeEntity, ActivitySkuEntity activitySkuEntity, ActivityEntity activityEntity, ActivityCountEntity activityCountEntity) {
        // 订单实体对象
        ActivityOrderEntity activityOrderEntity = new ActivityOrderEntity();
        activityOrderEntity.setUserId(skuRechargeEntity.getUserId());
        activityOrderEntity.setSku(skuRechargeEntity.getSku());
        activityOrderEntity.setActivityId(activityEntity.getActivityId());
        activityOrderEntity.setActivityName(activityEntity.getActivityName());
        activityOrderEntity.setStrategyId(activityEntity.getStrategyId());
        // 公司里一般会有专门的雪花算法UUID服务，我们这里直接生成个12位就可以了。
        activityOrderEntity.setOrderId(RandomStringUtils.randomNumeric(12));
        activityOrderEntity.setOrderTime(new Date());
        activityOrderEntity.setTotalCount(activityCountEntity.getTotalCount());
        activityOrderEntity.setDayCount(activityCountEntity.getDayCount());
        activityOrderEntity.setMonthCount(activityCountEntity.getMonthCount());
        activityOrderEntity.setState(OrderStateVO.completed);
        activityOrderEntity.setOutBusinessNo(skuRechargeEntity.getOutBusinessNo());

        // 构建聚合对象
        return CreateQuotaOrderAggregate.builder()
                .userId(skuRechargeEntity.getUserId())
                .activityId(activitySkuEntity.getActivityId())
                .totalCount(activityCountEntity.getTotalCount())
                .dayCount(activityCountEntity.getDayCount())
                .monthCount(activityCountEntity.getMonthCount())
                .activityOrderEntity(activityOrderEntity)
                .build();
    }

    @Override
    protected void doSaveOrder(CreateQuotaOrderAggregate createOrderAggregate) {
        activityRepository.doSaveOrder(createOrderAggregate);
    }

    @Override
    public ActivitySkuStockKeyVO takeQueueValue() {
        return activityRepository.takeQueueValue();
    }

    @Override
    public void updateActivitySkuStock(Long sku) {
        activityRepository.updateActivitySkuStock(sku);
    }

    @Override
    public void clearActivitySkuStock(Long sku) {
        activityRepository.clearActivitySkuStock(sku);
    }

    @Override
    public void clearQueueValue() {
        activityRepository.clearQueueValue();
    }

    @Override
    public Integer queryRaffleActivityAccountDayPartakeCount(Long activityId, String userId) {
        return activityRepository.queryRaffleActivityAccountDayPartakeCount(activityId, userId);
    }

}
