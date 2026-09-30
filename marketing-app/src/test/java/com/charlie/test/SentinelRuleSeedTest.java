package com.charlie.test;

import com.alibaba.nacos.api.config.ConfigService;
import com.charlie.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * @description: Sentinel 限流/熔断规则种子 - 把初始规则发布到 Nacos，应用启动时由 SentinelRuleNacosConfig
 * 的数据源拉取生效，之后在 Nacos 控制台改动对应 dataId 即可秒级热更新，无需重启
 * @author: Charlie
 * @date: 2026/9/30 08:30
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class SentinelRuleSeedTest {

    private static final String GROUP = Constants.Sentinel.RULE_GROUP;

    @Resource
    private ConfigService configService;

    /**
     * 接口级流控 - draw 全局限流：QPS 超过 100 快速失败。
     * count 保守起步(约为 tomcat max 线程 200 的一半)，draw 涉及 DB 事务 + Redis + MQ，压测后按实际水位调整
     */
    @Test
    public void publishFlowRules() throws Exception {
        String rules = "[{"
                + "\"resource\":\"draw_api\","
                + "\"limitApp\":\"default\","
                + "\"grade\":1,"                  // 1=QPS 阈值，0=并发线程数
                + "\"count\":100,"
                + "\"strategy\":0,"                // 0=直接拒绝，1=关联，2=链路
                + "\"controlBehavior\":0,"         // 0=快速失败，1=Warm Up，2=匀速排队
                + "\"warmUpPeriodSec\":10,"
                + "\"maxQueueingTimeMs\":0,"
                + "\"clusterMode\":false"
                + "}]";
        log.info("发布接口级流控规则 dataId:{} result:{}", Constants.Sentinel.FLOW_DATA_ID,
                configService.publishConfig(Constants.Sentinel.FLOW_DATA_ID, GROUP, rules));
    }

    /**
     * 热点参数流控 - 按 userId 限流：paramIdx=0 对应 RaffleActivityController 中
     * SphU.entry("draw_api", EntryType.IN, 1, userId) 传入的第 0 个热点参数，单用户每秒超过 20 次即拦截，
     * 与接口级规则形成「全局 + 单用户」双闸。clusterMode=false 为单机判定，多实例总量约为阈值 × 实例数
     */
    @Test
    public void publishParamFlowRules() throws Exception {
        String rules = "[{"
                + "\"resource\":\"draw_api\","
                + "\"limitApp\":\"default\","
                + "\"paramIdx\":0,"
                + "\"grade\":1,"                  // 1=QPS 阈值，0=并发线程数
                + "\"count\":20,"                 // 每个不同 userId 的每秒阈值
                + "\"durationInSec\":1,"
                + "\"burstCount\":0,"             // 额外允许的突发请求数
                + "\"controlBehavior\":0,"        // 0=快速失败，1=匀速排队
                + "\"maxQueueingTimeMs\":0,"
                + "\"clusterMode\":false"
                + "}]";
        log.info("发布热点参数流控规则 dataId:{} result:{}", Constants.Sentinel.PARAM_FLOW_DATA_ID,
                configService.publishConfig(Constants.Sentinel.PARAM_FLOW_DATA_ID, GROUP, rules));
    }

    /**
     * 熔断降级 - 慢调用比例：draw 的 RT 超过 200ms 记为慢调用，1s 统计窗口内请求数不少于 20 且慢调用占比
     * 超过 50% 时熔断 10s（后半开探测自愈）。异常比例熔断(grade=1)可后续按需追加
     */
    @Test
    public void publishDegradeRules() throws Exception {
        String rules = "[{"
                + "\"resource\":\"draw_api\","
                + "\"grade\":0,"                  // 0=慢调用比例，1=异常比例，2=异常数
                + "\"count\":200,"                // RT 阈值(ms)，超过记为慢调用
                + "\"timeWindow\":10,"            // 熔断持续时长(s)
                + "\"minRequestAmount\":20,"      // 触发熔断的最小请求数，防个别慢请求误熔
                + "\"slowRatioThreshold\":0.5,"   // 慢调用比例阈值
                + "\"statIntervalMs\":1000"       // 统计时长(ms)
                + "}]";
        log.info("发布熔断降级规则 dataId:{} result:{}", Constants.Sentinel.DEGRADE_DATA_ID,
                configService.publishConfig(Constants.Sentinel.DEGRADE_DATA_ID, GROUP, rules));
    }

    /**
     * 查看已发布规则 - 确认 Nacos 上的规则内容
     */
    @Test
    public void getRules() throws Exception {
        log.info("接口级流控规则: {}", configService.getConfig(Constants.Sentinel.FLOW_DATA_ID, GROUP, 3000L));
        log.info("热点参数流控规则: {}", configService.getConfig(Constants.Sentinel.PARAM_FLOW_DATA_ID, GROUP, 3000L));
        log.info("熔断降级规则: {}", configService.getConfig(Constants.Sentinel.DEGRADE_DATA_ID, GROUP, 3000L));
    }

}
