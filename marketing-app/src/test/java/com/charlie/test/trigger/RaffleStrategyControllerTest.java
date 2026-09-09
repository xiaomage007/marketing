package com.charlie.test.trigger;

import com.alibaba.fastjson.JSON;
import com.charlie.api.IRaffleStrategyService;
import com.charlie.api.dto.RaffleAwardListRequestDTO;
import com.charlie.api.dto.RaffleAwardListResponseDTO;
import com.charlie.api.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.List;

/**
 * @description: 营销抽奖服务测试
 * @author: Charlie
 * @date: 2026/9/9 9:57
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class RaffleStrategyControllerTest {

    @Resource
    private IRaffleStrategyService raffleStrategyService;

    @Test
    public void test_queryRaffleAwardList() {
        RaffleAwardListRequestDTO request = new RaffleAwardListRequestDTO();
        request.setUserId("Charlie");
        request.setActivityId(100301L);
        Response<List<RaffleAwardListResponseDTO>> response = raffleStrategyService.queryRaffleAwardList(request);

        log.info("请求参数：{}", JSON.toJSONString(request));
        log.info("测试结果：{}", JSON.toJSONString(response));
    }

}
