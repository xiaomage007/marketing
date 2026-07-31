package com.charlie.infrastructure.persistent.redis;

import org.redisson.api.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;

/**
 * Redis 服务 - Redisson 实现
 * <p>
 * 本类是对 RedissonClient 的薄封装,提供 Redis 常见数据结构(KV / Hash / List / Set / SortedSet / 分布式锁 / 信号量 / 闭锁 / 布隆过滤器 等)
 * 的统一访问入口。所有 key 均不做前缀拼接,调用方需自行保证 key 全局唯一,推荐做法是在 {@code Constants.RedisKey} 中集中维护。
 *
 * <h3>通用说明</h3>
 * <ul>
 *   <li>底层使用 {@link RBucket} / {@link RMap} / {@link RAtomicLong} 等 Redisson 高级对象,自带本地缓存、Lua 脚本、连接池等能力</li>
 *   <li>所有"自增/自减/分布式锁"操作均为线程安全(Lua 单线程执行),可放心用于库存扣减、抢锁等场景</li>
 *   <li>序列化方式由 {@code RedisClientConfig} 决定,默认 {@code JsonJacksonCodec}——存对象时无需实现 Serializable</li>
 * </ul>
 *
 * @author Charlie
 */
@Service("redissonService")
public class RedissonService implements IRedisService {

    @Resource
    private RedissonClient redissonClient;

    /**
     * 设置普通 KV(无过期时间)。
     * <p>
     * 底层对应 Redis {@code SET key value},包装为 {@link RBucket} 对象。<b>不会</b>覆盖已存在的 TTL——
     * 如果目标 key 之前被 {@link #setValue(String, Object, long)} 设置过 TTL,本次 set 后 TTL 会被清除(key 变成永不过期)。
     *
     * @param key   键,如 {@code "user:token:abc123"}
     * @param value 值,任意类型,序列化后存入 Redis
     * @see #setValue(String, Object, long)
     */
    public <T> void setValue(String key, T value) {
        redissonClient.<T>getBucket(key).set(value);
    }

    /**
     * 设置带过期时间的 KV。
     * <p>
     * 底层 Redis 命令 {@code SET key value PX <expired>},过期时间 {@code expired} 单位为<b>毫秒</b>。
     * 过期后 Redis 自动删除 key,下次 {@link #getValue(String)} 返回 null。
     *
     * @param key     键
     * @param value   值
     * @param expired 过期时间,单位<b>毫秒</b>(注意不是秒)。例如 5 分钟 = {@code 5 * 60 * 1000}
     * @throws IllegalArgumentException 若 expired 为负数,Redisson 会抛异常
     *
     * <h4>使用示例:短信验证码 5 分钟过期</h4>
     * <pre>{@code
     * redissonService.setValue("sms:code:13800138000", "6523", 5 * 60 * 1000L);
     * // 5 分钟后该 key 自动消失,验证码失效
     * }</pre>
     */
    @Override
    public <T> void setValue(String key, T value, long expired) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        bucket.set(value, Duration.ofMillis(expired));
    }

    /**
     * 读取 KV。
     * <p>
     * 底层 Redis 命令 {@code GET key}。key 不存在或已过期返回 {@code null}(不会抛异常)。
     * 反序列化时若对象结构与序列化时不兼容(常见于类字段增减),会抛出运行时异常——生产环境建议 {@code try/catch} 兜底。
     *
     * @param key 键
     * @param <T> 期望的返回类型(由调用方保证类型安全)
     * @return 值,不存在返回 {@code null}
     */
    public <T> T getValue(String key) {
        return redissonClient.<T>getBucket(key).get();
    }

    /**
     * 获取非阻塞队列 {@link RQueue}。
     * <p>
     * 等价于 Redis {@code List},但提供了丰富的 Java 集合 API。{@code poll()} 不阻塞,无元素返回 null。
     *
     * @param key 队列 key,如 {@code "queue:async:order"}
     * @param <T> 元素类型
     * @return 队列对象
     *
     * <h4>使用示例:异步任务队列</h4>
     * <pre>{@code
     * RQueue<Task> queue = redissonService.getQueue("queue:async:order");
     * queue.offer(new Task(...));           // 入队
     * Task task = queue.poll();             // 非阻塞出队,无元素返回 null
     * }</pre>
     */
    @Override
    public <T> RQueue<T> getQueue(String key) {
        return redissonClient.getQueue(key);
    }

    /**
     * 获取阻塞队列 {@link RBlockingQueue}。
     * <p>
     * 底层 Redis {@code List + BLPOP/BRPOP}。{@code take()} 在队列空时会<b>阻塞</b>当前线程直到有新元素入队,
     * 适合"消费者空转浪费 CPU"的场景。
     *
     * @param key 队列 key
     * @param <T> 元素类型
     * @return 阻塞队列
     *
     * <h4>使用示例:生产者-消费者</h4>
     * <pre>{@code
     * // 消费者线程
     * new Thread(() -> {
     *     RBlockingQueue<Task> q = redissonService.getBlockingQueue("queue:async:order");
     *     while (running) {
     *         Task task = q.take();   // 没有元素就阻塞,不会空转
     *         handle(task);
     *     }
     * }).start();
     * }</pre>
     */
    @Override
    public <T> RBlockingQueue<T> getBlockingQueue(String key) {
        return redissonClient.getBlockingQueue(key);
    }

    /**
     * 获取延迟队列 {@link RDelayedQueue}。
     * <p>
     * <b>必须基于一个已有的 {@link RBlockingQueue}</b>——延迟队列本身只是个"调度器",
     * 元素到期后会自动"搬运"到目标 BlockingQueue 中供消费者消费。
     * 底层使用 {@code zset + 轮询} 实现,精度为毫秒级,支持大量延迟任务(单节点 10w+ 任务无压力)。
     *
     * @param rBlockingQueue 目标阻塞队列,延迟元素到期后会移入此队列
     * @param <T>            元素类型
     * @return 延迟队列
     *
     * <h4>使用示例:订单 30 分钟未支付自动关闭</h4>
     * <pre>{@code
     * RBlockingQueue<Order> backing = redissonService.getBlockingQueue("queue:order:close");
     * RDelayedQueue<Order> delayed = redissonService.getDelayedQueue(backing);
     *
     * // 下单时投递
     * delayed.offer(order, 30, TimeUnit.MINUTES);   // 30 分钟后会自动出现在 backing 中
     *
     * // 消费者
     * Order expired = backing.take();
     * closeOrder(expired);
     * }</pre>
     */
    @Override
    public <T> RDelayedQueue<T> getDelayedQueue(RBlockingQueue<T> rBlockingQueue) {
        return redissonClient.getDelayedQueue(rBlockingQueue);
    }

    /**
     * 原子自增 1。
     * <p>
     * 底层 {@link RAtomicLong},通过 Redis Lua 脚本保证原子性,并发安全。
     * key 不存在时会自动初始化为 {@code 0},自增后变为 {@code 1}。
     *
     * @param key 键
     * @return 自增后的值
     *
     * <h4>使用示例:文章阅读量计数</h4>
     * <pre>{@code
     * long views = redissonService.incr("article:views:10086");
     * // 并发 1w 个请求阅读同一篇文章,views 一定准确等于 1w
     * }</pre>
     */
    @Override
    public long incr(String key) {
        return redissonClient.getAtomicLong(key).incrementAndGet();
    }

    /**
     * 原子增加指定步长(可正可负)。
     * <p>
     * 与 {@link #incr(String)} 区别:本方法可指定增量值。{@code delta} 为负时等价于 {@link #decrBy(String, long)}。
     *
     * @param key   键
     * @param delta 增量值,正数=加,负数=减
     * @return 增加后的值
     *
     * <h4>使用示例:用户积分变动</h4>
     * <pre>{@code
     * redissonService.incrBy("user:score:u001", 100);   // +100 分
     * redissonService.incrBy("user:score:u001", -50);   // -50 分
     * }</pre>
     */
    @Override
    public long incrBy(String key, long delta) {
        return redissonClient.getAtomicLong(key).addAndGet(delta);
    }

    /**
     * 原子自减 1。
     *
     * @param key 键
     * @return 自减后的值
     *
     * <h4>使用示例:抽奖剩余次数</h4>
     * <pre>{@code
     * long remain = redissonService.decr("raffle:remain:user:u001");
     * if (remain < 0) {
     *     throw new AppException(ResponseCode.RAFFLE_NO_TIMES);
     * }
     * }</pre>
     */
    @Override
    public long decr(String key) {
        return redissonClient.getAtomicLong(key).decrementAndGet();
    }

    /**
     * 原子减少指定步长。
     * <p>
     * 用于库存扣减等关键场景。注意:<b>本方法不做"扣到 0 就拒绝"的兜底</b>,
     * 业务层需要在调用后判断返回值是否小于 0 并回滚(或配合 Lua 脚本做"compare and swap")。
     *
     * @param key   键
     * @param delta 减少量,必须为正数
     * @return 自减后的值(可能为负数)
     *
     * <h4>使用示例:秒杀库存扣减(配合 Lua 保证不超卖)</h4>
     * <pre>{@code
     * long remain = redissonService.decrBy("seckill:stock:sku:10086", 1);
     * if (remain < 0) {
     *     redissonService.incrBy("seckill:stock:sku:10086", 1);   // 回滚
     *     throw new AppException(ResponseCode.SOLD_OUT);
     * }
     * }</pre>
     */
    @Override
    public long decrBy(String key, long delta) {
        return redissonClient.getAtomicLong(key).addAndGet(-delta);
    }

    /**
     * 删除指定 key。
     * <p>
     * 底层 Redis {@code DEL key}。key 不存在时静默返回,不会抛异常。
     *
     * @param key 键
     */
    @Override
    public void remove(String key) {
        redissonClient.getBucket(key).delete();
    }

    /**
     * 判断 key 是否存在。
     * <p>
     * 底层 Redis {@code EXISTS key}。注意:对 {@code Hash / List / Set / SortedSet} 中的元素不适用,
     * 本方法仅判断 key 这个"容器"是否存在。
     *
     * @param key 键
     * @return true=存在,false=不存在
     */
    @Override
    public boolean isExists(String key) {
        return redissonClient.getBucket(key).isExists();
    }

    /**
     * 向 Set 集合中添加元素。
     * <p>
     * 底层 Redis {@code SADD key value}。元素已存在则自动去重。
     *
     * @param key   Set 的 key
     * @param value 待添加的元素
     *
     * <h4>使用示例:用户标签</h4>
     * <pre>{@code
     * redissonService.addToSet("user:tag:u001", "VIP");
     * redissonService.addToSet("user:tag:u001", "新用户");
     * redissonService.addToSet("user:tag:u001", "VIP");    // 重复添加无副作用
     * }</pre>
     */
    public void addToSet(String key, String value) {
        RSet<String> set = redissonClient.getSet(key);
        set.add(value);
    }

    /**
     * 判断 value 是否是 Set 的成员。
     * <p>
     * 底层 Redis {@code SISMEMBER key value}。
     *
     * @param key   Set 的 key
     * @param value 待判断的元素
     * @return true=是成员,false=不是成员(Set 不存在时也返回 false)
     *
     * <h4>使用示例:判断用户是否参与过某活动</h4>
     * <pre>{@code
     * if (redissonService.isSetMember("activity:joined:1001", "u001")) {
     *     throw new AppException(ResponseCode.HAS_JOINED);
     * }
     * }</pre>
     */
    public boolean isSetMember(String key, String value) {
        RSet<String> set = redissonClient.getSet(key);
        return set.contains(value);
    }

    /**
     * 向 List 末尾追加元素(等价于 {@code RPUSH})。
     *
     * @param key   List 的 key
     * @param value 待追加的元素
     *
     * <h4>使用示例:最近浏览记录(只保留最近 N 条)</h4>
     * <pre>{@code
     * redissonService.addToList("user:browse:u001", "商品A");
     * redissonService.addToList("user:browse:u001", "商品B");
     * RList<String> list = redissonService.getQueue("user:browse:u001");
     * list.trim(list.size() - 10, list.size() - 1);   // 只保留最近 10 条
     * }</pre>
     */
    public void addToList(String key, String value) {
        RList<String> list = redissonClient.getList(key);
        list.add(value);
    }

    /**
     * 按索引读取 List 元素(0-based)。
     * <p>
     * 底层 Redis {@code LINDEX key index}。负数索引支持 {@code -1=最后一个, -2=倒数第二个}。
     *
     * @param key   List 的 key
     * @param index 索引,从 0 开始
     * @return 元素值;索引越界返回 null
     *
     * <h4>使用示例:取最新一条消息</h4>
     * <pre>{@code
     * String latest = redissonService.getFromList("chat:msg:room:1", -1);
     * }</pre>
     */
    public String getFromList(String key, int index) {
        RList<String> list = redissonClient.getList(key);
        return list.get(index);
    }

    /**
     * 获取 {@link RMap} 高级对象(Hash 结构 + 本地缓存)。
     * <p>
     * <b>与普通 Redis Hash 的区别:</b>RMap 默认会在本地 JVM 缓存 entries,读取时优先命中本地,
     * 减少 Redis 网络往返——非常适合"读多写少"的场景(如策略概率表)。
     *
     * @param key Map 的 key
     * @param <K> 字段类型
     * @param <V> 值类型
     * @return RMap 实例,可直接当 {@code Map<K, V>} 使用
     *
     * <h4>使用示例:策略概率表查询</h4>
     * <pre>{@code
     * RMap<Integer, Integer> rateTable = redissonService.getMap("strategy:rate:table:100001");
     * Integer awardId = rateTable.get(randomIndex);
     * }</pre>
     */
    @Override
    public <K, V> RMap<K, V> getMap(String key) {
        return redissonClient.getMap(key);
    }

    /**
     * 向 Map 中放一个 KV(等价于 {@code HSET key field value})。
     *
     * @param key   Map 的 key
     * @param field 字段
     * @param value 值
     */
    public void addToMap(String key, String field, String value) {
        RMap<String, String> map = redissonClient.getMap(key);
        map.put(field, value);
    }

    /**
     * 从 Map 中取一个字段(等价于 {@code HGET key field})。
     *
     * @param key   Map 的 key
     * @param field 字段
     * @return 字段值,不存在返回 null
     */
    public String getFromMap(String key, String field) {
        RMap<String, String> map = redissonClient.getMap(key);
        return map.get(field);
    }

    /**
     * 从 Map 中取一个字段(泛型版本,可指定 K 的实际类型)。
     *
     * @param key   Map 的 key
     * @param field 字段,类型由 K 决定
     * @param <K>   字段类型
     * @param <V>   值类型
     * @return 字段值
     */
    @Override
    public <K, V> V getFromMap(String key, K field) {
        return redissonClient.<K, V>getMap(key).get(field);
    }

    /**
     * 向有序集合(ZSet)中加一个成员(默认 score = 当前时间戳毫秒)。
     * <p>
     * <b>注意:</b>本方法未显式传 score,Redisson 默认按"自然顺序"——{@link String} 实现下是字典序,通常不是想要的"按时间排序"。
     * 业务上一般用 {@code getSortedSet(key).add(score, value)} 来自定义权重。
     *
     * @param key   ZSet 的 key
     * @param value 成员
     *
     * <h4>使用示例:排行榜(按分数排序)</h4>
     * <pre>{@code
     * RSortedSet<Integer> rank = redissonClient.getSortedSet("rank:score:room:1");
     * rank.add(95, 1001);   // 用户 1001 分数 95
     * rank.add(88, 1002);   // 用户 1002 分数 88
     * // 取分数前三
     * List<Integer> top3 = rank.entryRange(0, 2);
     * }</pre>
     */
    public void addToSortedSet(String key, String value) {
        RSortedSet<String> sortedSet = redissonClient.getSortedSet(key);
        sortedSet.add(value);
    }

    /**
     * 获取可重入锁 {@link RLock}。
     * <p>
     * <b>特性</b>:同一个线程在已持有锁的情况下可再次获取(计数器 +1),不会死锁;解锁也需调用相同次数。
     * <b>非公平</b>:多个线程竞争时,谁先抢到谁拿到,不保证 FIFO。
     *
     * @param key 锁的 key,推荐用资源 id 拼接,如 {@code "lock:award:stock:10086"}
     * @return Lock 对象,需手动 {@code lock() / unlock()}(通常 try/finally 包裹)
     *
     * <h4>使用示例:抽奖扣库存同步</h4>
     * <pre>{@code
     * RLock lock = redissonService.getLock("lock:strategy:100001");
     * lock.lock();
     * try {
     *     // 扣库存逻辑,防止并发超卖
     *     strategyArmory.subtractAwardStock(strategyId, awardId);
     * } finally {
     *     lock.unlock();
     * }
     * }</pre>
     */
    @Override
    public RLock getLock(String key) {
        return redissonClient.getLock(key);
    }

    /**
     * 获取公平锁 {@link RLock}({@link #getLock(String)} 的公平版本)。
     * <p>
     * 与普通锁区别:公平锁内部维护一个 FIFO 队列,线程按申请顺序排队获取锁,避免"饥饿"。
     * 代价是吞吐量比普通锁略低——<b>只在确实需要排队语义的场景使用</b>(如秒杀队列)。
     */
    @Override
    public RLock getFairLock(String key) {
        return redissonClient.getFairLock(key);
    }

    /**
     * 获取读写锁 {@link RReadWriteLock}。
     * <p>
     * 读锁可被多个线程同时持有(共享),写锁独占(排他)。适合<b>读多写少</b>的场景:
     * <ul>
     *   <li>读多写少:配置热加载、商品详情缓存重建</li>
     *   <li>读写互斥:读写同时进行可能读到脏数据</li>
     * </ul>
     *
     * <h4>使用示例:策略装配(装配一次后大量读取)</h4>
     * <pre>{@code
     * RReadWriteLock rwLock = redissonService.getReadWriteLock("rwlock:strategy:100001");
     * // 写:装配策略
     * rwLock.writeLock().lock();
     * try { strategyArmory.assembleLotteryStrategy(100001L); }
     * finally { rwLock.writeLock().unlock(); }
     *
     * // 读:抽奖时查策略
     * rwLock.readLock().lock();
     * try { return strategyRepo.queryStrategy(100001L); }
     * finally { rwLock.readLock().unlock(); }
     * }</pre>
     */
    @Override
    public RReadWriteLock getReadWriteLock(String key) {
        return redissonClient.getReadWriteLock(key);
    }

    /**
     * 获取分布式信号量 {@link RSemaphore}。
     * <p>
     * 等价于 {@code java.util.concurrent.Semaphore} 的分布式版本,用于<b>控制同时访问某资源的线程数</b>。
     * 底层 Redis {@code SET ... NX} + Lua。
     *
     * @param key 信号量 key
     * @return RSemaphore 实例
     *
     * <h4>使用示例:接口限流(最多 100 个并发请求)</h4>
     * <pre>{@code
     * RSemaphore semaphore = redissonService.getSemaphore("limit:api:createOrder");
     * semaphore.trySetPermits(100);    // 设置上限 100
     *
     * if (semaphore.tryAcquire()) {    // 尝试获取凭证,获取不到立即返回 false
     *     try { createOrder(); }
     *     finally { semaphore.release(); }
     * } else {
     *     throw new AppException(ResponseCode.SYSTEM_BUSY);
     * }
     * }</pre>
     */
    @Override
    public RSemaphore getSemaphore(String key) {
        return redissonClient.getSemaphore(key);
    }

    /**
     * 获取"凭证可过期"的分布式信号量 {@link RPermitExpirableSemaphore}。
     * <p>
     * 与 {@link RSemaphore} 的区别:每个凭证可以设 leaseTime,到期自动释放——避免"持锁线程宕机导致永久阻塞"。
     * 适合"凭证发放出去后允许中途失效"的场景(如排队领号、过号作废)。
     *
     * @param key 信号量 key
     * @return RPermitExpirableSemaphore 实例
     *
     * <h4>使用示例:银行排队叫号(过号作废)</h4>
     * <pre>{@code
     * RPermitExpirableSemaphore sem = redissonService.getPermitExpirableSemaphore("queue:bank:001");
     * sem.trySetPermits(10);                                   // 窗口上限 10 个
     * String permitId = sem.tryAcquire(5, TimeUnit.MINUTES);   // 凭证 5 分钟有效
     * if (permitId != null) {
     *     try { serve(); }
     *     finally { sem.release(permitId); }                    // 或者不调,5 分钟后自动失效
     * }
     * }</pre>
     */
    @Override
    public RPermitExpirableSemaphore getPermitExpirableSemaphore(String key) {
        return redissonClient.getPermitExpirableSemaphore(key);
    }

    /**
     * 获取分布式闭锁 {@link RCountDownLatch}。
     * <p>
     * 等价于 {@code java.util.concurrent.CountDownLatch} 的分布式版本。
     * 调用 {@code countDown()} 减计数,当计数到 0 时所有 {@code await()} 的线程被唤醒。
     *
     * @param key 闭锁 key
     * @return RCountDownLatch 实例
     *
     * <h4>使用示例:并行任务汇总</h4>
     * <pre>{@code
     * RCountDownLatch latch = redissonService.getCountDownLatch("latch:report:20260731");
     * latch.trySetCount(3);                          // 等待 3 个子任务
     *
     * // 主线程阻塞,等所有子任务完成
     * new Thread(() -> { doA(); latch.countDown(); }).start();
     * new Thread(() -> { doB(); latch.countDown(); }).start();
     * new Thread(() -> { doC(); latch.countDown(); }).start();
     *
     * latch.await();                                 // 直到计数 = 0 才返回
     * generateReport();
     * }</pre>
     */
    @Override
    public RCountDownLatch getCountDownLatch(String key) {
        return redissonClient.getCountDownLatch(key);
    }

    /**
     * 获取布隆过滤器 {@link RBloomFilter}。
     * <p>
     * 一种<b>概率型数据结构</b>:判断"元素一定不存在 / 可能存在"。
     * 空间效率极高(1 亿数据 ~ 100MB),但有<b>误判率</b>(默认 3%,可通过 {@code tryReserve(expectedInsertions, falseProbability)} 调小)。
     * <b>不支持删除</b>。
     *
     * @param key 布隆过滤器 key
     * @param <T> 元素类型
     * @return RBloomFilter 实例
     *
     * <h4>使用示例:防止缓存穿透(查 DB 前先过布隆)</h4>
     * <pre>{@code
     * RBloomFilter<Long> filter = redissonService.getBloomFilter("bf:user:id");
     * filter.tryReserve(10_000_000L, 0.01);             // 预估 1000w,误判率 1%
     *
     * Long userId = 12345L;
     * if (!filter.contains(userId)) {
     *     return null;                                  // 一定不存在,直接返回,不打 DB
     * }
     * return userDao.findById(userId);                 // 可能存在,正常查 DB
     * }</pre>
     */
    @Override
    public <T> RBloomFilter<T> getBloomFilter(String key) {
        return redissonClient.getBloomFilter(key);
    }

    /**
     * 设置 {@link RAtomicLong} 的初始值。
     * <p>
     * 仅在 key 不存在时生效;key 已存在则不修改(避免覆盖当前计数)。
     * 常用于"幂等初始化"——重启服务时不破坏在途计数。
     *
     * @param key   键
     * @param value 初始值
     *
     * <h4>使用示例:系统启动时初始化每日库存</h4>
     * <pre>{@code
     * redissonService.setAtomicLong("stock:sku:10086:today", 1000);  // 仅首次生效
     * }</pre>
     */
    @Override
    public void setAtomicLong(String key, long value) {
        redissonClient.getAtomicLong(key).set(value);
    }

    /**
     * 分布式锁的"setnx"简化版:仅当 key 不存在时设置成功。
     * <p>
     * 底层 Redis {@code SETNX key "lock"}(无过期时间,需自行处理死锁)。
     * <b>返回 true 表示抢锁成功,后续业务逻辑完成后请手动 {@link #remove(String)} 释放</b>。
     *
     * @param key 锁 key
     * @return true=抢锁成功;false=key 已存在,锁被别人持有
     *
     * <h4>使用示例:分布式定时任务防重</h4>
     * <pre>{@code
     * if (redissonService.setNx("lock:scheduled:dailyReport")) {
     *     try { generateDailyReport(); }
     *     finally { redissonService.remove("lock:scheduled:dailyReport"); }
     * } else {
     *     log.info("任务正在其他节点执行,本次跳过");
     * }
     * }</pre>
     */
    @Override
    public Boolean setNx(String key) {
        return redissonClient.getBucket(key).trySet("lock");
    }

}