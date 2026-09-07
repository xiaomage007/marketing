package com.charlie.trigger.http;

import com.charlie.api.IRaffleActivityService;
import com.charlie.api.dto.ActivityDrawRequestDTO;
import com.charlie.api.dto.ActivityDrawResponseDTO;
import com.charlie.api.response.Response;
import com.charlie.domain.activity.model.entity.UserRaffleOrderEntity;
import com.charlie.domain.activity.service.IRaffleActivityPartakeService;
import com.charlie.domain.activity.service.armory.IActivityArmory;
import com.charlie.domain.award.model.entity.UserAwardRecordEntity;
import com.charlie.domain.award.model.valobj.AwardStateVO;
import com.charlie.domain.award.service.IAwardService;
import com.charlie.domain.strategy.model.entity.RaffleAwardEntity;
import com.charlie.domain.strategy.model.entity.RaffleFactorEntity;
import com.charlie.domain.strategy.service.IRaffleStrategy;
import com.charlie.domain.strategy.service.armory.IStrategyArmory;
import com.charlie.types.enums.ResponseCode;
import com.charlie.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @description 抽奖活动服务 注意；在不引用 application/case 层的时候，就需要让接口实现层来做领域的串联。一些较大规模的系统，需要加入 case 层。
 * @author: Charlie
 * @date: 2026/8/31 10:24
 */
@Slf4j
@RestController()
@CrossOrigin("${app.config.cross-origin}")
@RequestMapping("/api/${app.config.api-version}/raffle/activity/")
public class RaffleActivityController implements IRaffleActivityService {

    @Resource
    private IRaffleActivityPartakeService raffleActivityPartakeService;
    @Resource
    private IRaffleStrategy raffleStrategy;
    @Resource
    private IAwardService awardService;
    @Resource
    private IActivityArmory activityArmory;
    @Resource
    private IStrategyArmory strategyArmory;

    /**
     * 活动装配 - 数据预热 | 把活动配置的对应的 sku 一起装配
     *
     * @param activityId 活动ID
     * @return 装配结果
     * <p>
     * 接口：<a href="http://localhost:8091/api/v1/raffle/activity/armory">/api/v1/raffle/activity/armory</a>
     * 入参：{"activityId":100001,"userId":"Charlie"}
     * <p>
     * curl --request GET \
     * --url 'http://localhost:8091/api/v1/raffle/activity/armory?activityId=100301'
     */
    @RequestMapping(value = "armory", method = RequestMethod.GET)
    @Override
    public Response<Boolean> armory(Long activityId) {
        try {
            log.info("活动装配，数据预热，开始 activityId:{}", activityId);
            // 1. 活动装配
            // 详细：根据活动ID查询活动下配置的全部sku，把每个sku的剩余库存写入Redis库存计数器，
            //      同时把活动信息、活动次数配置预热到缓存，供后续参与抽奖时扣减库存与额度校验使用
            // 举例：activityId=100301下配置sku=9011，装配后Redis写入该sku的库存计数key，
            //      每次抽奖通过decr原子扣减1，扣到0则该sku库存售罄，无法继续参与
            activityArmory.assembleActivitySkuByActivityId(activityId);
            // 2. 策略装配
            // 详细：先根据活动ID查询关联的策略ID，再把策略下全部奖品按概率展开为O(1)命中的概率查找表写入Redis
            //      （String存概率总区间，Hash存「索引->奖品ID」），同时预热各奖品库存缓存；
            //      若策略配置了rule_weight权重规则，还会按权重档位装配概率子表
            // 举例：activityId=100301对应strategyId=100006，奖品按概率占比展开为多个槽位的Hash表，
            //      抽奖时随机取一个索引HGET即得奖品ID，如索引命中返回awardId=101「随机积分」
            strategyArmory.assembleLotteryStrategyByActivityId(activityId);
            Response<Boolean> response = Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(true)
                    .build();
            log.info("活动装配，数据预热，完成 activityId:{}", activityId);
            return response;
        } catch (Exception e) {
            log.error("活动装配，数据预热，失败 activityId:{}", activityId, e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    /**
     * 抽奖接口
     *
     * @param request 请求对象
     * @return 抽奖结果
     * <p>
     * 接口：<a href="http://localhost:8091/api/v1/raffle/activity/draw">/api/v1/raffle/activity/draw</a>
     * 入参：{"activityId":100001,"userId":"Charlie"}
     * <p>
     * curl --request POST \
     * --url http://localhost:8091/api/v1/raffle/activity/draw \
     * --header 'content-type: application/json' \
     * --data '{
     * "userId":"Charlie",
     * "activityId": 100301
     * }'
     */
    @RequestMapping(value = "draw", method = RequestMethod.POST)
    @Override
    public Response<ActivityDrawResponseDTO> draw(ActivityDrawRequestDTO request) {
        try {
            log.info("活动抽奖 userId:{} activityId:{}", request.getUserId(), request.getActivityId());
            // 1. 参数校验
            // 详细：校验用户ID非空、活动ID非空，任一不满足则抛出参数异常，由下方catch捕获后返回错误码
            // 举例：入参{"userId":"","activityId":100301}，userId为空串，返回code=ILLEGAL_PARAMETER的响应
            if (StringUtils.isBlank(request.getUserId()) || null == request.getActivityId()) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
            }
            // 2. 参与活动 - 创建参与记录订单
            // 详细：领域服务内部先校验活动状态（须为open）与活动日期（当前时间须在起止时间内），
            //      再查询该用户是否存在未使用的参与订单，存在则直接复用返回（保证幂等，不重复扣减额度）；
            //      不存在则做额度账户过滤（扣减用户可用抽奖次数）、构建新订单，与额度扣减在同一事务内落库
            // 举例：userId=Charlie、activityId=100301首次抽奖，额度账户扣1次并生成新订单返回orderId；
            //      若本次抽奖后续流程中断，再次请求会命中未使用订单，直接返回原orderId，不重复扣额度
            UserRaffleOrderEntity orderEntity = raffleActivityPartakeService.createOrder(request.getUserId(), request.getActivityId());
            log.info("活动抽奖，创建订单 userId:{} activityId:{} orderId:{}", request.getUserId(), request.getActivityId(), orderEntity.getOrderId());
            // 3. 抽奖策略 - 执行抽奖
            // 详细：以参与订单中的userId与strategyId构建抽奖因子，领域服务先做抽奖前规则过滤
            //      （黑名单用户直接返回固定兜底奖品，权重规则按用户积分走对应概率子表），
            //      未被规则接管则走默认概率查找表随机抽奖，返回中奖奖品实体（含奖品ID、名称、排序）
            // 举例：userId=user001命中黑名单规则，直接返回固定兜底奖品，不走概率抽奖；
            //      正常用户在strategyId=100006概率表中随机命中，如返回awardId=101「随机积分」
            RaffleAwardEntity raffleAwardEntity = raffleStrategy.performRaffle(RaffleFactorEntity.builder()
                    .userId(orderEntity.getUserId())
                    .strategyId(orderEntity.getStrategyId())
                    .endDateTime(orderEntity.getEndDateTime())
                    .build());
            // 4. 存放结果 - 写入中奖记录
            // 详细：用参与订单信息与中奖结果构建用户中奖记录，领域服务内部同时构建一条发奖MQ任务，
            //      两者组装成聚合对象在同一事务内落库，后续由MQ消费或定时任务补偿完成异步发货
            // 举例：orderId=xxx命中awardId=101，中奖记录表插入一条create状态的中奖记录，
            //      任务表同时插入一条指向发奖队列的任务记录，MQ发送成功后任务状态更新为完成
            UserAwardRecordEntity userAwardRecord = UserAwardRecordEntity.builder()
                    .userId(orderEntity.getUserId())
                    .activityId(orderEntity.getActivityId())
                    .strategyId(orderEntity.getStrategyId())
                    .orderId(orderEntity.getOrderId())
                    .awardId(raffleAwardEntity.getAwardId())
                    .awardTitle(raffleAwardEntity.getAwardTitle())
                    .awardTime(new Date())
                    .awardState(AwardStateVO.create)
                    .build();
            awardService.saveUserAwardRecord(userAwardRecord);
            // 5. 返回结果
            // 详细：把中奖奖品ID、奖品名称、奖品排序封装为响应DTO，以成功码返回给前端
            // 举例：返回{"code":"0000","info":"成功","data":{"awardId":101,"awardTitle":"随机积分","awardIndex":1}}
            return Response.<ActivityDrawResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(ActivityDrawResponseDTO.builder()
                            .awardId(raffleAwardEntity.getAwardId())
                            .awardTitle(raffleAwardEntity.getAwardTitle())
                            .awardIndex(raffleAwardEntity.getSort())
                            .build())
                    .build();
        } catch (AppException e) {
            log.error("活动抽奖失败 userId:{} activityId:{}", request.getUserId(), request.getActivityId(), e);
            return Response.<ActivityDrawResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("活动抽奖失败 userId:{} activityId:{}", request.getUserId(), request.getActivityId(), e);
            return Response.<ActivityDrawResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

}
