package com.charlie.domain.credit.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @description: 交易类型枚举值
 * @author: Charlie
 * @date: 2026/9/17 8:32
 */
@Getter
@AllArgsConstructor
public enum TradeTypeVO {

    FORWARD("forward", "正向交易，+ 积分"),
    REVERSE("reverse", "逆向交易，- 积分"),

    ;

    private final String code;
    private final String info;

}
