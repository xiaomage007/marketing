package com.charlie.domain.activity.service.quota.policy.impl;

import com.charlie.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import com.charlie.domain.activity.model.valobj.OrderStateVO;
import com.charlie.domain.activity.repository.IActivityRepository;
import com.charlie.domain.activity.service.quota.policy.ITradePolicy;
import org.springframework.stereotype.Service;

/**
 * @description: 积分兑换，支付类订单
 * @author: Charlie
 * @date: 2026/9/20 14:19
 */
@Service("credit_pay_trade")
public class CreditPayTradePolicy implements ITradePolicy {

    private final IActivityRepository activityRepository;

    public CreditPayTradePolicy(IActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    @Override
    public void trade(CreateQuotaOrderAggregate createQuotaOrderAggregate) {
        createQuotaOrderAggregate.setOrderState(OrderStateVO.wait_pay);
        activityRepository.doSaveCreditPayOrder(createQuotaOrderAggregate);
    }

}
