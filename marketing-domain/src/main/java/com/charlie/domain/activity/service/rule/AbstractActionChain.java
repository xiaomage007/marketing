package com.charlie.domain.activity.service.rule;

/**
 * @description:
 * @author: Charlie
 * @date: 2026/8/18 9:02
 */
public abstract class AbstractActionChain implements IActionChain {

    private IActionChain next;

    @Override
    public IActionChain next() {
        return next;
    }

    @Override
    public IActionChain appendNext(IActionChain next) {
        this.next = next;
        return next;
    }
}
