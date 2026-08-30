package com.charlie.domain.award.service;

import com.charlie.domain.award.event.SendAwardMessageEvent;
import com.charlie.domain.award.model.aggregate.UserAwardRecordAggregate;
import com.charlie.domain.award.model.entity.TaskEntity;
import com.charlie.domain.award.model.entity.UserAwardRecordEntity;
import com.charlie.domain.award.model.valobj.TaskStateVO;
import com.charlie.domain.award.repository.IAwardRepository;
import com.charlie.types.event.BaseEvent;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @description: 奖品服务
 * @author: Charlie
 * @date: 2026/8/30 7:24
 */
@Service
public class AwardService implements IAwardService {

    @Resource
    private IAwardRepository awardRepository;

    @Resource
    private SendAwardMessageEvent sendAwardMessageEvent;

    @Override
    public void saveUserAwardRecord(UserAwardRecordEntity userAwardRecordEntity) {

        // 构建消息对象
        SendAwardMessageEvent.SendAwardMessage sendAwardMessage
                = new SendAwardMessageEvent.SendAwardMessage();
        sendAwardMessage.setUserId(userAwardRecordEntity.getUserId());
        sendAwardMessage.setAwardId(userAwardRecordEntity.getAwardId());
        sendAwardMessage.setAwardTitle(userAwardRecordEntity.getAwardTitle());

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

}
