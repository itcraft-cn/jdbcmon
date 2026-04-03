# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**jdbcmon** - High-performance, extensible lightweight JDBC monitoring proxy framework.

- **Zero Intrusion**: No business code modification, transparent integration via dynamic proxy
- **Low Overhead**: Query < 10%, Update < 15% (JDK 17 recommended)
- **Multi-JDK**: Supports JDK 8/17, dual-version build
- **Features**: Slow query detection, adaptive P95 threshold, huge ResultSet monitoring, extensible listener system

## Module Structure

```
jdbcmon/
├── jdbcmon-core/           # Core module - JDBC proxy, metrics, listeners (JDK 8/17)
├── jdbcmon-spring/         # Spring Boot auto-configuration + Actuator endpoint (JDK 17+)
└── jdbcmon-test/           # Integration tests + JMH benchmarks
```

## Build Commands

```bash
# Build JDK 8 version (default)
export JAVA_HOME=/home/helly/lang/jdk8
mvn clean install -Pjdk8

# Build JDK 17 version (recommended for production)
export JAVA_HOME=/home/helly/lang/jdk17
mvn clean install -Pjdk17

# Run tests only
mvn test -pl jdbcmon-test

# Run single test class
mvn test -pl jdbcmon-test -Dtest=SqlMetricsTest

# Run JMH benchmarks
mvn test -pl jdbcmon-test -Dtest=*Benchmark
```

## Architecture

### Core Components (jdbcmon-core)

| Package | Responsibility |
|---------|----------------|
| `wrap` | JDBC proxy wrappers (Connection/Statement/PreparedStatement/ResultSet) |
| `monitor` | Metrics collection, SqlMonitor, adaptive threshold |
| `listener` | Event-driven listener system (SqlExecutionListener) |
| `event` | Event types (Success/Failure/SlowQuery/HugeResultSet) |
| `config` | WrappedConfig (builder pattern), MetricsLevel enum |
| `exception` | Custom exceptions (HugeResultSetException) |

### Entry Point

```java
WrappedConfig config = new WrappedConfig.Builder()
    .slowQueryThresholdMs(500)
    .logSlowQueries(true)
    .hugeResultSetThreshold(2000)
    .hugeResultSetAction(HugeResultSetAction.NOTIFY_IMMEDIATE)
    .build();

DataSource wrapped = new WrappedDataSource(originalDataSource, config);
```

### Spring Boot Integration (jdbcmon-spring)

Auto-configuration wraps all `DataSource` beans via `BeanPostProcessor`.

Properties prefix: `jdbcmon.`
- `slow-query-threshold-ms`: Slow query threshold
- `use-adaptive-threshold`: Enable P95 adaptive threshold
- `huge-result-set-threshold`: ResultSet row threshold
- `huge-result-set-action`: THROW_EXCEPTION | NOTIFY_IMMEDIATE | NOTIFY_AFTER

Actuator endpoint: `/actuator/jdbcmon` (exposes SqlMonitor metrics)

### Extension Pattern

Implement `SqlExecutionListener` for custom monitoring:

```java
public class CustomListener implements SqlExecutionListener {
    @Override
    public void onEvent(MonEvent event) {
        // Handle SUCCESS/FAILURE/SLOW_QUERY/HUGE_RESULT_SET events
    }
}
sqlMonitor.addListener(new CustomListener());
```

## Key Design Patterns

- **Decorator Pattern**: WrappedConnection/Statement/ResultSet wrap original JDBC objects
- **Observer Pattern**: Event system with listener registration
- **Builder Pattern**: WrappedConfig configuration
- **Strategy Pattern**: HugeResultSetAction (THROW_EXCEPTION/NOTIFY_IMMEDIATE/NOTIFY_AFTER)
- **Factory Pattern**: WrappedFactory creates appropriate proxy wrappers

## Performance Notes

- JDK 17: 3-5x throughput vs JDK 8, proxy overhead 1-12%
- ResultSet monitoring: Full read 10-12% overhead, partial read < 5%
- Recommended for functional/integration testing, staging, and production with JDK 17

## AI Guide

### Role

1. **Senior Architect**: Analyze requirements thoroughly, provide multiple solutions (upper/middle/lower strategies), consider non-functional requirements (security, scalability, observability, performance)
2. **Senior Developer**: Deep Java SDK/third-party library knowledge, JVM tuning expertise, performance optimization, OOP + interface preference

### Environment

Get via `skill /java-env`

### Interaction Rules

1. All communication in Simplified Chinese
2. Commit after each file change
3. Git commits use current `user.name`, do not push to remote
4. Follow Conventional Commits specification
5. Record important content/TODO Plan to MEMORY.md (ignored by git)

### Coding Standards

Read: `/disk2/helly_data/code/markdown/self-ai-spec/lang-spec/spec.java.md`

### Build Tools

Read: `/disk2/helly_data/code/markdown/self-ai-spec/lang-spec/ci.java.md`
