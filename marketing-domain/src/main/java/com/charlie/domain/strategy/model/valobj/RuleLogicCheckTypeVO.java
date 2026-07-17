package com.charlie.domain.strategy.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @description: 规则过滤校验类型值对象
 * @author: Charlie
 * @date: 2026/7/16 8:30
 */
@AllArgsConstructor
@Getter
public enum RuleLogicCheckTypeVO {
    // 规则过滤校验类型
    ALLOW("0000", "放行；执行后续的流程，不受规则引擎影响"),
    TAKE_OVER("0001","接管；后续的流程，受规则引擎执行结果影响"),
    ;

    private final String code;
    private final String info;
}
