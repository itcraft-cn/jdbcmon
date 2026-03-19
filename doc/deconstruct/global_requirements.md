# jdbcmon Global Requirements

## Project Vision

**jdbcmon** is a zero-intrusion, high-performance JDBC monitoring framework that enables applications to monitor database operations without code changes.

## Stakeholders

| Stakeholder | Interest |
|-------------|----------|
| Application Developers | Easy integration, no code changes |
| DevOps Engineers | Production monitoring, alerting |
| Performance Engineers | Low overhead, detailed metrics |
| Security Teams | No sensitive data exposure |

## Functional Requirements

### High Priority

#### REQ-001: Zero-Intrusion Monitoring
The system shall monitor JDBC operations without requiring changes to application code.

- Wrap existing DataSource objects
- Implement standard JDBC interfaces
- Transparent to application layer

#### REQ-002: Performance Metrics Collection
The system shall collect comprehensive performance metrics for all SQL operations.

- Execution count, success/failure counts
- Execution time (total, min, max, average)
- Rows affected for updates
- Time distribution histogram (optional)

#### REQ-003: Slow Query Detection
The system shall identify and report slow queries.

- Configurable threshold
- Adaptive threshold based on execution history
- Logging and listener notification

#### REQ-004: Multi-Level Monitoring
The system shall support multiple monitoring detail levels.

- BASIC: Minimal overhead (count + total time)
- EXTENDED: Additional metrics (min, max, rows)
- FULL: Complete metrics (histogram)

#### REQ-005: Async Event Notification
The system shall notify listeners asynchronously.

- Non-blocking listener execution
- Thread pool for async dispatch
- Support multiple listeners

### Medium Priority

#### REQ-006: Adaptive Threshold
The system shall support dynamic slow query thresholds.

- Percentile-based calculation
- Sliding window for history
- Configurable percentile and window

#### REQ-007: SQL Filtering
The system shall support selective monitoring.

- Exclude tables by name
- Exclude schemas by name
- Regex pattern matching

#### REQ-008: Statistics Aggregation
The system shall provide aggregated monitoring statistics.

- Global totals (queries, updates, errors)
- Error rate calculation
- Top slow queries

## Non-Functional Requirements

### Performance

#### NFR-001: Low Overhead
The system shall have minimal impact on application performance.

| Operation | Target | Measured (JDK 17) |
|-----------|--------|-------------------|
| Query | < 10% | ~5% |
| Update | < 15% | ~5% |
| Batch | < 20% | ~0% |
| Mixed | < 10% | ~-2% |

#### NFR-002: Scalability
The system shall scale with application load.

- Linear scalability with connection count
- No synchronization bottlenecks
- Efficient counter implementations (LongAdder)

#### NFR-003: Memory Efficiency
The system shall have bounded memory usage.

- Reuse SqlMetrics for same SQL
- Bounded executor queue
- No memory leaks

### Reliability

#### NFR-004: No Data Loss
The system shall not lose monitoring data.

- CallerRunsPolicy prevents task drops
- Failures in listeners don't affect application
- Graceful degradation under load

#### NFR-005: Thread Safety
All components shall be thread-safe.

- Concurrent access to shared state
- Lock-free counters where possible
- Immutable configuration

### Security

#### NFR-006: No Sensitive Data Exposure
The system shall not log or expose SQL parameters.

- Parameters are not recorded by default
- `logParameters` option requires explicit enablement
- SQL text only, no values

### Compatibility

#### NFR-007: JDK Compatibility
The system shall support multiple JDK versions.

- JDK 8 baseline
- JDK 17 recommended for best performance
- No JDK-specific APIs in core code

#### NFR-008: JDBC Driver Compatibility
The system shall work with any JDBC driver.

- Standard JDBC interfaces only
- No driver-specific APIs
- Tested with H2, MySQL, PostgreSQL

## Technical Constraints

### TC-001: No ResultSet Wrapping
ResultSet shall not be wrapped due to column access overhead.

### TC-002: Thread Pool Default
Default thread pool size: CPU cores / 2 (core), CPU cores (max).

### TC-003: Async Notification Only
All listener callbacks must be executed asynchronously.

## Quality Attributes

| Attribute | Requirement | Priority |
|-----------|-------------|----------|
| Performance | < 10% query overhead | Critical |
| Reliability | No application impact | Critical |
| Usability | Zero code changes | High |
| Extensibility | Listener interface | High |
| Compatibility | JDK 8+, Any JDBC driver | High |

## Success Metrics

| Metric | Target |
|--------|--------|
| Query overhead | < 10% |
| Update overhead | < 15% |
| Integration time | < 5 minutes |
| Code changes | 0 lines |
| Memory overhead | < 10MB per 1000 unique SQL |

## Out of Scope

1. SQL parsing and analysis
2. Connection pool management
3. Database-specific optimizations
4. Distributed tracing integration
5. Metrics export (Prometheus, etc.)