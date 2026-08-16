package com.charlie.test.domain.activity;

import com.alibaba.fastjson.JSON;
import com.charlie.domain.activity.model.entity.ActivityOrderEntity;
import com.charlie.domain.activity.model.entity.ActivityShopCartEntity;
import com.charlie.domain.activity.service.IRaffleOrder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * @description: 抽奖活动订单单测
 * @author: Charlie
 * @date: 2026/8/16 15:29
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class RaffleOrderTest {

    @Resource
    private IRaffleOrder raffleOrder;

    @Test
    public void test_createRaffleActivityOrder(){
        ActivityShopCartEntity activityShopCartEntity = new ActivityShopCartEntity();
        activityShopCartEntity.setUserId("Charlie");
        activityShopCartEntity.setSku(9011L);
        ActivityOrderEntity raffleActivityOrder = raffleOrder.createRaffleActivityOrder(activityShopCartEntity);
        log.info("测试结果:{}", JSON.toJSONString(raffleActivityOrder));
    }

}
