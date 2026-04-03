# JDBCMon 改进待办事项

**创建日期**: 2026/04/03  
**来源**: 代码走查报告 (java-review-20260403-001.md)

---

## P0 - 立即执行

### [ ] MonitoredResultSet 拆分

**问题**: `MonitoredResultSet.java` 1026 行，违反单一职责原则

**改进方案**:
```
MonitoredResultSet (主类，~100 行)
├── ResultSetGetters (数据获取，~300 行)
├── ResultSetUpdaters (数据更新，~400 行)
├── ResultSetNavigation (导航方法，~100 行)
└── ResultSetMetaData (元数据方法，~50 行)
```

**影响**: 可维护性、可测试性

---

## P1 - 短期执行

### [ ] PreparedStatement/CallableStatement 代码复用

**问题**: 两个类各有 700+ 行，大量参数设置方法完全重复

**改进方案**: 抽取 `BaseMonitoredStatement` 抽象基类
```java
abstract class BaseMonitoredStatement {
    // 公共的 60+ 个参数设置方法
    void setInt(int index, int x) { delegate.setInt(index, x); }
    void setString(int index, String x) { delegate.setString(index, x); }
}
```

**影响**: 代码重复、维护成本

---

### [ ] MonitoredConnection 事务监控完善

**问题**: `close()`、`setAutoCommit()` 等方法缺少监控

**改进方案**:
```java
@Override
public void close() throws SQLException {
    long start = System.nanoTime();
    try {
        delegate.close();
        monitor.recordTransaction("CONNECTION_CLOSE", System.nanoTime() - start);
    } catch (SQLException e) {
        monitor.recordError("CONNECTION_CLOSE", System.nanoTime() - start, e);
        throw e;
    }
}
```

**影响**: 监控完整性

---

## P2 - 中期执行

### [ ] LoggingSqlListener SQL 脱敏

**问题**: 日志中可能输出敏感数据（密码、身份证号等）

**改进方案**:
```java
private String sanitizeSql(String sql) {
    if (sql == null) return "";
    return sql.replaceAll("(?i)VALUES\\s*\\([^)]+\\)", "VALUES(...)");
}
```

**影响**: 安全性

---

### [ ] SqlExecutionContext ThreadLocal 清理

**问题**: 容器环境中可能导致内存泄漏

**改进方案**:
```java
public static void release(SqlExecutionContext ctx) {
    if (ctx != null) {
        ctx.reset();
        POOL.remove();  // 明确清理
    }
}
```

**影响**: 内存泄漏

---

## P3 - 择机执行

### [ ] CompositeSqlListener 异常日志

**问题**: 监听器异常被吞掉，无法调试

**改进方案**:
```java
catch (Exception e) {
    LoggerFactory.getLogger(CompositeSqlListener.class)
        .warn("Listener {} threw exception: {}", listener.getClass(), e.getMessage());
}
```

**影响**: 可调试性

---

### [ ] AdaptiveThreshold 精度修复

**问题**: 桶值相加而非次数累加，P95 计算不准确

**改进方案**: 修改为计数模式
```java
timeBuckets[bucketIndex].increment();  // 而非 add(bucketValue)
```

**影响**: 准确性

---

### [ ] 补充类级别 JavaDoc

**问题**: 大部分公共类缺少 JavaDoc

**改进方案**:
```java
/**
 * SQL 监控核心类，负责：
 * 1. 记录 SQL 执行时间、次数、错误等指标
 * 2. 检测慢查询并触发事件
 * 3. 管理监听器和事件分发
 * 
 * @author Helly Guo
 * @since 1.0.0
 */
public final class SqlMonitor {
    // ...
}
```

**影响**: 可读性

---

## 进度跟踪

| 任务 | 优先级 | 状态 | 完成日期 |
|------|--------|------|----------|
| MonitoredResultSet 拆分 | P0 | ⏳ 待办 | - |
| 代码复用抽取基类 | P1 | ⏳ 待办 | - |
| 事务监控完善 | P1 | ⏳ 待办 | - |
| SQL 脱敏 | P2 | ⏳ 待办 | - |
| ThreadLocal 清理 | P2 | ⏳ 待办 | - |
| 异常日志 | P3 | ⏳ 待办 | - |
| AdaptiveThreshold 修复 | P3 | ⏳ 待办 | - |
| JavaDoc 补充 | P3 | ⏳ 待办 | - |

---

## 备注

- 优先级说明：
  - **P0**: 严重影响可维护性，需立即处理
  - **P1**: 重要改进，应在下一个迭代周期完成
  - **P2**: 中等优先级，涉及安全性和稳定性
  - **P3**: 低优先级，可在空闲时完善

- 本文件由代码走查自动生成，后续根据实际开发进度更新

Reviewed by java-review+qwen3.5-plus
