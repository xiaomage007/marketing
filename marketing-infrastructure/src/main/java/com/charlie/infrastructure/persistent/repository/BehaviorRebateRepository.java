package com.charlie.infrastructure.persistent.repository;

import cn.bugstack.middleware.db.router.strategy.IDBRouterStrategy;
import com.alibaba.fastjson.JSON;
import com.charlie.domain.rebate.model.aggregate.BehaviorRebateAggregate;
import com.charlie.domain.rebate.model.entity.BehaviorRebateOrderEntity;
import com.charlie.domain.rebate.model.entity.TaskEntity;
import com.charlie.domain.rebate.model.valobj.BehaviorTypeVO;
import com.charlie.domain.rebate.model.valobj.DailyBehaviorRebateVO;
import com.charlie.domain.rebate.repository.IBehaviorRebateRepository;
import com.charlie.infrastructure.mq.EventPublisher;
import com.charlie.infrastructure.persistent.dao.IDailyBehaviorRebateDao;
import com.charlie.infrastructure.persistent.dao.ITaskDao;
import com.charlie.infrastructure.persistent.dao.IUserBehaviorRebateOrderDao;
import com.charlie.infrastructure.persistent.po.DailyBehaviorRebate;
import com.charlie.infrastructure.persistent.po.Task;
import com.charlie.infrastructure.persistent.po.UserBehaviorRebateOrder;
import com.charlie.types.enums.ResponseCode;
import com.charlie.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @description: 行为返利服务仓储实现
 * @author: Charlie
 * @date: 2026/9/8 7:48
 */
@Slf4j
@Repository
public class BehaviorRebateRepository implements IBehaviorRebateRepository {

    @Resource
    private IDailyBehaviorRebateDao dailyBehaviorRebateDao;
    @Resource
    private IUserBehaviorRebateOrderDao userBehaviorRebateOrderDao;
    @Resource
    private ITaskDao taskDao;
    @Resource
    private IDBRouterStrategy dbRouter;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private EventPublisher eventPublisher;


    @Override
    public List<DailyBehaviorRebateVO> queryDailyBehaviorRebateConfig(BehaviorTypeVO behaviorTypeVO) {
        List<DailyBehaviorRebate> dailyBehaviorRebates = dailyBehaviorRebateDao.queryDailyBehaviorRebateByBehaviorType(behaviorTypeVO.getCode());
        List<DailyBehaviorRebateVO> dailyBehaviorRebateVOS = new ArrayList<>(dailyBehaviorRebates.size());
        for (DailyBehaviorRebate dailyBehaviorRebate : dailyBehaviorRebates) {
            DailyBehaviorRebateVO dailyBehaviorRebateVO = DailyBehaviorRebateVO.builder()
                    .rebateType(dailyBehaviorRebate.getRebateType())
                    .behaviorType(dailyBehaviorRebate.getBehaviorType())
                    .rebateDesc(dailyBehaviorRebate.getRebateDesc())
                    .rebateConfig(dailyBehaviorRebate.getRebateConfig())
                    .build();
            dailyBehaviorRebateVOS.add(dailyBehaviorRebateVO);
        }
        return dailyBehaviorRebateVOS;
    }

    @Override
    public void saveUserRebateRecord(String userId, List<BehaviorRebateAggregate> behaviorRebateAggregates) {
        try {
            dbRouter.doRouter(userId);
            transactionTemplate.execute(status -> {
                try {
                    for (BehaviorRebateAggregate behaviorRebateAggregate : behaviorRebateAggregates) {
                        BehaviorRebateOrderEntity behaviorRebateOrderEntity = behaviorRebateAggregate.getBehaviorRebateOrderEntity();
                        // 用户行为返利订单对象
                        UserBehaviorRebateOrder userBehaviorRebateOrder = new UserBehaviorRebateOrder();
                        userBehaviorRebateOrder.setUserId(behaviorRebateOrderEntity.getUserId());
                        userBehaviorRebateOrder.setOrderId(behaviorRebateOrderEntity.getOrderId());
                        userBehaviorRebateOrder.setBehaviorType(behaviorRebateOrderEntity.getBehaviorType());
                        userBehaviorRebateOrder.setRebateDesc(behaviorRebateOrderEntity.getRebateDesc());
                        userBehaviorRebateOrder.setRebateType(behaviorRebateOrderEntity.getRebateType());
                        userBehaviorRebateOrder.setRebateConfig(behaviorRebateOrderEntity.getRebateConfig());
                        userBehaviorRebateOrder.setBizId(behaviorRebateOrderEntity.getBizId());
                        userBehaviorRebateOrderDao.insert(userBehaviorRebateOrder);

                        // 任务对象
                        TaskEntity taskEntity = behaviorRebateAggregate.getTaskEntity();
                        Task task = new Task();
                        task.setUserId(taskEntity.getUserId());
                        task.setExchange(taskEntity.getExchange());
                        task.setRoutingKey(taskEntity.getRoutingKey());
                        task.setQueue(taskEntity.getQueue());                        task.setMessageId(taskEntity.getMessageId());
                        task.setMessage(JSON.toJSONString(taskEntity.getMessage()));
                        task.setState(taskEntity.getState().getCode());
                        taskDao.insert(task);
                    }
                    return 1;
                } catch (DuplicateKeyException e) {
                    status.setRollbackOnly();
                    log.error("写入返利记录，唯一索引冲突 userId: {}", userId, e);
                    throw new AppException(ResponseCode.INDEX_DUP.getCode(), e);
                }
            });
        } finally {
            dbRouter.clear();
        }
        // 同步发送MQ消息
        for (BehaviorRebateAggregate behaviorRebateAggregate : behaviorRebateAggregates) {
            TaskEntity taskEntity = behaviorRebateAggregate.getTaskEntity();
            Task task = new Task();
            task.setUserId(taskEntity.getUserId());
            task.setMessageId(taskEntity.getMessageId());

            eventPublisher.publish(taskEntity.getExchange(), taskEntity.getRoutingKey(), taskEntity.getMessage());
        }
    }

}
