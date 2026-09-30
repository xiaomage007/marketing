package com.charlie.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @description: Nacos 配置中心客户端连接配置
 * @author: Charlie
 * @date: 2026/9/28 15:20
 */
@Data
@ConfigurationProperties(prefix = "nacos.sdk.config", ignoreInvalidFields = true)
public class NacosConfigClientConfigProperties {

    /**
     * Nacos 服务地址，集群时用逗号分隔
     */
    private String serverAddr;

    /**
     * 命名空间ID(不是命名空间名称)，用于隔离 dev/test/prod；使用 public 空间时留空
     */
    private String namespace;

}
