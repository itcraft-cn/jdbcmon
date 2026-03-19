# jdbcmon 设计文档

## 1. 设计目标

| 目标 | 说明 |
|------|------|
| **零侵入** | 无需修改业务代码，通过动态代理自动包装 JDBC 对象 |
| **高性能** | 代理链开销 < 5%，方法缓存 + LongAdder + 异步监听 |
| **可扩展** | 监听器机制支持自定义监控指标 |
| **JDK 兼容** | 同时支持 JDK 8/17/23，高版本启用专属优化 |
| **生产就绪** | 完善的异常处理和资源管理 |

## 2. 架构设计

### 2.1 模块结构

```
jdbcmon/
├── jdbcmon-core/                    # 核心模块（无外部依赖）
│   ├── src/main/java/               # JDK 8 基础代码
│   ├── src/main/java17/             # JDK 17+ 专用实现
│   └── src/main/java23/             # JDK 23+ 专用实现
├── jdbcmon-spring/                  # Spring Boot 集成
└── jdbcmon-test/                    # 集成测试
```

### 2.2 核心组件

```
jdbcmon-core/src/main/java/cn/itcraft/jdbcmon/
├── wrap/                              # 包装代理实现
│   ├── WrappedFactory.java            # 静态工厂
│   ├── WrappedDataSource.java         # 包装数据源
│   ├── WrappedDataSourceBuilder.java  # 建造者
│   ├── MonitoredConnection.java       # Connection 包装
│   ├── MonitoredStatement.java        # Statement 包装
│   ├── MonitoredPreparedStatement.java
│   └── MonitoredCallableStatement.java
│
├── thread/                            # 线程相关
│   └── AsyncThreadExecutor.java       # 异步线程执行器
│
├── core/                              # 核心代理
│   └── SqlExecutionContext.java       # SQL 执行上下文
│
├── monitor/                           # 监控核心
│   ├── SqlMonitor.java                # 监控核心
│   ├── SqlMetrics.java                # 单 SQL 指标
│   ├── SqlStatistics.java             # 统计聚合
│   └── AdaptiveThreshold.java         # 自适应阈值算法
│
├── listener/                          # 监听器
│   ├── SqlExecutionListener.java      # 监听器接口
│   ├── CompositeSqlListener.java      # 组合监听器
│   └── LoggingSqlListener.java        # 日志监听器
│
├── config/                            # 配置
│   └── WrappedConfig.java              # 配置类（Builder 模式）
│
└── consts/                            # 常量
    └── JdbcConsts.java                # 常量定义
```

### 2.3 多 JDK 版本架构

| 目录 | JDK 版本 | 特性 |
|------|----------|------|
| `src/main/java/` | 8+ | MethodHandle + LongAdder + ThreadPoolExecutor |
| `src/main/java17/` | 17+ | VarHandle + Record |
| `src/main/java23/` | 23+ | Virtual Threads + Scoped Values |

**运行时选择机制**：

```java
public final class Platform {
    private static final int JVM_VERSION = detectJvmVersion();
    
    public static AsyncExecutor createAsyncExecutor(WrappedConfig config) {
        if (JVM_VERSION >= 23) {
            return createVirtualThreadExecutor();  // JDK 23+ 虚拟线程
        }
        return new PlatformThreadExecutor(config); // JDK 8 兼容
    }
    
    public static <T> VarAccessor<T> createVarAccessor(Class<T> type, T initialValue) {
        if (JVM_VERSION >= 17) {
            return createVarHandleAccessor(type, initialValue);  // JDK 17+ VarHandle
        }
        return new AtomicReferenceAccessor<>(initialValue);      // JDK 8 兼容
    }
}
```

## 3. 核心实现

### 3.1 MethodHandle 方法调用

避免反射开销，使用 MethodHandle 实现高性能方法调用：

```java
public final class MethodHandleInvoker implements MethodInvoker {
    private final Method method;
    private final MethodHandle handle;
    private final boolean isStatic;
    
    static MethodHandleInvoker create(Method method) {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle handle = lookup.unreflect(method);
        return new MethodHandleInvoker(method, handle, 
            Modifier.isStatic(method.getModifiers()));
    }
    
    @Override
    public Object invoke(Object target, Object... args) throws Throwable {
        MethodHandle boundHandle = isStatic ? handle : handle.bindTo(target);
        return boundHandle.invokeWithArguments(args);
    }
}
```

### 3.2 自适应慢 SQL 阈值

基于滑动窗口计算 P95 阈值，动态调整慢 SQL 判定标准：

```java
public final class AdaptiveThreshold {
    private final LongAdder[] timeBuckets;  // 60 秒滑动窗口
    private volatile long currentThreshold;
    
    public long getThreshold() {
        maybeRecalculate();  // 定期重算
        return currentThreshold;
    }
    
    private void recalculateThreshold() {
        long[] buckets = snapshotBuckets();
        Arrays.sort(buckets);
        int p95Index = (int) (buckets.length * 0.95);
        long newThreshold = bucketToMs(buckets[p95Index]);
        this.currentThreshold = clamp(newThreshold, MIN, MAX);
    }
}
```

### 3.3 高并发计数

使用 LongAdder 替代 AtomicLong，避免 CAS 竞争：

```java
public final class SqlMetrics {
    private final LongAdder executionCount = new LongAdder();
    private final LongAdder successCount = new LongAdder();
    private final LongAdder totalTimeNanos = new LongAdder();
    
    public void recordSuccess(long elapsedNanos, Object result) {
        executionCount.increment();
        successCount.increment();
        totalTimeNanos.add(elapsedNanos);
        // ...
    }
}
```

### 3.4 代理链设计

```
┌─────────────────┐
│ ProxyDataSource │ ──wrap──> Connection (代理)
└─────────────────┘                │
                                   │ prepareStatement()
                                   ▼
                            PreparedStatement (代理)
                                   │
                                   │ executeQuery()
                                   ▼
                             ResultSet (代理)
```

## 4. 性能优化策略

### 4.1 方法缓存

缓存 MethodHandle 避免重复反射：

```java
private static final Map<String, MethodInvoker> METHOD_CACHE = new ConcurrentHashMap<>();

Object invokeMethod(Method method, Object[] args) throws Throwable {
    String key = method.getDeclaringClass().getName() + "#" + method.getName();
    MethodInvoker invoker = METHOD_CACHE.computeIfAbsent(key, 
        k -> MethodHandleInvoker.create(method));
    return invoker.invoke(target, args);
}
```

### 4.2 对象池复用

ThreadLocal 复用 SqlExecutionContext 减少 GC：

```java
public final class SqlExecutionContext {
    private static final ThreadLocal<SqlExecutionContext> POOL =
        ThreadLocal.withInitial(SqlExecutionContext::new);
    
    public static SqlExecutionContext acquire() {
        SqlExecutionContext ctx = POOL.get();
        ctx.reset();
        return ctx;
    }
    
    public static void release(SqlExecutionContext ctx) {
        POOL.set(ctx);
    }
}
```

### 4.3 异步监听器

监听器异步执行，不阻塞主线程：

```java
public void recordSuccess(SqlExecutionContext context, long elapsedNanos, Object result) {
    // ... 更新指标 ...
    
    asyncExecutor.submit(() -> {
        for (SqlExecutionListener listener : listeners) {
            try {
                listener.onSuccess(context, elapsedNanos, result);
            } catch (Exception ignored) {
                // 忽略监听器异常
            }
        }
    });
}
```

## 5. 扩展机制

### 5.1 监听器接口

```java
public interface SqlExecutionListener {
    void onSuccess(SqlExecutionContext context, long elapsedNanos, Object result);
    void onFailure(SqlExecutionContext context, long elapsedNanos, Throwable throwable);
    void onSlowQuery(SqlExecutionContext context, long elapsedMillis);
}
```

### 5.2 SPI 接口

```java
// 方法调用器 - 可替换实现
public interface MethodInvoker {
    Object invoke(Object target, Object... args) throws Throwable;
}

// 异步执行器 - 可替换实现
public interface AsyncExecutor {
    void submit(Runnable task);
    void shutdown();
}

// 变量访问器 - 可替换实现
public interface VarAccessor<T> {
    T get();
    void set(T value);
    boolean compareAndSet(T expect, T update);
}
```

## 6. 打包产物

| Profile | 产物 | 内容 |
|---------|------|------|
| `-Pjdk8` | `jdbcmon-core-jdk8.jar` | 仅 `src/main/java/` |
| `-Pjdk17` | `jdbcmon-core-jdk17.jar` | `java/` + `java17/` |
| `-Pjdk23` | `jdbcmon-core-jdk23.jar` | `java/` + `java17/` + `java23/` |

## 7. 性能预期

| 场景 | 预期开销 | 说明 |
|------|---------|------|
| **无监控** | < 1% | 只做方法转发，无额外处理 |
| **基础监控** | 1-3% | 记录执行时间、计数等基本指标 |
| **全量监控** | 3-5% | 包含直方图、错误分类、堆栈收集等 |
| **异步日志** | 1-2% | 异步处理监听器，不影响主线程 |

## 8. 设计决策

| 决策 | 选择 | 原因 |
|------|------|------|
| 方法调用 | MethodHandle | 比 Reflection 性能更好，JDK 8 支持 |
| 指标存储 | 内存 ConcurrentHashMap | 性能最优，满足实时监控需求 |
| 慢 SQL 阈值 | 动态自适应 P95 | 智能适应不同业务场景 |
| 参数记录 | 不记录 | 安全优先，避免敏感数据泄露 |
| 打包策略 | Classifier 分包 | 清晰区分不同 JDK 版本 |