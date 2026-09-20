package com.charlie.domain.activity.service.quota.policy.impl;

import com.charlie.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import com.charlie.domain.activity.repository.IActivityRepository;
import com.charlie.domain.activity.service.quota.policy.ITradePolicy;
import org.springframework.stereotype.Service;

/**
 * @description: 返利无支付交易订单，直接充值到账
 * @author: Charlie
 * @date: 2026/9/20 8:46
 */
@Service("rebate_no_pay_trade")
public class RebateNoPayTradePolicy implements ITradePolicy {

    private final IActivityRepository activityRepository;

    public RebateNoPayTradePolicy(IActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    @Override
    public void trade(CreateQuotaOrderAggregate createQuotaOrderAggregate) {
        activityRepository.
    }

}
