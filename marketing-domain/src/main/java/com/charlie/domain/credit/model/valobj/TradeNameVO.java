package com.charlie.domain.credit.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @description: 交易名称枚举值
 * @author: Charlie
 * @date: 2026/9/17 8:31
 */
@Getter
@AllArgsConstructor
public enum TradeNameVO {

    REBATE("行为返利"),
    CONVERT_SKU("兑换抽奖"),

    ;

    private final String name;

}
