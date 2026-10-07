# jdbcmon Manual

[Chinese Manual](manual.md)

This manual is for users and operators. It covers integration modes, configuration, monitoring capabilities, and common issues. See README and AGENTS.md for architecture and design.

## Table of Contents

1. Introduction
2. Requirements and Dependencies
3. Integration Modes
4. Configuration
5. Monitoring Capabilities
6. Events and Listeners
7. Statistics
8. Build, Test and Release
9. Performance Benchmarks
10. FAQ and Known Limitations

---

## 1. Introduction

jdbcmon is a zero-intrusion, extensible, lightweight JDBC monitoring proxy framework. Without modifying business SQL, it transparently wraps connections, statements, and result sets, collecting slow queries, huge result sets, errors, and more, and provides event callbacks with asynchronous notification.

Highlights:

- Zero intrusion: three integration modes, the lightest of which only adds a `-javaagent` startup option
- Low overhead: query overhead is typically under 10%, negligible in functional testing
- Observable: slow queries, adaptive threshold, huge result sets, error rate
- Extensible: a unified event model with a single listener method
- Multi-JDK: works on JDK 8 and 17, with higher throughput on JDK 17

## 2. Requirements and Dependencies

### 2.1 JDK

| Item | Requirement |
|------|-------------|
| Minimum | JDK 8 |
| Recommended | JDK 17 |
| Mode 2 / Mode 3 runtime | Runs in the business JVM, no extra dependency (Mode 3 is a fat jar) |

### 2.2 Maven Coordinates

```xml
<!-- Core engine, required by all three modes -->
<dependency>
    <groupId>cn.itcraft</groupId>
    <artifactId>jdbcmon-core</artifactId>
    <version>1.1.0-dev</version>
</dependency>

<!-- Mode 2: Driver/URL proxy -->
<dependency>
    <groupId>cn.itcraft</groupId>
    <artifactId>jdbcmon-driver</artifactId>
    <version>1.1.0-dev</version>
</dependency>

<!-- Mode 1 Spring Boot integration -->
<dependency>
    <groupId>cn.itcraft</groupId>
    <artifactId>jdbcmon-spring</artifactId>
    <version>1.1.0-dev</version>
</dependency>
```

Mode 3 needs no Maven dependency; use the built artifact `jdbcmon-agent.jar`.

## 3. Integration Modes

The monitoring engine is decoupled from the delivery layer, so the same engine supports three integration modes that can be chosen or combined.

| Mode | Delivery | Entry Point | Intrusiveness | Coverage |
|------|----------|-------------|---------------|----------|
| Mode 1 Explicit wrapping | jdbcmon-core / jdbcmon-spring | Wrap DataSource in code | Code change or dependency | Only wrapped data sources |
| Mode 2 Driver/URL proxy | jdbcmon-driver | Prefix the JDBC URL | Zero code, config only | Connections via that driver |
| Mode 3 javaagent | jdbcmon-agent | JVM startup option | Zero | All JDBC driver connections in the JVM |

Selection guidance:

- Can change code and need precise control: Mode 1
- Cannot change code but can change connection pool config: Mode 2
- Fully zero-intrusion and connections are created internally by a framework: Mode 3

### 3.1 Mode 1: Explicit Wrapping

#### 3.1.1 Plain Java

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
    .sampleRate(10000)                 // basis points, 10000 = full capture
    .slowQueryThresholdMs(1000)
    .hugeResultSetThreshold(2000)
    .hugeResultSetAction(HugeResultSetAction.NOTIFY_IMMEDIATE)
    .build();

DataSource wrapped = new WrappedDataSource(targetDataSource, config);

SqlMonitor monitor = ((WrappedDataSource) wrapped).getSqlMonitor();
SqlStatistics stats = monitor.getStatistics();
System.out.println("Total queries: " + stats.getTotalQueries());
System.out.println("Slow queries: " + stats.getTotalSlowQueries());
```

Note: reuse the wrapped `DataSource` throughout the application lifecycle to avoid duplicate monitors and thread pools.

#### 3.1.2 Spring Boot

After adding `jdbcmon-spring`, auto-configuration wraps every `DataSource` bean in the context (those already being `WrappedDataSource` are skipped). See 4.3 for configuration.

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

With Actuator enabled, statistics are available at:

```
GET /actuator/jdbcmon
```

Returned fields: `totalQueries`, `totalUpdates`, `totalBatchOps`, `totalErrors`, `totalSlowQueries`, `totalExecutions`, `errorRate`, `currentSlowQueryThreshold`.

### 3.2 Mode 2: Driver/URL Proxy

Prefix the JDBC URL with `jdbc:jdbcmon:`. jdbcmon strips the prefix, lets the real driver establish the connection, and wraps the returned connection.

```
Original URL: jdbc:mysql://host:3306/db
Proxied  URL: jdbc:jdbcmon:mysql://host:3306/db
```

HikariCP example (properties):

```properties
spring.datasource.driver-class-name=cn.itcraft.jdbcmon.driver.JdbcMonDriver
spring.datasource.url=jdbc:jdbcmon:mysql://host:3306/db
```

Supported URL forms:

```
jdbc:jdbcmon:mysql://host:3306/db
jdbc:jdbcmon:h2:mem:test
jdbc:jdbcmon:oracle:thin:@host:1521:orcl
```

Configuration sources (highest priority first):

1. System properties `jdbcmon.*`
2. classpath `jdbcmon.properties`
3. Built-in defaults

`jdbcmon.properties` example:

```properties
jdbcmon.sampleRate=10000
jdbcmon.slowQueryThresholdMs=1000
jdbcmon.hugeResultSetThreshold=2000
jdbcmon.hugeResultSetAction=NOTIFY_IMMEDIATE
```

Programmatic override (testing or embedded use):

```java
cn.itcraft.jdbcmon.driver.JdbcMonDriverConfig.setConfig(config);
```

Known limitation: the real driver must be visible to the ClassLoader of the DriverManager caller. In application containers with multiple ClassLoaders, prefer Mode 3 or explicitly specify the real driver.

### 3.3 Mode 3: javaagent

Attach the agent jar to the JVM startup options; no code or data source changes are required.

```bash
java -javaagent:/path/to/jdbcmon-agent.jar -jar app.jar
```

With arguments (separated by `;` after `=`; the `jdbcmon.` prefix is optional):

```bash
java -javaagent:/path/to/jdbcmon-agent.jar=sampleRate=10000;slowQueryThresholdMs=1000 -jar app.jar
```

Configuration sources (highest priority first):

1. agent startup arguments
2. System properties `jdbcmon.*`
3. classpath `jdbcmon.properties`
4. Built-in defaults

How it works: it instruments the `connect(String, Properties)` method of every `java.sql.Driver` implementation and wraps the returned connection. This single interception point covers both DriverManager and connection pool paths and naturally avoids double wrapping (already monitored connections are skipped).

Programmatic override:

```java
cn.itcraft.jdbcmon.agent.JdbcMonAgentConfig.setConfig(config);
```

Known limitation: agent classes are made visible by appending the agent jar to the system class loader search path. Drivers loaded by child ClassLoaders in application containers may fail to resolve agent classes; combine with Mode 1 or Mode 2 in that case.

## 4. Configuration

### 4.1 WrappedConfig Programming API

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

### 4.2 Option Reference

| Option | Default | Description | Effective Now |
|--------|---------|-------------|---------------|
| metricsLevel | BASIC | Metric level: BASIC / EXTENDED / FULL | Yes |
| sampleRate | 100 | Sampling rate (basis points, 1..10000): 1=0.01%, 100=1%, 10000=100% | Yes |
| slowQueryThresholdMs | 1000 | Slow query threshold in ms | Yes |
| logSlowQueries | true | Log slow queries | Yes |
| collectStackTrace | false | Collect call stack (attached to slow query event) | Yes |
| useAdaptiveThreshold | true | Enable adaptive threshold | Yes |
| adaptivePercentile | 95.0 | Adaptive threshold percentile | Yes |
| adaptiveWindowSize | 60 | Adaptive window in seconds | Yes |
| enableLogging | true | Register the default logging listener | Yes |
| hugeResultSetThreshold | 2000 | Huge result set threshold (rows) | Yes |
| hugeResultSetAction | NOTIFY_IMMEDIATE | Trigger strategy | Yes |
| threadPool(core,max,queue) | 2/4/1000 | Async event thread pool | Yes |
| topSlowQueryLimit | 10 | TOP N slow queries in statistics | Yes |
| enableMonitoring | true | Master switch | Reserved (honored via `jdbcmon.enabled` on Spring) |
| logParameters | false | Log parameters | Reserved |
| enableMetrics | true | Enable metrics | Reserved |
| metricsFlushInterval | 60 | Metrics flush interval (s) | Reserved |
| monitorConnections | true | Connection monitoring | Reserved |
| monitorTransactions | true | Transaction monitoring | Reserved |
| monitorBatchOperations | true | Batch operation monitoring | Reserved |
| excludedTables / excludedSchemas / sqlPatternFilter | empty | SQL filtering | Reserved |

`HugeResultSetAction` values:

| Value | Behavior |
|-------|----------|
| THROW_EXCEPTION | Throw `HugeResultSetException` immediately at the threshold, aborting the read |
| NOTIFY_IMMEDIATE | Emit an alert event immediately at the threshold, then continue |
| NOTIFY_AFTER | Emit an alert event after the result set is fully read (on close) |

### 4.3 Spring Boot Properties

| Property | Default |
|----------|---------|
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

### 4.4 Property File (Mode 2 / Mode 3)

Place `jdbcmon.properties` at the classpath root. Supported keys (all with the `jdbcmon.` prefix):

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

Same-named system properties override these.

### 4.5 Agent Startup Arguments (Mode 3)

```bash
java -javaagent:jdbcmon-agent.jar=sampleRate=10000;slowQueryThresholdMs=1000;useAdaptiveThreshold=false -jar app.jar
```

The `jdbcmon.` prefix is optional. Invalid values are logged as warnings and fall back to defaults, never blocking startup.

## 5. Monitoring Capabilities

### 5.1 Slow Queries

When execution time exceeds `slowQueryThresholdMs`:

- The SQL is counted as a slow query in metricsMap
- If `logSlowQueries=true`, a WARN log `[SLOW_SQL]` is emitted
- A `SlowQueryEvent` is triggered (asynchronous listener notification)

### 5.2 Adaptive Threshold

When enabled, jdbcmon dynamically adjusts the slow query threshold based on the P95 of execution times over a sliding window, clamped between 100ms and 30000ms. It can be overridden manually via `SqlMonitor.setSlowQueryThresholdMs`.

### 5.3 Huge Result Sets

When a result set reaches `hugeResultSetThreshold` rows, it is handled per `hugeResultSetAction` (see 4.2). Rows are counted in `next()` and recorded to the SQL metric on `close()` (`getTotalResultRows()`). Result sets not closed normally are not recorded in the final row count.

The exception `HugeResultSetException` extends `SQLException` and provides `getSql()`, `getRowCount()`, `getThreshold()`.

### 5.4 Error Monitoring

Failures are counted toward the total error count and trigger a `FailureEvent`. The error rate is available via `getErrorRate()`.

### 5.5 Sampling

`sampleRate` is in basis points and determines how many executions participate in monitoring:

- 1 = 0.01%, 100 = 1% (default), 10000 = 100%
- At 10000 it short-circuits and generates no random number

Executions that miss the sampling are delegated directly to the underlying object, skipping monitoring logic, to control overhead under high QPS.

### 5.6 Metric Levels

| Level | Collected | Relative Overhead |
|-------|-----------|-------------------|
| BASIC | Execution count, errors, total time | Lowest (~3-5%) |
| EXTENDED | Adds avg/min/max time | Medium (~5-8%) |
| FULL | Adds time histogram/percentiles | Highest (~10-15%) |

## 6. Events and Listeners

All events implement the unified `MonEvent` interface, and a listener has a single method.

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

Common event methods (`MonEvent`): `getEventType()`, `getContext()`, `getElapsedNanos()`, `getTimestampMillis()`, `getSource()`, `getSql()`.

Listeners are invoked asynchronously on a dedicated thread pool, so they do not block business threads; an exception in one listener does not affect the others.

## 7. Statistics

### 7.1 SqlStatistics

`monitor.getStatistics()` returns a snapshot:

| Method | Meaning |
|--------|---------|
| getTotalQueries | Total queries |
| getTotalUpdates | Total updates |
| getTotalBatchOps | Total batch operations |
| getTotalErrors | Total errors |
| getTotalSlowQueries | Total slow queries |
| getCurrentSlowQueryThreshold | Current slow query threshold (ms) |
| getTotalExecutions | Total executions (sum of the three) |
| getErrorRate | Error rate |
| getSlowQueries | TOP N slow query list (`SlowQueryInfo`) |
| getMetricsMap | SQL -> metrics map |

### 7.2 SqlMetrics (per SQL)

| Method | Meaning |
|--------|---------|
| getSqlKey | SQL key |
| getExecutionCount / getSuccessCount / getFailureCount | Counts |
| getTotalTimeNanos / getMinTimeNanos / getMaxTimeNanos / getAvgTimeNanos | Timing |
| getRowsAffected | Affected rows |
| getTotalResultRows | Accumulated result set rows |
| getHistogramData | Time histogram |

### 7.3 SqlMonitor Runtime Methods

| Method | Meaning |
|--------|---------|
| getStatistics() | Get a statistics snapshot |
| addListener() / removeListener() | Register/unregister listeners |
| setMetricsLevel() / getMetricsLevel() | Switch metric level at runtime |
| setSlowQueryThresholdMs() | Adjust slow query threshold at runtime |
| getMetricsMap() | Get all SQL metrics |
| getConfig() | Get configuration |
| shutdown() | Shut down (release the async thread pool) |

## 8. Build, Test and Release

### 8.1 Common Commands

```bash
# Compile
mvn compile

# Run all tests
mvn test

# Run a single test class / method
mvn test -Dtest=IntegrationTest
mvn test -Dtest=IntegrationTest#test_executeQuery_recordsMetrics

# Package (skip tests)
mvn package -DskipTests

# Full build (with source/javadoc/gpg checks)
mvn clean verify
```

End-to-end tests for jdbcmon-agent use failsafe, bound to the `integration-test` phase, and run with `mvn verify`.

### 8.2 Multi-JDK Build

```bash
./build.sh          # JDK 8 + JDK 17 dual versions
```

Profiles:

- `-Pjdk8` (default): target 1.8, artifacts without classifier
- `-Pjdk17`: target 17, artifacts with classifier jdk17

### 8.3 Release

Maven Central publishing is configured (source, javadoc, gpg, central-publishing plugins):

```bash
mvn clean deploy -Pjdk8
mvn clean deploy -Pjdk17
```

### 8.4 Benchmarks

```bash
./benchmark8.sh      # JDK 8
./benchmark17.sh     # JDK 17
./benchmark_all.sh   # all
```

## 9. Performance Benchmarks

### JDK 17 (recommended)

| Scenario | Direct | Proxied | Overhead |
|----------|--------|---------|----------|
| PreparedQuery | 1,802,693 ops/s | 1,744,145 ops/s | 3.2% |
| MultiRowQuery | 564,842 ops/s | 466,772 ops/s | 17.4% |
| Insert | 788,785 ops/s | 746,455 ops/s | 5.4% |
| Update | 551,891 ops/s | 541,953 ops/s | 1.8% |
| ResultSet (10000 rows) | 4,661 ops/s | 4,124 ops/s | 11.5% |

### JDK 8

| Scenario | Direct | Proxied | Overhead |
|----------|--------|---------|----------|
| PreparedQuery | 302,096 ops/s | 277,258 ops/s | 8.2% |
| MultiRowQuery | 153,412 ops/s | 153,909 ops/s | -0.3% |
| Insert | 305,411 ops/s | 290,696 ops/s | 4.8% |
| ResultSet (10000 rows) | 4,477 ops/s | 4,040 ops/s | 9.8% |

Conclusion: query overhead is typically under 10%, meeting the design goal; some JDK 8 scenarios show high variance, likely measurement noise.

## 10. FAQ and Known Limitations

### 10.1 Default Sampling Rate

The default `sampleRate=100` (1%) means only about 1% of executions are monitored. For full monitoring, set it to 10000 explicitly:

```properties
jdbcmon.sampleRate=10000
```

### 10.2 Connection Not Wrapped

Troubleshooting order:

1. Mode 2: whether the URL has the `jdbc:jdbcmon:` prefix; whether the pool specifies `JdbcMonDriver`
2. Mode 3: whether `-javaagent` is effective; whether the driver is in a ClassLoader visible to the agent
3. Sampling miss: confirm `sampleRate` and whether the sample was hit

### 10.3 Double Wrapping

jdbcmon does not re-wrap connections that are already `MonitoredConnection`, so Mode 2 and Mode 3 can coexist without double counting.

### 10.4 Multi-ClassLoader Environments

In application containers (e.g., Tomcat, WildFly), drivers loaded by child ClassLoaders may fail to resolve agent classes, disabling Mode 3. In that case:

- Use Mode 1 explicit wrapping, or
- Use Mode 2 and explicitly specify the real driver in the connection pool

### 10.5 Agent vs ByteBuddy Conflict

The agent bundles ByteBuddy without package relocation; if the application also loads ByteBuddy, a version conflict is theoretically possible. Validate in a test environment first.

### 10.6 Disabling Monitoring

- Mode 1: do not wrap the data source, or set `jdbcmon.enabled=false` in Spring
- Mode 3: do not add `-javaagent`

### 10.7 Thread Pool and Resource Release

A `WrappedDataSource` created explicitly in Mode 1 should call `monitor.shutdown()` on application shutdown to release the async thread pool.
