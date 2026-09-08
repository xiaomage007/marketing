package com.charlie.domain.rebate.event;

import com.charlie.types.event.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @description: 发送返利消息事件
 * @author: Charlie
 * @date: 2026/9/7 10:28
 */
@Component
public class SendRebateMessageEvent extends BaseEvent<SendRebateMessageEvent.RebateMessage> {

    /**
     * 目标队列名,对应 rabbitmq.topology.queues.send_rebate.name。
     */
    @Value("${rabbitmq.topology.queues.send_rebate.name}")
    private String queue;

    @Override
    public EventMessage<RebateMessage> buildEventMessage(RebateMessage data) {
        return EventMessage.<SendRebateMessageEvent.RebateMessage>builder()
                .id(RandomStringUtils.randomNumeric(11))
                .timestamp(new Date())
                .data(data)
                .build();
    }

    @Override
    public String exchange() {
        return "";
    }

    @Override
    public String routingKey() {
        return queue;
    }

    @Override
    public String queue() {
        return queue;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RebateMessage{
        /**
         * 用户ID
         */
        private String userId;
        /**
         * 返利描述
         */
        private String rebateDesc;
        /**
         * 返利类型（sku 活动库存充值商品、integral 用户活动积分）
         */
        private String rebateType;
        /**
         * 返利配置【sku值，积分值】
         */
        private String rebateConfig;
        /**
         * 业务ID - 拼接的唯一值
         */
        private String bizId;
    }

}
