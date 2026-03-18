# jdbcmon AGENTS.md

## 项目概述

jdbcmon 是一个高性能、可扩展的轻量级 JDBC 监控代理框架。

**设计目标**：
- 零侵入：无需修改业务代码
- 高性能：代理链开销 < 5%（WRAPPER 模式）
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
# 编译测试模块
mvn test-compile -pl jdbcmon-test

# 运行基准测试（指定类）
java -cp "jdbcmon-test/target/test-classes:jdbcmon-core/target/jdbcmon-core-1.0.0-SNAPSHOT-jdk8.jar:jdbcmon-test/target/dependency/*" \
  cn.itcraft.jdbcmon.benchmark.BenchmarkRunner QueryBenchmark

# 可用的基准测试类：QueryBenchmark, UpdateBenchmark, BatchBenchmark, MixedBenchmark, ProxyModeBenchmark
```

## 双代理模式

jdbcmon 支持两种代理模式，通过 `ProxyConfig` 配置：

```java
// WRAPPER 模式（默认，高性能）
ProxyConfig config = new ProxyConfig.Builder()
    .proxyMode(ProxyMode.WRAPPER)
    .slowQueryThresholdMs(1000)
    .build();

// REFLECTION 模式（兼容性）
ProxyConfig config = new ProxyConfig.Builder()
    .proxyMode(ProxyMode.REFLECTION)
    .build();
```

| 模式 | 性能 | 特点 |
|------|------|------|
| WRAPPER | ~3-5% 开销 | 零反射，JIT 可内联，直接委托 |
| REFLECTION | ~15-20% 开销 | 动态代理，灵活，自动支持所有接口 |

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
├── core/                     # 核心代理
├── monitor/                  # 监控核心
├── config/                   # 配置
│   ├── ProxyConfig.java
│   └── ProxyMode.java
└── consts/                   # 常量
```

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

- WRAPPER 模式使用直接方法调用，无反射开销
- 使用对象池/ThreadLocal 减少 GC
- 监听器异步执行，不阻塞主线程
- LongAdder 替代 AtomicLong 实现高并发计数