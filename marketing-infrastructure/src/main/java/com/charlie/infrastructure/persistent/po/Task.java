package com.charlie.infrastructure.persistent.po;

import lombok.Data;

import java.util.Date;

/**
 * @description: 任务表，发送MQ（落库消息与 RabbitMQ 投递参数）
 * @author: Charlie
 * @date: 2026/8/24 9:37
 */
@Data
public class Task {

    /** 自增ID */
    private String id;
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
    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;

}
