package com.charlie.api.dto;

import lombok.Data;

/**
 * @description: 活动抽奖请求对象
 * @author: Charlie
 * @date: 2026/8/31 10:12
 */
@Data
public class ActivityDrawRequestDTO {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 活动ID
     */
    private Long activityId;

}
