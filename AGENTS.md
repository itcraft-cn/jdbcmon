# jdbcmon AGENTS.md

## 项目概述

jdbcmon 是一个高性能、可扩展的轻量级 JDBC 监控代理框架。

**设计目标**：
- 零侵入：无需修改业务代码
- 高性能：Query 开销 < 2%，Update 开销 < 15%
- 可扩展：支持自定义监控指标
- JDK兼容：8/17/23 多版本支持

## 模块结构

```
jdbcmon/
├── jdbcmon-core/           # 核心模块
│   ├── src/main/java/      # JDK 8 基础代码
│   ├── src/main/java17/    # JDK 17+ 优化
│   └── src/main/java23/    # JDK 23+ 优化
├── jdbcmon-spring/         # Spring Boot 集成
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
export JAVA_HOME=/home/helly/lang/jdk17
mvn clean install -Pjdk17
```

## JMH 基准测试

```bash
# 编译并运行基准测试
mvn clean compile test-compile -DskipTests -q
cd jdbcmon-test && mvn -q dependency:copy-dependencies -DoutputDirectory=target/dependency
java -cp "target/test-classes:../jdbcmon-core/target/jdbcmon-core-1.0.0-SNAPSHOT-jdk8.jar:target/dependency/*" \
  cn.itcraft.jdbcmon.benchmark.BenchmarkRunner QueryBenchmark

# 可用基准测试类：QueryBenchmark, UpdateBenchmark, BatchBenchmark, MixedBenchmark, ProxyModeBenchmark
```

## 核心配置

### 代理模式

```java
ProxyConfig config = new ProxyConfig.Builder()
    .proxyMode(ProxyMode.WRAPPER)  // WRAPPER(默认) 或 REFLECTION
    .metricsLevel(MetricsLevel.BASIC)  // BASIC/EXTENDED/FULL
    .slowQueryThresholdMs(1000)
    .build();
```

| 模式 | Query 开销 | 特点 |
|------|------------|------|
| WRAPPER | ~1.5% | 零反射，JIT 可内联 |
| REFLECTION | ~16% | 动态代理，灵活 |

### 度量级别

| 级别 | 统计内容 | 开销 |
|------|----------|------|
| BASIC | count + time + errors | ~1-2% |
| EXTENDED | + min/max + rows | ~3-5% |
| FULL | + histogram | ~5-10% |

```java
// 运行时调整
dataSource.getSqlMonitor().setMetricsLevel(MetricsLevel.FULL);
```

## 项目结构

```
jdbcmon-core/src/main/java/cn/itcraft/jdbcmon/
├── spi/                      # SPI 接口层
│   └── JdbcProxyFactory.java
├── proxy/                    # 代理实现
│   ├── wrapper/              # 套壳模式（高性能）
│   │   ├── WrapperProxyFactory.java
│   │   ├── MonitoredConnection.java
│   │   ├── MonitoredStatement.java
│   │   ├── MonitoredPreparedStatement.java
│   │   └── MonitoredCallableStatement.java
│   └── reflection/           # 反射模式
│       └── ReflectionProxyFactory.java
├── monitor/                  # 监控核心
│   ├── SqlMonitor.java       # 监控入口
│   ├── SqlMetrics.java       # 指标存储
│   ├── MetricsRecorder.java  # 策略接口
│   ├── BasicMetricsRecorder.java
│   ├── ExtendedMetricsRecorder.java
│   └── FullMetricsRecorder.java
├── config/                   # 配置
│   ├── ProxyConfig.java
│   ├── ProxyMode.java
│   └── MetricsLevel.java
└── consts/                   # 常量
```

## 代码风格指南

### 导入规范

- 分组导入：先标准 Java 库，再第三方库，用空行分隔
- 导入整个类：`import java.util.Objects`
- 内部常量使用 static import：`import static cn.itcraft.jdbcmon.consts.JdbcConsts.*`

### 格式化规范

- 4 空格缩进，不使用制表符
- 花括号与语句在同一行：`public void method() {`
- 最大行长度 120 字符
- 方法可见性修饰符放最前：`public static final`

### 命名约定

| 类型 | 规则 | 示例 |
|------|------|------|
| 类名 | 帕斯卡命名法 | `SqlMonitor`, `ProxyDataSource` |
| 方法名 | 驼峰命名法 | `recordSuccess`, `wrapConnection` |
| 变量名 | 驼峰命名法 | `sqlMonitor`, `proxyId` |
| 常量 | 大写蛇形命名法 | `DEFAULT_THRESHOLD_MS` |
| 布尔方法 | is/has 前缀 | `isActive`, `hasConnection` |

### 错误处理

- 尽早验证：`Objects.requireNonNull(param, "param 不能为空")`
- 快速失败：构造函数和公共方法验证前置条件
- 描述性异常消息

### 日志规范（SLF4J）

```java
log.info("已注册 {}: {}", className, code);
log.error("执行失败: {}", e.getMessage(), e);  // 三参数形式保留堆栈
```

## 并发规范

- 禁止 `new Thread()`，必须使用线程池
- 高并发计数使用 `LongAdder` 而非 `AtomicLong`
- `SimpleDateFormat` 禁止声明为静态变量

## Git 提交规范

遵循约定式提交规范（Conventional Commits）：

```
feat: 新增功能
fix: 修复问题
docs: 文档更新
refactor: 重构代码
test: 测试相关
perf: 性能优化
```

## 性能要点

- WRAPPER 模式：SqlMetrics 缓存在 PreparedStatement 中，避免 Map 查找
- MetricsRecorder 策略模式：消除运行时级别检查
- 预计算阈值：slowQueryThresholdNanos 避免每次 TimeUnit 转换
- LongAdder 替代 AtomicLong 实现高并发计数

## 基准测试结果

| 场景 | Direct | Proxied | 开销 |
|------|--------|---------|------|
| Query | 1,790,914 | 1,763,265 | **1.5%** |
| Update | 646,896 | 563,557 | **12.9%** |
| Batch | 586 | 651 | **-11%** |
| Mixed 80/20 | 1,042,905 | 975,167 | **6.5%** |