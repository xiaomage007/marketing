package com.charlie.infrastructure.persistent.repository;

import cn.bugstack.middleware.db.router.strategy.IDBRouterStrategy;
import com.charlie.domain.activity.event.ActivitySkuStockZeroMessageEvent;
import com.charlie.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import com.charlie.domain.activity.model.entity.*;
import com.charlie.domain.activity.model.valobj.ActivitySkuStockKeyVO;
import com.charlie.domain.activity.model.valobj.ActivityStateVO;
import com.charlie.domain.activity.repository.IActivityRepository;
import com.charlie.infrastructure.mq.EventPublisher;
import com.charlie.infrastructure.persistent.dao.*;
import com.charlie.infrastructure.persistent.po.*;
import com.charlie.infrastructure.persistent.redis.IRedisService;
import com.charlie.types.common.Constants;
import com.charlie.types.enums.ResponseCode;
import com.charlie.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @description: 活动仓储服务
 * @author: Charlie
 * @date: 2026/8/16 15:37
 */
@Slf4j
@Repository
public class ActivityRepository implements IActivityRepository {

    @Resource
    private IRedisService redisService;
    @Resource
    private IRaffleActivityDao raffleActivityDao;
    @Resource
    private IRaffleActivitySkuDao raffleActivitySkuDao;
    @Resource
    private IRaffleActivityCountDao raffleActivityCountDao;
    @Resource
    private IRaffleActivityOrderDao raffleActivityOrderDao;
    @Resource
    private IRaffleActivityAccountDao raffleActivityAccountDao;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private IDBRouterStrategy dbRouter;
    @Resource
    private ActivitySkuStockZeroMessageEvent activitySkuStockZeroMessageEvent;
    @Resource
    private EventPublisher eventPublisher;

    /**
     * 根据 SKU 编号查询活动 SKU 详情。
     *
     * <p>直接走数据库查询（未走 Redis），因为 SKU 的剩余库存会被高频扣减，
     * 缓存一致性成本高于收益，调用方按需决定是否引入缓存。
     *
     * <p>典型调用示例：
     * <pre>{@code
     * ActivitySkuEntity sku = repository.queryActivitySku(9001L);
     * // sku.getStockCountSurplus() 即为该 SKU 的剩余可领数量
     * }</pre>
     *
     * @param sku 活动 SKU 主键（{@code raffle_activity_sku.sku}）
     * @return 活动 SKU 实体，包含活动 ID、活动次数 ID、库存总量与剩余库存
     */
    @Override
    public ActivitySkuEntity queryActivitySku(Long sku) {
        RaffleActivitySku raffleActivitySku = raffleActivitySkuDao.queryActivitySku(sku);
        return ActivitySkuEntity.builder()
                .sku(raffleActivitySku.getSku())
                .activityId(raffleActivitySku.getActivityId())
                .activityCountId(raffleActivitySku.getActivityCountId())
                .stockCount(raffleActivitySku.getStockCount())
                .stockCountSurplus(raffleActivitySku.getStockCountSurplus())
                .build();
    }

    /**
     * 根据活动 ID 查询活动基本信息（活动名称、时间窗、策略 ID、状态等）。
     *
     * <p>采用 Cache-Aside 模式：先读 Redis（{@code ACTIVITY_KEY + activityId}），
     * 未命中再查数据库并回写缓存。
     *
     * <p>典型调用示例：
     * <pre>{@code
     * ActivityEntity activity = repository.queryRaffleActivityByActivityId(100301L);
     * if (activity.getState() != ActivityStateVO.open) {
     *     throw new AppException(ResponseCode.ACTIVITY_STATE_ERROR);
     * }
     * }</pre>
     *
     * @param activityId 活动主键
     * @return 活动实体
     */
    @Override
    public ActivityEntity queryRaffleActivityByActivityId(Long activityId) {
        // 优先从缓存获取
        String cacheKey = Constants.RedisKey.ACTIVITY_KEY + activityId;
        ActivityEntity activityEntity = redisService.getValue(cacheKey);
        if (null != activityEntity) return activityEntity;
        // 从库中获取数据
        RaffleActivity raffleActivity = raffleActivityDao.queryRaffleActivityByActivityId(activityId);
        activityEntity = ActivityEntity.builder()
                .activityId(raffleActivity.getActivityId())
                .activityName(raffleActivity.getActivityName())
                .activityDesc(raffleActivity.getActivityDesc())
                .beginDateTime(raffleActivity.getBeginDateTime())
                .endDateTime(raffleActivity.getEndDateTime())
                .strategyId(raffleActivity.getStrategyId())
                .state(ActivityStateVO.valueOf(raffleActivity.getState()))
                .build();
        redisService.setValue(cacheKey, activityEntity);
        return activityEntity;
    }

    /**
     * 根据活动次数 ID 查询活动可参与次数配置（总次数 / 日次数 / 月次数）。
     *
     * <p>采用 Cache-Aside 模式：先读 Redis（{@code ACTIVITY_COUNT_KEY + activityCountId}），
     * 未命中再查数据库并回写缓存。
     *
     * <p>典型调用示例：
     * <pre>{@code
     * ActivityCountEntity count = repository.queryRaffleActivityCountByActivityCountId(401L);
     * // 校验用户日/月参与次数是否已用尽
     * if (userTodayUsed >= count.getDayCount()) {
     *     throw new AppException(ResponseCode.ACCOUNT_DAY_QUOTA_ERROR);
     * }
     * }</pre>
     *
     * @param activityCountId 活动次数配置主键
     * @return 活动次数配置实体
     */
    @Override
    public ActivityCountEntity queryRaffleActivityCountByActivityCountId(Long activityCountId) {
        // 优先从缓存获取
        String cacheKey = Constants.RedisKey.ACTIVITY_COUNT_KEY + activityCountId;
        ActivityCountEntity activityCountEntity = redisService.getValue(cacheKey);
        if (null != activityCountEntity) return activityCountEntity;
        // 从库中获取数据
        RaffleActivityCount raffleActivityCount = raffleActivityCountDao.queryRaffleActivityCountByActivityCountId(activityCountId);
        activityCountEntity = ActivityCountEntity.builder()
                .activityCountId(raffleActivityCount.getActivityCountId())
                .totalCount(raffleActivityCount.getTotalCount())
                .dayCount(raffleActivityCount.getDayCount())
                .monthCount(raffleActivityCount.getMonthCount())
                .build();
        redisService.setValue(cacheKey, activityCountEntity);
        return activityCountEntity;
    }

    /**
     * 保存下单聚合根：同时写入订单记录与用户活动账户。
     *
     * <p>处理流程：
     * <ol>
     *   <li>以 {@code userId} 作为分库路由键，保证订单与账户落在同一物理库，确保事务一致性；</li>
     *   <li>在编程式事务中先写订单，再用 {@code update} 累扣账户额度；</li>
     *   <li>若 {@code update} 影响 0 行（账户尚未初始化），则改为 {@code insert} 创建账户；</li>
     *   <li>捕获唯一索引冲突并回滚，对外抛 {@code INDEX_DUP} 业务异常。</li>
     * </ol>
     *
     * <p>典型调用示例：
     * <pre>{@code
     * CreateOrderAggregate agg = CreateOrderAggregate.builder()
     *         .userId("xiaomage")
     *         .activityId(100301L)
     *         .activityOrderEntity(orderEntity)
     *         .totalCount(1).dayCount(1).monthCount(1)
     *         .build();
     * repository.doSaveOrder(agg);
     * }</pre>
     *
     * @param createOrderAggregate 下单聚合根，含订单实体、用户 ID、活动 ID、本次消耗的额度
     * @throws com.charlie.types.exception.AppException 当用户/活动/SKU 已存在订单（唯一索引冲突）时抛出
     */
    @Override
    public void doSaveOrder(CreateQuotaOrderAggregate createOrderAggregate) {
        try {
            // 订单对象
            ActivityOrderEntity activityOrderEntity = createOrderAggregate.getActivityOrderEntity();
            RaffleActivityOrder raffleActivityOrder = new RaffleActivityOrder();
            raffleActivityOrder.setUserId(activityOrderEntity.getUserId());
            raffleActivityOrder.setSku(activityOrderEntity.getSku());
            raffleActivityOrder.setActivityId(activityOrderEntity.getActivityId());
            raffleActivityOrder.setActivityName(activityOrderEntity.getActivityName());
            raffleActivityOrder.setStrategyId(activityOrderEntity.getStrategyId());
            raffleActivityOrder.setOrderId(activityOrderEntity.getOrderId());
            raffleActivityOrder.setOrderTime(activityOrderEntity.getOrderTime());
            raffleActivityOrder.setTotalCount(activityOrderEntity.getTotalCount());
            raffleActivityOrder.setDayCount(activityOrderEntity.getDayCount());
            raffleActivityOrder.setMonthCount(activityOrderEntity.getMonthCount());
            raffleActivityOrder.setTotalCount(createOrderAggregate.getTotalCount());
            raffleActivityOrder.setDayCount(createOrderAggregate.getDayCount());
            raffleActivityOrder.setMonthCount(createOrderAggregate.getMonthCount());
            raffleActivityOrder.setState(activityOrderEntity.getState().getCode());
            raffleActivityOrder.setOutBusinessNo(activityOrderEntity.getOutBusinessNo());

            // 账户对象
            RaffleActivityAccount raffleActivityAccount = new RaffleActivityAccount();
            raffleActivityAccount.setUserId(createOrderAggregate.getUserId());
            raffleActivityAccount.setActivityId(createOrderAggregate.getActivityId());
            raffleActivityAccount.setTotalCount(createOrderAggregate.getTotalCount());
            raffleActivityAccount.setTotalCountSurplus(createOrderAggregate.getTotalCount());
            raffleActivityAccount.setDayCount(createOrderAggregate.getDayCount());
            raffleActivityAccount.setDayCountSurplus(createOrderAggregate.getDayCount());
            raffleActivityAccount.setMonthCount(createOrderAggregate.getMonthCount());
            raffleActivityAccount.setMonthCountSurplus(createOrderAggregate.getMonthCount());

            // 以用户ID作为切分键，通过 doRouter 设定路由【这样就保证了下面的操作，都是同一个链接下，也就保证了事务的特性】
            dbRouter.doRouter(createOrderAggregate.getUserId());
            // 编程式事务
            transactionTemplate.execute(status -> {
                try {
                    // 1. 写入订单
                    raffleActivityOrderDao.insert(raffleActivityOrder);
                    // 2. 更新账户
                    int count = raffleActivityAccountDao.updateAccountQuota(raffleActivityAccount);
                    // 3. 创建账户 - 更新为0，则账户不存在，创新新账户。
                    if (0 == count) {
                        raffleActivityAccountDao.insert(raffleActivityAccount);
                    }
                    return 1;
                } catch (DuplicateKeyException e) {
                    status.setRollbackOnly();
                    log.error("写入订单记录，唯一索引冲突 userId: {} activityId: {} sku: {}", activityOrderEntity.getUserId(), activityOrderEntity.getActivityId(), activityOrderEntity.getSku(), e);
                    throw new AppException(ResponseCode.INDEX_DUP.getCode());
                }
            });
        } finally {
            dbRouter.clear();
        }
    }

    /**
     * 将活动 SKU 的库存数加载到 Redis，作为后续扣减的初始值。
     *
     * <p>仅当 key 不存在时才写入，避免覆盖正在被并发扣减的实时库存。
     * 该方法通常在活动开启时由装配流程调用一次，用于「冷启动」库存计数器。
     *
     * <p>典型调用示例：
     * <pre>{@code
     * String cacheKey = Constants.RedisKey.ACTIVITY_SKU_STOCK_COUNT_KEY + sku;
     * repository.cacheActivitySkuStockCount(cacheKey, raffleActivitySku.getStockCountSurplus());
     * }</pre>
     *
     * @param cacheKey   Redis 计数 key，通常为 {@code ACTIVITY_SKU_STOCK_COUNT_KEY + sku}
     * @param stockCount 库存初始值（一般是数据库的剩余库存）
     */
    @Override
    public void cacheActivitySkuStockCount(String cacheKey, Integer stockCount) {
        if (redisService.isExists(cacheKey)) return;
        redisService.setAtomicLong(cacheKey, stockCount);
    }

    /**
     * 原子扣减 Redis 中的 SKU 库存，并在扣减为 0 时通知下游持久化。
     *
     * <p>核心逻辑：
     * <ul>
     *   <li>{@code DECR} 后 surplus == 0：发送「库存清零」MQ 消息，触发数据库库存更新；</li>
     *   <li>surplus &lt; 0：说明存在并发超额，回填为 0 防止负数扩散；</li>
     *   <li>surplus &gt; 0：对该剩余值加分布式锁（{@code cacheKey_UNDERLINE_surplus}），
     *       用于驱动后续延迟队列消费逻辑（同一剩余区间只消费一次），锁的过期时间延伸到活动结束次日。</li>
     * </ul>
     *
     * <p>典型调用示例：
     * <pre>{@code
     * boolean locked = repository.subtractionActivitySkuStock(
     *         9001L,
     *         Constants.RedisKey.ACTIVITY_SKU_STOCK_COUNT_KEY + 9001L,
     *         activity.getEndDateTime());
     * if (!locked) {
     *     // 已被其他线程抢先消费，无需重复入队
     *     return;
     * }
     * }</pre>
     *
     * <h3>具体执行实例</h3>
     *
     * <p>假设装配阶段已通过 {@link #cacheActivitySkuStockCount(String, Integer)} 将
     * SKU {@code 9001L} 的初始库存（{@code stockCount=5}）写入 Redis：
     * <pre>{@code
     * redis 127.0.0.1:16379> GET activity_sku_stock_count_key9001
     * "5"
     * }</pre>
     *
     * <p><b>实例 A：surplus == 0（最后一次扣减成功，归零通知）</b><br>
     * 线程 T1 第 5 次扣减：
     * <pre>{@code
     * DECR activity_sku_stock_count_key9001  -> 返回 0
     * }</pre>
     * 进入分支②：发布 {@code activity_sku_stock_zero} 事件到 MQ（topic 由
     * {@code activitySkuStockZeroMessageEvent.topic()} 提供），消费者收到后落库
     * {@code raffle_activity_sku.stock_count_surplus = 0}；{@code return false}，
     * 调用方（{@code ActivitySkuStockActionChain}）不再入延迟队列。
     *
     * <p><b>实例 B：surplus < 0（超卖兜底）</b><br>
     * 库存已经为 0，但线程 T2 继续扣减：
     * <pre>{@code
     * DECR activity_sku_stock_count_key9001  -> 返回 -1
     * }</pre>
     * 进入分支③：{@code SET activity_sku_stock_count_key9001 0} 把 key 校正回 0，
     * 避免负数污染后续 DECR 结果；{@code return false}，调用方放弃。
     *
     * <p><b>实例 C：surplus > 0（抢分段锁，驱动延迟队列）</b><br>
     * 线程 T3 第 2 次扣减：
     * <pre>{@code
     * DECR activity_sku_stock_count_key9001  -> 返回 3
     * }</pre>
     * 进入分支④：
     * <ul>
     *   <li>{@code lockKey = activity_sku_stock_count_key9001_3}</li>
     *   <li>假设 {@code endDateTime = 2026-08-25 00:00:00}，则
     *       {@code expireMillis ≈ (endDateTime - now) + 1 day}，锁会在活动结束次日自动释放</li>
     *   <li>{@code SETNX lockKey EXPIRE_MILLIS} 抢锁成功：{@code return true}，
     *       调用方把 {@code sku + activityId} 投递到延迟队列（3 秒后消费、按当前剩余值落库），
     *       同一剩余值 {@code 3} 在锁过期前不会再被其他线程重复入队</li>
     *   <li>抢锁失败：记日志 {@code "活动sku库存加锁失败 ..."} 并 {@code return false}，
     *       调用方放弃本次入队，避免重复消费同一剩余区间</li>
     * </ul>
     *
     * @param sku         活动 SKU 主键
     * @param cacheKey    Redis 库存计数 key
     * @param endDateTime 活动结束时间，用于推导分布式锁的过期时间
     * @return true 表示获取到分段锁（当前剩余值首次出现），false 表示扣减到 0 或锁竞争失败
     */
    @Override
    public boolean subtractionActivitySkuStock(Long sku, String cacheKey, Date endDateTime) {
        long surplus = redisService.decr(cacheKey);
        if (surplus == 0) {
            // 库存消耗没了以后，发送MQ消息，更新数据库库存
            eventPublisher.publish(activitySkuStockZeroMessageEvent, sku);
            return false;
        } else if (surplus < 0) {
            redisService.setAtomicLong(cacheKey, 0);
            return false;
        }
        String lockKey = cacheKey + Constants.UNDERLINE + surplus;
        long expireMillis = endDateTime.getTime() - System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1);
        Boolean lock = redisService.setNx(lockKey, expireMillis, TimeUnit.MILLISECONDS);
        if (!lock) {
            log.info("活动sku库存加锁失败 {}", lockKey);
        }
        return lock;
    }

    /**
     * 将 SKU 库存扣减记录投递到 Redis 延迟队列，延迟 3 秒消费。
     *
     * <p>消费端会按扣减后的剩余值执行数据库层面的库存更新。使用延迟队列是为了把高频
     * Redis 扣减合并为少量数据库写，削峰填谷。
     *
     * <p>典型调用示例：
     * <pre>{@code
     * repository.activitySkuStockConsumeSendQueue(
     *         ActivitySkuStockKeyVO.builder()
     *                 .sku(9001L)
     *                 .activityId(100301L)
     *                 .surplusCount(98)
     *                 .build());
     * }</pre>
     *
     * @param activitySkuStockKeyVO 队列消息体，含 SKU、活动 ID、扣减后的剩余库存
     */
    @Override
    public void activitySkuStockConsumeSendQueue(ActivitySkuStockKeyVO activitySkuStockKeyVO) {
        // 队列在 Redis 中的 Key 名，定义在 Constants.RedisKey 中
        // 生产者（这里）和消费者（库存更新监听器）必须使用同一个 Key 才能完成投递-消费闭环
        String cacheKey = Constants.RedisKey.ACTIVITY_SKU_COUNT_QUERY_KEY;

        // 1) 拿到阻塞队列的「句柄」
        //    - 底层数据结构：Redis List（不是 Set，不是 Hash，是有序可重复的列表）
        //    - 返回类型：RBlockingQueue<T>，是 Redisson 的远程代理对象（不是本地集合）
        //    - 这一步并不发起任何网络请求到 Redis，只是构造一个绑定到 cacheKey 的代理
        //    - 同 key 多次调用返回同一实例（Redisson 内部用 refcount 复用连接，避免重复建连）
        //    - 后续 offer()/take() 才会真正走 Redis：take() 底层用 BLPOP，空队列时阻塞等待
        RBlockingQueue<ActivitySkuStockKeyVO> blockingQueue = redisService.getBlockingQueue(cacheKey);

        // 2) 基于上面那个 BlockingQueue 构造延迟队列「视图」
        //    - 延迟队列本身并不存数据，它只是「调度器」+「目标 BlockingQueue」的封装
        //    - 底层实现：Redisson 用一个 zset 记录「到期时间戳 -> 消息」的映射，到期后由
        //      Redisson 内部 scheduler 线程把消息从 zset 搬到目标 BlockingQueue
        //    - 所以这里必须传 blockingQueue 作为"目的地"，否则 offer 后没人能 take 出来
        RDelayedQueue<ActivitySkuStockKeyVO> delayedQueue = redisService.getDelayedQueue(blockingQueue);

        // 3) 投递一条扣减消息，3 秒后才在 BlockingQueue 中可见
        //    - 这里 offer 立刻返回（不阻塞），真正的"延迟"由 Redisson 后台线程负责
        //    - 3 秒后消息自动入队 BlockingQueue，等待消费端 take() 后更新数据库库存
        //    - 为什么用 3 秒：扣减高峰时多笔相同 surplus 的消息会合并（消费者按 surplus 去重更新），
        //      兼顾「DB 写入及时性」与「削峰聚合效果」
        //    - 注意：这里入的是「延迟队列」而非「阻塞队列」，别误写成 blockingQueue.offer
        delayedQueue.offer(activitySkuStockKeyVO, 3, TimeUnit.SECONDS);
    }

    @Override
    public ActivitySkuStockKeyVO takeQueueValue() {
        String cacheKey = Constants.RedisKey.ACTIVITY_SKU_COUNT_QUERY_KEY;
        RBlockingQueue<ActivitySkuStockKeyVO> destinationQueue = redisService.getBlockingQueue(cacheKey);
        return destinationQueue.poll();
    }

    @Override
    public void updateActivitySkuStock(Long sku) {
        raffleActivitySkuDao.updateActivitySkuStock(sku);
    }

    @Override
    public void clearActivitySkuStock(Long sku) {
        raffleActivitySkuDao.clearActivitySkuStock(sku);
    }

    @Override
    public void clearQueueValue() {
        String cacheKey = Constants.RedisKey.ACTIVITY_SKU_COUNT_QUERY_KEY;
        RBlockingQueue<ActivitySkuStockKeyVO> destinationQueue = redisService.getBlockingQueue(cacheKey);
        destinationQueue.clear();
    }

    @Override
    public UserRaffleOrderEntity queryNoUsedRaffleOrder(PartakeRaffleActivityEntity partakeRaffleActivityEntity) {
        return null;
    }

}
