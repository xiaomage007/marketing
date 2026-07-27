package com.charlie.infrastructure.persistent.dao;

import com.charlie.infrastructure.persistent.po.RuleTreeNode;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @description: 规则树节点表DAO
 * @author: Charlie
 * @date: 2026/7/27 16:32
 */
@Mapper
public interface IRuleTreeNodeDao {

    List<RuleTreeNode> queryRuleTreeNodeListByTreeId(String treeId);

}
