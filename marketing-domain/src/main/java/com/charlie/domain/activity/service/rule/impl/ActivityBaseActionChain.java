package com.charlie.domain.activity.service.rule.impl;

import com.charlie.domain.activity.model.entity.ActivityCountEntity;
import com.charlie.domain.activity.model.entity.ActivityEntity;
import com.charlie.domain.activity.model.entity.ActivitySkuEntity;
import com.charlie.domain.activity.service.rule.AbstractActionChain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @description: 活动规则过滤【日期、状态】
 * @author: Charlie
 * @date: 2026/8/18 9:05
 */
@Slf4j
@Component("activity_base_action")
public class ActivityBaseActionChain  extends AbstractActionChain {

    @Override
    public boolean action(ActivitySkuEntity activitySkuEntity, ActivityEntity activityEntity, ActivityCountEntity activityCountEntity) {
        log.info("活动责任链-基础信息【有效期、状态】校验开始。");
        return next().action(activitySkuEntity, activityEntity, activityCountEntity);
    }
}
