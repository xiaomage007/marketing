package com.charlie.trigger.job;

import cn.bugstack.middleware.db.router.strategy.IDBRouterStrategy;
import com.charlie.domain.task.model.entity.TaskEntity;
import com.charlie.domain.task.service.ITaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @description: 发送MQ消息任务队列
 * @author: Charlie
 * @date: 2026/8/31 7:48
 */
@Slf4j
@Component
public class SendMessageTaskJob {

    @Resource
    private ITaskService taskService;
    @Resource
    private ThreadPoolExecutor executor;
    @Resource
    private IDBRouterStrategy dbRouter;

    /**
     * 补偿任务：定时扫描任务表，把漏发的 MQ 消息重新发送出去
     * 举例：用户抽奖中奖后写入 task 表，如果当时 MQ 发送失败（网络抖动/服务重启），
     * 消息状态仍是「待发送」，就靠这个 Job 每 5 秒兜底补发一次，保证最终一致性
     */
    @Scheduled(cron = "0/5 * * * * ?") // cron 表达式：从第 0 秒开始、每 5 秒触发一次（如 00:00:00、00:00:05、00:00:10 ...）
    public void execute() {
        try {
            // 获取分库数量。举例：db-router 配置 dbCount=2，说明有 2 个库，每个库各有一张 task 表，需要分别扫描
            int dbCount = dbRouter.dbCount();

            // 逐个库扫描表【每个库一个任务表】。举例：dbIdx=1 扫 db_01.task，dbIdx=2 扫 db_02.task
            for (int dbIdx = 1; dbIdx <= dbCount; dbIdx++) {
                // dbIdx 是循环变量（每次迭代会变），而 lambda 只能捕获不变量，
                // 所以复制一份赋给 final 变量 finalDbIdx，供线程体内部使用
                int finalDbIdx = dbIdx;
                // 把「扫描一个库」这个动作丢进线程池异步执行，多个库可以并行扫描，互不阻塞
                executor.execute(() -> {
                    try {
                        // 路由键存放在 ThreadLocal 中，只对当前线程生效；
                        // 手动指定库编号，后续本线程内的 SQL 都会路由到 db_0{finalDbIdx}
                        dbRouter.setDBKey(finalDbIdx);
                        // 不分表（tbCount 未启用），固定路由到第 0 张表，即 task 表本身
                        dbRouter.setTBKey(0);
                        // 查询该库中 status=未发送 的任务列表
                        // 举例返回：[{userId: "xiaofuge", messageId: "1024", topic: "send_award", exchange: ...}, ...]
                        List<TaskEntity> taskEntities = taskService.queryNoSendMessageTaskList();
                        // 该库没有待补偿的任务，直接结束本次扫描，无需继续
                        if (taskEntities.isEmpty()) return;
                        // 发送MQ消息
                        for (TaskEntity taskEntity : taskEntities) {
                            // 每条消息再单独开一个线程发送，提高发送效率。配置的线程池策略为 CallerRunsPolicy，在 ThreadPoolConfig 配置中有4个策略，面试中容易对比提问。可以检索下相关资料。
                            // 举例：任务列表有 100 条，如果串行发送要 100 次网络往返，并行发送可大幅缩短总耗时
                            executor.execute(() -> {
                                try {
                                    // 真正发送 MQ 消息：按 taskEntity 里的 exchange/routingKey 投递到 RabbitMQ
                                    // 举例：发送一条 userId=xiaofuge 的中奖发货消息到 send_award 队列
                                    taskService.sendMessage(taskEntity);
                                    // 发送成功后更新任务状态为「发送完成」，下次扫描就不会再查到这条记录（幂等的关键）
                                    taskService.updateTaskSendMessageCompleted(taskEntity.getUserId(), taskEntity.getMessageId());
                                } catch (Exception e) {
                                    // 发送失败（如 MQ 不可用）：记录日志（注意不能把 e 打出来，否则会把消费端返回的异常堆栈重复打到生产者日志里）
                                    log.error("定时任务，发送MQ消息失败 userId: {} exchange: {} routingKey: {} queue: {}",
                                            taskEntity.getUserId(), taskEntity.getExchange(), taskEntity.getRoutingKey(), taskEntity.getQueue());
                                    // 更新任务状态为「发送失败」，下一轮扫描（5 秒后）继续尝试补发，直到成功为止
                                    taskService.updateTaskSendMessageFail(taskEntity.getUserId(), taskEntity.getMessageId());
                                }
                            });
                        }
                    } finally {
                        // 无论成功失败都要清掉 ThreadLocal 中的路由键；
                        // 线程池的线程会被复用，不清除会导致下个任务继承旧路由，查错库
                        dbRouter.clear();
                    }
                });
            }
        } catch (Exception e) {
            // 兜底捕获：任何非预期异常（如配置读取失败）只记日志，不让定时任务线程中断，
            // 否则 @Scheduled 会停止调度，补偿能力彻底失效
            log.error("定时任务，扫描MQ任务表发送消息失败。", e);
        } finally {
            // 主线程（调度线程）也清一次路由键，防止主线程残留路由状态污染后续任务
            dbRouter.clear();
        }
    }

}
