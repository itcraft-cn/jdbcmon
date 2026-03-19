# config Package Requirements

## Functional Requirements

### REQ-CFG-001: Builder Pattern
**Priority:** High

Configuration shall use the Builder pattern for construction.

**Acceptance Criteria:**
- AC-001: `WrappedConfig.Builder` provides fluent API
- AC-002: All options have setter methods
- AC-003: `build()` returns immutable configuration
- AC-004: Default values are applied automatically

### REQ-CFG-002: Monitoring Settings
**Priority:** High

The system shall provide configuration for monitoring behavior.

**Acceptance Criteria:**
- AC-001: `metricsLevel` configures recording detail
- AC-002: `enableMonitoring` enables/disables monitoring
- AC-003: `slowQueryThresholdMs` sets slow query threshold
- AC-004: `logSlowQueries` enables slow query logging

### REQ-CFG-003: Adaptive Threshold Settings
**Priority:** Medium

The system shall provide configuration for adaptive threshold.

**Acceptance Criteria:**
- AC-001: `useAdaptiveThreshold` enables/disables feature
- AC-002: `adaptivePercentile` sets percentile for threshold
- AC-003: `adaptiveWindowSizeSeconds` sets window size

### REQ-CFG-004: Thread Pool Settings
**Priority:** Medium

The system shall provide configuration for thread pool.

**Acceptance Criteria:**
- AC-001: `corePoolSize` sets core thread count
- AC-002: `maxPoolSize` sets maximum thread count
- AC-003: `queueCapacity` sets queue size
- AC-004: `threadPool(core, max, queue)` sets all at once

### REQ-CFG-005: Logging Settings
**Priority:** Low

The system shall provide configuration for logging.

**Acceptance Criteria:**
- AC-001: `enableLogging` enables event logging
- AC-002: `logParameters` enables parameter logging (security risk)
- AC-003: `collectStackTrace` enables stack trace collection

### REQ-CFG-006: Filter Settings
**Priority:** Medium

The system shall provide configuration for SQL filtering.

**Acceptance Criteria:**
- AC-001: `addExcludedTable(table)` excludes table from monitoring
- AC-002: `addExcludedSchema(schema)` excludes schema
- AC-003: `sqlPatternFilter(regex)` filters by pattern
- AC-004: Multiple filters can be combined

### REQ-CFG-007: Metrics Level
**Priority:** High

The system shall define monitoring granularity levels.

**Acceptance Criteria:**
- AC-001: BASIC level for minimal overhead
- AC-002: EXTENDED level for additional metrics
- AC-003: FULL level for complete metrics

## Non-Functional Requirements

### NFR-CFG-001: Immutability
**Priority:** High

Configuration objects shall be immutable after construction.

**Acceptance Criteria:**
- AC-001: No setters on WrappedConfig
- AC-002: All fields are final
- AC-003: Thread-safe without synchronization

### NFR-CFG-002: Default Values
**Priority:** High

Sensible defaults shall be provided for all options.

**Acceptance Criteria:**
- AC-001: Configuration works with no explicit settings
- AC-002: Defaults defined in JdbcConsts
- AC-003: Defaults are production-ready

### NFR-CFG-003: Validation
**Priority:** Medium

Configuration shall validate input values.

**Acceptance Criteria:**
- AC-001: Invalid values throw exception
- AC-002: Validation occurs in `build()`
- AC-003: Error messages are descriptive

## Technical Constraints

### TC-CFG-001: Backward Compatibility
- New options should have defaults
- Existing options should not change behavior

### TC-CFG-002: Type Safety
- Use enums for finite options (MetricsLevel)
- Use primitives for numeric values where possible