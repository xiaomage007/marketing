package com.charlie.api.dto;

import lombok.Data;

/**
 * @description: 用户活动账户请求对象
 * @author: Charlie
 * @date: 2026/9/11 15:10
 */
@Data
public class UserActivityAccountRequestDTO {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 活动ID
     */
    private Long activityId;

}
