package com.charlie.trigger.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @description: 用户奖品记录消息消费者
 * @author: Charlie
 * @date: 2026/8/31 8:35
 */
@Slf4j
@Component
public class SendAwardCustomer {


    public void listener(String message) {
        try {
            log.info("监听用户奖品发送消息 message: {}", message);
        } catch (Exception e) {
            log.error("监听用户奖品发送消息，消费失败 message: {}", message);
            throw e;
        }
    }
}
