package cn.itcraft.jdbcmon.benchmark;

import cn.itcraft.jdbcmon.config.ProxyConfig;
import cn.itcraft.jdbcmon.datasource.ProxyDataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class BatchBenchmark {

    @Param({"10", "100", "1000"})
    public int batchSize;

    private DataSource directDataSource;
    private DataSource proxiedDataSource;

    private Connection directConnection;
    private Connection proxiedConnection;

    private PreparedStatement directInsertPS;
    private PreparedStatement proxiedInsertPS;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        directDataSource = createDirectDataSource();
        proxiedDataSource = createProxiedDataSource();

        initSchema(directDataSource);
        initSchema(proxiedDataSource);
    }

    @Setup(Level.Iteration)
    public void setupIteration() throws Exception {
        directConnection = directDataSource.getConnection();
        proxiedConnection = proxiedDataSource.getConnection();

        directConnection.setAutoCommit(false);
        proxiedConnection.setAutoCommit(false);

        directInsertPS = directConnection.prepareStatement(
                "INSERT INTO test_batch (id, name, val) VALUES (?, ?, ?)");
        proxiedInsertPS = proxiedConnection.prepareStatement(
                "INSERT INTO test_batch (id, name, val) VALUES (?, ?, ?)");
    }

    @TearDown(Level.Iteration)
    public void tearDownIteration() throws Exception {
        if (directInsertPS != null) directInsertPS.close();
        if (proxiedInsertPS != null) proxiedInsertPS.close();

        if (directConnection != null) {
            directConnection.rollback();
            directConnection.close();
        }
        if (proxiedConnection != null) {
            proxiedConnection.rollback();
            proxiedConnection.close();
        }
    }

    private DataSource createDirectDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:direct_batch;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    private DataSource createProxiedDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:proxied_batch;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");

        ProxyConfig config = new ProxyConfig.Builder()
                .slowQueryThresholdMs(1000)
                .logSlowQueries(false)
                .enableLogging(false)
                .useAdaptiveThreshold(false)
                .monitorBatchOperations(true)
                .build();

        return new ProxyDataSource(ds, config);
    }

    private void initSchema(DataSource ds) throws Exception {
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE test_batch (id INT PRIMARY KEY, name VARCHAR(100), val INT)");
        }
    }

    @Benchmark
    public void directBatchInsert(Blackhole bh) throws Exception {
        for (int i = 0; i < batchSize; i++) {
            directInsertPS.setInt(1, i);
            directInsertPS.setString(2, "name" + i);
            directInsertPS.setInt(3, i);
            directInsertPS.addBatch();
        }
        int[] results = directInsertPS.executeBatch();
        bh.consume(results);
    }

    @Benchmark
    public void proxiedBatchInsert(Blackhole bh) throws Exception {
        for (int i = 0; i < batchSize; i++) {
            proxiedInsertPS.setInt(1, i);
            proxiedInsertPS.setString(2, "name" + i);
            proxiedInsertPS.setInt(3, i);
            proxiedInsertPS.addBatch();
        }
        int[] results = proxiedInsertPS.executeBatch();
        bh.consume(results);
    }

    @Benchmark
    public void directBatchInsertWithCommit(Blackhole bh) throws Exception {
        for (int i = 0; i < batchSize; i++) {
            directInsertPS.setInt(1, i);
            directInsertPS.setString(2, "name" + i);
            directInsertPS.setInt(3, i);
            directInsertPS.addBatch();
        }
        int[] results = directInsertPS.executeBatch();
        directConnection.commit();
        bh.consume(results);
    }

    @Benchmark
    public void proxiedBatchInsertWithCommit(Blackhole bh) throws Exception {
        for (int i = 0; i < batchSize; i++) {
            proxiedInsertPS.setInt(1, i);
            proxiedInsertPS.setString(2, "name" + i);
            proxiedInsertPS.setInt(3, i);
            proxiedInsertPS.addBatch();
        }
        int[] results = proxiedInsertPS.executeBatch();
        proxiedConnection.commit();
        bh.consume(results);
    }
}