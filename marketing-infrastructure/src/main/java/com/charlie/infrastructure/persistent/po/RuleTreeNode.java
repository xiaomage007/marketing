package com.charlie.infrastructure.persistent.po;

import lombok.Data;

import java.util.Date;

/**
 * @description: 规则树节点
 * @author: Charlie
 * @date: 2026/7/27 16:22
 */
@Data
public class RuleTreeNode {
    /** 自增ID */
    private Long id;
    /** 规则树ID */
    private String treeId;
    /** 规则Key */
    private String ruleKey;
    /** 规则描述 */
    private String ruleDesc;
    /** 规则比值 */
    private String ruleValue;
    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;
}
