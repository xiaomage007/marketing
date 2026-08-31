package com.charlie.trigger.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.charlie.domain.award.event.SendAwardMessageEvent;
import com.charlie.types.event.BaseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * MQ 消费者 - 用户中奖发奖消息
 * <p>
 * 消费 {@code send_award} 队列(默认交换机直发,拓扑见 {@code application-dev.yml} 的
 * {@code rabbitmq.topology.queues.send_award}),消息体为 {@code EventMessage<SendAwardMessage>}。
 * 当前仅完成消息接收与解析,发奖逻辑后续实现。
 *
 * @author Charlie
 */
@Slf4j
@Component
public class SendAwardCustomer {

    /**
     * MQ 消息处理入口。SpEL 从 {@code rabbitMqTopologyProperties} 按逻辑 key 取队列 broker 物理名,与配置文件同源。
     * <p>
     * 消费失败必须 rethrow(不能 log 后吞掉):acknowledge-mode=auto 下,异常抛出走
     * 拒绝 -> 本地重试(max-attempts=3)-> 死信,避免「任务表已补偿投递、消费侧却静默丢失」的脏数据。
     */
    @RabbitListener(queues = "#{@rabbitMqTopologyProperties.queues['send_award'].name}")
    public void listener(String message) {
        try {
            log.info("监听用户奖品发送消息 message: {}", message);
            BaseEvent.EventMessage<SendAwardMessageEvent.SendAwardMessage> eventMessage
                    = JSON.parseObject(message, new TypeReference<BaseEvent.EventMessage<SendAwardMessageEvent.SendAwardMessage>>() {
            }.getType());
            SendAwardMessageEvent.SendAwardMessage sendAwardMessage = eventMessage.getData();
            log.info("用户奖品发送消息解析完成 userId: {} awardId: {} awardTitle: {}",
                    sendAwardMessage.getUserId(), sendAwardMessage.getAwardId(), sendAwardMessage.getAwardTitle());
            // TODO 发奖逻辑后续实现(组装发奖策略、发货、更新中奖记录状态等)
        } catch (Exception e) {
            log.error("监听用户奖品发送消息，消费失败 message: {}", message, e);
            throw e;
        }
    }

}
