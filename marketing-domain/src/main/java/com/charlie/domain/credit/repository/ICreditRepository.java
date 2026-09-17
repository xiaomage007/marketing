package com.charlie.domain.credit.repository;

import com.charlie.domain.credit.model.aggregate.TradeAggregate;

/**
 * @description: 用户积分仓储
 * @author: Charlie
 * @date: 2026/9/17 9:09
 */
public interface ICreditRepository {

    void saveUserCreditTradeOrder(TradeAggregate tradeAggregate);

}
