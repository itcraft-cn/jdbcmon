# JDBC Monitoring Core Data Flow

## Overview

The jdbcmon framework intercepts JDBC calls through wrapper classes and records performance metrics asynchronously. This document describes the data flow from application to database and back through the monitoring layer.

## High-Level Architecture

```mermaid
graph TB
    subgraph Application
        A[Application Code]
    end
    
    subgraph "jdbcmon Layer"
        DS[WrappedDataSource]
        MC[MonitoredConnection]
        MPS[MonitoredPreparedStatement]
        SM[SqlMonitor]
        MR[MetricsRecorder]
        M[SqlMetrics]
        ATE[AsyncThreadExecutor]
        L[SqlExecutionListener]
    end
    
    subgraph Database
        DB[(Database)]
    end
    
    A --> DS
    DS --> MC
    MC --> MPS
    MPS --> SM
    SM --> MR
    MR --> M
    SM --> ATE
    ATE --> L
    MPS --> DB
```

## Connection Acquisition Flow

```mermaid
sequenceDiagram
    participant App as Application
    participant WDS as WrappedDataSource
    participant MC as MonitoredConnection
    participant RealDS as Target DataSource
    
    App->>WDS: getConnection()
    WDS->>RealDS: getConnection()
    RealDS-->>WDS: Connection
    WDS->>MC: new MonitoredConnection(conn, monitor, config)
    WDS-->>App: MonitoredConnection
```

## PreparedStatement Creation Flow

```mermaid
sequenceDiagram
    participant App as Application
    participant MC as MonitoredConnection
    participant MPS as MonitoredPreparedStatement
    participant SM as SqlMonitor
    participant Metrics as SqlMetrics
    
    App->>MC: prepareStatement(sql)
    MC->>MC: delegate.prepareStatement(sql)
    MC->>MPS: new MonitoredPreparedStatement(stmt, monitor, sql)
    MPS->>SM: getOrCreateMetrics(sql)
    SM->>Metrics: new SqlMetrics(sql)
    SM-->>MPS: cachedMetrics
    MC-->>App: MonitoredPreparedStatement
    
    Note over MPS,Metrics: SqlMetrics is cached ONCE per PreparedStatement
```

## Query Execution Flow (Fast Path)

```mermaid
sequenceDiagram
    participant App as Application
    participant MPS as MonitoredPreparedStatement
    participant DB as Database
    participant SM as SqlMonitor
    participant MR as MetricsRecorder
    participant Metrics as SqlMetrics
    participant ATE as AsyncThreadExecutor
    
    App->>MPS: executeQuery()
    MPS->>MPS: start = System.nanoTime()
    MPS->>DB: delegate.executeQuery()
    DB-->>MPS: ResultSet
    MPS->>MPS: elapsed = nanoTime - start
    MPS->>SM: recordQueryFast(cachedMetrics, elapsed)
    
    SM->>SM: totalQueries.increment()
    SM->>MR: recordSuccess(metrics, elapsed, null)
    MR->>Metrics: addExecutionCount()
    MR->>Metrics: addTotalTime(elapsed)
    
    alt elapsed > threshold
        SM->>ATE: submit(onSlowQuery)
        Note over ATE: Async execution
    end
    
    SM-->>MPS: return
    MPS-->>App: ResultSet
```

## Statement vs PreparedStatement Flow

```mermaid
flowchart LR
    subgraph PreparedStatement [PreparedStatement - FAST]
        PS1[Application] --> PS2[MonitoredPreparedStatement]
        PS2 --> PS3[executeQuery]
        PS3 --> PS4[SqlMonitor.recordQueryFast]
        PS4 --> PS5[Cached SqlMetrics]
    end
    
    subgraph Statement [Statement - SLOWER]
        S1[Application] --> S2[MonitoredStatement]
        S2 --> S3[executeQuery sql]
        S3 --> S4[SqlMonitor.recordQuery]
        S4 --> S5[Map.computeIfAbsent]
        S5 --> S6[New SqlMetrics]
    end
```

**Key Difference:**
- **PreparedStatement**: SqlMetrics is cached at creation time, avoiding Map lookup on each execution
- **Statement**: Must perform Map lookup (`computeIfAbsent`) on every execution since SQL is dynamic

## Metrics Recording Strategy

```mermaid
flowchart TB
    subgraph MetricsLevel
        BASIC[BASIC<br/>Count + TotalTime]
        EXTENDED[EXTENDED<br/>+ Min/Max + Rows]
        FULL[FULL<br/>+ Histogram]
    end
    
    subgraph Recorders
        BMR[BasicMetricsRecorder]
        EMR[ExtendedMetricsRecorder]
        FMR[FullMetricsRecorder]
    end
    
    BASIC --> BMR
    EXTENDED --> EMR
    FULL --> FMR
```

| Level | Overhead | Features |
|-------|----------|----------|
| BASIC | ~1% | Execution count, total time |
| EXTENDED | ~3% | + Min/max time, rows affected |
| FULL | ~5% | + Time histogram for percentiles |

## Async Event Notification

```mermaid
sequenceDiagram
    participant SM as SqlMonitor
    participant ATE as AsyncThreadExecutor
    participant CSL as CompositeSqlListener
    participant L1 as Listener 1
    participant L2 as Listener 2
    
    SM->>ATE: submit(event)
    Note over ATE: Thread pool execution
    ATE->>CSL: onSuccess/onFailure/onSlowQuery
    CSL->>L1: onSuccess(context, elapsed, result)
    CSL->>L2: onSuccess(context, elapsed, result)
```

**Important:** All listener callbacks are executed asynchronously to avoid blocking the application thread.

## Slow Query Detection

```mermaid
flowchart TB
    Start[SQL Execution Complete] --> Check{elapsed > threshold?}
    Check -->|No| Return[Return Result]
    Check -->|Yes| Increment[Increment totalSlowQueries]
    Increment --> Log{logSlowQueries enabled?}
    Log -->|Yes| LogSlow[Log WARN message]
    Log -->|No| CheckAdaptive
    LogSlow --> CheckAdaptive{useAdaptiveThreshold?}
    CheckAdaptive -->|Yes| Adaptive[Update AdaptiveThreshold]
    CheckAdaptive -->|No| Notify
    Adaptive --> Notify[Notify listeners async]
    Notify --> Return
```

## Adaptive Threshold Algorithm

```mermaid
flowchart LR
    subgraph Input
        Time[Execution Time]
    end
    
    subgraph AdaptiveThreshold
        Bucket[Time Bucket<br/>e.g., 0-50ms, 50-100ms...]
        Window[Sliding Window<br/>60 seconds]
        Calc[Calculate P95]
    end
    
    subgraph Output
        Threshold[Dynamic Threshold]
    end
    
    Time --> Bucket
    Bucket --> Window
    Window --> Calc
    Calc --> Threshold
```

The adaptive threshold:
1. Buckets execution times into ranges
2. Maintains counts per bucket over a sliding window
3. Calculates the configured percentile (default P95)
4. Returns dynamic threshold for slow query detection