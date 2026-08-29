package com.charlie.infrastructure.mq;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RabbitMQ 拓扑声明配置 - 绑定 {@code application*.yml} 中 {@code rabbitmq.topology} 前缀。
 * <p>
 * <b>设计意图:</b> Exchange / Queue / Binding 的拓扑定义全部收敛到配置文件,
 * {@link RabbitMqConfig} 启动时遍历本配置统一构建 {@code Declarables},
 * 新增业务只改 yml、不改 Java 代码。
 * <p>
 * <b>三段独立、按需组合:</b>
 * <ul>
 *   <li><b>只配 exchanges</b>:fanout/direct 广播场景,生产者只管发,消费方自行建队列绑定</li>
 *   <li><b>只配 queues</b>:纯消费场景,队列由本服务声明、上游已存在(或不需要交换机路由)</li>
 *   <li><b>三段全配</b>:标准「交换机 --[routingKey]--> 队列」路由场景</li>
 * </ul>
 * <p>
 * <b>配置示例(完整三件套):</b>
 * <pre>{@code
 * rabbitmq:
 *   topology:
 *     exchanges:
 *       activity_sku_stock_zero:            # 逻辑 key,bindings 通过它引用
 *         type: direct                       # direct | fanout | topic | headers
 *         name: activity_sku_stock_zero_exchange   # 省略时默认取 key
 *         routing-key: activity_sku_stock_zero     # 默认路由键(发送&绑定共用),fanout 可省略
 *         durable: true
 *         auto-delete: false
 *     queues:
 *       activity_sku_stock_zero:
 *         name: activity_sku_stock_zero      # 省略时默认取 key
 *         durable: true
 *     bindings:
 *       activity_sku_stock_zero:
 *         destination-type: queue            # queue | exchange(exchange 到 exchange 绑定)
 *         source: activity_sku_stock_zero          # exchanges 段的逻辑 key
 *         destination: activity_sku_stock_zero     # queues(或 exchanges)段的逻辑 key
 *         routing-key: activity_sku_stock_zero     # 省略时继承 source 交换机的 routing-key
 * }</pre>
 *
 * @author Charlie
 */
@Data
@ConfigurationProperties(prefix = "rabbitmq.topology")
public class RabbitMqTopologyProperties {

    /** 交换机声明,key 为逻辑名(供 bindings.source / destination-type=exchange 引用)。 */
    private Map<String, ExchangeProperties> exchanges = new LinkedHashMap<>();

    /** 队列声明,key 为逻辑名(供 bindings.destination 引用)。 */
    private Map<String, QueueProperties> queues = new LinkedHashMap<>();

    /** 绑定声明,把队列(或交换机)绑定到交换机。 */
    private Map<String, BindingProperties> bindings = new LinkedHashMap<>();

    /**
     * 交换机配置。
     */
    @Data
    public static class ExchangeProperties {

        /** 交换机类型:direct / fanout / topic / headers。 */
        private String type = "direct";

        /** broker 上的物理名;为空时取所在 Map 的 key。 */
        private String name;

        /**
         * 默认路由键,生产者向该交换机发送消息时使用(direct/topic 与绑定路由键一致,
         * fanout 可省略)。bindings 段的 routing-key 省略时也继承此值,实现单一配置源。
         */
        private String routingKey = "";

        /** 是否持久化(broker 重启后定义不丢失)。 */
        private boolean durable = true;

        /** 是否自动删除(最后一个消费者断开后删除)。 */
        private boolean autoDelete = false;

        /** 附加参数(如 alternate-exchange)。 */
        private Map<String, Object> arguments;
    }

    /**
     * 队列配置。
     */
    @Data
    public static class QueueProperties {

        /** broker 上的物理名;为空时取所在 Map 的 key。 */
        private String name;

        /** 是否持久化(broker 重启后队列及消息不丢失)。 */
        private boolean durable = true;

        /** 是否排他(仅当前连接可用,连接断开自动删除)。 */
        private boolean exclusive = false;

        /** 是否自动删除(最后一个消费者断开后删除)。 */
        private boolean autoDelete = false;

        /** 附加参数(如 x-dead-letter-exchange 死信、x-message-ttl 等)。 */
        private Map<String, Object> arguments;
    }

    /**
     * 绑定配置。
     */
    @Data
    public static class BindingProperties {

        /** 目标类型:queue(默认)/ exchange(exchange 到 exchange 绑定)。 */
        private String destinationType = "queue";

        /** 源交换机的逻辑 key,必须存在于 exchanges 段。 */
        private String source;

        /** 目标队列(或交换机)的逻辑 key,必须存在于 queues(或 exchanges)段。 */
        private String destination;

        /** 路由键;省略时继承 source 交换机的 routing-key(单一配置源),fanout 可不配。 */
        private String routingKey = "";

        /** 附加参数(headers 类型交换机匹配条件等)。 */
        private Map<String, Object> arguments;
    }

}
