package com.charlie.domain.credit.model.entity;

import com.charlie.domain.credit.model.valobj.TradeNameVO;
import com.charlie.domain.credit.model.valobj.TradeTypeVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @description: 增加额度实体
 * @author: Charlie
 * @date: 2026/9/17 8:34
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradeEntity {

    /**
     * 用户ID
     */
    private String userId;
    /**
     * 交易名称
     */
    private TradeNameVO tradeName;
    /**
     * 交易类型；交易类型；forward-正向、reverse-逆向
     */
    private TradeTypeVO tradeType;
    /**
     * 交易金额
     */
    private BigDecimal amount;
    /**
     * 业务仿重ID - 外部透传。返利、行为等唯一标识
     */
    private String outBusinessNo;

}