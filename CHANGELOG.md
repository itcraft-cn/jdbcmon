# 更新日志

[English Changelog](CHANGELOG_en.md)

本项目遵循 [语义化版本](https://semver.org/lang/zh-CN/) 与 [约定式提交](https://www.conventionalcommits.org/zh-hans/)，格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)。

## [1.1.0-dev] - 未发布

### 新增

- 采样率配置（万分比 1-10000），在语句执行热路径前置判断，支持按比例采集
- `jdbcmon-driver` 模块：方式二 Driver/URL 代理，URL 加 `jdbc:jdbcmon:` 前缀即可零代码接入
- `jdbcmon-agent` 模块：方式三 javaagent，拦截所有 JDBC 驱动 `connect` 调用，零侵入接入
- `WrappedConfigLoader`：统一的 Properties 配置加载器，供 driver/agent 复用
- 使用手册 `MANUAL.md` 与英文版 `MANUAL_en.md`

### 变更

- 采样率单位由百分比（1-100）改为万分比（1-10000）
- 补充大量 Javadoc 与配置说明

### 构建

- 配置 Maven Central 发布（source、javadoc、gpg、central-publishing 插件）
- javadoc 插件设置 `doclint none`，兼容高版本 JDK
- gpg 插件由 1.5 升级至 3.2.7
- 忽略 `MEMORY.md`

### 文档

- 双语文档体系规范化：默认中文，英文以 `_en` 结尾（README、MANUAL、CHANGELOG）
- 补充三模接入说明，AGENTS.md 合并 Java 模板

## [1.0.0] - 2026-04-03

### 新增

- 完整的 JDBC 监控代理框架：Connection、Statement、PreparedStatement、CallableStatement 包装
- ResultSet 包装与超大结果集检测：阈值配置 + 抛异常/立即通知/延迟通知三种策略
- 统一事件体系：`MonEvent` 单一监听方法，覆盖 Success/Failure/SlowQuery/HugeResultSet
- 慢查询检测与自适应阈值（基于 P95 滑动窗口）
- 指标级别 BASIC/EXTENDED/FULL 与 `MetricsRecorder` 策略模式
- Spring Boot 集成：自动包装 DataSource，提供 Actuator 端点 `/actuator/jdbcmon`
- JMH 基准测试与多 JDK 构建脚本

### 变更

- `ProxyConfig` 重命名为 `WrappedConfig`
- 监听器改为完全异步
- 重构包结构，移除 `spi`、`Platform`、`java17` 目录

### 修复

- 修正基准测试列名保留字等构建问题
- 修正多版本构建与依赖解析问题

### 移除

- 移除 REFLECTION 代理模式，仅保留包装模式
- 移除 JDK 23 支持

### 性能

- `MonitoredResultSet` 采用策略模式，消除 `next()` 热路径分支
- `MetricsRecorder` 策略模式与预计算慢查询阈值
- `SqlMetrics` 缓存于 PreparedStatement，避免 Map 查找
