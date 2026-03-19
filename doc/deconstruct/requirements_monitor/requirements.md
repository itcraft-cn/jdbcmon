# monitor Package Requirements

## Functional Requirements

### REQ-MON-001: SQL Metrics Collection
**Priority:** Critical

The system shall collect performance metrics for each SQL statement.

**Acceptance Criteria:**
- AC-001: Execution count is tracked
- AC-002: Success count is tracked
- AC-003: Failure count is tracked
- AC-004: Total execution time is tracked
- AC-005: Min/max execution times are tracked (EXTENDED+)
- AC-006: Rows affected are tracked (EXTENDED+)
- AC-007: Time histogram is maintained (FULL)

### REQ-MON-002: Metrics Level Support
**Priority:** High

The system shall support multiple monitoring levels.

**Acceptance Criteria:**
- AC-001: BASIC level records count and total time
- AC-002: EXTENDED level adds min, max, and rows
- AC-003: FULL level adds time histogram
- AC-004: Level can be changed at runtime

### REQ-MON-003: Slow Query Detection
**Priority:** High

The system shall detect and report slow queries.

**Acceptance Criteria:**
- AC-001: Threshold is configurable in milliseconds
- AC-002: Slow query count is tracked
- AC-003: Slow queries can be logged
- AC-004: Stack traces can be collected optionally

### REQ-MON-004: Adaptive Threshold
**Priority:** Medium

The system shall support dynamic slow query thresholds.

**Acceptance Criteria:**
- AC-001: Threshold is calculated from execution history
- AC-002: Percentile is configurable (default P95)
- AC-003: Sliding window is used for calculation
- AC-004: Window size is configurable

### REQ-MON-005: Statistics Aggregation
**Priority:** High

The system shall provide aggregated statistics.

**Acceptance Criteria:**
- AC-001: Total queries count is available
- AC-002: Total updates count is available
- AC-003: Total errors count is available
- AC-004: Error rate is calculated
- AC-005: Top slow queries are identified

### REQ-MON-006: Per-SQL Metrics
**Priority:** High

The system shall maintain separate metrics for each unique SQL statement.

**Acceptance Criteria:**
- AC-001: SQL is used as key for metrics map
- AC-002: Same SQL shares metrics across executions
- AC-003: Metrics can be retrieved by SQL

### REQ-MON-007: Fast Path Recording
**Priority:** High

The system shall provide optimized recording for PreparedStatement.

**Acceptance Criteria:**
- AC-001: `recordQueryFast()` uses pre-cached metrics
- AC-002: `recordUpdateFast()` uses pre-cached metrics
- AC-003: No Map lookup on fast path methods

## Non-Functional Requirements

### NFR-MON-001: Thread Safety
**Priority:** Critical

All monitoring operations shall be thread-safe.

**Acceptance Criteria:**
- AC-001: Concurrent executions don't corrupt metrics
- AC-002: LongAdder is used for high-contention counters
- AC-003: AtomicLong is used for min/max updates

### NFR-MON-002: Low Overhead
**Priority:** Critical

Monitoring overhead shall be minimal.

**Acceptance Criteria:**
- AC-001: Recording overhead < 100ns for cached metrics
- AC-002: No synchronization on hot path
- AC-003: Pre-computed threshold comparison

### NFR-MON-003: Memory Efficiency
**Priority:** High

Memory usage shall not grow unbounded.

**Acceptance Criteria:**
- AC-001: SqlMetrics objects are reused for same SQL
- AC-002: No memory leak from listener registrations
- AC-003: Executor queue is bounded

## Technical Constraints

### TC-MON-001: Counter Types
- Use `LongAdder` for increment-only counters
- Use `AtomicLong` for compare-and-set operations (min/max)

### TC-MON-002: Pre-computation
Threshold in nanoseconds shall be pre-computed to avoid TimeUnit conversion on each check.