package com.charlie.domain.strategy.service.armory;

import com.charlie.domain.strategy.model.entity.StrategyAwardEntity;
import com.charlie.domain.strategy.model.entity.StrategyEntity;
import com.charlie.domain.strategy.model.entity.StrategyRuleEntity;
import com.charlie.domain.strategy.repository.IStrategyRepository;
import com.charlie.types.enums.ResponseCode;
import com.charlie.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.*;

/**
 * @description: 策略装配库，负责初始化策略计算
 * @author: Charlie
 * @date: 2026/7/13 8:24
 */
@Slf4j
@Service
public class StrategyArmoryDispatch implements IStrategyArmory, IStrategyDispatch {

    @Resource
    private IStrategyRepository repository;

    @Override
    public boolean assembleLotteryStrategy(Long strategyId) {
        // 1. 查询策略配置
        // 详细：通过仓储查询当前策略下的全部奖品配置（含奖品ID、概率、库存等），仓储内部已带 Redis 缓存
        List<StrategyAwardEntity> strategyAwardEntities = repository.queryStrategyAwardList(strategyId);
        assembleLotteryStrategy(String.valueOf(strategyId), strategyAwardEntities);

        // 2. 权重策略配置 - 适用于 rule_weight 权重规则配置
        StrategyEntity strategyEntity = repository.queryStrategyEntityByStrategyId(strategyId);
        String ruleWeight = strategyEntity.getRuleWeight();
        if (null == ruleWeight) {
            return true;
        }

        StrategyRuleEntity strategyRuleEntity = repository.queryStrategyRule(strategyId, ruleWeight);
        if (null == strategyRuleEntity) {
            throw new AppException(ResponseCode.STRATEGY_RULE_WEIGHT_IS_NULL.getCode(), ResponseCode.STRATEGY_RULE_WEIGHT_IS_NULL.getInfo());
        }
        Map<String, List<Integer>> ruleWeightValueMap = strategyRuleEntity.getRuleWeightValues();
        Set<String> keys = ruleWeightValueMap.keySet();
        for (String key : keys) {
            List<Integer> ruleWeightValues = ruleWeightValueMap.get(key);
            ArrayList<StrategyAwardEntity> strategyAwardEntitiesClone = new ArrayList<>(strategyAwardEntities);
            strategyAwardEntitiesClone.removeIf(entity -> !ruleWeightValues.contains(entity.getAwardId()));
            assembleLotteryStrategy(String.valueOf(strategyId).concat("_").concat(key), strategyAwardEntitiesClone);
        }

        // 装配成功返回 true
        return true;
    }

    private void assembleLotteryStrategy(String key, List<StrategyAwardEntity> strategyAwardEntities) {
        // 1. 获取最小概率值
        // 详细：找出所有奖品中的最小概率值，作为切分概率区间的「最小刻度」
        //      stream 取出每个 awardRate -> min 比较 -> 若列表为空兜底返回 0
        BigDecimal minAwardRate = strategyAwardEntities.stream()
                .map(StrategyAwardEntity::getAwardRate)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // 2. 获取概率值总和
        // 详细：累加全部奖品概率值得到总和（理想等于 1，实际可能略有偏差），用于推算查找表总容量
        //      reduce 以 0 为初值，逐个 BigDecimal 相加
        BigDecimal totalAwardRate = strategyAwardEntities.stream()
                .map(StrategyAwardEntity::getAwardRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. 用 1 % 0.0001 获得概率范围，百分位、千分位、万分位
        // 详细：用「概率总和 ÷ 最小概率」算出查找表总容量 rateRange
        //      例如总和=1、最小=0.0001 -> rateRange=10000（万分位精度）
        //      保留 0 位小数 + CEILING 向上取整，保证覆盖全部概率不丢份额
        BigDecimal rateRange = totalAwardRate.divide(minAwardRate, 0, RoundingMode.CEILING);

        // 4. 生成策略奖品概率查找表「这里指需要在list集合中，存放上对应的奖品占位即可，占位越多等于概率越高」
        // 详细：初始化查找表 List，奖品ID 作为占位元素，占位次数 = 概率占比 × 总容量，概率越高占位越多
        //      预分配容量 rateRange.intValue()，避免扩容拷贝
        List<Integer> strategyAwardSearchRateTables = new ArrayList<>(rateRange.intValue());
        // 遍历每个奖品，按其概率占比把奖品ID 重复填入查找表
        for (StrategyAwardEntity strategyAward : strategyAwardEntities) {
            // 取出当前奖品ID，作为查找表中的占位值
            Integer awardId = strategyAward.getAwardId();
            // 取出当前奖品的概率值
            BigDecimal awardRate = strategyAward.getAwardRate();
            // 计算出每个概率值需要存放到查找表的数量，循环填充
            // 详细：占位数 = rateRange × awardRate，setScale(0, CEILING) 向上取整转 int
            //      内层循环：把 awardId 重复 add 这么多次到查找表中
            for (int i = 0; i < rateRange.multiply(awardRate).setScale(0, RoundingMode.CEILING).intValue(); i++) {
                strategyAwardSearchRateTables.add(awardId);
            }
        }

        // 5. 对存储的奖品进行乱序操作
        // 详细：Collections.shuffle 随机乱序，打破「相同奖品ID 连续排布」的顺序
        //      避免相邻随机数总命中同一奖品，提升分布的随机性
        Collections.shuffle(strategyAwardSearchRateTables);

        // 6. 生成出Map集合，key值，对应的就是后续的概率值。通过概率来获得对应的奖品ID
        // 详细：把 List 转为 LinkedHashMap：key=索引(0 ~ size-1)，value=该位置对应的奖品ID
        //      使用 LinkedHashMap 保持乱序后的写入顺序，最终存入 Redis Hash 供抽奖时按随机数索引取值
        Map<Integer, Integer> shuffleStrategyAwardSearchRateTable = new LinkedHashMap<>();
        for (int i = 0; i < strategyAwardSearchRateTables.size(); i++) {
            // 把索引 i 与对应位置的奖品ID 一一映射写入 Map
            shuffleStrategyAwardSearchRateTable.put(i, strategyAwardSearchRateTables.get(i));
        }

        // 7. 存放到 Redis
        // 详细：调用仓储把「概率区间总量」和「查找表 Map」写入 Redis，供后续 getRandomAwardId 抽奖时读取
        //      第二个参数传 Map.size()，即查找表实际长度（等价于 rateRange 对应容量）
        repository.storeStrategyAwardSearchRateTable(key, shuffleStrategyAwardSearchRateTable.size(), shuffleStrategyAwardSearchRateTable);

    }

    @Override
    public Integer getRandomAwardId(Long strategyId) {
        // 分布式部署下，不一定为当前应用做的策略装配。也就是值不一定会保存到本应用，而是分布式应用，所以需要从 Redis 中获取。
        // 详细：策略装配结果统一写入 Redis 共享，因此抽奖时必须从 Redis 读取该策略的概率区间总量 rateRange（如 10000），用于限定随机数生成范围
        int rateRange = repository.getRateRange(strategyId);
        // 通过生成的随机值，获取概率值奖品查找表的结果
        // 详细：用 SecureRandom 生成 [0, rateRange) 区间内的强随机数作为查找表索引；
        //      再通过仓储从 Redis Hash 中以该随机数为 field 取出对应奖品ID，O(1) 完成抽奖命中
        return repository.getStrategyAwardAssemble(String.valueOf(strategyId), new SecureRandom().nextInt(rateRange));
    }

    @Override
    public Integer getRandomAwardId(Long strategyId, String ruleWeightValue) {
        String key = String.valueOf(strategyId).concat("_").concat(ruleWeightValue);
        // 分布式部署下，不一定为当前应用做的策略装配。也就是值不一定会保存到本应用，而是分布式应用，所以需要从 Redis 中获取。
        int rateRange = repository.getRateRange(key);
        // 通过生成的随机值，获取概率值奖品查找表的结果
        return repository.getStrategyAwardAssemble(key, new SecureRandom().nextInt(rateRange));
    }
}
