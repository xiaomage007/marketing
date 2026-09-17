package com.charlie.domain.credit.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @description: 积分账户实体
 * @author: Charlie
 * @date: 2026/9/17 8:32
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreditAccountEntity {

    /**
     * 用户ID
     */
    private String userId;
    /**
     * 可用积分，每次扣减的值
     */
    private BigDecimal adjustAmount;

}
