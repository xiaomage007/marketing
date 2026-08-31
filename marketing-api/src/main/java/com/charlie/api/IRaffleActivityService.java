package com.charlie.api;

import com.charlie.api.dto.ActivityDrawRequestDTO;
import com.charlie.api.dto.ActivityDrawResponseDTO;
import com.charlie.api.response.Response;

/**
 * @description: 抽奖活动服务
 * @author: Charlie
 * @date: 2026/8/31 10:21
 */
public interface IRaffleActivityService {

    /**
     * 活动装配，数据预热缓存
     * @param activityId 活动ID
     * @return 装配结果
     */
    Response<Boolean> armory(Long activityId);

    /**
     * 活动抽奖接口
     * @param request 请求对象
     * @return 返回结果
     */
    Response<ActivityDrawResponseDTO> draw(ActivityDrawRequestDTO request);

}
