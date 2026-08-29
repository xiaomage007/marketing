package com.charlie.domain.award.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description: 任务实体对象
 * @author: Charlie
 * @date: 2026/8/30 7:55
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskEntity {

    /** 用户ID（分库路由键，与 marketing_01/marketing_02 分库键一致） */
    private String userId;
    /** 消息唯一ID（与 BaseEvent.EventMessage.id 对齐，消费侧用作幂等键） */
    private String messageId;
    /** 业务主题（如 activity_sku_stock_zero，按业务语义聚合） */
    private String topic;
    /** RabbitMQ 交换机名；空串表示走 broker 默认交换机（与 BaseEvent.exchange() 对齐） */
    private String exchange;
    /** RabbitMQ 路由键；默认交换机下等于队列名（与 BaseEvent.routingKey() 对齐） */
    private String routingKey;
    /** 目标队列名（与 BaseEvent.queue() 对齐）；fanout 等无路由场景可为空，便于消费侧 @RabbitListener 定位 */
    private String queue;
    /** 消息主体 */
    private String message;
    /** 任务状态；create-待发送、completed-发送成功、fail-发送失败 */
    private String state;

}
