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
 * 与 broker 拓扑解耦:拓扑定义统一在 {@code application*.yml} 的 {@code rabbitmq.topology}
 * 配置段(由 {@link RabbitMqConfig} 读取并声明),本类不硬编码任何队列/交换机名。
 * <p>
 * <b>三个重载对应三种场景:</b>
 * <ul>
 *   <li>{@link #publish(BaseEvent, Object)}:标准场景,事件对象自带 exchange/routingKey,一行直发</li>
 *   <li>{@link #publish(String, String, BaseEvent.EventMessage)}:显式指定 exchange + routingKey,
 *       适合路由键需要运行时动态决定的场景</li>
 *   <li>{@link #publish(String, BaseEvent.EventMessage)}:fanout 广播场景,只需交换机、无需路由键</li>
 * </ul>
 *
 * @author Charlie
 */
@Slf4j
@Component
public class EventPublisher {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 标准事件发送 - exchange/routingKey/消息体全部由事件对象提供。
     * <p>
     * 调用方只需持有 {@link BaseEvent} 子类 Bean,一行完成发送:
     * <pre>{@code
     * eventPublisher.publish(activitySkuStockZeroMessageEvent, sku);
     * }</pre>
     *
     * @param event 业务事件(提供 exchange/routingKey 及消息体构造)
     * @param data  业务数据,经 {@code event.buildEventMessage(data)} 包装为标准消息体
     */
    public <T> void publish(BaseEvent<T> event, T data) {
        publish(event.exchange(), event.routingKey(), event.buildEventMessage(data));
    }

    /**
     * fanout 广播发送 - 只指定交换机,fanout 类型交换机会忽略路由键,消息投递到所有绑定队列。
     *
     * @param exchange      目标 Exchange 名(对应 rabbitmq.topology.exchanges.*.name)
     * @param eventMessage  事件消息体,会被序列化为 JSON
     */
    public void publish(String exchange, BaseEvent.EventMessage<?> eventMessage) {
        publish(exchange, "", eventMessage);
    }

    /**
     * 显式指定目标发送 - exchange 与 routingKey 由调用方运行时决定。
     *
     * @param exchange     目标 Exchange 名
     * @param routingKey   目标 routingKey
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
