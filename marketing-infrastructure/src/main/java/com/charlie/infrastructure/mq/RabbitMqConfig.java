package com.charlie.infrastructure.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RabbitMQ 拓扑声明 - 配置驱动,启动时由 {@code RabbitAdmin} 自动 declare。
 * <p>
 * <b>拓扑来源:</b> {@code application*.yml} 的 {@code rabbitmq.topology} 配置段,
 * 由 {@link RabbitMqTopologyProperties} 绑定;本类遍历配置统一构建
 * {@link Declarables}(Exchange / Queue / Binding 集合),交由 RabbitAdmin 声明。
 * 新增业务<b>只改 yml、不改 Java 代码</b>。
 * <p>
 * <b>按需组合的三种场景:</b>
 * <ul>
 *   <li><b>只配 exchanges</b>:如 fanout/direct 广播,生产者只管发,消费方自行绑定</li>
 *   <li><b>只配 queues</b>:纯消费场景,消费的队列由本服务声明</li>
 *   <li><b>三段全配 + bindings</b>:标准路由场景</li>
 * </ul>
 * <p>
 * <b>运行时序:</b>
 * <ol>
 *   <li>Spring Boot 启动 -> {@code AmqpAutoConfiguration} 创建 {@code RabbitAdmin}</li>
 *   <li>{@code RabbitAdmin} 扫描容器中的 {@link Declarables} Bean,逐个 declare
 *       (已存在则跳过,参数不一致抛 {@code PRECONDITION_FAILED})</li>
 *   <li>声明完成后,Spring 启动 {@code @RabbitListener} 容器开始消费</li>
 * </ol>
 *
 * @author Charlie
 */
@Configuration
@EnableConfigurationProperties(RabbitMqTopologyProperties.class)
public class RabbitMqConfig {

    /**
     * 把 yml 中三段拓扑配置统一构建为一个 {@link Declarables},
     * RabbitAdmin 会对其中每个 {@link Declarable} 自动 declare。
     */
    @Bean
    public Declarables rabbitTopologyDeclarables(RabbitMqTopologyProperties properties) {
        List<Declarable> declarables = new ArrayList<>();
        Map<String, String> exchangeNames = new HashMap<>();
        Map<String, String> exchangeDefaultRoutingKeys = new HashMap<>();
        Map<String, String> queueNames = new HashMap<>();

        // 1. 交换机:name 省略时默认取配置 key;routing-key 为生产者发送的默认路由键
        properties.getExchanges().forEach((key, conf) -> {
            String name = StringUtils.hasText(conf.getName()) ? conf.getName() : key;
            exchangeNames.put(key, name);
            exchangeDefaultRoutingKeys.put(key, conf.getRoutingKey());
            declarables.add(buildExchange(key, conf, name));
        });

        // 2. 队列:name 省略时默认取配置 key
        properties.getQueues().forEach((key, conf) -> {
            String name = StringUtils.hasText(conf.getName()) ? conf.getName() : key;
            queueNames.put(key, name);
            declarables.add(new Queue(name, conf.isDurable(), conf.isExclusive(), conf.isAutoDelete(), conf.getArguments()));
        });

        // 3. 绑定:source/destination 引用上面的逻辑 key,引用不存在的 key 直接启动失败
        properties.getBindings().forEach((key, conf) ->
                declarables.add(buildBinding(key, conf, exchangeNames, exchangeDefaultRoutingKeys, queueNames)));

        return new Declarables(declarables);
    }

    /**
     * 按配置的 type 构建 Exchange,不支持的 type 启动即失败。
     */
    private Exchange buildExchange(String key, RabbitMqTopologyProperties.ExchangeProperties conf, String name) {
        String type = conf.getType() == null ? "" : conf.getType().toLowerCase();
        switch (type) {
            case "direct":
                return new DirectExchange(name, conf.isDurable(), conf.isAutoDelete(), conf.getArguments());
            case "fanout":
                return new FanoutExchange(name, conf.isDurable(), conf.isAutoDelete(), conf.getArguments());
            case "topic":
                return new TopicExchange(name, conf.isDurable(), conf.isAutoDelete(), conf.getArguments());
            case "headers":
                return new HeadersExchange(name, conf.isDurable(), conf.isAutoDelete(), conf.getArguments());
            default:
                throw new IllegalStateException(String.format("RabbitMQ 拓扑配置错误:exchanges.%s.type=%s 不支持(仅支持 direct/fanout/topic/headers)", key, conf.getType()));
        }
    }

    /**
     * 用通用 Binding 构造(destination + destinationType + source + routingKey + arguments),
     * 统一覆盖 queue 绑定与 exchange 到 exchange 绑定,不依赖具体交换机类型的 BindingBuilder API。
     * routing-key 省略时继承 source 交换机的默认路由键(单一配置源,fanout 场景两者都可为空)。
     */
    private Binding buildBinding(String key, RabbitMqTopologyProperties.BindingProperties conf,
                                 Map<String, String> exchangeNames, Map<String, String> exchangeDefaultRoutingKeys,
                                 Map<String, String> queueNames) {
        if (!StringUtils.hasText(conf.getSource()) || !exchangeNames.containsKey(conf.getSource())) {
            throw new IllegalStateException(String.format("RabbitMQ 拓扑配置错误:bindings.%s.source=%s 在 exchanges 段不存在", key, conf.getSource()));
        }

        boolean toQueue = "queue".equalsIgnoreCase(conf.getDestinationType());
        String destinationName = toQueue ? queueNames.get(conf.getDestination()) : exchangeNames.get(conf.getDestination());
        if (!StringUtils.hasText(conf.getDestination()) || destinationName == null) {
            throw new IllegalStateException(String.format("RabbitMQ 拓扑配置错误:bindings.%s.destination=%s 在 %s 段不存在",
                    key, conf.getDestination(), toQueue ? "queues" : "exchanges"));
        }

        String routingKey = StringUtils.hasText(conf.getRoutingKey())
                ? conf.getRoutingKey()
                : exchangeDefaultRoutingKeys.get(conf.getSource());

        return new Binding(destinationName,
                toQueue ? Binding.DestinationType.QUEUE : Binding.DestinationType.EXCHANGE,
                exchangeNames.get(conf.getSource()),
                routingKey,
                conf.getArguments());
    }

}
