# OpenCode 设置

## 项目概述

jdbcmon 是一个高性能、可扩展的轻量级 JDBC 监控代理框架。

**设计目标**：
- 零侵入：无需修改业务代码
- 高性能：代理链开销 < 5%
- 可扩展：支持自定义监控指标
- JDK兼容：8-17 无缝迁移

## 角色定位

1. 你是资深架构师
   - 开发前详尽分析需求，提供上、中、下三策方案
   - 设计充分考虑非功能性需求：安全性、可扩展性、可用性、可观测性、性能
   - 设计细节充分考虑各种设计模式及语言特性

2. 你是资深开发者
   - 对 Java SDK/第三方库非常了解
   - 熟悉 JDK 各版本差异、JVM 调优、性能调优、反射、多线程、Unsafe、网络通信
   - 开发偏好面向对象编程（OOP）+ 接口

## 环境信息

通过 skill java-env 获取

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

# 完整构建（清理、编译、测试、打包）
mvn clean verify
```

## 代码风格指南

### 导入规范

- 分组导入：先标准 Java 库，再第三方库，用空行分隔
- 导入整个类：`import java.util.Objects`（而非单个静态方法）
- 内部常量类使用 static import：`import static cn.itcraft.jdbcmon.consts.JdbcConsts.*`
- `@throws` JavaDoc 中引用异常时使用完全限定类名

### 格式化规范

- 4 空格缩进，不使用制表符
- 花括号与语句在同一行：`public void method() {`
- 最大行长度 120 字符，超长换行
- 方法可见性修饰符放最前：`public static final`
- 开括号后、闭括号前、类/方法注释周围使用空行

### 命名约定

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| 类名 | 帕斯卡命名法 | `SqlMonitor`, `ProxyDataSource` |
| 方法名 | 驼峰命名法 | `recordSuccess`, `wrapConnection` |
| 变量名 | 驼峰命名法 | `sqlMonitor`, `proxyId` |
| 常量 | 大写蛇形命名法 | `SQL_DDL`, `TIME_BOUNDARIES` |
| 布尔方法 | is/has 前缀 | `isActive`, `isSlowQuery`, `hasConnection` |
| 处理器类 | Handler 后缀 | `ResultSetHandler` |
| 执行器类 | Executor 后缀 | `DbSqlExecutor` |
| 常量类 | Consts 后缀 | `JdbcConsts` |

### 类型系统

- 正确使用泛型：`<T extends SqlMetrics>`
- 尽可能指定泛型类型
- 字段不可变时标记为 `final`
- 高并发计数使用 `LongAdder` 而非 `AtomicLong`

### 错误处理

- 尽早验证：`Objects.requireNonNull(param, "param 不能为空")`
- 快速失败：构造函数和公共方法验证前置条件
- 描述性异常消息：`"ParameterName 不能为空"` 或 `"Code 不能为空"`
- 必要时使用防御性复制

### 日志规范（SLF4J）

```java
// 使用占位符
log.info("已注册 {}: {}", className, code);

// 错误日志三参数形式（完整堆栈）
log.error("执行失败: {}", e.getMessage(), e);

// 调试日志确保对象重写 toString()
log.debug("上下文: {}", context);
```

## 线程与并发规范

### 线程创建

- 禁止 `new Thread()`，必须使用线程池
- 线程必须命名：`new ThreadFactory() { ... }` 或 `ThreadBuilder`
- 线程池必须正确关闭，避免泄漏

### 并发工具

```java
// CountDownLatch 正确处理中断
try {
    latch.await();
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    log.warn("线程被中断", e);
}

// 高并发计数
private final LongAdder queryCount = new LongAdder();

// 线程安全存储
private final ConcurrentHashMap<String, SqlMetrics> metricsMap = new ConcurrentHashMap<>();
```

### 线程不安全类

- `SimpleDateFormat`、`DecimalFormat` 禁止声明为静态变量
- 使用 `ThreadLocal` 或 Java 8+ `DateTimeFormatter`

## 类设计模式

### 工具类

```java
final class SqlUtils {
    private SqlUtils() {}
    
    static String extractSql(Statement stmt) { ... }
}
```

### 常量类

```java
final class JdbcConsts {
    static final String SQL_QUERY = "SELECT ...";
    static final long DEFAULT_THRESHOLD_MS = 1000L;
    
    private JdbcConsts() {}
}
```

### 函数式接口

```java
@FunctionalInterface
interface ResultSetHandler<T> {
    T handle(ResultSet rs) throws SQLException;
}
```

## 项目结构

```
src/main/java/cn/itcraft/jdbcmon/
├── core/                    # 核心代理
│   ├── JdbcProxy.java
│   ├── ProxyInvocationHandler.java
│   └── SqlExecutionContext.java
├── datasource/              # 数据源代理
│   ├── ProxyDataSource.java
│   └── DataSourceBuilder.java
├── monitor/                 # 监控核心
│   ├── SqlMonitor.java
│   ├── SqlMetrics.java
│   └── SlowSqlDetector.java
├── listener/                # 监听器
│   ├── SqlExecutionListener.java
│   └── CompositeSqlListener.java
├── config/                  # 配置
│   └── ProxyConfig.java
└── consts/                  # 常量
    └── JdbcConsts.java
```

## Git 提交规范

1. 每次产出文件后执行 git 提交
2. 仅以当前 `user.name` 提交，不推送到远端
3. 遵循约定式提交规范（Conventional Commits）

```
feat: 新增 ResultSet 代理功能
fix: 修复连接池泄漏问题
docs: 更新 README
refactor: 重构指标收集逻辑
test: 添加 SqlMonitor 单元测试
```

## 测试规范

- 命名约定：`test_方法名_条件或预期行为()`
- 覆盖边界条件、无效输入、并发场景
- 使用 `@Before`/`@After` 保持测试隔离
- 包含负面测试用例（无效参数、不存在的值）

## 性能要点

- 使用对象池/ThreadLocal 减少 GC
- 方法缓存避免反射开销
- 异步处理监听器，不影响主线程
- LongAdder 替代 AtomicLong 实现高并发计数

## 编码规范详细

授权读取 `/disk2/helly_data/code/markdown/self-ai-spec/java.spec.md`