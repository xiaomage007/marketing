package com.charlie.config;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.AbstractListener;
import com.alibaba.nacos.api.exception.NacosException;
import com.charlie.types.annotation.DCCValue;
import com.charlie.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;

/**
 * @description: 基于 Nacos 的配置中心实现原理
 * @author: Charlie
 * @date: 2026/9/28 7:56
 */
@Slf4j
@Configuration
public class DCCValueBeanFactory implements BeanPostProcessor {

    /**
     * 配置读取超时(ms)
     */
    private static final long CONFIG_TIMEOUT_MS = 3000L;

    private final ConfigService configService;

    public DCCValueBeanFactory(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        Field[] fields = beanClass.getDeclaredFields();
        for (Field field : fields) {
            if (!field.isAnnotationPresent(DCCValue.class)) {
                continue;
            }

            DCCValue dccValue = field.getAnnotation(DCCValue.class);

            String value = dccValue.value();
            if (StringUtils.isBlank(value)) {
                throw new RuntimeException(field.getName() + " @DCCValue is not config value config case 「isSwitch/isSwitch:1」");
            }

            String[] splits = value.split(Constants.COLON);
            String key = splits[0];
            String defaultValue = splits.length == 2 ? splits[1] : null;

            try {
                String configValue = configService.getConfig(key, Constants.DCC.CONFIG_GROUP, CONFIG_TIMEOUT_MS);
                if (StringUtils.isBlank(configValue) && StringUtils.isNotBlank(defaultValue)) {
                    // 配置不存在时把代码里的默认值发布到 Nacos，让配置在控制台可见、可被动态修改
                    configService.publishConfig(key, Constants.DCC.CONFIG_GROUP, defaultValue);
                    configValue = defaultValue;
                    log.info("DCC 节点监听 配置不存在，默认值已发布到 Nacos key:{} value:{}", key, defaultValue);
                }

                if (StringUtils.isNotBlank(configValue)) {
                    setFieldValue(bean, field, configValue);
                    log.info("DCC 节点监听 设置配置 key:{} field:{} value:{}", key, field.getName(), configValue);
                } else {
                    log.warn("DCC 节点监听 配置不存在且未配置默认值，待控制台创建后自动生效 key:{} field:{}", key, field.getName());
                }

                addConfigListener(bean, field, key, defaultValue);
            } catch (NacosException e) {
                throw new RuntimeException("DCC 节点监听 Nacos 配置读写失败 key:" + key, e);
            }
        }
        return bean;
    }

    private void addConfigListener(Object bean, Field field, String key, String defaultValue) throws NacosException {
        configService.addListener(key, Constants.DCC.CONFIG_GROUP, new AbstractListener() {
            @Override
            public void receiveConfigInfo(String configInfo) {
                // 配置被删除时 Nacos 推送空值，此时回退到代码里的默认值
                String refreshValue = StringUtils.isNotBlank(configInfo) ? configInfo : defaultValue;
                setFieldValue(bean, field, refreshValue);
                log.info("DCC 动态配置值变更 key:{} field:{} value:{}", key, field.getName(), refreshValue);
            }
        });
    }

    private void setFieldValue(Object bean, Field field, String value) {
        try {
            field.setAccessible(true);
            field.set(bean, value);
            field.setAccessible(false);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("DCC 动态配置值注入失败 field:" + field.getName(), e);
        }
    }
}
