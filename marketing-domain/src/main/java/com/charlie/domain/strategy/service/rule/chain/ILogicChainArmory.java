package com.charlie.domain.strategy.service.rule.chain;

/**
 * @description: 责任链装配
 * @author: Charlie
 * @date: 2026/7/23 14:16
 */
public interface ILogicChainArmory {

    ILogicChain next();

    ILogicChain appendNext(ILogicChain next);
}
