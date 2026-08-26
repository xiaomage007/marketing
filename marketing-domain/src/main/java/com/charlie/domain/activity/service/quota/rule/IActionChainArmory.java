package com.charlie.domain.activity.service.quota.rule;

/**
 * @description:
 * @author: Charlie
 * @date: 2026/8/18 9:00
 */
public interface IActionChainArmory {

    IActionChain next();

    IActionChain appendNext(IActionChain next);

}
