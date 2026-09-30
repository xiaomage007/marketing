package com.charlie.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @description: Sentinel 限流/熔断规则托管配置。规则以 JSON 发布在 Nacos，由数据源启动拉取 + 监听秒级热更新
 * @author: Charlie
 * @date: 2026/9/30 08:30
 */
@Data
@ConfigurationProperties(prefix = "sentinel.rule", ignoreInvalidFields = true)
public class SentinelRuleProperties {

    /**
     * Nacos 服务地址，与 DCC 共用同一个 Nacos；集群时用逗号分隔
     */
    private String serverAddr = "127.0.0.1:8848";

    /**
     * 规则分组，同一分组下按 dataId 区分规则类型（接口流控/热点流控/熔断降级）
     */
    private String group = "marketing-sentinel";

    /**
     * 接口级流控规则 dataId（resource 维度 QPS）
     */
    private String flowDataId = "draw-flow-rules";

    /**
     * 热点参数流控规则 dataId（按 userId 维度 QPS）
     */
    private String paramFlowDataId = "draw-param-flow-rules";

    /**
     * 熔断降级规则 dataId（慢调用比例/异常比例）
     */
    private String degradeDataId = "draw-degrade-rules";

}
