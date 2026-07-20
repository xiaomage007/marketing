package com.charlie.domain.strategy.service.rule.factory;

import com.charlie.domain.strategy.model.entity.RuleActionEntity;
import com.charlie.domain.strategy.service.annotation.LogicStrategy;
import com.charlie.domain.strategy.service.rule.ILogicFilter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName: DefaultLogicFactory
 * @Description: 抽奖规则过滤器工厂(默认实现)
 * <p>
 * 该类是抽奖前规则过滤器的统一注册与查找入口,采用 <b>策略模式 + 工厂模式 + 注解驱动</b> 的设计思想,
 * 将所有实现 {@link ILogicFilter} 接口的规则过滤器以单例 Bean 的形式收集到当前工厂中,
 * 并以 {@link LogicStrategy} 注解中声明的 {@link LogicModel#getCode()} 作为 Key 进行索引,
 * 供业务方按规则编码快速获取对应的规则过滤器执行抽奖前/后的逻辑过滤。
 * <p>
 * <b>核心职责:</b>
 * <ul>
 *     <li>1. 启动时扫描所有 {@link ILogicFilter} 类型的 Spring Bean;</li>
 *     <li>2. 读取实现类上的 {@link LogicStrategy} 注解,提取规则模型枚举;</li>
 *     <li>3. 以 {@code code -> ILogicFilter} 的形式缓存到 {@link #logicFilterMap};</li>
 *     <li>4. 对外暴露 {@link #openLogicFilter()} 方法,提供类型安全的过滤器查找能力。</li>
 * </ul>
 * <p>
 * <b>使用示例:</b> 在新的规则过滤器实现类上添加 {@link LogicStrategy} 注解即可被自动注册,
 * 无需在工厂中写任何硬编码映射,符合「对扩展开放、对修改关闭」的开闭原则。
 * @Author: Charlie
 * @Date: 2026/7/19 17:14
 * @Version: 1.0
 */
@Service
public class DefaultLogicFactory {

    /**
     * 规则过滤器注册表
     * <p>
     * Key   : {@link LogicModel#getCode()} 规则模型编码(如 "rule_weight"、"rule_blacklist");<br>
     * Value : 对应规则模型的具体过滤器实现 {@link ILogicFilter} 实例(Spring Bean 单例)。
     * <p>
     * 使用 {@link ConcurrentHashMap} 是为了保证在多线程并发获取过滤器时的线程安全,
     * 防止在初始化或运行时出现并发读写异常。
     */
    public Map<String, ILogicFilter<?>> logicFilterMap = new ConcurrentHashMap<>();

    /**
     * 构造函数(Spring 自动注入)
     * <p>
     * Spring 启动时会自动将所有实现 {@link ILogicFilter} 接口的 Bean 注入到本构造函数,
     * 本工厂在实例化阶段完成所有规则过滤器的注册工作,典型的一次性加载逻辑。
     * <p>
     * <b>注册流程:</b>
     * <ol>
     *     <li>遍历 Spring 容器收集到的全部 {@link ILogicFilter} 实现;</li>
     *     <li>使用 {@link AnnotationUtils#findAnnotation(Class, Class)} 在实现类上查找 {@link LogicStrategy} 注解;</li>
     *     <li>若注解存在,则以注解中 {@code logicModel().getCode()} 作为 Key、当前过滤器实例作为 Value 写入 {@link #logicFilterMap};</li>
     *     <li>若注解不存在,则跳过当前实现(避免将未声明规则模型的过滤器误注册到工厂)。</li>
     * </ol>
     * <p>
     * <b>注意:</b> 由于 Key 来自枚举 {@link LogicModel},因此在业务上具有唯一性,
     * 当出现两个过滤器声明了相同的 {@code code} 时,后者将覆盖前者,
     * 开发时需保证规则编码与过滤器实现的一一对应。
     *
     * @param logicFilters Spring 容器中所有 {@link ILogicFilter} 类型的 Bean 集合
     */
    public DefaultLogicFactory(List<ILogicFilter<?>> logicFilters) {
        logicFilters.forEach(logic -> {
            // 通过 Spring 提供的注解工具查找类上的 @LogicStrategy 注解,
            // findAnnotation 会同时扫描父类与接口上的注解,比 getAnnotation 更强大
            LogicStrategy strategy = AnnotationUtils.findAnnotation(logic.getClass(), LogicStrategy.class);
            // 防御性判断:只有显式声明了规则模型的过滤器才会被注册,
            // 未声明的视为通用/抽象实现,不应进入工厂
            if (null != strategy) {
                // 以规则模型编码作为 Key,过滤器实例作为 Value 放入注册表
                logicFilterMap.put(strategy.logicMode().getCode(), logic);
            }
        });
    }

    /**
     * 对外暴露规则过滤器注册表(带类型转换)
     * <p>
     * 业务方在拿到本工厂后,通常会调用本方法获取到类型化的过滤器注册表,
     * 然后根据具体的 {@link LogicModel#getCode()} 取出对应过滤器执行过滤逻辑。
     * <p>
     * <b>泛型说明:</b> 由于 {@link #logicFilterMap} 在注册阶段以 {@code ILogicFilter<?>} 存储,
     * 实际每个过滤器在调用 {@code filter()} 时所需的具体泛型 T 可能不同
     * (如 {@code RaffleBeforeEntity}、{@code RaffleAfterEntity}),
     * 因此本方法通过二次强制转换返回带泛型的 Map,使调用方在 IDE 层面即可获得类型提示。
     * <p>
     * <b>类型安全提示:</b> 调用方需要自行保证所取出的过滤器泛型与调用场景匹配,
     * 否则在 {@code filter()} 调用时可能抛出 {@link ClassCastException}。
     *
     * @param <T> 规则动作实体的具体子类型(如 {@link RuleActionEntity.RaffleBeforeEntity})
     * @return 类型化的过滤器注册表 {@code Map<规则编码, 过滤器>}
     */
    @SuppressWarnings("unchecked")
    public <T extends RuleActionEntity.RaffleEntity> Map<String, ILogicFilter<T>> openLogicFilter() {
        // 双重类型转换:先将带通配符的 Map<?,?> 强转为 Map<String, ILogicFilter<T>>,
        // 由于 JVM 泛型擦除,运行时不会真正校验元素类型,仅在编译期生效
        return (Map<String, ILogicFilter<T>>) (Map<?, ?>) logicFilterMap;
    }

    /**
     * 规则模型枚举
     * <p>
     * 用于统一维护所有「规则编码(code)」与「规则说明(info)」,作为 {@link LogicStrategy} 注解的合法取值。
     * 新增规则时,只需在此枚举中追加一项,并在对应的 {@link ILogicFilter} 实现上使用 {@link LogicStrategy} 引用即可。
     */
    @Getter
    @AllArgsConstructor
    public enum LogicModel {

        /**
         * 抽奖权重规则
         * <p>
         * 作用于抽奖前阶段,根据用户/策略维度的权重配置,返回可参与抽奖的范围 KEY 列表,
         * 通常与 {@code rule_weight} 装配数据配合使用,用于控制不同奖品的命中概率。
         */
        RULE_WIGHT("rule_weight", "【抽奖前规则】根据抽奖权重返回可抽奖范围KEY"),

        /**
         * 黑名单规则
         * <p>
         * 作用于抽奖前阶段,用于判断当前用户是否命中黑名单;
         * 命中黑名单的用户将被直接拦截,不进入后续抽奖流程,常用于风控场景。
         */
        RULE_BLACKLIST("rule_blacklist", "【抽奖前规则】黑名单规则过滤,命中黑名单则直接返回"),

        ;

        private final String code;

        private final String info;
    }
}
