package com.charlie.config;

import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.csp.sentinel.datasource.nacos.NacosDataSource;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.PropertyKeyConst;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * @description: Sentinel 规则 Nacos 数据源。启动拉取 + 监听变更，把流控/热点/熔断规则动态加载到 RuleManager，
 * 规则发布到 Nacos 后秒级生效，无需重启；规则 JSON 数组见 com.charlie.test.SentinelRuleSeedTest。
 * 数据源构造时已同步拉取一次初始配置存入 property，register2Property 注册监听时会同步补推当前值，
 * 之后 Nacos 上的规则变更由数据源内部监听经 property 通知链自动生效
 * @author: Charlie
 * @date: 2026/9/30 08:30
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(SentinelRuleProperties.class)
public class SentinelRuleNacosConfig {

    private final SentinelRuleProperties props;

    public SentinelRuleNacosConfig(SentinelRuleProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void init() {
        registerFlowRule();
        registerParamFlowRule();
        registerDegradeRule();
    }

    /**
     * 接口级流控：draw 资源全局 QPS
     */
    private void registerFlowRule() {
        try {
            NacosDataSource<List<FlowRule>> dataSource = new NacosDataSource<>(
                    buildNacosProperties(), props.getGroup(), props.getFlowDataId(),
                    source -> parseRules(source, FlowRule.class));
            FlowRuleManager.register2Property(dataSource.getProperty());
            log.info("Sentinel 流控规则数据源注册完成 dataId:{} group:{} rules:{}",
                    props.getFlowDataId(), props.getGroup(), FlowRuleManager.getRules().size());
        } catch (Exception e) {
            log.error("Sentinel 流控规则数据源注册失败，请确认 Nacos 可用后重启 dataId:{}", props.getFlowDataId(), e);
        }
    }

    /**
     * 热点参数流控：draw 资源按 userId 维度 QPS（paramIdx 对应 SphU.entry 传入的 userId）
     */
    private void registerParamFlowRule() {
        try {
            NacosDataSource<List<ParamFlowRule>> dataSource = new NacosDataSource<>(
                    buildNacosProperties(), props.getGroup(), props.getParamFlowDataId(),
                    source -> parseRules(source, ParamFlowRule.class));
            ParamFlowRuleManager.register2Property(dataSource.getProperty());
            log.info("Sentinel 热点流控规则数据源注册完成 dataId:{} group:{} rules:{}",
                    props.getParamFlowDataId(), props.getGroup(), ParamFlowRuleManager.getRules().size());
        } catch (Exception e) {
            log.error("Sentinel 热点流控规则数据源注册失败，请确认 Nacos 可用后重启 dataId:{}", props.getParamFlowDataId(), e);
        }
    }

    /**
     * 熔断降级：draw 资源慢调用比例/异常比例
     */
    private void registerDegradeRule() {
        try {
            NacosDataSource<List<DegradeRule>> dataSource = new NacosDataSource<>(
                    buildNacosProperties(), props.getGroup(), props.getDegradeDataId(),
                    source -> parseRules(source, DegradeRule.class));
            DegradeRuleManager.register2Property(dataSource.getProperty());
            log.info("Sentinel 熔断降级规则数据源注册完成 dataId:{} group:{} rules:{}",
                    props.getDegradeDataId(), props.getGroup(), DegradeRuleManager.getRules().size());
        } catch (Exception e) {
            log.error("Sentinel 熔断降级规则数据源注册失败，请确认 Nacos 可用后重启 dataId:{}", props.getDegradeDataId(), e);
        }
    }

    private Properties buildNacosProperties() {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, props.getServerAddr());
        return properties;
    }

    /**
     * Nacos 上的规则是 JSON 数组；dataId 未发布时 source 为空，按空规则处理，避免启动解析报错
     */
    private <T> List<T> parseRules(String source, Class<T> ruleClass) {
        if (source == null || source.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return JSON.parseArray(source, ruleClass);
    }

}
