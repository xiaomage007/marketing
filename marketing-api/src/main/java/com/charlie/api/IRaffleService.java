package com.charlie.api;

import com.charlie.api.dto.RaffleAwardListRequestDTO;
import com.charlie.api.dto.RaffleAwardListResponseDTO;
import com.charlie.api.dto.RaffleRequestDTO;
import com.charlie.api.dto.RaffleResponseDTO;
import com.charlie.api.response.Response;

import java.util.List;

/**
 * @description: 抽奖服务接口
 * @author: Charlie
 * @date: 2026/8/5 7:30
 */
public interface IRaffleService {
    /**
     * 策略装配接口
     *
     * @param strategyId 策略ID
     * @return 装配结果
     */
    Response<Boolean> strategyArmory(Long strategyId);

    /**
     * 查询抽奖奖品列表配置
     *
     * @param requestDTO 抽奖奖品列表查询请求参数
     * @return 奖品列表数据
     */
    Response<List<RaffleAwardListResponseDTO>> queryRaffleAwardList(RaffleAwardListRequestDTO requestDTO);

    /**
     * 随机抽奖接口
     *
     * @param requestDTO 请求参数
     * @return 抽奖结果
     */
    Response<RaffleResponseDTO> randomRaffle(RaffleRequestDTO requestDTO);

}
