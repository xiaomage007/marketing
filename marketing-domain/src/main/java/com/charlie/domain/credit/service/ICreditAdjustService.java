package com.charlie.domain.credit.service;

import com.charlie.domain.credit.model.entity.CreditAccountEntity;
import com.charlie.domain.credit.model.entity.TradeEntity;

/**
 * @description: 积分调额接口【正逆向，增减积分】
 * @author: Charlie
 * @date: 2026/9/17 9:08
 */
public interface ICreditAdjustService {

    /**
     * 创建增加积分额度订单
     * @param tradeEntity 交易实体对象
     * @return 单号
     */
    String createOrder(TradeEntity tradeEntity);

    /**
     * 查询用户积分账户
     * @param userId 用户ID
     * @return 积分账户实体
     */
    CreditAccountEntity queryUserCreditAccount(String userId);

}
