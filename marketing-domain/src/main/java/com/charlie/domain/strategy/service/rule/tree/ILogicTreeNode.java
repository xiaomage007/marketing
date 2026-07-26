package com.charlie.domain.strategy.service.rule.tree;

import com.charlie.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;

/**
 * @ClassName: ILogicTreeNode
 * @Description:
 * @Author: Charlie
 * @Date: 2026/7/26 16:25
 * @Version: 1.0
 */
public interface ILogicTreeNode {

    DefaultTreeFactory.TreeActionEntity logic(String userId, Long strategyId, Integer awardId);

}