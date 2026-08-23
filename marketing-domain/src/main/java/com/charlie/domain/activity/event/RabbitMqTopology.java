package com.charlie.domain.activity.event;

import lombok.Getter;

/**
 * RabbitMQ 拓扑元数据枚举 - 集中管理 Exchange / RoutingKey / Queue 名称。
 * <p>
 * <b>设计意图:</b> 把 broker 拓扑名称从 {@code public static final String} 常量
 * 和 {@code yml} 配置项统一收敛到一个枚举里,新增业务只需追加一项枚举值,
 * 无需修改 RabbitMqConfig / application*.yml。
 * <p>
 * <b>所在模块:</b> {@code marketing-domain}。
 * <ul>
 *   <li>事件类({@code ActivitySkuStockZeroMessageEvent})同模块引用,天然可用</li>
 *   <li>{@code marketing-infrastructure.RabbitMqConfig} 反向依赖 domain,引用枚举构造 Bean</li>
 *   <li>{@code marketing-trigger.ActivitySkuStockZeroCustomer} 通过 domain 间接可用,
 *       用 SpEL {@code #{T(...).ACTIVITY_SKU_STOCK_ZERO.queue()}} 引用</li>
 * </ul>
 * <p>
 * <b>命名约定:</b>
 * <ul>
 *   <li>枚举常量名 = 业务事件名的<b>SNAKE_CASE 大写</b>(如 {@code ACTIVITY_SKU_STOCK_ZERO})</li>
 *   <li>exchange 名 = {@code <业务名>_exchange} 后缀</li>
 *   <li>queue 名 = {@code <业务名>} 原名(默认与 routingKey 同名,可通过构造参数覆盖)</li>
 *   <li>routingKey 名 = {@code <业务名>} 原名(简洁约定;业务复杂时可改为 {@code <域>.<事件>.<动作>})</li>
 * </ul>
 * <p>
 * <b>新增业务示例:</b>
 * <pre>{@code
 * ACTIVITY_AWARD_DISTRIBUTE(
 *     "activity_award_distribute_exchange", // exchange
 *     "activity.award.distribute",          // routingKey(可与业务名不同)
 *     "activity_award_distribute_queue"     // queue
 * )
 * }</pre>
 * 之后只要在 {@code RabbitMqConfig} 里追加对应的 {@code @Bean} 三件套,
 * 生产者/消费者引用本枚举的 exchange()/routingKey()/queue() 即可。
 *
 * @author Charlie
 */
@Getter
public enum RabbitMqTopology {

    /**
     * 活动 SKU 库存清零事件拓扑。
     * <pre>
     *   DirectExchange: activity_sku_stock_zero_exchange
     *   RoutingKey    : activity_sku_stock_zero
     *   Queue         : activity_sku_stock_zero
     *   Binding       : exchange --[routingKey]--> queue
     * </pre>
     */
    ACTIVITY_SKU_STOCK_ZERO(
            "activity_sku_stock_zero_exchange",
            "activity_sku_stock_zero",
            "activity_sku_stock_zero"
    );

    /** DirectExchange 名。 */
    private final String exchange;

    /** routingKey,作为 BindingBuilder.with(rk) 的参数。 */
    private final String routingKey;

    /** Queue 名,作为消费者 {@code @RabbitListener(queues=...)} 的引用目标。 */
    private final String queue;

    RabbitMqTopology(String exchange, String routingKey, String queue) {
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.queue = queue;
    }

}