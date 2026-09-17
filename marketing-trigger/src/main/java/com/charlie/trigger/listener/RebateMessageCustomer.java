package com.charlie.trigger.listener;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.charlie.domain.activity.model.entity.SkuRechargeEntity;
import com.charlie.domain.activity.service.IRaffleActivityAccountQuotaService;
import com.charlie.domain.credit.model.entity.TradeEntity;
import com.charlie.domain.credit.model.valobj.TradeNameVO;
import com.charlie.domain.credit.model.valobj.TradeTypeVO;
import com.charlie.domain.credit.service.ICreditAdjustService;
import com.charlie.domain.rebate.event.SendRebateMessageEvent;
import com.charlie.types.enums.ResponseCode;
import com.charlie.types.event.BaseEvent;
import com.charlie.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * @description: 行为返利消息
 * @author: Charlie
 * @date: 2026/9/9 7:34
 */
@Slf4j
@Component
public class RebateMessageCustomer {

    @Resource
    private IRaffleActivityAccountQuotaService raffleActivityAccountQuotaService;
    @Resource
    private ICreditAdjustService creditAdjustService;


    @RabbitListener(queues = "#{@rabbitMqTopologyProperties.queues['send_award'].name}")
    public void listener(String message) {
        try {
            log.info("监听用户行为返利消息 message: {}", message);
            // 1. 转换消息
            BaseEvent.EventMessage<SendRebateMessageEvent.RebateMessage> eventMessage =
                    JSON.parseObject(message, new TypeReference<BaseEvent.EventMessage<SendRebateMessageEvent.RebateMessage>>() {
                    }.getType());
            SendRebateMessageEvent.RebateMessage rebateMessage = eventMessage.getData();

            // 2. 入账奖励
            switch (rebateMessage.getRebateType()) {
                case "sku":
                    SkuRechargeEntity skuRechargeEntity = new SkuRechargeEntity();
                    skuRechargeEntity.setUserId(rebateMessage.getUserId());
                    skuRechargeEntity.setSku(Long.valueOf(rebateMessage.getRebateConfig()));
                    skuRechargeEntity.setOutBusinessNo(rebateMessage.getBizId());
                    raffleActivityAccountQuotaService.createOrder(skuRechargeEntity);
                    break;
                case "integral":
                    TradeEntity tradeEntity = new TradeEntity();
                    tradeEntity.setUserId(rebateMessage.getUserId());
                    tradeEntity.setTradeName(TradeNameVO.REBATE);
                    tradeEntity.setTradeType(TradeTypeVO.FORWARD);
                    tradeEntity.setAmount(new BigDecimal(rebateMessage.getRebateConfig()));
                    tradeEntity.setOutBusinessNo(rebateMessage.getBizId());
                    creditAdjustService.createOrder(tradeEntity);
                    break;
            }
        } catch (AppException e) {
            if (ResponseCode.INDEX_DUP.getCode().equals(e.getCode())) {
                log.warn("监听用户行为返利消息，消费重复 message: {}", message, e);
                return;
            }
            throw e;
        } catch (Exception e) {
            log.error("监听用户行为返利消息，消费失败 message: {}", message, e);
            throw e;
        }

    }

}
