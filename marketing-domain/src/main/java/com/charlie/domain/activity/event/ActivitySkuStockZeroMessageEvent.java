package com.charlie.domain.activity.event;

import com.charlie.types.event.BaseEvent;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 活动 SKU 库存清零事件。
 * <p>
 * Exchange / routingKey 从 {@link RabbitMqTopology} 枚举读取,与 broker 拓扑定义保持一致;
 * 业务代码不硬编码 broker 拓扑名,新增/重命名 broker 拓扑只改枚举不改事件类。
 *
 * @author Charlie
 */
@Component
public class ActivitySkuStockZeroMessageEvent extends BaseEvent<Long> {

    @Override
    public EventMessage<Long> buildEventMessage(Long sku) {
        return EventMessage.<Long>builder()
                .id(RandomStringUtils.randomNumeric(11))
                .timestamp(new Date())
                .data(sku)
                .build();
    }

    @Override
    public String exchange() {
        return RabbitMqTopology.ACTIVITY_SKU_STOCK_ZERO.getExchange();
    }

    @Override
    public String routingKey() {
        return RabbitMqTopology.ACTIVITY_SKU_STOCK_ZERO.getRoutingKey();
    }
}