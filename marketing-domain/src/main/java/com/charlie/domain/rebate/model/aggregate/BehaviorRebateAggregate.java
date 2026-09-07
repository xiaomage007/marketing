package com.charlie.domain.rebate.model.aggregate;

import com.charlie.domain.rebate.model.entity.BehaviorRebateOrderEntity;
import com.charlie.domain.rebate.model.entity.TaskEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description:
 * @author: Charlie
 * @date: 2026/9/7 10:45
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BehaviorRebateAggregate {

    /**
     * 用户ID
     */
    private String userId;
    /**
     * 行为返利订单实体对象
     */
    private BehaviorRebateOrderEntity behaviorRebateOrderEntity;
    /**
     * 任务实体对象
     */
    private TaskEntity taskEntity;

}
