package com.charlie.api.dto;

import lombok.Data;

/**
 * @ClassName: RaffleStrategyRuleWeightRequestDTO
 * @Description: 抽奖策略规则，权重配置，查询N次抽奖可解锁奖品范围，请求对象
 * @Author: Charlie
 * @Date: 2026/9/13 16:34
 * @Version: 1.0
 */
@Data
public class RaffleStrategyRuleWeightRequestDTO {

    // 用户ID
    private String userId;
    // 抽奖活动ID
    private Long activityId;

}