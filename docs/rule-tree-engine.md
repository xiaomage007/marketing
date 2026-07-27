# 抽奖规则树引擎文档

> 包路径：`com.charlie.domain.strategy.service.rule.tree`
> 最后更新：2026-07-27

## 一、设计目标

把抽奖流程中的「后置规则」（次数锁、库存扣减、兜底奖励……）从硬编码的责任链里抽出来，用一棵可配置的决策树表达。每个节点只关心自己的判断逻辑，节点之间的跳转关系由数据驱动，新增规则不用改引擎。

## 二、包结构

```
rule.tree/
├── ILogicTreeNode.java                  决策树节点接口
├── factory/
│   ├── DefaultTreeFactory.java          规则树工厂（@Service，容器 + 静态 DTO）
│   └── engine/
│       ├── IDecisionTreeEngine.java     引擎接口
│       └── impl/
│           └── DecisionTreeEngine.java  引擎实现（决策树主循环）
└── impl/
    ├── RuleLockLogicTreeNode.java       次数锁节点   @Component("rule_lock")
    ├── RuleStockLogicTreeNode.java      库存扣减节点 @Component("rule_stock")
    └── RuleLuckAwardLogicTreeNode.java  兜底奖励节点 @Component("rule_luck_award")
```

| 类型 | 文件 | 职责 |
|---|---|---|
| 接口 | `ILogicTreeNode` | 节点统一契约，定义 `logic(userId, strategyId, awardId)` |
| 接口 | `IDecisionTreeEngine` | 引擎契约，定义 `process(...)` |
| 实现 | `DecisionTreeEngine` | 决策树主循环：取节点 → 执行 → 选下一节点 → 直到叶子 |
| 工厂 | `DefaultTreeFactory` | Spring 注入节点 map，提供 `openLogicTree()` 创建引擎，承载 `TreeActionEntity`/`StrategyAwardData` 两个静态 DTO |
| 节点 | `RuleLockLogicTreeNode` | 次数锁：达到阈值放行，否则接管 |
| 节点 | `RuleStockLogicTreeNode` | 库存扣减：扣减成功放行，否则接管 |
| 节点 | `RuleLuckAwardLogicTreeNode` | 兜底：用固定奖品覆盖上游 awardId |

## 三、调用链与依赖

```
DefaultTreeFactory (@Service)
   └─ Spring 注入 Map<String, ILogicTreeNode> logicTreeNodeGroup
        key   = bean 名（rule_lock / rule_stock / rule_luck_award）
        value = 节点实现
   └─ openLogicTree(ruleTreeVO) → DecisionTreeEngine

DecisionTreeEngine (构造时传入 logicTreeNodeGroup + RuleTreeVO)
   └─ process() 循环：
        ILogicTreeNode.logic()  -> TreeActionEntity{ RuleLogicCheckTypeVO, StrategyAwardData }
        nextNode(code, lines)   -> 按 EQUAL 匹配出边
        直到 nextNode == null    -> 返回最后一次的 StrategyAwardData
```

### 核心值对象（位于 `model.valobj`）

| 值对象 | 字段 | 说明 |
|---|---|---|
| `RuleTreeVO` | `treeId` / `treeName` / `treeDesc` / `treeRootRuleNode` / `treeNodeMap` | 一棵完整的规则树；`treeRootRuleNode` 是入口节点 key |
| `RuleTreeNodeVO` | `treeId` / `ruleKey` / `ruleDesc` / `ruleValue` / `treeNodeLineVOList` | 单个节点；`ruleKey` 与 Spring bean 名一致 |
| `RuleTreeNodeLineVO` | `treeId` / `ruleNodeFrom` / `ruleNodeTo` / `ruleLimitType` / `ruleLimitValue` | 节点间的有向边；`ruleLimitType` 决定匹配方式 |
| `RuleLimitTypeVO` | `EQUAL`/`GT`/`LT`/`GE`/`LE`/`ENUM` | 出边限定类型，当前只实现 `EQUAL` |
| `RuleLogicCheckTypeVO` | `ALLOW("0000")` / `TAKE_OVER("0001")` | 节点决策结果，兼作路由信号 |

## 四、关键方法说明

### 4.1 `ILogicTreeNode.logic`

```java
DefaultTreeFactory.TreeActionEntity logic(String userId, Long strategyId, Integer awardId);
```

- 入参：用户ID、策略ID、上游已选出的奖品ID（可被节点改写）
- 返回：`TreeActionEntity`，含
  - `ruleLogicCheckType`：`ALLOW` 放行 / `TAKE_OVER` 接管
  - `strategyAwardData`：奖品数据，非叶子节点可不填

### 4.2 `IDecisionTreeEngine.process`

```java
DefaultTreeFactory.StrategyAwardData process(String userId, Long strategyId, Integer awardId);
```

从根节点迭代执行至叶子节点，返回最终决策产出的奖品数据。

### 4.3 `DecisionTreeEngine.process`（主循环）

执行流程：

```
1. 取根节点 key = ruleTreeVO.getTreeRootRuleNode()
2. while (nextNode != null):
     2.1 logicTreeNode = logicTreeNodeGroup.get(ruleTreeNode.getRuleKey())
     2.2 logicEntity  = logicTreeNode.logic(userId, strategyId, awardId)
     2.3 strategyAwardData = logicEntity.getStrategyAwardData()   // 每次覆盖
     2.4 nextNode     = nextNode(logicEntity.getRuleLogicCheckType().getCode(),
                                 ruleTreeNode.getTreeNodeLineVOList())
     2.5 ruleTreeNode = treeNodeMap.get(nextNode)
3. return strategyAwardData
```

要点：
- 决策结果 `code`（"0000"/"0001"）既是节点输出，又是下一节点选择的输入
- `strategyAwardData` 每轮都会被覆盖，最终返回值是「最后一个填充它的节点」的结果
- 叶子节点靠 `nextNode == null` 判定（无出边）

### 4.4 `DecisionTreeEngine.nextNode`

```java
public String nextNode(String matterValue, List<RuleTreeNodeLineVO> treeNodeLineVOList)
```

- 无出边 → 返回 `null`（叶子）
- 按列表顺序短路匹配第一条满足条件的出边（**配置顺序即优先级**）
- 有出边但无一匹配 → 抛 `RuntimeException`（配置异常硬失败）

### 4.5 `DecisionTreeEngine.decisionLogic`

```java
public boolean decisionLogic(String matterValue, RuleTreeNodeLineVO nodeLine)
```

按 `RuleLimitTypeVO` 分派，当前只实现 `EQUAL`：`matterValue.equals(nodeLine.getRuleLimitValue().getCode())`。`GT/LT/GE/LE/ENUM` 预留扩展位。

### 4.6 `DefaultTreeFactory.openLogicTree`

```java
public IDecisionTreeEngine openLogicTree(RuleTreeVO ruleTreeVO) {
    return new DecisionTreeEngine(logicTreeNodeGroup, ruleTreeVO);
}
```

工厂方法，创建决策树引擎实例。已实现，可直接使用。

## 五、执行示例（来自 LogicTreeTest）

### 5.1 规则树拓扑结构

```
                    ┌─────────────┐
                    │  rule_lock  │  (根节点：次数锁)
                    └──────┬──────┘
                           │
              ┌────────────┴────────────┐
              │                         │
         TAKE_OVER                   ALLOW
              │                         │
              ▼                         ▼
    ┌─────────────────┐      ┌─────────────────┐
    │ rule_luck_award │      │   rule_stock    │
    │   (兜底奖励)    │      │   (库存扣减)    │
    └─────────────────┘      └────────┬────────┘
                                        │
                                   TAKE_OVER
                                        │
                                        ▼
                              ┌─────────────────┐
                              │ rule_luck_award │
                              │   (兜底奖励)    │
                              └─────────────────┘
```

### 5.2 规则树配置（代码构建方式）

参考 `LogicTreeTest.test_tree_rule()`：

```java
// 1. 构建根节点 rule_lock
RuleTreeNodeVO rule_lock = RuleTreeNodeVO.builder()
    .treeId(100000001)
    .ruleKey("rule_lock")
    .ruleDesc("限定用户已完成N次抽奖后解锁")
    .ruleValue("1")
    .treeNodeLineVOList(List.of(
        // TAKE_OVER -> rule_luck_award（左分支）
        RuleTreeNodeLineVO.builder()
            .ruleNodeFrom("rule_lock")
            .ruleNodeTo("rule_luck_award")
            .ruleLimitType(RuleLimitTypeVO.EQUAL)
            .ruleLimitValue(RuleLogicCheckTypeVO.TAKE_OVER)
            .build(),
        // ALLOW -> rule_stock（右分支）
        RuleTreeNodeLineVO.builder()
            .ruleNodeFrom("rule_lock")
            .ruleNodeTo("rule_stock")
            .ruleLimitType(RuleLimitTypeVO.EQUAL)
            .ruleLimitValue(RuleLogicCheckTypeVO.ALLOW)
            .build()
    ))
    .build();

// 2. 构建叶子节点 rule_luck_award
RuleTreeNodeVO rule_luck_award = RuleTreeNodeVO.builder()
    .treeId(100000001)
    .ruleKey("rule_luck_award")
    .ruleDesc("兜底奖励节点")
    .ruleValue("1")
    .treeNodeLineVOList(null)  // 无出边，叶子
    .build();

// 3. 构建中间节点 rule_stock
RuleTreeNodeVO rule_stock = RuleTreeNodeVO.builder()
    .treeId(100000001)
    .ruleKey("rule_stock")
    .ruleDesc("库存处理规则")
    .ruleValue(null)
    .treeNodeLineVOList(List.of(
        // TAKE_OVER -> rule_luck_award
        RuleTreeNodeLineVO.builder()
            .ruleNodeFrom("rule_stock")  // 注意：这里是 rule_stock，不是 rule_lock
            .ruleNodeTo("rule_luck_award")
            .ruleLimitType(RuleLimitTypeVO.EQUAL)
            .ruleLimitValue(RuleLogicCheckTypeVO.TAKE_OVER)
            .build()
    ))
    .build();

// 4. 组装整棵树
RuleTreeVO ruleTreeVO = RuleTreeVO.builder()
    .treeId(100000001)
    .treeName("决策树规则；增加dall-e-3画图模型")
    .treeDesc("决策树规则；增加dall-e-3画图模型")
    .treeRootRuleNode("rule_lock")
    .treeNodeMap(Map.of(
        "rule_lock", rule_lock,
        "rule_stock", rule_stock,
        "rule_luck_award", rule_luck_award
    ))
    .build();
```

### 5.3 执行路径分析

#### 场景 A：用户未达到次数锁阈值 → 走左分支

```
调用：engine.process("xiaomage", 100001L, 100)

第 1 轮：rule_lock
  └─ logic() 返回 TAKE_OVER（未解锁）
  └─ nextNode("0001") 匹配到 rule_luck_award
第 2 轮：rule_luck_award
  └─ logic() 返回 TAKE_OVER，strategyAwardData={awardId=101, awardRuleValue="1,100"}
  └─ 无出边，循环结束

返回：StrategyAwardData{awardId=101, awardRuleValue="1,100"}
（原 awardId=100 被兜底覆盖）
```

#### 场景 B：用户已解锁 + 库存充足 → 走全路径（当前占位实现不会走到）

```
第 1 轮：rule_lock → ALLOW → rule_stock
第 2 轮：rule_stock → ALLOW → 无出边？（需要配置 ALLOW 分支）
```

#### 场景 C：用户已解锁 + 库存不足 → 走右分支再兜底（当前占位实现）

```
第 1 轮：rule_lock → ALLOW（当前占位固定返回）→ rule_stock
第 2 轮：rule_stock → TAKE_OVER（当前占位固定返回）→ rule_luck_award
第 3 轮：rule_luck_award → 叶子

返回：StrategyAwardData{awardId=101, awardRuleValue="1,100"}
```

## 六、设计要点

1. **节点选择靠 bean 名**：`@Component("rule_lock")` 的 bean 名既是 Spring 注入 map 的 key，也是 `RuleTreeNodeVO.ruleKey` 的取值，两者必须一致。
2. **决策结果即路由信号**：`RuleLogicCheckTypeVO.getCode()`（"0000"/"0001"）既是节点输出，又是出边匹配输入，整棵树只在这两种 code 间分叉。
3. **叶子判定靠「无出边」**：`nextNode` 返回 null 即结束，`process` 返回最后一次填充的 `strategyAwardData`。
4. **awardId 流转可被覆盖**：兜底节点用 `StrategyAwardData` 覆盖原 awardId，是「接管」语义的落地。
5. **配置异常硬失败**：有出边但都不匹配 → `RuntimeException`，避免静默走错路径。
6. **DTO 与节点解耦**：`TreeActionEntity`/`StrategyAwardData` 定义在 `DefaultTreeFactory` 内部，节点只依赖接口契约，不感知引擎实现。
7. **出边配置顺序即优先级**：`nextNode` 按列表顺序短路匹配，第一条满足条件的边立即生效。

## 七、扩展指南

### 7.1 新增一个规则节点

三步：

1. 在 `marketing-domain/.../rule/tree/impl/` 下新建 `RuleXxxLogicTreeNode`
2. 类上加 `@Component("rule_xxx")`（bean 名即规则 code），实现 `ILogicTreeNode`
3. 完成。Spring 启动时自动注入 `DefaultTreeFactory.logicTreeNodeGroup`，决策树配置里把 `ruleKey` 写成 `rule_xxx` 即可被引擎识别

示例：

```java
@Slf4j
@Component("rule_xxx")
public class RuleXxxLogicTreeNode implements ILogicTreeNode {

    @Override
    public DefaultTreeFactory.TreeActionEntity logic(String userId, Long strategyId, Integer awardId) {
        // 业务判断...
        return DefaultTreeFactory.TreeActionEntity.builder()
                .ruleLogicCheckType(RuleLogicCheckTypeVO.ALLOW)
                .build();
    }
}
```

### 7.2 扩展限定类型

如果需要 `GT/LT/GE/LE/ENUM` 等匹配方式：

1. 在 `decisionLogic` 的 `switch` 中补对应分支
2. `matterValue` 当前是 `RuleLogicCheckTypeVO` 的 code 字符串，扩展时需要把决策结果改成数值/枚举范围类型，或为节点输出增加一个独立的「路由值」字段

### 7.3 接入抽奖流程

`DefaultTreeFactory.openLogicTree()` 已实现，接入步骤：

1. 从 `strategy_rule` 表读取树配置，组装成 `RuleTreeVO`
2. 在 `AbstractRaffleStrategy` 抽奖后置规则阶段调用：

   ```java
   IDecisionTreeEngine engine = defaultTreeFactory.openLogicTree(ruleTreeVO);
   DefaultTreeFactory.StrategyAwardData data = engine.process(userId, strategyId, awardId);
   ```

## 八、当前实现状态与 TODO

### 8.1 实现状态总览

| 组件 | 状态 | 说明 |
|---|---|---|
| `DefaultTreeFactory` | ✅ 已完成 | `openLogicTree()` 工厂方法已实现 |
| `DecisionTreeEngine` | ✅ 已完成 | 主循环、路由匹配逻辑完整 |
| `RuleLockLogicTreeNode` | ⚠️ 占位实现 | 固定返回 `ALLOW`，未读取用户次数 |
| `RuleStockLogicTreeNode` | ⚠️ 占位实现 | 固定返回 `TAKE_OVER`，未扣减库存 |
| `RuleLuckAwardLogicTreeNode` | ⚠️ 占位实现 | 硬编码 `awardId=101`，未读配置 |

### 8.2 TODO 清单

| 优先级 | 任务 | 说明 | 涉及文件 |
|---|---|---|---|
| P0 | 实现 `RuleLockLogicTreeNode` 业务逻辑 | 注入仓储，查询用户累计抽奖次数，与 `ruleValue` 阈值比较：≥阈值返回 `ALLOW`，否则返回 `TAKE_OVER` | `RuleLockLogicTreeNode.java` |
| P0 | 实现 `RuleStockLogicTreeNode` 业务逻辑 | 注入仓储，对 `awardId` 执行 Redis 库存扣减（DECR）：扣减成功返回 `ALLOW`，库存不足返回 `TAKE_OVER` | `RuleStockLogicTreeNode.java` |
| P0 | 实现 `RuleLuckAwardLogicTreeNode` 业务逻辑 | 从 `strategy_rule` 表读取当前策略的兜底奖品配置，动态填充 `awardId` 与 `awardRuleValue` | `RuleLuckAwardLogicTreeNode.java` |
| P1 | 为规则树配置增加数据库表结构 | 在 `strategy_rule` 表中增加字段或新增关联表，支持持久化 `RuleTreeVO` 树结构（节点 + 出边） | `marketing.sql` |
| P1 | 在 `IStrategyRepository` 增加规则树装配方法 | 从数据库读取配置并组装 `RuleTreeVO` | `IStrategyRepository.java` + 实现 |
| P2 | 接入 `AbstractRaffleStrategy` 抽奖后置流程 | 在抽奖后调用决策树引擎，根据结果决定最终奖品 | `AbstractRaffleStrategy.java` |

### 8.3 节点实现细节 TODO

#### RuleLockLogicTreeNode

```java
// 当前占位
return DefaultTreeFactory.TreeActionEntity.builder()
    .ruleLogicCheckType(RuleLogicCheckTypeVO.ALLOW)
    .build();

// 期望实现伪代码
// 1. 从 ruleValue 解析阈值（如 "1" → 1次）
// 2. 查询用户 userId 在 strategyId 下的累计抽奖次数
// 3. if (count >= threshold) → ALLOW, else → TAKE_OVER
```

#### RuleStockLogicTreeNode

```java
// 当前占位
return DefaultTreeFactory.TreeActionEntity.builder()
    .ruleLogicCheckType(RuleLogicCheckTypeVO.TAKE_OVER)
    .build();

// 期望实现伪代码
// 1. 调用仓储对 awardId 执行 Redis DECR（带判定）
// 2. if (扣减成功) → ALLOW, else → TAKE_OVER
// 3. 注意：库存扣减节点通常不填充 strategyAwardData，由后续节点处理
```

#### RuleLuckAwardLogicTreeNode

```java
// 当前硬编码
return DefaultTreeFactory.TreeActionEntity.builder()
    .ruleLogicCheckType(RuleLogicCheckTypeVO.TAKE_OVER)
    .strategyAwardData(DefaultTreeFactory.StrategyAwardData.builder()
        .awardId(101)
        .awardRuleValue("1,100")
        .build())
    .build();

// 期望实现伪代码
// 1. 从 strategy_rule 读取 strategyId + ruleKey="rule_luck_award" 的配置
// 2. 解析出 awardId 和 awardRuleValue
// 3. 填充到 strategyAwardData 返回
```

## 九、命名设计说明：为什么是 `ILogicTreeNode`

### 9.1 候选命名对比

| 候选 | 问题 |
|---|---|
| `Tree` | 范围错了。`Tree` 暗示整棵决策树的抽象，但整棵树由 `RuleTreeVO`（数据）+ `DecisionTreeEngine`（执行）共同表达，节点接口不该占用这个名字 |
| `TreeNodeLine` | 概念错了。`NodeLine` 是「节点间的连线」（即 `RuleTreeNodeLineVO`），描述 `from -> to` 的指向与限定条件，是数据结构而非行为接口 |
| `ITreeNode` | 太泛。看不出是「逻辑」节点，可能与 UI 树、组织树、文件树等混淆 |
| `ILogicTreeNode` | ✅ 既限定语义（规则逻辑），又明确职责（树中的一个节点 + 它的行为） |

### 9.2 命名拆解

- **`I` 前缀**：领域层接口统一约定（参考 `IStrategyRepository`、`ILogicFilter`、`IDecisionTreeEngine`），保持一致
- **`Logic`**：
  - 每个实现类对应一种规则逻辑（次数锁、库存扣减、兜底奖励）
  - 接口核心方法叫 `logic()`，命名自洽
  - 与既有责任链接口 `ILogicFilter` 形成对照——两者都是「规则逻辑单元」，只是组织方式不同（链 vs 树）
- **`Node`**：
  - 实现类是树结构中的一个节点（`RuleLockLogicTreeNode` 等）
  - 与 `RuleTreeNodeVO`（节点数据）形成「行为/数据」分离：节点接口定义「到了这个节点要做什么」，节点 VO 描述「这个节点长什么样」

### 9.3 三层命名的关系

```
ILogicTreeNode          行为接口：节点要做什么（logic 方法）
        ↑ 实现
RuleLockLogicTreeNode   具体节点的行为实现
        ↓ 关联
RuleTreeNodeVO          节点数据：ruleKey / ruleValue / 出边列表
        ↓ 包含
RuleTreeNodeLineVO      连线数据：from -> to + 限定条件
```

「点（`Node`）」与「边（`NodeLine`）」是树结构的两个基本元素，命名上区分清楚；「行为（`ILogicTreeNode`）」与「数据（`RuleTreeNodeVO`）」是同一个点的两面，命名上一一对应。

### 9.4 为什么不直接复用 `ILogicFilter`

责任链的 `ILogicFilter` 与决策树的 `ILogicTreeNode` 长得很像，但语义不同，不可合并：

| 维度 | `ILogicFilter`（责任链） | `ILogicTreeNode`（决策树） |
|---|---|---|
| 组织方式 | 线性链，按注册顺序执行 | 树形，按出边配置跳转 |
| 路由信号 | 上一个过滤器主动调用下一个 | 当前节点返回 code，由引擎查表决定下一个 |
| 终止条件 | 任一过滤器 `TAKE_OVER` 即短路 | 走到无出边的叶子节点 |
| 扩展形态 | 加新过滤器 + 工厂注册 | 加新节点 + 出边配置 |
| 跳转能力 | 只能往后走，不能回退或分支 | 可根据 `ALLOW`/`TAKE_OVER` 分叉到不同子树 |

两者解耦后，未来可以让同一个规则同时挂在责任链和决策树上（前置过滤 vs 后置决策），互不干扰。

### 9.5 小结

`ILogicTreeNode` 这个名字回答了三个问题：

1. **是什么**——`Node`，树里的一个点（不是整棵树，也不是连线）
2. **做什么**——`Logic`，执行一种规则逻辑（不是数据描述）
3. **怎么用**——`I` 前缀，领域层接口契约，由 Spring 注入到工厂

## 十、相关文件索引

- 测试用例：`marketing-app/src/test/java/com/charlie/test/domain/LogicTreeTest.java`
- 引擎实现：`marketing-domain/src/main/java/com/charlie/domain/strategy/service/rule/tree/factory/engine/impl/DecisionTreeEngine.java`
- 节点接口：`marketing-domain/src/main/java/com/charlie/domain/strategy/service/rule/tree/ILogicTreeNode.java`
- 工厂：`marketing-domain/src/main/java/com/charlie/domain/strategy/service/rule/tree/factory/DefaultTreeFactory.java`
- 节点实现：
  - `RuleLockLogicTreeNode.java`
  - `RuleStockLogicTreeNode.java`
  - `RuleLuckAwardLogicTreeNode.java`
- 值对象：`marketing-domain/src/main/java/com/charlie/domain/strategy/model/valobj/` 下
  - `RuleTreeVO.java`
  - `RuleTreeNodeVO.java`
  - `RuleTreeNodeLineVO.java`
  - `RuleLimitTypeVO.java`
  - `RuleLogicCheckTypeVO.java`
- 数据库：`docs/dev-ops/mysql/sql/marketing.sql`（`strategy_rule` 表）
