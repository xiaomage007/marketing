package com.charlie.infrastructure.mq;

import com.charlie.domain.activity.event.RabbitMqTopology;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 拓扑声明 - 通过 {@code @Bean} 启动时由 {@code RabbitAdmin} 自动 declare。
 * <p>
 * <b>名称来源:</b> Exchange / Queue / Binding 三件套的名称统一从
 * {@link RabbitMqTopology} 枚举读取,不再用 {@code public static final String} 散落管理。
 * 新增业务:① 追加一个枚举项 → ② 在本类追加对应的 @Bean 三件套,即可完成拓扑声明。
 * <p>
 * <b>运行时序:</b>
 * <ol>
 *   <li>Spring Boot 启动 → {@code AmqpAutoConfiguration} 创建 {@code RabbitAdmin}</li>
 *   <li>{@code RabbitAdmin} 扫描本类所有 {@code Queue / Exchange / Binding} Bean</li>
 *   <li>{@code RabbitAdmin} 与 broker 建立连接后,逐个 declare(若已存在则跳过,
 *       参数不一致时抛 {@code PRECONDITION_FAILED})</li>
 *   <li>本类 Bean 全部 declare 完成后,Spring 开始启动 {@code @RabbitListener} 容器消费</li>
 * </ol>
 *
 * @author Charlie
 */
@Configuration
public class RabbitMqConfig {

    /**
     * 库存清零 DirectExchange。
     * durable=true 表示 broker 重启后交换机定义不丢失;
     */
    @Bean
    public DirectExchange activitySkuStockZeroExchange() {
        return ExchangeBuilder.directExchange(RabbitMqTopology.ACTIVITY_SKU_STOCK_ZERO.getExchange())
                .durable(true)
                .autoDelete()
                .build();
    }

    /**
     * 库存清零队列。durable=true 表示 broker 重启后队列及消息不丢失
     * (需配合 PERSISTENT 投递模式,Spring AMQP 默认就是 PERSISTENT)。
     */
    @Bean
    public Queue activitySkuStockZeroQueue() {
        return QueueBuilder.durable(RabbitMqTopology.ACTIVITY_SKU_STOCK_ZERO.getQueue()).build();
    }

    /**
     * 把 Queue 绑定到 Exchange,routingKey 决定消息路由目标。
     */
    @Bean
    public Binding activitySkuStockZeroBinding(Queue activitySkuStockZeroQueue,
                                               DirectExchange activitySkuStockZeroExchange) {
        return BindingBuilder.bind(activitySkuStockZeroQueue)
                .to(activitySkuStockZeroExchange)
                .with(RabbitMqTopology.ACTIVITY_SKU_STOCK_ZERO.getRoutingKey());
    }

}