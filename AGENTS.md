# jdbcmon AGENTS.md

## 角色定位

1. 你是资深架构师
    - 在开发前，会对需求进行详尽分析，提供多套方案，以上、中、下三策的形式呈现
    - 在设计时，会充分考虑非功能性需求：安全性、可扩展性、可用性、可观测性、性能等
    - 在设计细节时，充分考虑各种设计模式及各语言特性
2. 你是资深开发者，对 Java 的 SDK/第三方库均非常了解，对 JDK 各版本间细节均了解，对 JVM 调优也非常擅长，尤其擅长性能调优/反射/多线程/Unsafe底层/网络通信

## 交互规则

1. 每次沟通产出文件后，均执行 git 提交
2. git 仅以当前 `user.name` 提交，不推送到远端
3. git 提交均遵循约定式提交规范（Conventional Commits）执行

## 环境信息

通过 skill /java-env 获取

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
ProxyConfig config = new ProxyConfig.Builder()
    .metricsLevel(MetricsLevel.BASIC)  // BASIC/EXTENDED/FULL
    .slowQueryThresholdMs(1000)
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
│   └── MonitoredCallableStatement.java
├── thread/                   # 线程相关
│   └── AsyncThreadExecutor.java
├── monitor/                  # 监控核心
├── listener/                 # 监听器接口（异步触发）
├── config/                   # 配置
└── consts/                   # 常量
```

## 编码规范

授权读取 /disk2/helly_data/code/markdown/self-ai-spec/java.spec.md

### 代码风格
- 使用 final 修饰不可变字段和类
- 避免不必要的注释，代码应自解释
- 使用 Builder 模式构建复杂对象
- 优先使用组合而非继承

### 性能要点
- SqlMetrics 缓存在 PreparedStatement 中，避免 Map 查找
- MetricsRecorder 策略模式：消除运行时级别检查
- 预计算阈值：slowQueryThresholdNanos 避免每次 TimeUnit 转换
- LongAdder 替代 AtomicLong 实现高并发计数
- 使用 ThreadLocal 复用对象（如 SqlExecutionContext）

## 基准测试结果

### JDK 8
| 场景 | Direct | Proxied | 开销 |
|------|--------|---------|------|
| PreparedQuery | 312,415 | 317,297 | **-1.6%** |
| MultiRowQuery | 170,814 | 165,715 | **3.0%** |
| Insert | 257,259 | 301,628 | **-17%** |
| Update | 113,232 | 84,548 | **25%** |
| Mixed 80/20 | 302,545 | 331,455 | **-9.5%** |

### JDK 17（推荐）
| 场景 | Direct | Proxied | 开销 |
|------|--------|---------|------|
| PreparedQuery | 1,969,363 | 1,878,606 | **4.6%** |
| MultiRowQuery | 567,270 | 581,435 | **-2.5%** |
| Insert | 781,382 | 755,629 | **3.3%** |
| Update | 565,425 | 539,450 | **4.6%** |
| Mixed 80/20 | 1,052,542 | 1,072,243 | **-1.9%** |

### 结论
- **JDK 17 推荐使用**：吞吐量最高，代理开销最低且稳定
- JDK 8 Update 开销偏高（25%），但其他场景表现良好