package com.charlie.types.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 基础事件 - 定义 MQ 消息的统一规范。
 * <p>
 * 每个具体事件子类需要提供:
 * <ul>
 *   <li>{@link #buildEventMessage(Object)}:消息体构造(含 id/timestamp/data)</li>
 *   <li>{@link #exchange()}:目标 Exchange 名</li>
 *   <li>{@link #routingKey()}:目标 routingKey</li>
 * </ul>
 *
 * @author Charlie
 */
@Data
public abstract class BaseEvent<T> {

    public abstract EventMessage<T> buildEventMessage(T data);

    /**
     * 目标 Exchange 名,与 RabbitMqConfig(marketing-infrastructure 模块)中声明的
     * {@code @Bean Exchange} 同名,保证生产者与 broker 拓扑一致。
     */
    public abstract String exchange();

    /**
     * 目标 routingKey,在 RabbitMqConfig 中通过
     * {@code BindingBuilder.with(rk)} 与 Queue 绑定。
     */
    public abstract String routingKey();

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EventMessage<T> {
        private String id;
        private Date timestamp;
        private T data;
    }

}