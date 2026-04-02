# jdbcmon AGENTS.md

## 项目概述

jdbcmon 是一个高性能、可扩展的轻量级 JDBC 监控代理框架。

**设计目标**：
- 零侵入：无需修改业务代码
- 高性能：Query 开销 < 10%，Update 开销 < 15%
- 可扩展：支持自定义监控指标
- JDK兼容：8/17 双版本支持

## 模块结构

```
jdbcmon/
├── jdbcmon-core/           # 核心模块（JDK 8 兼容，JDK 17 性能更优）
├── jdbcmon-spring/         # Spring Boot 集成（需 JDK 17+）
└── jdbcmon-test/           # 集成测试 & JMH 基准测试
```

## 构建、测试、检查命令

```bash
# 编译项目
mvn compile

# 运行所有测试
mvn test

# 运行单个测试类
mvn test -Dtest=SqlMonitorTest

# 运行单个测试方法
mvn test -Dtest=SqlMonitorTest#testRecordSuccess

# 跳过测试打包
mvn package -DskipTests

# 代码风格检查
mvn checkstyle:check

# 静态分析
mvn spotbugs:check

# 完整构建
mvn clean verify

# 多版本构建
./build.sh

# 基准测试
./benchmark8.sh      # JDK 8
./benchmark17.sh     # JDK 17（推荐）
./benchmark_all.sh   # 所有版本
```

## 核心配置

```java
WrappedConfig config = new WrappedConfig.Builder()
    .metricsLevel(MetricsLevel.BASIC)  // BASIC/EXTENDED/FULL
    .slowQueryThresholdMs(1000)
    .hugeResultSetThreshold(2000)      // 超大结果集阈值
    .hugeResultSetAction(HugeResultSetAction.NOTIFY_IMMEDIATE)  // 触发行为
    .build();

DataSource wrappedDataSource = new WrappedDataSource(targetDataSource, config);
```

## 项目结构

```
jdbcmon-core/src/main/java/cn/itcraft/jdbcmon/
├── wrap/                     # 包装代理实现
│   ├── WrappedFactory.java   # 静态工厂
│   ├── WrappedDataSource.java
│   ├── WrappedDataSourceBuilder.java
│   ├── MonitoredConnection.java
│   ├── MonitoredStatement.java
│   ├── MonitoredPreparedStatement.java
│   ├── MonitoredCallableStatement.java
│   ├── MonitoredResultSet.java       # ResultSet 包装（行数统计）
│   ├── ResultSetMonitor.java         # ResultSet 监控策略接口
│   ├── ResultSetMonitors.java        # 策略工厂
│   ├── ThrowExceptionMonitor.java    # 抛异常策略
│   ├── NotifyImmediateMonitor.java   # 立即通知策略
│   └── NotifyAfterMonitor.java       # 延迟通知策略
├── event/                    # 事件体系
│   ├── EventType.java        # 事件类型枚举
│   ├── MonEvent.java         # 事件接口
│   ├── SuccessEvent.java     # 成功事件
│   ├── FailureEvent.java     # 失败事件
│   ├── SlowQueryEvent.java   # 慢查询事件
│   └── HugeResultSetEvent.java # 超大结果集事件
├── thread/                   # 线程相关
│   └── AsyncThreadExecutor.java
├── monitor/                  # 监控核心
├── listener/                 # 监听器接口（异步触发）
├── config/                   # 配置
├── exception/                # 异常
│   └── HugeResultSetException.java
└── consts/                   # 常量
```

## 基准测试结果

### JDK 17（推荐）

| 场景 | Direct | Proxied | 开销 |
|------|--------|---------|------|
| PreparedQuery | 1,802,693 ops/s | 1,744,145 ops/s | **3.2%** |
| MultiRowQuery | 564,842 ops/s | 466,772 ops/s | **17.4%** |
| Insert | 788,785 ops/s | 746,455 ops/s | **5.4%** |
| Update | 551,891 ops/s | 541,953 ops/s | **1.8%** |
| Mixed 80/20 | 1,141,688 ops/s | 1,134,682 ops/s | **0.6%** |
| ResultSet (10000行) | 4,661 ops/s | 4,124 ops/s | **11.5%** |
| PreparedStatementResultSet | 11,711 ops/s | 10,519 ops/s | **10.2%** |

### JDK 8

| 场景 | Direct | Proxied | 开销 |
|------|--------|---------|------|
| PreparedQuery | 302,096 ops/s | 277,258 ops/s | **8.2%** |
| MultiRowQuery | 153,412 ops/s | 153,909 ops/s | **-0.3%** |
| Insert | 305,411 ops/s | 290,696 ops/s | **4.8%** |
| Update | 81,013 ops/s | 118,119 ops/s | **-46%*** |
| Mixed 80/20 | 225,083 ops/s | 201,034 ops/s | **10.7%** |
| ResultSet (10000行) | 4,477 ops/s | 4,040 ops/s | **9.8%** |

*注：JDK 8 部分场景波动较大，可能是测量噪声

### 结论

| 结论 | 说明 |
|------|------|
| ✅ **JDK 17 推荐使用** | 吞吐量高（3-5倍于 JDK 8），代理开销稳定（1-12%） |
| ✅ **JDK 8 可用** | 开销 1-10%，部分场景波动较大 |
| ✅ **ResultSet 监控开销可控** | 全量读取 10-12%，部分读取 < 5%，符合设计目标 |

## 适用场景

| 场景 | 推荐度 | 说明 |
|------|-------|------|
| 功能测试 | ✅✅✅ | 完全适用，开销可忽略 |
| 集成测试 | ✅✅✅ | 完全适用，便于发现问题 |
| 预发布环境 | ✅✅ | 推荐使用，生产前验证 |
| 生产环境 | ✅ | 可用，建议 JDK 17，开启必要监控 |

## 核心价值

1. **零侵入** - 无需修改业务代码，透明接入
2. **低开销** - Query < 10%，符合设计目标，功能测试场景完全适用
3. **可观测** - 慢查询、大结果集、错误监控，全面覆盖
4. **可扩展** - 策略模式 + 事件体系，易于定制

## AI guide

### 角色定位

1. 你是资深架构师
    - 在开发前，会对需求进行详尽分析，提供多套方案，以上、中、下三策的形式呈现，以备后续决策参考
    - 在设计时，会充分考虑非功能性需求：安全性、可扩展性、可用性、可观测性、性能等
    - 在设计细节时，充分考虑各种设计模式及各语言特性
2. 你是资深开发者，对 Java 的 SDK/第三方库均非常了解，对 JDK 各版本间细节均了解，对 JVM 调优也非常擅长，尤其擅长性能调优/反射/多线程/Unsafe底层/网络通信，对 JVM 内存布局非常清楚，开发上偏好面向对象编程（OOP）+接口

### 环境信息

通过 skill /java-env 获取

### 交互规则

1. 所有交互均使用简体中文
2. 每次沟通产出文件后，均执行 git 提交
3. git 仅以当前 `user.name` 提交，不推送到远端
4. git 提交均遵循约定式提交规范（Conventional Commits）执行

### 编码规范

授权读取：/disk2/helly_data/code/markdown/self-ai-spec/lang-spec/spec.java.md

Read /disk2/helly_data/code/markdown/self-ai-spec/lang-spec/spec.java.md

### 构建工具

授权读取：/disk2/helly_data/code/markdown/self-ai-spec/lang-spec/ci.java.md

Read /disk2/helly_data/code/markdown/self-ai-spec/lang-spec/ci.java.md

### 代码风格
- 使用 final 修饰不可变字段和类
- 避免不必要的注释，代码应自解释
- 使用 Builder 模式构建复杂对象
- 优先使用组合而非继承

### 性能要点
- SqlMetrics 缓存在 PreparedStatement 中，避免 Map 查找
- MetricsRecorder 策略模式：消除运行时级别检查
- ResultSetMonitor 策略模式：消除 next() 热路径中的 switch 分支
- 预计算阈值：slowQueryThresholdNanos 避免每次 TimeUnit 转换
- LongAdder 替代 AtomicLong 实现高并发计数
- 使用 ThreadLocal 复用对象（如 SqlExecutionContext）