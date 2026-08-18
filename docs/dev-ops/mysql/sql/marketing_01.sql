CREATE
database if NOT EXISTS `marketing_01` default character set utf8mb4;
use
`marketing_01`;


DROP TABLE IF EXISTS `raffle_activity_account`;

CREATE TABLE `raffle_activity_account`
(
    `id`                  bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `user_id`             varchar(32) NOT NULL COMMENT '用户ID',
    `activity_id`         bigint(12) NOT NULL COMMENT '活动ID',
    `total_count`         int(8) NOT NULL COMMENT '总次数',
    `total_count_surplus` int(8) NOT NULL COMMENT '总次数-剩余',
    `day_count`           int(8) NOT NULL COMMENT '日次数',
    `day_count_surplus`   int(8) NOT NULL COMMENT '日次数-剩余',
    `month_count`         int(8) NOT NULL COMMENT '月次数',
    `month_count_surplus` int(8) NOT NULL COMMENT '月次数-剩余',
    `create_time`         datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_user_id_activity_id` (`user_id`,`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽奖活动账户表';


DROP TABLE IF EXISTS `raffle_activity_order_000`;

CREATE TABLE `raffle_activity_order_000`
(
    `id`              bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `user_id`         varchar(32) NOT NULL COMMENT '用户ID',
    `sku`             bigint(12) NOT NULL COMMENT '商品sku',
    `activity_id`     bigint(12) NOT NULL COMMENT '活动ID',
    `activity_name`   varchar(64) NOT NULL COMMENT '活动名称',
    `strategy_id`     bigint(8) NOT NULL COMMENT '抽奖策略ID',
    `order_id`        varchar(12) NOT NULL COMMENT '订单ID',
    `order_time`      datetime    NOT NULL COMMENT '下单时间',
    `total_count`     int(8) NOT NULL COMMENT '总次数',
    `day_count`       int(8) NOT NULL COMMENT '日次数',
    `month_count`     int(8) NOT NULL COMMENT '月次数',
    `state`           varchar(8)  NOT NULL DEFAULT 'complete' COMMENT '订单状态（complete）',
    `out_business_no` varchar(64) NOT NULL COMMENT '业务仿重ID - 外部透传的，确保幂等',
    `create_time`     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_order_id` (`order_id`),
    KEY               `idx_user_id_activity_id` (`user_id`,`activity_id`,`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽奖活动单';


INSERT INTO `raffle_activity_order_001` (`id`, `user_id`, `sku`, `activity_id`, `activity_name`, `strategy_id`,
                                         `order_id`, `order_time`, `total_count`, `day_count`, `month_count`, `state`,
                                         `out_business_no`, `create_time`, `update_time`)
VALUES (3, 'Charlie', 9011, 100301, '测试活动', 100006, '383240888158', '2024-03-23 04:38:23', 1, 1, 1, 'completed',
        '700091009111', '2024-03-23 12:38:23', '2024-03-23 12:38:23');


DROP TABLE IF EXISTS `raffle_activity_order_001`;

CREATE TABLE `raffle_activity_order_001`
(
    `id`              bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `user_id`         varchar(32) NOT NULL COMMENT '用户ID',
    `sku`             bigint(12) NOT NULL COMMENT '商品sku',
    `activity_id`     bigint(12) NOT NULL COMMENT '活动ID',
    `activity_name`   varchar(64) NOT NULL COMMENT '活动名称',
    `strategy_id`     bigint(8) NOT NULL COMMENT '抽奖策略ID',
    `order_id`        varchar(12) NOT NULL COMMENT '订单ID',
    `order_time`      datetime    NOT NULL COMMENT '下单时间',
    `total_count`     int(8) NOT NULL COMMENT '总次数',
    `day_count`       int(8) NOT NULL COMMENT '日次数',
    `month_count`     int(8) NOT NULL COMMENT '月次数',
    `state`           varchar(8)  NOT NULL DEFAULT 'complete' COMMENT '订单状态（complete）',
    `out_business_no` varchar(64) NOT NULL COMMENT '业务仿重ID - 外部透传的，确保幂等',
    `create_time`     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_order_id` (`order_id`),
    KEY               `idx_user_id_activity_id` (`user_id`,`activity_id`,`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽奖活动单';


DROP TABLE IF EXISTS `raffle_activity_order_002`;

CREATE TABLE `raffle_activity_order_002`
(
    `id`              bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `user_id`         varchar(32) NOT NULL COMMENT '用户ID',
    `sku`             bigint(12) NOT NULL COMMENT '商品sku',
    `activity_id`     bigint(12) NOT NULL COMMENT '活动ID',
    `activity_name`   varchar(64) NOT NULL COMMENT '活动名称',
    `strategy_id`     bigint(8) NOT NULL COMMENT '抽奖策略ID',
    `order_id`        varchar(12) NOT NULL COMMENT '订单ID',
    `order_time`      datetime    NOT NULL COMMENT '下单时间',
    `total_count`     int(8) NOT NULL COMMENT '总次数',
    `day_count`       int(8) NOT NULL COMMENT '日次数',
    `month_count`     int(8) NOT NULL COMMENT '月次数',
    `state`           varchar(8)  NOT NULL DEFAULT 'complete' COMMENT '订单状态（complete）',
    `out_business_no` varchar(64) NOT NULL COMMENT '业务仿重ID - 外部透传的，确保幂等',
    `create_time`     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_order_id` (`order_id`),
    KEY               `idx_user_id_activity_id` (`user_id`,`activity_id`,`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽奖活动单';


DROP TABLE IF EXISTS `raffle_activity_order_003`;

CREATE TABLE `raffle_activity_order_003`
(
    `id`              bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `user_id`         varchar(32) NOT NULL COMMENT '用户ID',
    `sku`             bigint(12) NOT NULL COMMENT '商品sku',
    `activity_id`     bigint(12) NOT NULL COMMENT '活动ID',
    `activity_name`   varchar(64) NOT NULL COMMENT '活动名称',
    `strategy_id`     bigint(8) NOT NULL COMMENT '抽奖策略ID',
    `order_id`        varchar(12) NOT NULL COMMENT '订单ID',
    `order_time`      datetime    NOT NULL COMMENT '下单时间',
    `total_count`     int(8) NOT NULL COMMENT '总次数',
    `day_count`       int(8) NOT NULL COMMENT '日次数',
    `month_count`     int(8) NOT NULL COMMENT '月次数',
    `state`           varchar(8)  NOT NULL DEFAULT 'complete' COMMENT '订单状态（complete）',
    `out_business_no` varchar(64) NOT NULL COMMENT '业务仿重ID - 外部透传的，确保幂等',
    `create_time`     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_order_id` (`order_id`),
    KEY               `idx_user_id_activity_id` (`user_id`,`activity_id`,`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽奖活动单';


