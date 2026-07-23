package com.charlie.domain.strategy.service.rule.chain.factory;

import com.charlie.domain.strategy.model.entity.StrategyEntity;
import com.charlie.domain.strategy.repository.IStrategyRepository;
import com.charlie.domain.strategy.service.rule.chain.ILogicChain;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @description: 工厂
 * @author: Charlie
 * @date: 2026/7/23 14:37
 */
@Service
public class DefaultChainFactory {

    private final Map<String, ILogicChain> logicChainGroup;
    protected IStrategyRepository repository;

    public DefaultChainFactory(Map<String, ILogicChain> logicChainGroup, IStrategyRepository repository) {
        this.logicChainGroup = logicChainGroup;
        this.repository = repository;
    }

    /**
     * 通过策略ID，构建责任链
     * @param strategyId 策略ID
     * @return ILogicChain
     */
    public ILogicChain openLogicChain(Long strategyId) {
        // 1. 通过仓储查询策略实体；StrategyEntity 内部将 strategy 表的 rule_models 字段(逗号分隔的规则 code)拆成数组
        StrategyEntity strategy = repository.queryStrategyEntityByStrategyId(strategyId);
        String[] ruleModels = strategy.ruleModels();

        // 2. 兜底:策略未配置任何前置规则时,直接返回 default 链,保证调用方一定能拿到一条可用的责任链
        //    logicChainGroup 是 Spring 按 BeanName 注入的 Map,key 为 @Component("xxx") 的名称(如 "default"/"rule_blacklist"/"rule_weight")
        if (null == ruleModels || 0 == ruleModels.length) return logicChainGroup.get("default");

        // 3. 取第一个规则作为链头单独保存,因为后续 current 指针会不断后移,最终返回的必须是链头
        ILogicChain logicChain = logicChainGroup.get(ruleModels[0]);
        // 4. current 指针初始指向链头,用于向后追加节点
        ILogicChain current = logicChain;
        // 5. 从第二个规则开始遍历,逐个拼接到链尾
        //    appendNext(next) 会把 next 设为当前节点的 next,并返回 next 本身,因此 current = current.appendNext(next) 让指针后移一位
        for (int i = 1; i < ruleModels.length; i++) {
            ILogicChain nextChain = logicChainGroup.get(ruleModels[i]);
            current = current.appendNext(nextChain);
        }

        // 6. 末尾追加 default 链作为兜底:前置规则全部放行(TAKE_OVER 之外)时,最终落到默认抽奖
        //    不需要再接返回值,因为其后不再有节点
        current.appendNext(logicChainGroup.get("default"));

        // 7. 返回链头,调用方从头节点开始调 logic() 即可沿责任链向后传递
        return logicChain;

    }
}
