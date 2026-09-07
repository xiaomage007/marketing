package com.charlie.api.dto;

import lombok.Data;

/**
 * @description: 抽奖奖品列表，请求对象
 * @author: Charlie
 * @date: 2026/8/4 8:27
 */
@Data
public class RaffleAwardListRequestDTO {

    // 用户ID
    private String userId;
    // 抽奖活动ID
    private Long activityId;

}
