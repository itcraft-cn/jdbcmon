# jdbcmon

[中文版](README_cn.md)

A high-performance, extensible lightweight JDBC monitoring proxy framework.

## Features

- **Zero Intrusion**: No need to modify business code, automatically wraps JDBC objects via dynamic proxy
- **High Performance**: Query overhead < 10%, Update overhead < 15%, suitable for functional testing scenarios
- **Extensible**: Listener mechanism supports custom monitoring metrics, event system easy to customize
- **Multi-JDK Support**: Supports both JDK 8/17, JDK 17 offers better performance
- **Adaptive Threshold**: Dynamically calculates slow SQL threshold based on P95
- **Huge ResultSet Detection**: Supports threshold configuration with multiple trigger strategies (throw exception / immediate notification / delayed notification)

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>cn.itcraft</groupId>
    <artifactId>jdbcmon-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Basic Usage

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
System.out.println("Total queries: " + stats.getTotalQueries());
System.out.println("Slow queries: " + stats.getTotalSlowQueries());
```

## Multi-Version Build

```bash
# Build JDK 8 version
export JAVA_HOME=/home/helly/lang/jdk8
mvn clean install -Pjdk8

# Build JDK 17 version (recommended)
export JAVA_HOME=/home/helly/lang/jdk17
mvn clean install -Pjdk17

# Or use build script
./build.sh
```

## Spring Boot Integration

### Maven Dependency

```xml
<dependency>
    <groupId>cn.itcraft</groupId>
    <artifactId>jdbcmon-spring</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Configuration Properties

```yaml
jdbcmon:
  enabled: true
  slow-query-threshold-ms: 1000
  log-slow-queries: true
  use-adaptive-threshold: true
  huge-result-set-threshold: 2000
  huge-result-set-action: NOTIFY_IMMEDIATE
```

## Listener Extension

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
                // Handle successful execution
                break;
            case FAILURE:
                FailureEvent failure = (FailureEvent) event;
                // Handle execution failure
                break;
            case SLOW_QUERY:
                SlowQueryEvent slow = (SlowQueryEvent) event;
                // Handle slow query
                break;
            case HUGE_RESULT_SET:
                HugeResultSetEvent huge = (HugeResultSetEvent) event;
                // Handle huge result set
                break;
        }
    }
}

sqlMonitor.addListener(new CustomListener());
```

## Performance Benchmarks

### JDK 17 (Recommended)

| Scenario | Direct | Proxied | Overhead |
|----------|--------|---------|----------|
| PreparedQuery | 1,802,693 ops/s | 1,744,145 ops/s | **3.2%** |
| MultiRowQuery | 564,842 ops/s | 466,772 ops/s | **17.4%** |
| Insert | 788,785 ops/s | 746,455 ops/s | **5.4%** |
| Update | 551,891 ops/s | 541,953 ops/s | **1.8%** |
| ResultSet (10000 rows) | 4,661 ops/s | 4,124 ops/s | **11.5%** |

### JDK 8

| Scenario | Direct | Proxied | Overhead |
|----------|--------|---------|----------|
| PreparedQuery | 302,096 ops/s | 277,258 ops/s | **8.2%** |
| MultiRowQuery | 153,412 ops/s | 153,909 ops/s | **-0.3%** |
| Insert | 305,411 ops/s | 290,696 ops/s | **4.8%** |
| ResultSet (10000 rows) | 4,477 ops/s | 4,040 ops/s | **9.8%** |

### Conclusions

- **JDK 17 Recommended**: High throughput (3-5x JDK 8), stable proxy overhead (1-12%)
- **JDK 8 Usable**: 1-10% overhead, some scenarios show variance
- **ResultSet Monitoring Overhead**: Full read 10-12%, partial read < 5%

## Usage Scenarios

| Scenario | Recommendation | Description |
|----------|----------------|-------------|
| Functional Testing | ✅✅✅ | Fully suitable, negligible overhead |
| Integration Testing | ✅✅✅ | Fully suitable, helps identify issues |
| Staging Environment | ✅✅ | Recommended for pre-production validation |
| Production | ✅ | Usable, recommend JDK 17 with essential monitoring |

## Core Value

1. **Zero Intrusion** - No business code modification required, transparent integration
2. **Low Overhead** - Query < 10%, meets design goals, fully suitable for functional testing
3. **Observable** - Slow queries, huge result sets, error monitoring, comprehensive coverage
4. **Extensible** - Strategy pattern + event system, easy to customize

## Module Structure

```
jdbcmon/
├── jdbcmon-core/           # Core module (JDK 8/17 dual versions)
├── jdbcmon-spring/         # Spring Boot integration (requires JDK 17+)
└── jdbcmon-test/           # Integration tests & JMH benchmarks
```

## License

MIT License