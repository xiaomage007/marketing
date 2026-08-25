package com.charlie.domain.activity.service.partake;

import com.charlie.domain.activity.model.entity.ActivityEntity;
import com.charlie.domain.activity.model.entity.PartakeRaffleActivityEntity;
import com.charlie.domain.activity.model.entity.UserRaffleOrderEntity;
import com.charlie.domain.activity.model.valobj.ActivityStateVO;
import com.charlie.domain.activity.repository.IActivityRepository;
import com.charlie.domain.activity.service.IRaffleActivityPartakeService;
import com.charlie.types.enums.ResponseCode;
import com.charlie.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

/**
 * @description: 抽奖活动参与抽奖类
 * @author: Charlie
 * @date: 2026/8/25 8:04
 */
@Slf4j
public abstract class AbstractRaffleActivityPartake implements IRaffleActivityPartakeService {

    protected final IActivityRepository activityRepository;

    protected AbstractRaffleActivityPartake(IActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    @Override
    public UserRaffleOrderEntity createOrder(PartakeRaffleActivityEntity partakeRaffleActivityEntity) {
        // 0. 基础信息
        String userId = partakeRaffleActivityEntity.getUserId();
        Long activityId = partakeRaffleActivityEntity.getActivityId();
        Date currentDate = new Date();

        // 1. 活动查询
        ActivityEntity activityEntity = activityRepository.queryRaffleActivityByActivityId(activityId);
        // 校验；活动状态
        if (!ActivityStateVO.open.equals(activityEntity.getState())) {
            throw new AppException(ResponseCode.ACTIVITY_STATE_ERROR.getCode(), ResponseCode.ACTIVITY_STATE_ERROR.getInfo());
        }
        // 校验；活动日期「开始时间 <- 当前时间 -> 结束时间」
        if (activityEntity.getBeginDateTime().after(currentDate) || activityEntity.getEndDateTime().before(currentDate)) {
            throw new AppException(ResponseCode.ACTIVITY_DATE_ERROR.getCode(), ResponseCode.ACTIVITY_DATE_ERROR.getInfo());
        }
        // 2. 查询未被使用的活动参与订单记录

        // 3. 额度账户过滤&返回账户构建对象

        // 4. 构建订单

        // 5. 填充抽奖单实体对象

        // 6. 保存聚合对象 - 一个领域内的一个聚合是一个事务操作

        return null;
    }

}
