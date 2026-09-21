package com.charlie.domain.credit.service.adjust;

import com.charlie.domain.award.model.valobj.TaskStateVO;
import com.charlie.domain.credit.event.CreditAdjustSuccessMessageEvent;
import com.charlie.domain.credit.model.aggregate.TradeAggregate;
import com.charlie.domain.credit.model.entity.CreditAccountEntity;
import com.charlie.domain.credit.model.entity.CreditOrderEntity;
import com.charlie.domain.credit.model.entity.TaskEntity;
import com.charlie.domain.credit.model.entity.TradeEntity;
import com.charlie.domain.credit.repository.ICreditRepository;
import com.charlie.domain.credit.service.ICreditAdjustService;
import com.charlie.types.event.BaseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @description: 积分调额服务【正逆向，增减积分】
 * @author: Charlie
 * @date: 2026/9/17 9:08
 */
@Slf4j
@Service
public class CreditAdjustService implements ICreditAdjustService {

    @Resource
    private ICreditRepository creditRepository;
    @Resource
    private CreditAdjustSuccessMessageEvent creditAdjustSuccessMessageEvent;

    @Override
    public String createOrder(TradeEntity tradeEntity) {
        log.info("增加账户积分额度开始 userId:{} tradeName:{} amount:{}", tradeEntity.getUserId(), tradeEntity.getTradeName(), tradeEntity.getAmount());
        // 1. 创建账户积分实体
        CreditAccountEntity creditAccountEntity = TradeAggregate.createCreditAccountEntity(
                tradeEntity.getUserId(),
                tradeEntity.getAmount());

        // 2. 创建账户订单实体
        CreditOrderEntity creditOrderEntity = TradeAggregate.createCreditOrderEntity(
                tradeEntity.getUserId(),
                tradeEntity.getTradeName(),
                tradeEntity.getTradeType(),
                tradeEntity.getAmount(),
                tradeEntity.getOutBusinessNo());

        // 3. 构建消息对象
        CreditAdjustSuccessMessageEvent.CreditAdjustSuccessMessage creditAdjustSuccessMessage = CreditAdjustSuccessMessageEvent.CreditAdjustSuccessMessage.builder()
                .userId(tradeEntity.getUserId())
                .orderId(creditOrderEntity.getOrderId())
                .amount(tradeEntity.getAmount())
                .outBusinessNo(tradeEntity.getOutBusinessNo())
                .build();
        BaseEvent.EventMessage<CreditAdjustSuccessMessageEvent.CreditAdjustSuccessMessage> creditAdjustSuccessMessageEventMessage
                = creditAdjustSuccessMessageEvent.buildEventMessage(creditAdjustSuccessMessage);

        // 4. 构建任务对象
        TaskEntity taskEntity = TaskEntity.builder()
                .userId(tradeEntity.getUserId())
                .messageId(creditAdjustSuccessMessageEventMessage.getId())
                .exchange(creditAdjustSuccessMessageEvent.exchange())
                .routingKey(creditAdjustSuccessMessageEvent.routingKey())
                .queue(creditAdjustSuccessMessageEvent.queue())
                .message(creditAdjustSuccessMessageEventMessage)
                .state(TaskStateVO.create)
                .build();

        // 5. 构建交易聚合对象
        TradeAggregate tradeAggregate = TradeAggregate.builder()
                .userId(tradeEntity.getUserId())
                .creditAccountEntity(creditAccountEntity)
                .creditOrderEntity(creditOrderEntity)
                .taskEntity(taskEntity)
                .build();
        // 6. 保存积分交易订单
        creditRepository.saveUserCreditTradeOrder(tradeAggregate);
        log.info("增加账户积分额度完成 userId:{} orderId:{}", tradeEntity.getUserId(), creditOrderEntity.getOrderId());

        return creditOrderEntity.getOrderId();
    }

}
