package com.charlie.domain.award.event;

import com.charlie.types.event.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;

import java.util.Date;

/**
 * @description: 用户奖品记录事件消息
 * @author: Charlie
 * @date: 2026/8/30 7:34
 */
public class SendAwardMessageEvent extends BaseEvent<SendAwardMessageEvent.SendAwardMessage> {

    /**
     * 目标队列名,对应 rabbitmq.topology.queues.send_award.name。
     */
    @Value("${rabbitmq.topology.queues.send_award.name}")
    private String queue;

    @Override
    public EventMessage<SendAwardMessage> buildEventMessage(SendAwardMessage data) {
        return EventMessage.<SendAwardMessage>builder()
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
    public static class SendAwardMessage {
        /**
         * 用户ID
         */
        private String userId;
        /**
         * 奖品ID
         */
        private Integer awardId;
        /**
         * 奖品标题（名称）
         */
        private String awardTitle;
    }

}
