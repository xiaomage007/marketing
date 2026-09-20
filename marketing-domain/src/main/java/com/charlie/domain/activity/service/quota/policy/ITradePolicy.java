package com.charlie.domain.activity.service.quota.policy;

import com.charlie.domain.activity.model.aggregate.CreateQuotaOrderAggregate;

/**
 * @description: 交易策略接口，包括；返利兑换（不用支付），积分订单（需要支付）
 * @author: Charlie
 * @date: 2026/9/20 8:37
 */
public interface ITradePolicy {

    void trade(CreateQuotaOrderAggregate createQuotaOrderAggregate);

}
