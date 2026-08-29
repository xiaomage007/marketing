package com.charlie.domain.activity.event;

import com.charlie.types.event.BaseEvent;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 活动 SKU 库存清零事件 - 「只配置队列」的最简直发场景。
 * <p>
 * 不声明显式交换机与绑定,发送走 RabbitMQ <b>默认交换机</b>(exchange 名为空串,
 * broker 内置、无需声明,隐式绑定所有队列):routingKey 即队列名,消息直接投递到同名队列。
 * 因此本事件<b>只依赖队列配置</b> {@code rabbitmq.topology.queues.activity_sku_stock_zero.name},
 * 生产者(exchange=""/routingKey=队列名)与消费者(@RabbitListener 监听同名队列)天然一致。
 * <p>
 * 若后续需要 fanout 广播、topic 路由等复杂拓扑,再在 yml 配置 exchanges/bindings 段并
 * 让 {@code exchange()} 返回显式交换机名即可,本类的消息体构造不受影响。
 *
 * @author Charlie
 */
@Component
public class ActivitySkuStockZeroMessageEvent extends BaseEvent<Long> {

    /** 目标队列名,对应 rabbitmq.topology.queues.activity_sku_stock_zero.name。 */
    @Value("${rabbitmq.topology.queues.activity_sku_stock_zero.name}")
    private String queue;

    @Override
    public EventMessage<Long> buildEventMessage(Long sku) {
        return EventMessage.<Long>builder()
                .id(RandomStringUtils.randomNumeric(11))
                .timestamp(new Date())
                .data(sku)
                .build();
    }

    /**
     * 返回空串表示走 RabbitMQ 默认交换机,无需在 yml 中声明 exchanges 段。
     */
    @Override
    public String exchange() {
        return "";
    }

    /**
     * 默认交换机下 routingKey 即队列名。
     */
    @Override
    public String routingKey() {
        return queue;
    }

    @Override
    public String queue() {
        return queue;
    }
}
