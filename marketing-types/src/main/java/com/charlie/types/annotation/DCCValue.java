package com.charlie.types.annotation;

import java.lang.annotation.*;

/**
 * @description: 动态配置中心。配置项托管在 Nacos 上(dataId=注解value的key、分组=Constants.DCC.CONFIG_GROUP)，
 * 值由监听线程异步刷新，为保证多线程可见性，被注解字段需声明为 volatile
 * @author: Charlie
 * @date: 2026/9/24 15:31
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface DCCValue {

    String value() default "";

}
