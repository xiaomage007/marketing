package com.charlie.domain.award.service;

import com.charlie.domain.award.event.SendAwardMessageEvent;
import com.charlie.domain.award.model.aggregate.UserAwardRecordAggregate;
import com.charlie.domain.award.model.entity.DistributeAwardEntity;
import com.charlie.domain.award.model.entity.TaskEntity;
import com.charlie.domain.award.model.entity.UserAwardRecordEntity;
import com.charlie.domain.award.model.valobj.TaskStateVO;
import com.charlie.domain.award.repository.IAwardRepository;
import com.charlie.domain.award.service.distribute.IDistributeAward;
import com.charlie.types.event.BaseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @description: 奖品服务
 * @author: Charlie
 * @date: 2026/8/30 7:24
 */
@Slf4j
@Service
public class AwardService implements IAwardService {

    private final IAwardRepository awardRepository;
    private final SendAwardMessageEvent sendAwardMessageEvent;
    private final Map<String, IDistributeAward> distributeAwardMap;

    public AwardService(IAwardRepository awardRepository, SendAwardMessageEvent sendAwardMessageEvent, Map<String, IDistributeAward> distributeAwardMap) {
        this.awardRepository = awardRepository;
        this.sendAwardMessageEvent = sendAwardMessageEvent;
        this.distributeAwardMap = distributeAwardMap;
    }

    @Override
    public void saveUserAwardRecord(UserAwardRecordEntity userAwardRecordEntity) {

        // 构建消息对象
        SendAwardMessageEvent.SendAwardMessage sendAwardMessage
                = new SendAwardMessageEvent.SendAwardMessage();
        sendAwardMessage.setUserId(userAwardRecordEntity.getUserId());
        sendAwardMessage.setOrderId(userAwardRecordEntity.getOrderId());
        sendAwardMessage.setAwardId(userAwardRecordEntity.getAwardId());
        sendAwardMessage.setAwardTitle(userAwardRecordEntity.getAwardTitle());
        sendAwardMessage.setAwardConfig(userAwardRecordEntity.getAwardConfig());

        BaseEvent.EventMessage<SendAwardMessageEvent.SendAwardMessage> sendAwardMessageEventMessage
                = sendAwardMessageEvent.buildEventMessage(sendAwardMessage);

        // 构建任务对象
        TaskEntity taskEntity = TaskEntity.builder().userId(userAwardRecordEntity.getUserId())
                .messageId(sendAwardMessageEventMessage.getId())
                .exchange(sendAwardMessageEvent.exchange())
                .routingKey(sendAwardMessageEvent.routingKey())
                .queue(sendAwardMessageEvent.queue())
                .message(sendAwardMessageEventMessage)
                .state(TaskStateVO.create).build();

        // 构建聚合对象
        UserAwardRecordAggregate userAwardRecordAggregate = UserAwardRecordAggregate.builder().
                userAwardRecordEntity(userAwardRecordEntity).
                taskEntity(taskEntity).
                build();

        // 存储聚合对象 - 一个事务下，用户的中奖记录
        awardRepository.saveUserAwardRecord(userAwardRecordAggregate);

    }

    @Override
    public void distributeAward(DistributeAwardEntity distributeAwardEntity) {
        // 奖品Key
        String awardKey = awardRepository.queryAwardKey(distributeAwardEntity.getAwardId());
        if (null == awardKey) {
            log.error("分发奖品，奖品ID不存在。awardKey:{}", awardKey);
            return;
        }

        // 奖品服务
        IDistributeAward distributeAward = distributeAwardMap.get(awardKey);

        if (null == distributeAward) {
            log.error("分发奖品，对应的服务不存在。awardKey:{}", awardKey);
            throw new RuntimeException("分发奖品，奖品" + awardKey + "对应的服务不存在");
        }

        // 发放奖品
        distributeAward.giveOutPrizes(distributeAwardEntity);
    }

}
