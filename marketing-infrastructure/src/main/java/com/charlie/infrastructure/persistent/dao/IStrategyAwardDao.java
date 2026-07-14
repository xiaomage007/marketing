package com.charlie.infrastructure.persistent.dao;

import com.charlie.infrastructure.persistent.po.StrategyAward;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author Charlie
 * @description 抽奖策略奖品明细配置 - 概率、规则 DAO
 * @create 2023-12-16 13:24
 */
@Mapper
public interface IStrategyAwardDao {

    List<StrategyAward> queryStrategyAwardList();

    List<StrategyAward> queryStrategyAwardListByStrategyId(Long strategyId);

}
