package com.charlie.trigger.listener;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.charlie.domain.activity.model.entity.DeliveryOrderEntity;
import com.charlie.domain.activity.service.IRaffleActivityAccountQuotaService;
import com.charlie.domain.credit.event.CreditAdjustSuccessMessageEvent;
import com.charlie.types.enums.ResponseCode;
import com.charlie.types.event.BaseEvent;
import com.charlie.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @description: 积分调整成功消息
 * @author: Charlie
 * @date: 2026/9/20 14:47
 */
@Slf4j
@Component
public class CreditAdjustSuccessCustomer {

    /**
     * 目标队列名,对应 rabbitmq.topology.queues.credit_adjust_success.name。
     */
    @Value("${rabbitmq.topology.queues.credit_adjust_success.name}")
    private String queue;

    @Resource
    private IRaffleActivityAccountQuotaService raffleActivityAccountQuotaService;

    @RabbitListener(queues = "#{@rabbitMqTopologyProperties.queues['credit_adjust_success'].name}")
    public void listener(String message){
        try {
            log.info("监听积分账户调整成功消息，进行交易商品发货 queue: {} message: {}", queue, message);
            BaseEvent.EventMessage<CreditAdjustSuccessMessageEvent.CreditAdjustSuccessMessage> eventMessage = JSON.parseObject(message, new TypeReference<BaseEvent.EventMessage<CreditAdjustSuccessMessageEvent.CreditAdjustSuccessMessage>>() {
            }.getType());
            CreditAdjustSuccessMessageEvent.CreditAdjustSuccessMessage creditAdjustSuccessMessage = eventMessage.getData();

            // 积分发货
            DeliveryOrderEntity deliveryOrderEntity = new DeliveryOrderEntity();
            deliveryOrderEntity.setUserId(creditAdjustSuccessMessage.getUserId());
            deliveryOrderEntity.setOutBusinessNo(creditAdjustSuccessMessage.getOutBusinessNo());
            raffleActivityAccountQuotaService.updateOrder(deliveryOrderEntity);
        } catch (AppException e) {
            if (ResponseCode.INDEX_DUP.getCode().equals(e.getCode())) {
                log.warn("监听积分账户调整成功消息，进行交易商品发货，消费重复 queue: {} message: {}", queue, message, e);
                return;
            }
            throw e;
        } catch (Exception e) {
            log.error("监听积分账户调整成功消息，进行交易商品发货失败 queue: {} message: {}", queue, message, e);
            throw e;
        }
    }

}
