package com.charlie.infrastructure.persistent.po;

import lombok.Data;

import java.util.Date;

/**
 * @description: 规则树
 * @author: Charlie
 * @date: 2026/7/27 16:20
 */
@Data
public class RuleTree {
    /**
     * 自增ID
     */
    private Long id;
    /**
     * 规则树ID
     */
    private String treeId;
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
    private String treeRootRuleKey;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;
}
