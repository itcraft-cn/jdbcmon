# jdbcmon 使用手册

[English Manual](manual_en.md)

本手册面向使用与运维人员，说明 jdbcmon 的接入方式、配置、监控能力与常见问题。架构与设计文档见 README 与 AGENTS.md。

## 目录

1. 简介
2. 环境要求与依赖
3. 接入方式
4. 配置
5. 监控能力
6. 事件与监听器
7. 统计指标
8. 构建、测试与发布
9. 性能基准
10. 常见问题与已知限制

---

## 1. 简介

jdbcmon 是一个零侵入、可扩展的轻量级 JDBC 监控代理框架。它在不修改业务 SQL 的前提下，对连接、语句、结果集进行透明包装，采集慢查询、超大结果集、错误等指标，并提供事件回调与异步通知。

核心特性：

- 零侵入：三种接入方式，最低可做到仅加 `-javaagent` 启动参数
- 低开销：Query 开销通常 < 10%，功能测试场景可忽略
- 可观测：慢查询、自适应阈值、超大结果集、错误率
- 可扩展：统一事件模型，单一监听方法即可接管全部事件
- 多 JDK：兼容 JDK 8 与 17，JDK 17 吞吐量更高

## 2. 环境要求与依赖

### 2.1 JDK

| 项 | 要求 |
|----|------|
| 最低 | JDK 8 |
| 推荐 | JDK 17 |
| 模式2/模式3 运行时 | 与业务同 JVM，无需额外依赖（模式3 为 fat jar） |

### 2.2 Maven 坐标

```xml
<!-- 核心引擎，三种模式都依赖 -->
<dependency>
    <groupId>cn.itcraft</groupId>
    <artifactId>jdbcmon-core</artifactId>
    <version>1.1.0-dev</version>
</dependency>

<!-- 模式2：Driver/URL 代理 -->
<dependency>
    <groupId>cn.itcraft</groupId>
    <artifactId>jdbcmon-driver</artifactId>
    <version>1.1.0-dev</version>
</dependency>

<!-- 模式1 的 Spring Boot 集成 -->
<dependency>
    <groupId>cn.itcraft</groupId>
    <artifactId>jdbcmon-spring</artifactId>
    <version>1.1.0-dev</version>
</dependency>
```

模式3 无需 Maven 依赖，使用构建产物 `jdbcmon-agent.jar`。

## 3. 接入方式

监控引擎与交付层分离，同一套引擎支持三种接入方式，可按场景选择或组合。

| 方式 | 载体 | 接入点 | 侵入性 | 覆盖范围 |
|------|------|--------|--------|----------|
| 方式一 显式包装 | jdbcmon-core / jdbcmon-spring | 代码包装 DataSource | 需改代码或加依赖 | 仅被包装的数据源 |
| 方式二 Driver/URL 代理 | jdbcmon-driver | JDBC URL 加前缀 | 零代码，改配置 | 走该驱动的连接 |
| 方式三 javaagent | jdbcmon-agent | JVM 启动参数 | 零 | 该 JVM 内所有 JDBC 驱动连接 |

选择建议：

- 可改代码、需精确控制：方式一
- 不便改代码、连接池可改配置：方式二
- 完全零侵入、连接由框架内部创建：方式三

### 3.1 方式一：显式包装

#### 3.1.1 纯 Java

```java
import cn.itcraft.jdbcmon.config.WrappedConfig;
import cn.itcraft.jdbcmon.config.MetricsLevel;
import cn.itcraft.jdbcmon.config.HugeResultSetAction;
import cn.itcraft.jdbcmon.wrap.WrappedDataSource;
import cn.itcraft.jdbcmon.monitor.SqlMonitor;
import cn.itcraft.jdbcmon.monitor.SqlStatistics;

import javax.sql.DataSource;

WrappedConfig config = new WrappedConfig.Builder()
    .metricsLevel(MetricsLevel.BASIC)
    .sampleRate(10000)                 // 万分比，10000 = 全量
    .slowQueryThresholdMs(1000)
    .hugeResultSetThreshold(2000)
    .hugeResultSetAction(HugeResultSetAction.NOTIFY_IMMEDIATE)
    .build();

DataSource wrapped = new WrappedDataSource(targetDataSource, config);

SqlMonitor monitor = ((WrappedDataSource) wrapped).getSqlMonitor();
SqlStatistics stats = monitor.getStatistics();
System.out.println("总查询数: " + stats.getTotalQueries());
System.out.println("慢查询数: " + stats.getTotalSlowQueries());
```

注意：包装后的 `DataSource` 应在应用生命周期内复用，避免重复创建导致监控器与线程池重复。

#### 3.1.2 Spring Boot

引入 `jdbcmon-spring` 后，自动配置会包装容器中的所有 `DataSource` Bean（已是 `WrappedDataSource` 的跳过）。配置见 4.3。

```yaml
jdbcmon:
  enabled: true
  sample-rate: 10000
  slow-query-threshold-ms: 1000
  use-adaptive-threshold: true
  adaptive-percentile: 95.0
  adaptive-window-size-seconds: 60
  thread-pool:
    core-size: 2
    max-size: 4
    queue-capacity: 1000
  monitoring:
    connections: true
    transactions: true
    batch-operations: true
```

启用 Actuator 后可通过端点读取统计：

```
GET /actuator/jdbcmon
```

返回字段：`totalQueries`、`totalUpdates`、`totalBatchOps`、`totalErrors`、`totalSlowQueries`、`totalExecutions`、`errorRate`、`currentSlowQueryThreshold`。

### 3.2 方式二：Driver/URL 代理

在 JDBC URL 前追加 `jdbc:jdbcmon:` 前缀，jdbcmon 会剥离前缀交由真实驱动建连，并包装返回的连接。

```
原始 URL：jdbc:mysql://host:3306/db
代理 URL：jdbc:jdbcmon:mysql://host:3306/db
```

连接池以 HikariCP 为例（properties）：

```properties
spring.datasource.driver-class-name=cn.itcraft.jdbcmon.driver.JdbcMonDriver
spring.datasource.url=jdbc:jdbcmon:mysql://host:3306/db
```

支持的 URL 形态：

```
jdbc:jdbcmon:mysql://host:3306/db
jdbc:jdbcmon:h2:mem:test
jdbc:jdbcmon:oracle:thin:@host:1521:orcl
```

配置来源（优先级从高到低）：

1. 系统属性 `jdbcmon.*`
2. classpath 下 `jdbcmon.properties`
3. 内置默认值

`jdbcmon.properties` 示例：

```properties
jdbcmon.sampleRate=10000
jdbcmon.slowQueryThresholdMs=1000
jdbcmon.hugeResultSetThreshold=2000
jdbcmon.hugeResultSetAction=NOTIFY_IMMEDIATE
```

可编程覆盖（测试或嵌入场景）：

```java
cn.itcraft.jdbcmon.driver.JdbcMonDriverConfig.setConfig(config);
```

已知限制：真实驱动必须对 DriverManager 的调用方 ClassLoader 可见；应用容器多 ClassLoader 场景建议改用方式三或显式指定真实驱动。

### 3.3 方式三：javaagent

在 JVM 启动参数中挂载 agent jar，无需改动代码或数据源配置。

```bash
java -javaagent:/path/to/jdbcmon-agent.jar -jar app.jar
```

带参数（`=` 之后以 `;` 分隔，键可不带 `jdbcmon.` 前缀）：

```bash
java -javaagent:/path/to/jdbcmon-agent.jar=sampleRate=10000;slowQueryThresholdMs=1000 -jar app.jar
```

配置来源（优先级从高到低）：

1. agent 启动参数
2. 系统属性 `jdbcmon.*`
3. classpath 下 `jdbcmon.properties`
4. 内置默认值

工作原理：拦截所有 `java.sql.Driver` 实现类的 `connect(String, Properties)`，在返回处将连接包装为监控代理。选择该拦截点可同时覆盖 DriverManager 与连接池两条路径，并天然避免重复包装（已是监控连接则跳过）。

可编程覆盖：

```java
cn.itcraft.jdbcmon.agent.JdbcMonAgentConfig.setConfig(config);
```

已知限制：agent 自身类通过追加到系统类加载器搜索路径使其可见；应用容器子 ClassLoader 中的驱动可能无法解析 agent 类，此时需结合方式一或方式二。

## 4. 配置

### 4.1 WrappedConfig 编程 API

```java
WrappedConfig config = new WrappedConfig.Builder()
    .metricsLevel(MetricsLevel.BASIC)
    .sampleRate(100)
    .slowQueryThresholdMs(1000)
    .logSlowQueries(true)
    .collectStackTrace(false)
    .useAdaptiveThreshold(true)
    .adaptivePercentile(95.0)
    .adaptiveWindowSize(60)
    .enableLogging(true)
    .hugeResultSetThreshold(2000)
    .hugeResultSetAction(HugeResultSetAction.NOTIFY_IMMEDIATE)
    .threadPool(2, 4, 1000)
    .topSlowQueryLimit(10)
    .build();
```

### 4.2 配置项说明

| 配置项 | 默认值 | 说明 | 当前生效 |
|--------|--------|------|----------|
| metricsLevel | BASIC | 指标级别：BASIC / EXTENDED / FULL | 是 |
| sampleRate | 100 | 采样率（万分比，1..10000）：1=0.01%，100=1%，10000=100% | 是 |
| slowQueryThresholdMs | 1000 | 慢查询阈值（毫秒） | 是 |
| logSlowQueries | true | 慢查询是否写日志 | 是 |
| collectStackTrace | false | 是否采集调用栈（慢查询事件附带） | 是 |
| useAdaptiveThreshold | true | 是否启用自适应阈值 | 是 |
| adaptivePercentile | 95.0 | 自适应阈值分位数 | 是 |
| adaptiveWindowSize | 60 | 自适应窗口（秒） | 是 |
| enableLogging | true | 是否注册默认日志监听器 | 是 |
| hugeResultSetThreshold | 2000 | 超大结果集阈值（行） | 是 |
| hugeResultSetAction | NOTIFY_IMMEDIATE | 触发策略 | 是 |
| threadPool(core,max,queue) | 2/4/1000 | 事件异步线程池 | 是 |
| topSlowQueryLimit | 10 | 统计中 TOP N 慢查询条数 | 是 |
| enableMonitoring | true | 总开关 | 预留（Spring 侧经 `jdbcmon.enabled` 生效） |
| logParameters | false | 是否记录参数 | 预留 |
| enableMetrics | true | 是否启用指标 | 预留 |
| metricsFlushInterval | 60 | 指标刷新间隔（秒） | 预留 |
| monitorConnections | true | 连接监控 | 预留 |
| monitorTransactions | true | 事务监控 | 预留 |
| monitorBatchOperations | true | 批量操作监控 | 预留 |
| excludedTables / excludedSchemas / sqlPatternFilter | 空 | SQL 过滤 | 预留 |

`HugeResultSetAction` 取值：

| 取值 | 行为 |
|------|------|
| THROW_EXCEPTION | 达到阈值立即抛出 `HugeResultSetException`，终止读取 |
| NOTIFY_IMMEDIATE | 达到阈值立即发送告警事件，继续读取 |
| NOTIFY_AFTER | 结果集完全读取（close）后发送告警事件 |

### 4.3 Spring Boot 属性

| 属性 | 默认值 |
|------|--------|
| jdbcmon.enabled | true |
| jdbcmon.slow-query-threshold-ms | 1000 |
| jdbcmon.log-slow-queries | true |
| jdbcmon.collect-stack-trace | false |
| jdbcmon.use-adaptive-threshold | true |
| jdbcmon.adaptive-percentile | 95.0 |
| jdbcmon.adaptive-window-size-seconds | 60 |
| jdbcmon.thread-pool.core-size | 2 |
| jdbcmon.thread-pool.max-size | 4 |
| jdbcmon.thread-pool.queue-capacity | 1000 |
| jdbcmon.monitoring.connections | true |
| jdbcmon.monitoring.transactions | true |
| jdbcmon.monitoring.batch-operations | true |

### 4.4 属性文件（方式二/方式三）

`jdbcmon.properties` 置于 classpath 根目录，支持的键（均带 `jdbcmon.` 前缀）：

```properties
jdbcmon.enableMonitoring=true
jdbcmon.metricsLevel=BASIC
jdbcmon.sampleRate=100
jdbcmon.slowQueryThresholdMs=1000
jdbcmon.logSlowQueries=true
jdbcmon.collectStackTrace=false
jdbcmon.useAdaptiveThreshold=true
jdbcmon.enableLogging=true
jdbcmon.hugeResultSetThreshold=2000
jdbcmon.hugeResultSetAction=NOTIFY_IMMEDIATE
```

系统属性同名可覆盖。

### 4.5 agent 启动参数（方式三）

```bash
java -javaagent:jdbcmon-agent.jar=sampleRate=10000;slowQueryThresholdMs=1000;useAdaptiveThreshold=false -jar app.jar
```

键可不带前缀；值非法时记录告警并回退默认配置，不阻断启动。

## 5. 监控能力

### 5.1 慢查询

执行耗时超过 `slowQueryThresholdMs` 时：

- metricsMap 中该 SQL 计入慢查询计数
- 若 `logSlowQueries=true`，输出 WARN 日志 `[SLOW_SQL]`
- 触发 `SlowQueryEvent`（异步通知监听器）

### 5.2 自适应阈值

启用后，jdbcmon 基于滑动窗口内的耗时 P95 动态调整慢查询阈值，范围限制在 100ms 至 30000ms 之间。可通过 `SqlMonitor.setSlowQueryThresholdMs` 手动覆盖。

### 5.3 超大结果集

当结果集行数达到 `hugeResultSetThreshold` 时，按 `hugeResultSetAction` 处理（见 4.2）。行数在 `next()` 时累计，`close()` 时记录到对应 SQL 指标（`getTotalResultRows()`）。未正常关闭的结果集不计入最终行数记录。

异常类型 `HugeResultSetException` 继承 `SQLException`，提供 `getSql()`、`getRowCount()`、`getThreshold()`。

### 5.4 错误监控

执行抛错时计入错误总数并触发 `FailureEvent`。统计中可读取错误率 `getErrorRate()`。

### 5.5 采样

`sampleRate` 为万分比，决定多少次执行参与监控：

- 1 = 0.01%，100 = 1%（默认），10000 = 100%
- 10000 时走短路，不生成随机数

采样未命中的执行直接委托底层，跳过监控逻辑，用于控制高 QPS 场景的开销。

### 5.6 指标级别

| 级别 | 采集内容 | 相对开销 |
|------|----------|----------|
| BASIC | 执行次数、错误数、总耗时 | 最低（约 3-5%） |
| EXTENDED | 增加平均/最小/最大耗时 | 中等（约 5-8%） |
| FULL | 增加耗时直方图/百分位 | 最高（约 10-15%） |

## 6. 事件与监听器

所有事件实现统一接口 `MonEvent`，监听器只有一个方法。

```java
import cn.itcraft.jdbcmon.listener.SqlExecutionListener;
import cn.itcraft.jdbcmon.event.MonEvent;
import cn.itcraft.jdbcmon.event.SuccessEvent;
import cn.itcraft.jdbcmon.event.FailureEvent;
import cn.itcraft.jdbcmon.event.SlowQueryEvent;
import cn.itcraft.jdbcmon.event.HugeResultSetEvent;

SqlExecutionListener listener = event -> {
    switch (event.getEventType()) {
        case SUCCESS: {
            SuccessEvent e = (SuccessEvent) event;
            // e.getResult() / e.getResultAsInt() / e.getElapsedMillis()
            break;
        }
        case FAILURE: {
            FailureEvent e = (FailureEvent) event;
            // e.getError() / e.getErrorMessage()
            break;
        }
        case SLOW_QUERY: {
            SlowQueryEvent e = (SlowQueryEvent) event;
            // e.getThresholdMs() / e.getStackTrace()
            break;
        }
        case HUGE_RESULT_SET: {
            HugeResultSetEvent e = (HugeResultSetEvent) event;
            // e.getRowCount() / e.getThreshold()
            break;
        }
        default:
            break;
    }
};

monitor.addListener(listener);
```

事件公共方法（`MonEvent`）：`getEventType()`、`getContext()`、`getElapsedNanos()`、`getTimestampMillis()`、`getSource()`、`getSql()`。

监听器在独立线程池中异步回调，不阻塞业务线程；单个监听器抛错不影响其他监听器。

## 7. 统计指标

### 7.1 SqlStatistics

`monitor.getStatistics()` 返回快照：

| 方法 | 含义 |
|------|------|
| getTotalQueries | 查询总数 |
| getTotalUpdates | 更新总数 |
| getTotalBatchOps | 批量操作总数 |
| getTotalErrors | 错误总数 |
| getTotalSlowQueries | 慢查询总数 |
| getCurrentSlowQueryThreshold | 当前慢查询阈值（ms） |
| getTotalExecutions | 执行总数（三者之和） |
| getErrorRate | 错误率 |
| getSlowQueries | TOP N 慢查询列表（`SlowQueryInfo`） |
| getMetricsMap | SQL -> 指标映射 |

### 7.2 SqlMetrics（单条 SQL）

| 方法 | 含义 |
|------|------|
| getSqlKey | SQL 键 |
| getExecutionCount / getSuccessCount / getFailureCount | 次数 |
| getTotalTimeNanos / getMinTimeNanos / getMaxTimeNanos / getAvgTimeNanos | 耗时 |
| getRowsAffected | 影响行数 |
| getTotalResultRows | 结果集累计行数 |
| getHistogramData | 耗时直方图 |

### 7.3 SqlMonitor 运行时方法

| 方法 | 含义 |
|------|------|
| getStatistics() | 获取统计快照 |
| addListener() / removeListener() | 注册/移除监听器 |
| setMetricsLevel() / getMetricsLevel() | 运行时切换指标级别 |
| setSlowQueryThresholdMs() | 运行时调整慢查询阈值 |
| getMetricsMap() | 获取全部 SQL 指标 |
| getConfig() | 获取配置 |
| shutdown() | 关闭（释放异步线程池） |

## 8. 构建、测试与发布

### 8.1 常用命令

```bash
# 编译
mvn compile

# 运行全部测试
mvn test

# 运行单个测试类 / 方法
mvn test -Dtest=IntegrationTest
mvn test -Dtest=IntegrationTest#test_executeQuery_recordsMetrics

# 打包（跳过测试）
mvn package -DskipTests

# 完整构建（含 source/javadoc/gpg 校验）
mvn clean verify
```

jdbcmon-agent 的端到端测试使用 failsafe，绑定在 `integration-test` 阶段，随 `mvn verify` 执行。

### 8.2 多 JDK 构建

```bash
./build.sh          # JDK 8 + JDK 17 双版本
```

profile：

- `-Pjdk8`（默认）：编译目标 1.8，产物无 classifier
- `-Pjdk17`：编译目标 17，产物 classifier 为 jdk17

### 8.3 发布

已配置 Maven Central 发布（source、javadoc、gpg、central-publishing 插件）：

```bash
mvn clean deploy -Pjdk8
mvn clean deploy -Pjdk17
```

### 8.4 基准测试

```bash
./benchmark8.sh      # JDK 8
./benchmark17.sh     # JDK 17
./benchmark_all.sh   # 全部
```

## 9. 性能基准

### JDK 17（推荐）

| 场景 | Direct | Proxied | 开销 |
|------|--------|---------|------|
| PreparedQuery | 1,802,693 ops/s | 1,744,145 ops/s | 3.2% |
| MultiRowQuery | 564,842 ops/s | 466,772 ops/s | 17.4% |
| Insert | 788,785 ops/s | 746,455 ops/s | 5.4% |
| Update | 551,891 ops/s | 541,953 ops/s | 1.8% |
| ResultSet（10000 行） | 4,661 ops/s | 4,124 ops/s | 11.5% |

### JDK 8

| 场景 | Direct | Proxied | 开销 |
|------|--------|---------|------|
| PreparedQuery | 302,096 ops/s | 277,258 ops/s | 8.2% |
| MultiRowQuery | 153,412 ops/s | 153,909 ops/s | -0.3% |
| Insert | 305,411 ops/s | 290,696 ops/s | 4.8% |
| ResultSet（10000 行） | 4,477 ops/s | 4,040 ops/s | 9.8% |

结论：Query 开销通常 < 10%，符合设计目标；JDK 8 部分场景波动较大，可能为测量噪声。

## 10. 常见问题与已知限制

### 10.1 采样默认值

默认 `sampleRate=100`（1%），意味着仅约 1% 的执行被监控。若需全量监控，请显式设置为 10000：

```properties
jdbcmon.sampleRate=10000
```

### 10.2 连接未被包装

排查顺序：

1. 方式二：URL 是否加了 `jdbc:jdbcmon:` 前缀；连接池是否指定了 `JdbcMonDriver`
2. 方式三：`-javaagent` 是否生效；驱动是否在 agent 可见的 ClassLoader 中
3. 采样未命中：确认 `sampleRate` 与是否命中采样

### 10.3 重复包装

jdbcmon 对已是 `MonitoredConnection` 的连接不重复包装，因此方式二与方式三可共存而不重复计数。

### 10.4 多 ClassLoader 环境

应用容器（如 Tomcat、WildFly）中，子 ClassLoader 加载的驱动可能无法解析 agent 类，导致方式三失效。此时建议：

- 使用方式一显式包装，或
- 使用方式二并在连接池中显式指定真实驱动

### 10.5 agent 与 ByteBuddy 冲突

agent 已内置 ByteBuddy 且不做包名重定位；若应用自身也加载 ByteBuddy，理论上存在版本冲突可能，建议在测试环境先行验证。

### 10.6 关闭监控

- 方式一：不包装数据源，或在 Spring 中设置 `jdbcmon.enabled=false`
- 方式三：不添加 `-javaagent`

### 10.7 线程池与资源释放

方式一显式创建的 `WrappedDataSource` 在应用关闭时应调用 `monitor.shutdown()` 释放异步线程池。
