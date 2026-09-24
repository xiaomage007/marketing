package com.charlie.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @description: 抽奖请求参数
 * @author: Charlie
 * @date: 2026/8/4 12:23
 */
@Data
public class RaffleStrategyRequestDTO implements Serializable {

    // 抽奖策略ID
    private Long strategyId;

}
