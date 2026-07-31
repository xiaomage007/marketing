package com.charlie.domain.strategy.service.raffle;

import com.charlie.domain.strategy.model.valobj.RuleTreeVO;
import com.charlie.domain.strategy.model.valobj.StrategyAwardRuleModelVO;
import com.charlie.domain.strategy.model.valobj.StrategyAwardStockKeyVO;
import com.charlie.domain.strategy.repository.IStrategyRepository;
import com.charlie.domain.strategy.service.AbstractRaffleStrategy;
import com.charlie.domain.strategy.service.armory.IStrategyDispatch;
import com.charlie.domain.strategy.service.rule.chain.ILogicChain;
import com.charlie.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import com.charlie.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import com.charlie.domain.strategy.service.rule.tree.factory.engine.IDecisionTreeEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @description: 默认的抽奖策略实现
 * @author: Charlie
 * @date: 2026/7/20 8:17
 */
@Slf4j
@Service
public class DefaultRaffleStrategy extends AbstractRaffleStrategy {

    public DefaultRaffleStrategy(DefaultChainFactory defaultChainFactory, DefaultTreeFactory defaultTreeFactory, IStrategyDispatch strategyDispatch, IStrategyRepository repository) {
        super(defaultChainFactory, defaultTreeFactory, strategyDispatch, repository);
    }

    @Override
    public DefaultChainFactory.StrategyAwardVO raffleLogicChain(String userId, Long strategyId) {
        ILogicChain logicChain = defaultChainFactory.openLogicChain(strategyId);
        return logicChain.logic(userId, strategyId);
    }

    @Override
    public DefaultTreeFactory.StrategyAwardVO raffleLogicTree(String userId, Long strategyId, Integer awardId) {
        StrategyAwardRuleModelVO strategyAwardRuleModelVO = repository.queryStrategyAwardRuleModelVO(strategyId, awardId);
        if (null == strategyAwardRuleModelVO) {
            return DefaultTreeFactory.StrategyAwardVO.builder().awardId(awardId).build();
        }
        RuleTreeVO ruleTreeVO = repository.queryRuleTreeVOByTreeId(strategyAwardRuleModelVO.getRuleModels());
        if (null == ruleTreeVO) {
            throw new RuntimeException("存在抽奖策略配置的规则模型 Key，未在库表 rule_tree、rule_tree_node、rule_tree_line 配置对应的规则树信息 " + strategyAwardRuleModelVO.getRuleModels());
        }
        IDecisionTreeEngine engine = defaultTreeFactory.openLogicTree(ruleTreeVO);
        return engine.process(userId, strategyId, awardId);
    }

    @Override
    public StrategyAwardStockKeyVO takeQueueValue() throws InterruptedException {
        return repository.takeQueueValue();    }

    @Override
    public void updateStrategyAwardStock(Long strategyId, Integer awardId) {
        repository.updateStrategyAwardStock(strategyId, awardId);
    }
}
