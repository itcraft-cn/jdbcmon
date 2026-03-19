# jdbcmon Global Design

## Project Overview

**jdbcmon** is a high-performance, zero-intrusion JDBC monitoring framework that wraps JDBC objects to provide metrics collection without modifying application code.

## Core Design Principles

### 1. Zero Intrusion
- No code changes required in application
- Wraps existing `DataSource` objects
- Transparent to application code

### 2. High Performance
- Query overhead < 10%
- Update overhead < 15%
- Achieved through:
  - Metrics caching in PreparedStatement
  - Strategy pattern for recording levels
  - Pre-computed thresholds
  - Async event notification

### 3. Extensibility
- Listener interface for custom metrics
- Configurable monitoring levels
- Filter patterns for selective monitoring

## Architecture Overview

```mermaid
graph TB
    subgraph Application
        A[App Code]
    end
    
    subgraph "jdbcmon Layer"
        subgraph "Entry Point"
            WDS[WrappedDataSource]
            WDSB[WrappedDataSourceBuilder]
        end
        
        subgraph "JDBC Wrappers"
            MC[MonitoredConnection]
            MS[MonitoredStatement]
            MPS[MonitoredPreparedStatement]
            MCS[MonitoredCallableStatement]
        end
        
        subgraph "Monitoring Core"
            SM[SqlMonitor]
            M[SqlMetrics]
            SS[SqlStatistics]
            AT[AdaptiveThreshold]
        end
        
        subgraph "Strategy"
            MR[MetricsRecorder]
            BMR[Basic]
            EMR[Extended]
            FMR[Full]
        end
        
        subgraph "Events"
            CSL[CompositeSqlListener]
            LSL[LoggingSqlListener]
            ATE[AsyncThreadExecutor]
        end
        
        subgraph "Configuration"
            WC[WrappedConfig]
            ML[MetricsLevel]
        end
    end
    
    subgraph Database
        DB[(Database)]
    end
    
    A --> WDS
    WDS --> MC
    MC --> MS
    MC --> MPS
    MC --> MCS
    MS --> SM
    MPS --> SM
    MCS --> SM
    SM --> MR
    MR --> M
    SM --> ATE
    ATE --> CSL
    CSL --> LSL
    MS --> DB
    MPS --> DB
    MCS --> DB
    WDS --> WC
    SM --> WC
```

## Package Dependencies

```mermaid
graph LR
    wrap --> monitor
    wrap --> config
    monitor --> config
    monitor --> listener
    monitor --> thread
    listener --> core
    thread --> config
```

## Core Data Structures

### SqlMetrics (Per-SQL Metrics)

```mermaid
classDiagram
    class SqlMetrics {
        +sqlKey: String
        +executionCount: LongAdder
        +successCount: LongAdder
        +failureCount: LongAdder
        +totalTimeNanos: LongAdder
        +minTimeNanos: AtomicLong
        +maxTimeNanos: AtomicLong
        +rowsAffected: LongAdder
        +recordSuccess()
        +recordFailure()
    }
```

### MetricsRecorder (Strategy)

```mermaid
classDiagram
    class MetricsRecorder {
        <<interface>>
        +recordSuccess(SqlMetrics, long, Object)
        +recordFailure(SqlMetrics, long, Throwable)
    }
    class BasicMetricsRecorder {
        +recordSuccess()
        +recordFailure()
    }
    class ExtendedMetricsRecorder {
        +recordSuccess()
        +recordFailure()
    }
    class FullMetricsRecorder {
        +recordSuccess()
        +recordFailure()
    }
    MetricsRecorder <|.. BasicMetricsRecorder
    MetricsRecorder <|.. ExtendedMetricsRecorder
    MetricsRecorder <|.. FullMetricsRecorder
```

## Key Design Patterns

### 1. Proxy Pattern
All `MonitoredXxx` classes wrap JDBC interfaces.

### 2. Builder Pattern
`WrappedConfig` and `WrappedDataSourceBuilder` provide fluent configuration.

### 3. Strategy Pattern
`MetricsRecorder` allows different recording behaviors.

### 4. Observer Pattern
`SqlExecutionListener` enables event-driven monitoring.

### 5. Composite Pattern
`CompositeSqlListener` manages multiple listeners.

### 6. Factory Pattern
`WrappedFactory` centralizes wrapper creation.

## Performance Optimizations

### PreparedStatement Metrics Caching

```mermaid
flowchart TB
    subgraph Without-Cache [Statement: Each Execution]
        S1[executeQuery] --> S2[Map.computeIfAbsent]
        S2 --> S3[Create SqlMetrics]
        S3 --> S4[Record]
    end
    
    subgraph With-Cache [PreparedStatement: Cached]
        P1[prepareStatement] --> P2[Cache SqlMetrics]
        P2 --> P3[executeQuery]
        P3 --> P4[Use Cached Metrics]
        P4 --> P5[Record - No Lookup]
    end
```

### Pre-computed Threshold

```java
// Computed once at construction
slowQueryThresholdNanos = TimeUnit.MILLISECONDS.toNanos(config.getSlowQueryThresholdMs());

// Fast comparison on each execution
if (elapsedNanos > slowQueryThresholdNanos) { ... }
```

### LongAdder for Counters

| Counter Type | Contention | Throughput |
|--------------|------------|------------|
| AtomicLong | High | Low |
| LongAdder | High | High |

### Async Notification

```mermaid
sequenceDiagram
    participant App
    participant Monitor
    participant Executor
    participant Listener
    
    App->>Monitor: recordQueryFast()
    Monitor->>Monitor: Update counters (sync)
    Monitor->>Executor: submit(event)
    Note over Executor: Non-blocking
    Monitor-->>App: Return
    Executor->>Listener: onSuccess() (async)
```

## Thread Safety

### Thread-Safe Components

| Component | Mechanism |
|-----------|-----------|
| SqlMetrics | LongAdder, AtomicLong |
| SqlMonitor | volatile fields, concurrent map |
| WrappedDataSource | Immutable after construction |
| MonitoredXxx | Thread-confined (per-connection) |

### Concurrency Model

```mermaid
graph TB
    subgraph "Thread 1"
        C1[Connection 1]
        PS1[PreparedStatement 1]
    end
    
    subgraph "Thread 2"
        C2[Connection 2]
        PS2[PreparedStatement 2]
    end
    
    subgraph "Shared"
        SM[SqlMonitor]
        M[MetricsMap]
    end
    
    PS1 --> SM
    PS2 --> SM
    SM --> M
```

- Each `MonitoredConnection` is used by one thread
- `SqlMonitor` is shared but thread-safe
- `SqlMetrics` uses lock-free counters

## Extension Points

### 1. Custom Listener

```java
sqlMonitor.addListener(new SqlExecutionListener() {
    @Override
    public void onSuccess(SqlExecutionContext ctx, long elapsed, Object result) {
        // Custom metrics collection
    }
});
```

### 2. Custom Configuration

```java
WrappedConfig config = new WrappedConfig.Builder()
    .metricsLevel(MetricsLevel.EXTENDED)
    .slowQueryThresholdMs(500)
    .build();
```

### 3. Selective Monitoring

```java
config.addExcludedTable("audit_log")
      .sqlPatternFilter("^(?!SELECT).*");  // Exclude non-SELECT
```

## Performance Targets

| Metric | Target | Achieved (JDK 17) |
|--------|--------|-------------------|
| Query overhead | < 10% | ~5% |
| Update overhead | < 15% | ~5% |
| Batch overhead | < 20% | ~0% |
| Mixed overhead | < 10% | ~-2% |

## Monitoring Flow Summary

```mermaid
sequenceDiagram
    participant App
    participant WDS as WrappedDataSource
    participant MC as MonitoredConnection
    participant MPS as MonitoredPreparedStatement
    participant SM as SqlMonitor
    participant MR as MetricsRecorder
    participant ATE as AsyncThreadExecutor
    
    App->>WDS: getConnection()
    WDS->>MC: wrap
    App->>MC: prepareStatement(sql)
    MC->>MPS: wrap + cache metrics
    App->>MPS: executeQuery()
    MPS->>SM: recordQueryFast(cached, elapsed)
    SM->>MR: recordSuccess()
    SM->>ATE: submit(listener callback)
    ATE-->>App: (async)
```

## Future Considerations

1. **Connection Pool Integration**: Direct integration with HikariCP, Druid
2. **Metrics Export**: Prometheus, Micrometer integration
3. **Query Analysis**: SQL parsing for table/operation identification
4. **Distributed Tracing**: OpenTelemetry integration