package com.charlie.domain.strategy.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @ClassName: RuleTreeNodeVO
 * @Description: 规则树节点对象
 * @Author: Charlie
 * @Date: 2026/7/26 15:18
 * @Version: 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RuleTreeNodeVO {
    /**
     * 规则树ID
     */
    private String treeId;
    /**
     * 规则Key
     */
    private String ruleKey;
    /**
     * 规则描述
     */
    private String ruleDesc;
    /**
     * 规则比值
     */
    private String ruleValue;

    /**
     * 规则连线
     */
    private List<RuleTreeNodeLineVO> treeNodeLineVOList;

}