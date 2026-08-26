package com.charlie.domain.activity.model.entity;

import lombok.Data;

/**
 * @description: 参与抽奖活动实体对象
 * @author: Charlie
 * @date: 2026/8/25 7:58
 */
@Data
public class PartakeRaffleActivityEntity {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 活动ID
     */
    private Long activityId;

}
