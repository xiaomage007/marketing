package com.charlie.domain.award.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @description: 用户积分奖品实体对象
 * @author: Charlie
 * @date: 2026/9/15 15:34
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserCreditAwardEntity {

    /**
     * 用户ID
     */
    private String userId;
    /**
     * 积分值
     */
    private BigDecimal creditAmount;

}
