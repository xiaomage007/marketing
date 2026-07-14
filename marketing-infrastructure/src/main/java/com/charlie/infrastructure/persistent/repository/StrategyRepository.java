package com.charlie.infrastructure.persistent.repository;

import com.charlie.domain.strategy.model.entity.StrategyAwardEntity;
import com.charlie.domain.strategy.repository.IStrategyRepository;
import com.charlie.infrastructure.persistent.dao.IStrategyAwardDao;
import com.charlie.infrastructure.persistent.po.StrategyAward;
import com.charlie.infrastructure.persistent.redis.IRedisService;
import com.charlie.types.common.Constants;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @description: 策略服务仓储实现
 * @author: Charlie
 * @date: 2026/7/13 10:04
 */
@Repository
public class StrategyRepository implements IStrategyRepository {

    @Resource
    private IStrategyAwardDao strategyAwardDao;
    @Resource
    private IRedisService redisService;

    @Override
    public List<StrategyAwardEntity> queryStrategyAwardList(Long strategyId) {
        // 优先从缓存获取
        String cacheKey = Constants.RedisKey.STRATEGY_AWARD_KEY + strategyId;
        List<StrategyAwardEntity> strategyAwardEntities = redisService.getValue(cacheKey);
        if (!CollectionUtils.isEmpty(strategyAwardEntities)) {
            return strategyAwardEntities;
        }
        // 从库中获取数据
        List<StrategyAward> strategyAwards = strategyAwardDao.queryStrategyAwardListByStrategyId(strategyId);
        strategyAwardEntities = new ArrayList<>(strategyAwards.size());
        for (StrategyAward strategyAward : strategyAwards) {
            StrategyAwardEntity strategyAwardEntity = StrategyAwardEntity.builder()
                    .strategyId(strategyAward.getStrategyId())
                    .awardId(strategyAward.getAwardId())
                    .awardCount(strategyAward.getAwardCount())
                    .awardCountSurplus(strategyAward.getAwardCountSurplus())
                    .awardRate(strategyAward.getAwardRate())
                    .build();
            strategyAwardEntities.add(strategyAwardEntity);
        }
        redisService.setValue(cacheKey, strategyAwardEntities);
        return strategyAwardEntities;
    }

    /**
     * 将装配好的「抽奖概率查找表」写入 Redis，供后续抽奖流程 O(1) 命中奖品。
     *
     * 数据结构说明：
     *   - strategyAwardSearchRateTable：Map<随机数索引, 奖品ID>
     *     例如 rateRange=10000 时，Map 含 10000 个元素，索引 0~9999 分布到各奖品（按概率占比分段填充）。
     *   - rateRange：概率总量区间（如 100、10000），抽奖时据此生成 [0, rateRange) 的随机数。
     *
     * Redis 存储拆为两份数据：
     *   1. STRATEGY_RATE_RANGE_KEY + strategyId  —— String 类型，存放 rateRange（如 10000）；
     *   2. STRATEGY_RATE_TABLE_KEY + strategyId  —— Hash 类型，存放完整查找表，field=索引，value=奖品ID。
     *
     * 抽奖时的查询路径（见 getStrategyAwardAssemble / getRateRange）：
     *   生成随机数 r ∈ [0, rateRange)  →  以 r 为 field 直接 HGET 查表  →  拿到奖品ID。
     *
     * 为什么用 Redis Hash 而不是 List：
     *   - Hash 按 field 精准取值，时间复杂度 O(1)，与索引解耦；
     *   - 方便增量更新（putAll 可局部覆盖），不必整表重写。
     *
     * @param strategyId                    策略ID，用于拼接 Redis Key 做隔离
     * @param rateRange                     概率区间总量（如 10000）
     * @param strategyAwardSearchRateTable  装配好的概率查找表（索引 → 奖品ID）
     */
    @Override
    public void storeStrategyAwardSearchRateTable(Long strategyId, Integer rateRange, Map<Integer, Integer> strategyAwardSearchRateTable) {
        // 1. 存储抽奖策略范围值，如10000，用于生成1000以内的随机数
        //    对应 Redis 的 SET 命令，Key 为 STRATEGY_RATE_RANGE_KEY + strategyId，Value 为 rateRange 整数。
        redisService.setValue(Constants.RedisKey.STRATEGY_RATE_RANGE_KEY + strategyId, rateRange);
        // 2. 存储概率查找表
        //    getMap 返回的是 Redisson 的 RMap 代理对象（对应 Redis Hash 结构），并非把数据拉到本地内存；
        //    后续对 cacheRateTable 的 putAll / get 等操作会实时映射到 Redis 服务端，保证多实例一致性。
        Map<Integer, Integer> cacheRateTable = redisService.getMap(Constants.RedisKey.STRATEGY_RATE_TABLE_KEY + strategyId);
        //    putAll 等价于 Redis 的 HMSET 命令，一次性把整张查找表批量写入 Hash：
        //      field = 随机数索引（0 ~ rateRange-1）
        //      value = 命中的奖品ID
        //    写入后即可通过 HGET(key, 随机数) 在 O(1) 时间内拿到抽奖结果。
        cacheRateTable.putAll(strategyAwardSearchRateTable);
    }

    @Override
    public Integer getStrategyAwardAssemble(Long strategyId, Integer rateKey) {
        return redisService.getFromMap(Constants.RedisKey.STRATEGY_RATE_TABLE_KEY + strategyId, rateKey);
    }

    @Override
    public int getRateRange(Long strategyId) {
        return redisService.getValue(Constants.RedisKey.STRATEGY_RATE_RANGE_KEY + strategyId);
    }
}
