# Changelog

[Chinese Changelog](CHANGELOG.md)

This project adheres to [Semantic Versioning](https://semver.org/) and [Conventional Commits](https://www.conventionalcommits.org/). The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [1.1.0-dev] - Unreleased

### Added

- Sampling rate configuration (basis points, 1-10000) evaluated on the statement hot path for proportional capture
- `jdbcmon-driver` module: Mode 2 Driver/URL proxy, zero-code integration by prefixing the JDBC URL with `jdbc:jdbcmon:`
- `jdbcmon-agent` module: Mode 3 javaagent, instruments every JDBC driver `connect` call for zero-intrusion integration
- `WrappedConfigLoader`: unified Properties-based configuration loader shared by driver and agent
- `manual.md` user manual and its English counterpart `manual_en.md`

### Changed

- Sampling rate unit changed from percent (1-100) to basis points (1-10000)
- Added extensive Javadoc and configuration documentation

### Build

- Configured Maven Central publishing (source, javadoc, gpg, central-publishing plugins)
- Set javadoc plugin `doclint none` for compatibility with newer JDKs
- Upgraded gpg plugin from 1.5 to 3.2.7
- Ignored `MEMORY.md`

### Documentation

- Standardized bilingual docs: Chinese by default, English suffixed with `_en` (README, manual, CHANGELOG)
- Added three-mode integration guide, merged the Java template into AGENTS.md

## [1.0.0] - 2026-04-03

### Added

- Complete JDBC monitoring proxy framework: Connection, Statement, PreparedStatement, CallableStatement wrapping
- ResultSet wrapping and huge result set detection: configurable threshold with throw/immediate-notify/delayed-notify strategies
- Unified event system: single-method `MonEvent` listener covering Success/Failure/SlowQuery/HugeResultSet
- Slow query detection and adaptive threshold (P95 sliding window)
- Metric levels BASIC/EXTENDED/FULL with the `MetricsRecorder` strategy pattern
- Spring Boot integration: automatic DataSource wrapping and the Actuator endpoint `/actuator/jdbcmon`
- JMH benchmarks and multi-JDK build scripts

### Changed

- Renamed `ProxyConfig` to `WrappedConfig`
- Listeners made fully asynchronous
- Reorganized package structure, removing the `spi`, `Platform`, and `java17` directories

### Fixed

- Fixed build issues such as reserved words in benchmark column names
- Fixed multi-version build and dependency resolution issues

### Removed

- Removed the REFLECTION proxy mode, keeping only the wrapper mode
- Removed JDK 23 support

### Performance

- `MonitoredResultSet` uses a strategy pattern to eliminate branches on the `next()` hot path
- `MetricsRecorder` strategy pattern and precomputed slow query threshold
- `SqlMetrics` cached in PreparedStatement to avoid Map lookups
