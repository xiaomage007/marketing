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
 *   <li>{@link #exchange()}:目标 Exchange 名,空串表示 RabbitMQ 默认交换机</li>
 *   <li>{@link #routingKey()}:目标 routingKey,默认交换机下即队列名</li>
 *   <li>{@link #queue()}:目标队列名(仅配置队列的直发场景使用,复杂拓扑可返回空串)</li>
 * </ul>
 * <p>
 * 拓扑名称统一来源于 {@code application*.yml} 的 {@code rabbitmq.topology} 配置段。
 *
 * @author Charlie
 */
@Data
public abstract class BaseEvent<T> {

    public abstract EventMessage<T> buildEventMessage(T data);

    /**
     * 目标 Exchange 名。返回空串表示走 RabbitMQ 默认交换机(broker 内置、无需声明,
     * 隐式绑定所有队列),此时 routingKey 即队列名,只需配置队列、无需 exchanges/bindings。
     */
    public abstract String exchange();

    /**
     * 目标 routingKey。默认交换机下等于队列名;显式交换机场景需与
     * {@code rabbitmq.topology.bindings.*.routing-key} 保持一致才能路由到队列。
     */
    public abstract String routingKey();

    /**
     * 目标队列名。「只配置队列」的直发场景与 routingKey 一致;
     * fanout 广播等复杂拓扑场景本方法可返回空串。
     */
    public abstract String queue();

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