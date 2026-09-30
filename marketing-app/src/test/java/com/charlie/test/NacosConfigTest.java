package com.charlie.test;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.AbstractListener;
import com.charlie.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * @description: Nacos 配置中心读写与监听
 * @author: Charlie
 * @date: 2026/9/28 15:30
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class NacosConfigTest {

    private static final String GROUP = Constants.DCC.CONFIG_GROUP;

    @Resource
    private ConfigService configService;

    /**
     * 发布配置 - 配置不存在时创建，存在时覆盖
     */
    @Test
    public void publishConfig() throws Exception {
        boolean result = configService.publishConfig("degradeSwitch", GROUP, "open");
        log.info("测试结果: {}", result);
    }

    /**
     * 读取配置
     */
    @Test
    public void getConfig() throws Exception {
        String degradeSwitch = configService.getConfig("degradeSwitch", GROUP, 3000L);
        log.info("测试结果: {}", degradeSwitch);
    }

    /**
     * 监听配置变更 - 注册后由控制台或 update_config 接口修改配置，回调里拿到新值
     */
    @Test
    public void addListener() throws Exception {
        configService.addListener("degradeSwitch", GROUP, new AbstractListener() {
            @Override
            public void receiveConfigInfo(String configInfo) {
                log.info("测试结果 配置变更: {}", configInfo);
            }
        });
        log.info("测试结果 监听已注册，10 秒内可通过控制台或 update_config 接口修改 degradeSwitch");
        Thread.sleep(10000);
    }

    /**
     * 删除配置 - 删除后监听端会收到空值
     */
    @Test
    public void removeConfig() throws Exception {
        boolean result = configService.removeConfig("degradeSwitch", GROUP);
        log.info("测试结果: {}", result);
    }

}
