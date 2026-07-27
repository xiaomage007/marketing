package com.charlie.domain.strategy.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @ClassName: RuleTreeVO
 * @Description: 规则树对象【注意；不具有唯一ID，不需要改变数据库结果的对象，可以被定义为值对象】
 * @Author: Charlie
 * @Date: 2026/7/26 15:24
 * @Version: 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RuleTreeVO {

    /**
     * 规则树ID
     */
    private Integer treeId;
    /**
     * 规则树名称
     */
    private String treeName;
    /**
     * 规则树描述
     */
    private String treeDesc;
    /**
     * 规则根节点
     */
    private String treeRootRuleNode;

    /**
     * 规则节点
     */
    private Map<String, RuleTreeNodeVO> treeNodeMap;

}