package com.charlie.types.annotation;

import java.lang.annotation.*;

/**
 * @description: 动态配置中心
 * @author: Charlie
 * @date: 2026/9/24 15:31
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface DCCValue {

    String value() default "";

}
