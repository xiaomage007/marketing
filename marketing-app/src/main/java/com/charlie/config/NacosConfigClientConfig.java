package com.charlie.config;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * @description: 多参数构建 Nacos 配置中心客户端连接
 * @author: Charlie
 * @date: 2026/9/28 15:22
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(NacosConfigClientConfigProperties.class)
public class NacosConfigClientConfig {

    @Bean(name = "nacosConfigService")
    public ConfigService createWithOptions(NacosConfigClientConfigProperties properties) throws NacosException {
        Properties props = new Properties();
        props.put(PropertyKeyConst.SERVER_ADDR, properties.getServerAddr());
        // namespace 留空表示走 public 空间，配置了才写入，避免空串被当成命名空间ID
        if (StringUtils.isNotBlank(properties.getNamespace())) {
            props.put(PropertyKeyConst.NAMESPACE, properties.getNamespace());
        }
        ConfigService configService = NacosFactory.createConfigService(props);
        log.info("Nacos 配置中心客户端连接完成 serverAddr:{} namespace:{}", properties.getServerAddr(), properties.getNamespace());
        return configService;
    }

}
