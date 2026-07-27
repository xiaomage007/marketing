package com.charlie.infrastructure.persistent.dao;

import com.charlie.infrastructure.persistent.po.RuleTree;
import org.apache.ibatis.annotations.Mapper;

/**
 * @description: 规则树表DAO
 * @author: Charlie
 * @date: 2026/7/27 16:28
 */
@Mapper
public interface IRuleTreeDao {

    RuleTree queryRuleTreeByTreeId(String treeId);

}
