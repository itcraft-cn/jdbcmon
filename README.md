# jdbcmon

高性能、可扩展的轻量级 JDBC 监控代理框架。

## 特性

- **零侵入**：无需修改业务代码，通过动态代理自动包装 JDBC 对象
- **高性能**：代理链开销 < 5%，MethodHandle 方法调用，LongAdder 高并发计数
- **可扩展**：监听器机制支持自定义监控指标
- **多 JDK 支持**：同时支持 JDK 8/17/23，高版本启用专属优化
- **自适应阈值**：基于 P95 动态计算慢 SQL 阈值

## 快速开始

### Maven 依赖

```xml
<dependency>
    <groupId>cn.itcraft</groupId>
    <artifactId>jdbcmon-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <classifier>jdk8</classifier>  <!-- 或 jdk17、jdk23 -->
</dependency>
```

### 基本使用

```java
import cn.itcraft.jdbcmon.config.ProxyConfig;
import cn.itcraft.jdbcmon.wrap.WrappedDataSourceBuilder;
import cn.itcraft.jdbcmon.monitor.SqlStatistics;

import javax.sql.DataSource;

// 创建配置
ProxyConfig config = new ProxyConfig.Builder()
    .slowQueryThresholdMs(500)
    .logSlowQueries(true)
    .useAdaptiveThreshold(true)
    .build();

// 包装数据源
DataSource wrappedDataSource = WrappedDataSourceBuilder.create(originalDataSource)
    .config(config)
    .build();

if (wrappedDataSource instanceof cn.itcraft.jdbcmon.wrap.WrappedDataSource) {
    SqlStatistics stats = ((cn.itcraft.jdbcmon.wrap.WrappedDataSource) wrappedDataSource)
        System.out.println("总查询数: " + stats.getTotalQueries());
    System.out.println("慢查询数: " + stats.getTotalSlowQueries());
}
```

## 多版本构建

```bash
# 构建 JDK 8 版本
export JAVA_HOME=/home/helly/lang/jdk8
mvn clean install -Pjdk8

# 构建 JDK 17 版本
export JAVA_HOME=/home/helly/lang/jdk17
mvn clean install -Pjdk17

# 构建 JDK 23 版本
export JAVA_HOME=/home/helly/lang/jdk23
mvn clean install -Pjdk23

# 或使用构建脚本
./build.sh
```

### 版本特性

| JDK | 特性 |
|-----|------|
| **8** | MethodHandle + LongAdder + ThreadPoolExecutor |
| **17+** | VarHandle + Record 数据类 |
| **23+** | Virtual Threads + Scoped Values |

## Spring Boot 集成

### Maven 依赖

```xml
<dependency>
    <groupId>cn.itcraft</groupId>
    <artifactId>jdbcmon-spring</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 配置属性

```yaml
jdbcmon:
  enabled: true
  slow-query-threshold-ms: 1000
  log-slow-queries: true
  use-adaptive-threshold: true
  adaptive-percentile: 95.0
  thread-pool:
    core-size: 2
    max-size: 4
    queue-capacity: 1000
  monitoring:
    connections: true
    transactions: true
    batch-operations: true
```

### Actuator 端点

访问 `/actuator/jdbcmon` 获取监控指标：

```json
{
  "totalQueries": 1234,
  "totalUpdates": 567,
  "totalBatchOps": 89,
  "totalErrors": 2,
  "totalSlowQueries": 15,
  "errorRate": 0.001,
  "currentSlowQueryThreshold": 850
}
```

## 监听器扩展

```java
import cn.itcraft.jdbcmon.listener.SqlExecutionListener;
import cn.itcraft.jdbcmon.core.SqlExecutionContext;

public class CustomListener implements SqlExecutionListener {
    
    @Override
    public void onSuccess(SqlExecutionContext context, long elapsedNanos, Object result) {
        // 处理成功执行
    }
    
    @Override
    public void onFailure(SqlExecutionContext context, long elapsedNanos, Throwable throwable) {
        // 处理执行失败
    }
    
    @Override
    public void onSlowQuery(SqlExecutionContext context, long elapsedMillis) {
        // 处理慢查询
    }
}

// 注册监听器
sqlMonitor.addListener(new CustomListener());
```

## 性能指标

| 场景 | 预期开销 | 说明 |
|------|---------|------|
| **无监控** | < 1% | 只做方法转发 |
| **基础监控** | 1-3% | 执行时间、计数 |
| **全量监控** | 3-5% | 直方图、错误分类 |
| **异步日志** | 1-2% | 异步处理监听器 |

## 模块结构

```
jdbcmon/
├── jdbcmon-core/           # 核心模块
│   ├── src/main/java/      # JDK 8 基础代码
│   ├── src/main/java17/    # JDK 17+ 优化
│   └── src/main/java23/    # JDK 23+ 优化
├── jdbcmon-spring/         # Spring Boot 集成
└── jdbcmon-test/           # 集成测试
```

## 许可证

MIT License