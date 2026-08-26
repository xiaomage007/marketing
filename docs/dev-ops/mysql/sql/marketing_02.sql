CREATE
database if NOT EXISTS `marketing_02` default character set utf8mb4;
use
`marketing_02`;


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

DROP TABLE IF EXISTS `raffle_activity_account_day`;

CREATE TABLE `raffle_activity_account_day`
(
    `id`                int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `user_id`           varchar(32) NOT NULL COMMENT '用户ID',
    `activity_id`       bigint(12) NOT NULL COMMENT '活动ID',
    `day`               varchar(10) NOT NULL COMMENT '日期（yyyy-mm-dd）',
    `day_count`         int(8) NOT NULL COMMENT '日次数',
    `day_count_surplus` int(8) NOT NULL COMMENT '日次数-剩余',
    `create_time`       datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_user_id_activity_id_day` (`user_id`,`activity_id`,`day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽奖活动账户表-日次数';


DROP TABLE IF EXISTS `raffle_activity_account_month`;

CREATE TABLE `raffle_activity_account_month`
(
    `id`                  int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `user_id`             varchar(32) NOT NULL COMMENT '用户ID',
    `activity_id`         bigint(12) NOT NULL COMMENT '活动ID',
    `month`               varchar(7)  NOT NULL COMMENT '月（yyyy-mm）',
    `month_count`         int(8) NOT NULL COMMENT '月次数',
    `month_count_surplus` int(8) NOT NULL COMMENT '月次数-剩余',
    `create_time`         datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_user_id_activity_id_month` (`user_id`,`activity_id`,`month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽奖活动账户表-月次数';

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
    `state`           varchar(16) NOT NULL DEFAULT 'complete' COMMENT '订单状态（complete）',
    `out_business_no` varchar(64) NOT NULL COMMENT '业务仿重ID - 外部透传的，确保幂等',
    `create_time`     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_order_id` (`order_id`),
    KEY               `idx_user_id_activity_id` (`user_id`,`activity_id`,`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽奖活动单';


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
    `state`           varchar(16) NOT NULL DEFAULT 'complete' COMMENT '订单状态（complete）',
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
    `state`           varchar(16) NOT NULL DEFAULT 'complete' COMMENT '订单状态（complete）',
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
    `state`           varchar(16) NOT NULL DEFAULT 'complete' COMMENT '订单状态（complete）',
    `out_business_no` varchar(64) NOT NULL COMMENT '业务仿重ID - 外部透传的，确保幂等',
    `create_time`     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_order_id` (`order_id`),
    KEY               `idx_user_id_activity_id` (`user_id`,`activity_id`,`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽奖活动单';

DROP TABLE IF EXISTS `task`;

CREATE TABLE `task`
(
    `id`          int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `user_id`     varchar(32)  NOT NULL COMMENT '用户ID（分库路由键，与 marketing_01/marketing_02 分库键一致）',
    `message_id`  varchar(32)  NOT NULL COMMENT '消息唯一ID（与 BaseEvent.EventMessage.id 对齐，消费侧用作幂等键）',
    `topic`       varchar(32)  NOT NULL COMMENT '业务主题（如 activity_sku_stock_zero，按业务语义聚合）',
    `exchange`    varchar(64)  NOT NULL DEFAULT '' COMMENT 'RabbitMQ 交换机名；空串表示走 broker 默认交换机（与 BaseEvent.exchange() 对齐）',
    `routing_key` varchar(64)  NOT NULL DEFAULT '' COMMENT 'RabbitMQ 路由键；默认交换机下等于队列名（与 BaseEvent.routingKey() 对齐）',
    `queue`       varchar(64)  DEFAULT NULL COMMENT '目标队列名（与 BaseEvent.queue() 对齐）；fanout 等无路由场景可为 NULL，便于消费侧 @RabbitListener 定位',
    `message`     varchar(512) NOT NULL COMMENT '消息主体（JSON 序列化）',
    `state`       varchar(16)  NOT NULL DEFAULT 'create' COMMENT '任务状态；create-待发送、completed-发送成功、fail-发送失败',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY          `idx_state` (`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务表，发送MQ（落库消息与 RabbitMQ 投递参数，调度器按 state 扫描推进）';


DROP TABLE IF EXISTS `user_award_record_000`;

CREATE TABLE `user_award_record_000`
(
    `id`          int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `user_id`     varchar(32)  NOT NULL COMMENT '用户ID',
    `activity_id` bigint(12) NOT NULL COMMENT '活动ID',
    `strategy_id` bigint(8) NOT NULL COMMENT '抽奖策略ID',
    `order_id`    varchar(12)  NOT NULL COMMENT '抽奖订单ID【作为幂等使用】',
    `award_id`    int(11) NOT NULL COMMENT '奖品ID',
    `award_title` varchar(128) NOT NULL COMMENT '奖品标题（名称）',
    `award_time`  datetime     NOT NULL COMMENT '中奖时间',
    `award_state` varchar(16)  NOT NULL DEFAULT 'create' COMMENT '奖品状态；create-创建、completed-发奖完成',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_order_id` (`order_id`),
    KEY           `idx_user_id` (`user_id`),
    KEY           `idx_activity_id` (`activity_id`),
    KEY           `idx_award_id` (`strategy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户中奖记录表';


DROP TABLE IF EXISTS `user_award_record_001`;

CREATE TABLE `user_award_record_001`
(
    `id`          int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `user_id`     varchar(32)  NOT NULL COMMENT '用户ID',
    `activity_id` bigint(12) NOT NULL COMMENT '活动ID',
    `strategy_id` bigint(8) NOT NULL COMMENT '抽奖策略ID',
    `order_id`    varchar(12)  NOT NULL COMMENT '抽奖订单ID【作为幂等使用】',
    `award_id`    int(11) NOT NULL COMMENT '奖品ID',
    `award_title` varchar(128) NOT NULL COMMENT '奖品标题（名称）',
    `award_time`  datetime     NOT NULL COMMENT '中奖时间',
    `award_state` varchar(16)  NOT NULL DEFAULT 'create' COMMENT '奖品状态；create-创建、completed-发奖完成',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_order_id` (`order_id`),
    KEY           `idx_user_id` (`user_id`),
    KEY           `idx_activity_id` (`activity_id`),
    KEY           `idx_award_id` (`strategy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户中奖记录表';


DROP TABLE IF EXISTS `user_award_record_002`;

CREATE TABLE `user_award_record_002`
(
    `id`          int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `user_id`     varchar(32)  NOT NULL COMMENT '用户ID',
    `activity_id` bigint(12) NOT NULL COMMENT '活动ID',
    `strategy_id` bigint(8) NOT NULL COMMENT '抽奖策略ID',
    `order_id`    varchar(12)  NOT NULL COMMENT '抽奖订单ID【作为幂等使用】',
    `award_id`    int(11) NOT NULL COMMENT '奖品ID',
    `award_title` varchar(128) NOT NULL COMMENT '奖品标题（名称）',
    `award_time`  datetime     NOT NULL COMMENT '中奖时间',
    `award_state` varchar(16)  NOT NULL DEFAULT 'create' COMMENT '奖品状态；create-创建、completed-发奖完成',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_order_id` (`order_id`),
    KEY           `idx_user_id` (`user_id`),
    KEY           `idx_activity_id` (`activity_id`),
    KEY           `idx_award_id` (`strategy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户中奖记录表';


DROP TABLE IF EXISTS `user_award_record_003`;

CREATE TABLE `user_award_record_003`
(
    `id`          int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `user_id`     varchar(32)  NOT NULL COMMENT '用户ID',
    `activity_id` bigint(12) NOT NULL COMMENT '活动ID',
    `strategy_id` bigint(8) NOT NULL COMMENT '抽奖策略ID',
    `order_id`    varchar(12)  NOT NULL COMMENT '抽奖订单ID【作为幂等使用】',
    `award_id`    int(11) NOT NULL COMMENT '奖品ID',
    `award_title` varchar(128) NOT NULL COMMENT '奖品标题（名称）',
    `award_time`  datetime     NOT NULL COMMENT '中奖时间',
    `award_state` varchar(16)  NOT NULL DEFAULT 'create' COMMENT '奖品状态；create-创建、completed-发奖完成',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_order_id` (`order_id`),
    KEY           `idx_user_id` (`user_id`),
    KEY           `idx_activity_id` (`activity_id`),
    KEY           `idx_award_id` (`strategy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户中奖记录表';


DROP TABLE IF EXISTS `user_raffle_order_000`;

CREATE TABLE `user_raffle_order_000`
(
    `id`            int(11) unsigned NOT NULL AUTO_INCREMENT,
    `user_id`       varchar(32) NOT NULL COMMENT '用户ID',
    `activity_id`   bigint(12) NOT NULL COMMENT '活动ID',
    `activity_name` varchar(64) NOT NULL COMMENT '活动名称',
    `strategy_id`   bigint(8) NOT NULL COMMENT '抽奖策略ID',
    `order_id`      varchar(12) NOT NULL COMMENT '订单ID',
    `order_time`    datetime    NOT NULL COMMENT '下单时间',
    `order_state`   varchar(16) NOT NULL DEFAULT 'create' COMMENT '订单状态；create-创建、used-已使用、cancle-已作废',
    `create_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_order_id` (`order_id`),
    KEY             `idx_user_id_activity_id` (`user_id`,`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户抽奖订单表';


DROP TABLE IF EXISTS `user_raffle_order_001`;

CREATE TABLE `user_raffle_order_001`
(
    `id`            int(11) unsigned NOT NULL AUTO_INCREMENT,
    `user_id`       varchar(32) NOT NULL COMMENT '用户ID',
    `activity_id`   bigint(12) NOT NULL COMMENT '活动ID',
    `activity_name` varchar(64) NOT NULL COMMENT '活动名称',
    `strategy_id`   bigint(8) NOT NULL COMMENT '抽奖策略ID',
    `order_id`      varchar(12) NOT NULL COMMENT '订单ID',
    `order_time`    datetime    NOT NULL COMMENT '下单时间',
    `order_state`   varchar(16) NOT NULL DEFAULT 'create' COMMENT '订单状态；create-创建、used-已使用、cancle-已作废',
    `create_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_order_id` (`order_id`),
    KEY             `idx_user_id_activity_id` (`user_id`,`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户抽奖订单表';


DROP TABLE IF EXISTS `user_raffle_order_002`;

CREATE TABLE `user_raffle_order_002`
(
    `id`            int(11) unsigned NOT NULL AUTO_INCREMENT,
    `user_id`       varchar(32) NOT NULL COMMENT '用户ID',
    `activity_id`   bigint(12) NOT NULL COMMENT '活动ID',
    `activity_name` varchar(64) NOT NULL COMMENT '活动名称',
    `strategy_id`   bigint(8) NOT NULL COMMENT '抽奖策略ID',
    `order_id`      varchar(12) NOT NULL COMMENT '订单ID',
    `order_time`    datetime    NOT NULL COMMENT '下单时间',
    `order_state`   varchar(16) NOT NULL DEFAULT 'create' COMMENT '订单状态；create-创建、used-已使用、cancle-已作废',
    `create_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_order_id` (`order_id`),
    KEY             `idx_user_id_activity_id` (`user_id`,`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户抽奖订单表';


DROP TABLE IF EXISTS `user_raffle_order_003`;

CREATE TABLE `user_raffle_order_003`
(
    `id`            int(11) unsigned NOT NULL AUTO_INCREMENT,
    `user_id`       varchar(32) NOT NULL COMMENT '用户ID',
    `activity_id`   bigint(12) NOT NULL COMMENT '活动ID',
    `activity_name` varchar(64) NOT NULL COMMENT '活动名称',
    `strategy_id`   bigint(8) NOT NULL COMMENT '抽奖策略ID',
    `order_id`      varchar(12) NOT NULL COMMENT '订单ID',
    `order_time`    datetime    NOT NULL COMMENT '下单时间',
    `order_state`   varchar(16) NOT NULL DEFAULT 'create' COMMENT '订单状态；create-创建、used-已使用、cancle-已作废',
    `create_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_order_id` (`order_id`),
    KEY             `idx_user_id_activity_id` (`user_id`,`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户抽奖订单表';
