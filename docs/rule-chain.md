# 抽奖规则责任链文档

> 包路径：`com.charlie.domain.strategy.service.rule.chain`
> 最后更新：2026-07-27

## 一、设计目标

把抽奖流程中的「前置规则」（黑名单过滤、权重抽奖、默认抽奖……）串成一条责任链：每个节点自行决定是接管（直接返回 awardId）还是放行（调 `next().logic(...)` 传给下一个节点），最后一个节点必然是 `default`，保证调用方一定能拿到 awardId。

与决策树（`rule.tree`）的对照：

| 维度 | 责任链（`rule.chain`） | 决策树（`rule.tree`） |
|---|---|---|
| 用途 | 抽奖**前置**规则（决定用哪种抽奖方式） | 抽奖**后置**规则（对已抽出的 awardId 二次决策） |
| 拓扑 | 线性，按注册顺序执行 | 树形，按出边配置跳转 |
| 路由 | 节点主动调 `next()` | 节点返回 code，引擎查表决定下一个 |
| 终止 | 任一节点返回 awardId 即结束 | 走到无出边的叶子节点 |
| 装配 | 工厂按 `rule_models` 数组顺序拼接 | 工厂按 `RuleTreeVO` 创建引擎 |

## 二、包结构

```
rule.chain/
├── ILogicChain.java                责任链接口（业务 + 装配合一）
├── ILogicChainArmory.java          装配接口（next / appendNext）
├── AbstractLogicChain.java         抽象类：实现 next/appendNext，定义 ruleModel 抽象方法
├── factory/
│   └── DefaultChainFactory.java    @Service 工厂，openLogicChain(strategyId) 按策略装配链
└── impl/
    ├── BackListLogicChain.java     黑名单链   @Component("rule_blacklist")
    ├── RuleWeightLogicChain.java   权重链     @Component("rule_weight")
    └── DefaultLogicChain.java      默认抽奖链 @Component("default")，链尾兜底
```

| 类型 | 文件 | 职责 |
|---|---|---|
| 接口 | `ILogicChainArmory` | 定义链装配能力：`next()` / `appendNext(next)` |
| 接口 | `ILogicChain` | 继承 `ILogicChainArmory`，加 `logic(userId, strategyId)` 业务方法 |
| 抽象类 | `AbstractLogicChain` | 实现 `next`/`appendNext`，声明 `ruleModel()` 抽象方法 |
| 工厂 | `DefaultChainFactory` | 按 `strategy.rule_models` 数组顺序拼接节点，末尾追加 `default` |
| 节点 | `BackListLogicChain` | userId 在黑名单则返回固定 awardId，否则 next |
| 节点 | `RuleWeightLogicChain` | 按 userScore 选最高档位走子表抽奖，否则 next |
| 节点 | `DefaultLogicChain` | 调 `strategyDispatch.getRandomAwardId(strategyId)` 走默认抽奖表 |

## 三、调用链与依赖

```
DefaultChainFactory (@Service)
   ├─ Spring 注入 Map<String, ILogicChain> logicChainGroup
   │     key   = bean 名（rule_blacklist / rule_weight / default）
   │     value = 链节点实现
   ├─ 注入 IStrategyRepository（查策略规则配置）
   └─ openLogicChain(strategyId)
        ├─ repository.queryStrategyEntityByStrategyId(strategyId) -> StrategyEntity
        ├─ strategy.ruleModels()  // 逗号分隔的 rule_models 字段拆成数组
        ├─ 按 ruleModels 顺序拼接：head -> n1 -> n2 -> ... -> default
        └─ 返回链头 head

调用方：
   ILogicChain chain = defaultChainFactory.openLogicChain(strategyId);
   Integer awardId = chain.logic(userId, strategyId);   // 从链头开始，沿 next 传递
```

### 节点内部流转

```
BackListLogicChain.logic()
   ├─ 查 ruleValue（"awardId:user1,user2,..."）
   ├─ userId 命中黑名单 -> 返回 awardId（接管）
   └─ 未命中 -> next().logic(...)（放行）

RuleWeightLogicChain.logic()
   ├─ 查 ruleValue（"4000:102,103,104,105 5000:102,103,104,105,106,107"）
   ├─ 解析成 Map<Long, String>，按 key 降序
   ├─ userScore >= 某档位 -> strategyDispatch.getRandomAwardId(strategyId, ruleWeightValueKey)（接管）
   └─ 档位都够不着 -> next().logic(...)（放行）

DefaultLogicChain.logic()
   └─ strategyDispatch.getRandomAwardId(strategyId)  // 兜底，必然返回 awardId
```

### 核心依赖（位于 `model` / `repository` / `service.armory`）

| 依赖 | 作用 |
|---|---|
| `StrategyEntity.ruleModels()` | 把 `strategy.rule_models` 字段（逗号分隔）拆成 `String[]` |
| `IStrategyRepository.queryStrategyRuleValue(strategyId, ruleModel)` | 查 `strategy_rule` 表对应规则的 `rule_value` |
| `IStrategyDispatch.getRandomAwardId(strategyId)` | 默认抽奖表抽奖，返回 awardId |
| `IStrategyDispatch.getRandomAwardId(strategyId, ruleWeightValueKey)` | 权重子表抽奖，`ruleWeightValueKey` 形如 `4000:102,103,104,105` |

## 四、关键方法说明

### 4.1 `ILogicChainArmory.next` / `appendNext`

```java
ILogicChain next();
ILogicChain appendNext(ILogicChain next);
```

- `next()`：返回当前节点的下一个节点
- `appendNext(next)`：把 `next` 挂到当前节点之后，**并返回 `next` 本身**，便于链式拼接：

  ```java
  current = current.appendNext(nextChain);   // current 后移一位
  ```

### 4.2 `ILogicChain.logic`

```java
Integer logic(String userId, Long strategyId);
```

- 入参：用户ID、策略ID
- 返回：awardId。**节点要么返回 awardId 接管，要么调 `next().logic(...)` 放行**，没有第三种分支

### 4.3 `AbstractLogicChain`

```java
public abstract class AbstractLogicChain implements ILogicChain {
    private ILogicChain next;
    public ILogicChain next() { return next; }
    public ILogicChain appendNext(ILogicChain next) { this.next = next; return next; }
    protected abstract String ruleModel();
}
```

- 把 `next` 指针的存取集中到抽象类，子类只关心 `logic` 与 `ruleModel`
- `ruleModel()` 返回规则 code（如 `"rule_blacklist"`），用于：
  - 仓储查规则值：`repository.queryStrategyRuleValue(strategyId, ruleModel())`
  - 日志标识

### 4.4 `DefaultChainFactory.openLogicChain`

```java
public ILogicChain openLogicChain(Long strategyId)
```

装配流程（7 步）：

```
1. 仓储查 StrategyEntity
2. 取 ruleModels()；为空则直接返回 default 链（第一道兜底）
3. ruleModels[0] 作为链头单独保存（最终要返回它）
4. current 指针初始指向链头
5. 从 ruleModels[1] 开始遍历，逐个拼接到链尾
6. 末尾追加 default 作为兜底（第二道兜底）
7. 返回链头
```

**两道兜底**：策略没配规则 -> default；策略配的规则都放行 -> 末尾 default 必然接管。

### 4.5 `BackListLogicChain.logic`

```
ruleValue 格式：awardId:user1,user2,user3   （例如 "1:user001,user002,user003"）
1. split(':') 取 splitRuleValue[0] 作为 awardId
2. split(',') 取 splitRuleValue[1] 拆成黑名单用户ID数组
3. userId 在数组中 -> 返回 awardId（接管）
4. 否则 -> next().logic(...)（放行）
```

### 4.6 `RuleWeightLogicChain.logic`

```
ruleValue 格式：4000:102,103,104,105 5000:102,103,104,105,106,107 6000:102,103,104,105,106,107,108,109
1. 按 SPACE 拆成多个档位
2. 每档按 COLON 拆成 [积分阈值, 子表奖品列表] -> Map<Long, String>
3. key 降序排序
4. 找第一个 userScore >= key 的档位（即「够得着的最高档」）
5. 命中 -> strategyDispatch.getRandomAwardId(strategyId, ruleWeightValueKey)（接管）
6. 未命中 -> next().logic(...)（放行）
```

举例：`userScore=4500`
- 档位 [6000, 5000, 4000]
- `6000: 4500>=6000? 否`
- `5000: 4500>=5000? 否`
- `4000: 4500>=4000? 是` -> 命中 `4000:102,103,104,105` 子表

### 4.7 `DefaultLogicChain.logic`

```java
Integer awardId = strategyDispatch.getRandomAwardId(strategyId);
return awardId;
```

链尾兜底，必然返回 awardId。

## 五、执行示例

### 5.1 策略配置

```
strategy 表（策略 100001）:
  strategy_id   = 100001
  rule_models   = "rule_blacklist,rule_weight"   ← 逗号分隔，决定链顺序

strategy_rule 表:
  rule_model     = rule_blacklist
  rule_value     = "1:user001,user002,user003"   ← 命中黑名单发 awardId=1

  rule_model     = rule_weight
  rule_value     = "4000:102,103,104,105 5000:102,103,104,105,106,107"
```

### 5.2 工厂装配结果

调用 `defaultChainFactory.openLogicChain(100001L)`：

```
ruleModels = ["rule_blacklist", "rule_weight"]
链头 = logicChainGroup.get("rule_blacklist") = BackListLogicChain实例
拼接：BackListLogicChain -> RuleWeightLogicChain
追加 default：BackListLogicChain -> RuleWeightLogicChain -> DefaultLogicChain
返回：BackListLogicChain（链头）
```

最终链：

```
[BackListLogicChain] -> next -> [RuleWeightLogicChain] -> next -> [DefaultLogicChain] -> next=null
```

### 5.3 场景 A：黑名单用户 `user001`

调用 `chain.logic("user001", 100001L)`：

**第 1 站：`BackListLogicChain`**
- `ruleValue = "1:user001,user002,user003"`
- `awardId = 1`，黑名单 `["user001","user002","user003"]`
- `"user001".equals("user001")` -> true -> 返回 `1`
- 日志：`抽奖责任链-黑名单接管 userId: user001 strategyId: 100001 ruleModel: rule_blacklist awardId: 1`

**返回**：`awardId = 1`（链后续节点未执行）

### 5.4 场景 B：非黑名单 + 积分 4500 的用户 `user888`

调用 `chain.logic("user888", 100001L)`，假设 `RuleWeightLogicChain.userScore = 4500L`：

**第 1 站：`BackListLogicChain`**
- 黑名单 `["user001","user002","user003"]`，`"user888"` 不在其中
- 调 `next().logic("user888", 100001L)` -> 进入 `RuleWeightLogicChain`
- 日志：`抽奖责任链-黑名单开始 ...` 后放行

**第 2 站：`RuleWeightLogicChain`**
- `ruleValue = "4000:102,103,104,105 5000:102,103,104,105,106,107"`
- 解析：`{4000: "4000:102,103,104,105", 5000: "5000:102,103,104,105,106,107"}`
- 降序 keys：`[5000, 4000]`
- `5000: 4500>=5000? 否`
- `4000: 4500>=4000? 是` -> `ruleWeightValueKey = "4000:102,103,104,105"`
- 调 `strategyDispatch.getRandomAwardId(100001L, "4000:102,103,104,105")` -> 假设返回 `103`
- 日志：`抽奖责任链-权重接管 ... awardId: 103`

**返回**：`awardId = 103`（链尾 default 未执行）

### 5.5 场景 C：非黑名单 + 积分 0 的用户 `user999`

**第 1 站**：黑名单未命中 -> next
**第 2 站**：`RuleWeightLogicChain`
- `userScore = 0`，所有档位都够不着
- 调 `next().logic(...)` -> 进入 `DefaultLogicChain`

**第 3 站：`DefaultLogicChain`**
- 调 `strategyDispatch.getRandomAwardId(100001L)` -> 假设返回 `107`
- 日志：`抽奖责任链-默认处理 ... awardId: 107`

**返回**：`awardId = 107`（兜底生效）

## 六、设计要点

1. **业务/装配分离**：`ILogicChainArmory` 只管链拼接（`next`/`appendNext`），`ILogicChain` 加业务方法 `logic`。调用方只看 `ILogicChain`，装配方才看 `ILogicChainArmory`。
2. **`appendNext` 返回 next**：让 `current = current.appendNext(next)` 一行完成「拼接 + 指针后移」，避免维护临时变量。
3. **链头单独保存**：装配时 `logicChain` 保存链头，`current` 用于后移；最终返回 `logicChain` 而不是 `current`。
4. **两道兜底**：策略无规则 -> default；规则都放行 -> 末尾 default。保证调用方一定能拿到 awardId。
5. **节点选择靠 bean 名**：`@Component("rule_blacklist")` 的 bean 名既是 Spring 注入 map 的 key，也是 `strategy.rule_models` 字段里的 code，两者必须一致。
6. **接管/放行二元决策**：节点要么返回 awardId 接管，要么 `next().logic(...)` 放行，没有第三种分支，简化心智模型。
7. **`ruleModel()` 自描述**：每个子类返回自己的规则 code，仓储查询和日志都依赖它，新增节点不需要改工厂。
8. **`AbstractLogicChain` 集中 next 指针**：子类不用关心 next 字段的存取，只实现 `logic` 与 `ruleModel`。

## 七、扩展指南

### 7.1 新增一个责任链节点

四步：

1. 在 `marketing-domain/.../rule/chain/impl/` 下新建 `RuleXxxLogicChain`
2. 类上加 `@Component("rule_xxx")`（bean 名即规则 code），继承 `AbstractLogicChain`
3. 实现 `logic(userId, strategyId)` 与 `ruleModel()`
4. 完成。Spring 启动时自动注入 `DefaultChainFactory.logicChainGroup`，策略表的 `rule_models` 字段加上 `rule_xxx` 即可被工厂装配

示例：

```java
@Slf4j
@Component("rule_xxx")
public class RuleXxxLogicChain extends AbstractLogicChain {

    @Resource
    private IStrategyRepository repository;

    @Override
    public Integer logic(String userId, Long strategyId) {
        log.info("抽奖责任链-xxx开始 userId: {} strategyId: {}", userId, strategyId);
        String ruleValue = repository.queryStrategyRuleValue(strategyId, ruleModel());
        // 业务判断...
        if (命中) {
            return awardId;                  // 接管
        }
        return next().logic(userId, strategyId);  // 放行
    }

    @Override
    protected String ruleModel() {
        return "rule_xxx";
    }
}
```

### 7.2 调整链顺序

链顺序由 `strategy.rule_models` 字段决定，**改数据库即可**，不动代码。例如：

```
rule_models = "rule_blacklist,rule_weight"   -> 黑名单 -> 权重 -> default
rule_models = "rule_weight,rule_blacklist"   -> 权重 -> 黑名单 -> default（顺序变了，行为可能不同）
rule_models = ""                              -> 直接走 default
```

### 7.3 接入抽奖流程

`DefaultChainFactory.openLogicChain(strategyId)` 已就绪，在 `AbstractRaffleStrategy` 抽奖前置规则阶段调用：

```java
// 1. 装配责任链
ILogicChain chain = defaultChainFactory.openLogicChain(strategyId);

// 2. 执行责任链，拿到 awardId
Integer awardId = chain.logic(userId, strategyId);
```

## 八、当前实现状态与 TODO

| 节点 | 状态 | TODO |
|---|---|---|
| `BackListLogicChain` | 实现完整 | 类名拼写应为 `BlackList`，但已被 `@Component("rule_blacklist")` 引用并写入数据库，重命名需同步改注解与 SQL |
| `RuleWeightLogicChain` | 占位 `userScore = 0L` | 替换为从数据库查询用户积分（当前测试通过 `ReflectionTestUtils.setField` 覆盖） |
| `DefaultLogicChain` | 实现完整 | 无 |
| `DefaultChainFactory` | 实现完整 | 无 |
| `AbstractLogicChain` | 实现完整 | 无 |

## 九、命名设计说明：为什么是 `ILogicChain`

### 9.1 候选命名对比

| 候选 | 问题 |
|---|---|
| `Chain` | 太泛。看不出是「规则逻辑」链，可能与过滤器链、拦截器链等混淆 |
| `LogicFilter` | 已被责任链旧实现占用（`service.rule.chain.factory.DefaultLogicFactory` 下的 `ILogicFilter`），且 `Filter` 偏「过滤」语义，不表达「链」的结构 |
| `IChainNode` | 缺少 `Logic`，看不出是规则逻辑节点；且责任链的接口是「整条链的入口」而非单个节点 |
| `ILogicChain` | ✅ 既限定语义（规则逻辑），又明确职责（一条可沿 `next` 传递的链） |

### 9.2 命名拆解

- **`I` 前缀**：领域层接口统一约定（参考 `ILogicChainArmory`、`IStrategyRepository`、`ILogicTreeNode`）
- **`Logic`**：
  - 每个实现类对应一种规则逻辑（黑名单、权重、默认抽奖）
  - 接口核心方法叫 `logic()`，命名自洽
  - 与决策树接口 `ILogicTreeNode` 形成对照--两者都是「规则逻辑单元」，只是组织方式不同（链 vs 树）
- **`Chain`**：
  - 表达「链」结构：节点间通过 `next` 单向传递
  - 调用方拿链头调 `logic()`，整条链沿 `next` 自动传递，对外表现为「一条链」而非「一个节点」

### 9.3 `ILogicChain` vs `ILogicChainArmory` 拆分

接口拆成两个，是单一职责的体现：

| 接口 | 方法 | 使用方 |
|---|---|---|
| `ILogicChainArmory` | `next()` / `appendNext(next)` | 工厂装配时使用（`DefaultChainFactory`） |
| `ILogicChain` | `logic(userId, strategyId)` | 业务调用方使用（`AbstractRaffleStrategy`） |

`ILogicChain extends ILogicChainArmory`，所以链节点实例同时具备两种能力，但调用方只看到自己需要的那一面。装配细节（`next` 指针）对业务调用方不可见。

### 9.4 与决策树 `ILogicTreeNode` 的对照

| 维度 | `ILogicChain` | `ILogicTreeNode` |
|---|---|---|
| 组织方式 | 线性，`next` 单向传递 | 树形，出边分叉 |
| 装配方 | `DefaultChainFactory.openLogicChain` | `DefaultTreeFactory.openLogicTree` |
| 节点决策 | 自己调 `next()` 或返回 awardId | 返回 code，由引擎决定下一个 |
| 适合场景 | 顺序过滤、线性兜底 | 多分支决策、复杂路由 |

两者命名结构对称（`ILogicChain` / `ILogicTreeNode`），都通过 `Logic` 表达「规则逻辑单元」的语义，通过后缀（`Chain` / `TreeNode`）区分组织方式。

## 十、相关文件索引

- 链接口：`marketing-domain/src/main/java/com/charlie/domain/strategy/service/rule/chain/ILogicChain.java`
- 装配接口：`marketing-domain/src/main/java/com/charlie/domain/strategy/service/rule/chain/ILogicChainArmory.java`
- 抽象类：`marketing-domain/src/main/java/com/charlie/domain/strategy/service/rule/chain/AbstractLogicChain.java`
- 工厂：`marketing-domain/src/main/java/com/charlie/domain/strategy/service/rule/chain/factory/DefaultChainFactory.java`
- 黑名单节点：`marketing-domain/src/main/java/com/charlie/domain/strategy/service/rule/chain/impl/BackListLogicChain.java`
- 权重节点：`marketing-domain/src/main/java/com/charlie/domain/strategy/service/rule/chain/impl/RuleWeightLogicChain.java`
- 默认节点：`marketing-domain/src/main/java/com/charlie/domain/strategy/service/rule/chain/impl/DefaultLogicChain.java`
- 依赖：`IStrategyRepository`、`IStrategyDispatch`、`StrategyEntity`
- 数据库：`docs/dev-ops/mysql/sql/marketing.sql`（`strategy`、`strategy_rule` 表）
- 配套文档：`docs/rule-tree-engine.md`（决策树，后置规则）
