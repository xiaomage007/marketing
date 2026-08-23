package com.charlie.infrastructure.mq;

import com.alibaba.fastjson.JSON;
import com.charlie.types.event.BaseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * MQ 消息发布器 - 通过 RabbitTemplate 向指定 Exchange 发送消息。
 * <p>
 * 与 broker 拓扑解耦:调用方传 {@code (exchange, routingKey, eventMessage)},
 * 拓扑定义统一在 {@link RabbitMqConfig} 中管理,本类不硬编码任何队列/交换机名。
 * <p>
 * <b>所在模块:</b> {@code marketing-infrastructure.mq}——与 {@link RabbitMqConfig} 同包,
 * 共同构成「MQ 基础设施层」:一个负责拓扑声明,一个负责消息发送。
 *
 * @author Charlie
 */
@Slf4j
@Component
public class EventPublisher {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 向指定 Exchange 发送事件消息。
     *
     * @param exchange     目标 Exchange 名(由 {@link BaseEvent#exchange()} 提供)
     * @param routingKey   目标 routingKey(由 {@link BaseEvent#routingKey()} 提供)
     * @param eventMessage 事件消息体,会被序列化为 JSON
     */
    public void publish(String exchange, String routingKey, BaseEvent.EventMessage<?> eventMessage) {
        try {
            String messageJson = JSON.toJSONString(eventMessage);
            rabbitTemplate.convertAndSend(exchange, routingKey, messageJson);
            log.info("发送MQ消息 exchange:{} routingKey:{} message:{}", exchange, routingKey, messageJson);
        } catch (Exception e) {
            log.error("发送MQ消息失败 exchange:{} routingKey:{} message:{}", exchange, routingKey, JSON.toJSONString(eventMessage), e);
            throw e;
        }
    }

}