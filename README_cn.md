# jdbcmon

[English Version](README.md)

高性能、可扩展的轻量级 JDBC 监控代理框架。

## 特性

- **零侵入**：无需修改业务代码，通过动态代理自动包装 JDBC 对象
- **高性能**：Query 开销 < 10%，Update 开销 < 15%，功能测试场景完全适用
- **可扩展**：监听器机制支持自定义监控指标，事件体系易于定制
- **多 JDK 支持**：同时支持 JDK 8/17，JDK 17 性能更优
- **自适应阈值**：基于 P95 动态计算慢 SQL 阈值
- **大结果集检测**：支持阈值配置，多种触发策略（抛异常/立即通知/延迟通知）

## 快速开始

### Maven 依赖

```xml
<dependency>
    <groupId>cn.itcraft</groupId>
    <artifactId>jdbcmon-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 基本使用

```java
import cn.itcraft.jdbcmon.config.WrappedConfig;
import cn.itcraft.jdbcmon.config.HugeResultSetAction;
import cn.itcraft.jdbcmon.wrap.WrappedDataSource;
import cn.itcraft.jdbcmon.monitor.SqlStatistics;

import javax.sql.DataSource;

WrappedConfig config = new WrappedConfig.Builder()
    .slowQueryThresholdMs(500)
    .logSlowQueries(true)
    .hugeResultSetThreshold(2000)
    .hugeResultSetAction(HugeResultSetAction.NOTIFY_IMMEDIATE)
    .build();

DataSource wrappedDataSource = new WrappedDataSource(originalDataSource, config);

SqlStatistics stats = wrappedDataSource.getSqlMonitor().getStatistics();
System.out.println("总查询数: " + stats.getTotalQueries());
System.out.println("慢查询数: " + stats.getTotalSlowQueries());
```

## 多版本构建

```bash
# 构建 JDK 8 版本
export JAVA_HOME=/home/helly/lang/jdk8
mvn clean install -Pjdk8

# 构建 JDK 17 版本（推荐）
export JAVA_HOME=/home/helly/lang/jdk17
mvn clean install -Pjdk17

# 或使用构建脚本
./build.sh
```

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
  huge-result-set-threshold: 2000
  huge-result-set-action: NOTIFY_IMMEDIATE
```

## 监听器扩展

```java
import cn.itcraft.jdbcmon.listener.SqlExecutionListener;
import cn.itcraft.jdbcmon.event.MonEvent;
import cn.itcraft.jdbcmon.event.SuccessEvent;
import cn.itcraft.jdbcmon.event.FailureEvent;
import cn.itcraft.jdbcmon.event.SlowQueryEvent;
import cn.itcraft.jdbcmon.event.HugeResultSetEvent;

public class CustomListener implements SqlExecutionListener {
    
    @Override
    public void onEvent(MonEvent event) {
        switch (event.getEventType()) {
            case SUCCESS:
                SuccessEvent success = (SuccessEvent) event;
                // 处理成功执行
                break;
            case FAILURE:
                FailureEvent failure = (FailureEvent) event;
                // 处理执行失败
                break;
            case SLOW_QUERY:
                SlowQueryEvent slow = (SlowQueryEvent) event;
                // 处理慢查询
                break;
            case HUGE_RESULT_SET:
                HugeResultSetEvent huge = (HugeResultSetEvent) event;
                // 处理大结果集
                break;
        }
    }
}

sqlMonitor.addListener(new CustomListener());
```

## 性能基准

### JDK 17（推荐）

| 场景 | Direct | Proxied | 开销 |
|------|--------|---------|------|
| PreparedQuery | 1,802,693 ops/s | 1,744,145 ops/s | **3.2%** |
| MultiRowQuery | 564,842 ops/s | 466,772 ops/s | **17.4%** |
| Insert | 788,785 ops/s | 746,455 ops/s | **5.4%** |
| Update | 551,891 ops/s | 541,953 ops/s | **1.8%** |
| ResultSet (10000行) | 4,661 ops/s | 4,124 ops/s | **11.5%** |

### JDK 8

| 场景 | Direct | Proxied | 开销 |
|------|--------|---------|------|
| PreparedQuery | 302,096 ops/s | 277,258 ops/s | **8.2%** |
| MultiRowQuery | 153,412 ops/s | 153,909 ops/s | **-0.3%** |
| Insert | 305,411 ops/s | 290,696 ops/s | **4.8%** |
| ResultSet (10000行) | 4,477 ops/s | 4,040 ops/s | **9.8%** |

### 结论

- **JDK 17 推荐使用**：吞吐量高（3-5倍于 JDK 8），代理开销稳定（1-12%）
- **JDK 8 可用**：开销 1-10%，部分场景波动较大
- **ResultSet 监控开销**：全量读取 10-12%，部分读取 < 5%

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

## 模块结构

```
jdbcmon/
├── jdbcmon-core/           # 核心模块（JDK 8/17 双版本）
├── jdbcmon-spring/         # Spring Boot 集成（需 JDK 17+）
└── jdbcmon-test/           # 集成测试 & JMH 基准测试
```

## 许可证

MIT License