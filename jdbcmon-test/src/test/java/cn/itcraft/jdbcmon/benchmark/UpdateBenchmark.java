package cn.itcraft.jdbcmon.benchmark;

import cn.itcraft.jdbcmon.config.ProxyConfig;
import cn.itcraft.jdbcmon.datasource.ProxyDataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class UpdateBenchmark {

    private DataSource directDataSource;
    private DataSource proxiedDataSource;

    private Connection directConnection;
    private Connection proxiedConnection;

    private PreparedStatement directInsertPS;
    private PreparedStatement proxiedInsertPS;
    private PreparedStatement directUpdatePS;
    private PreparedStatement proxiedUpdatePS;

    private AtomicInteger idCounter = new AtomicInteger(10000);

    @Setup(Level.Trial)
    public void setup() throws Exception {
        directDataSource = createDirectDataSource();
        proxiedDataSource = createProxiedDataSource();

        initTestData(directDataSource);
        initTestData(proxiedDataSource);
    }

    @Setup(Level.Iteration)
    public void setupIteration() throws Exception {
        directConnection = directDataSource.getConnection();
        proxiedConnection = proxiedDataSource.getConnection();

        directInsertPS = directConnection.prepareStatement(
                "INSERT INTO test_update (id, name, val) VALUES (?, ?, ?)");
        proxiedInsertPS = proxiedConnection.prepareStatement(
                "INSERT INTO test_update (id, name, val) VALUES (?, ?, ?)");
        directUpdatePS = directConnection.prepareStatement(
                "UPDATE test_update SET val = ? WHERE id = ?");
        proxiedUpdatePS = proxiedConnection.prepareStatement(
                "UPDATE test_update SET val = ? WHERE id = ?");
    }

    @TearDown(Level.Iteration)
    public void tearDownIteration() throws Exception {
        if (directInsertPS != null) {
            directInsertPS.close();
        }
        if (proxiedInsertPS != null) {
            proxiedInsertPS.close();
        }
        if (directUpdatePS != null) {
            directUpdatePS.close();
        }
        if (proxiedUpdatePS != null) {
            proxiedUpdatePS.close();
        }
        if (directConnection != null) {
            directConnection.close();
        }
        if (proxiedConnection != null) {
            proxiedConnection.close();
        }
    }

    private DataSource createDirectDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:direct_update;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    private DataSource createProxiedDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:proxied_update;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");

        ProxyConfig config = new ProxyConfig.Builder()
                .slowQueryThresholdMs(1000)
                .logSlowQueries(false)
                .enableLogging(false)
                .useAdaptiveThreshold(false)
                .build();

        return new ProxyDataSource(ds, config);
    }

    private void initTestData(DataSource ds) throws Exception {
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE test_update (id INT PRIMARY KEY, name VARCHAR(100), val INT)");
            for (int i = 0; i < 1000; i++) {
                stmt.execute("INSERT INTO test_update VALUES (" + i + ", 'name" + i + "', " + i + ")");
            }
        }
    }

    @Benchmark
    public void directInsert(Blackhole bh) throws Exception {
        int id = idCounter.incrementAndGet();
        try (Statement stmt = directConnection.createStatement()) {
            int rows = stmt.executeUpdate(
                    "INSERT INTO test_update (id, name, val) VALUES (" + id + ", 'test" + id + "', " + id + ")");
            bh.consume(rows);
        }
    }

    @Benchmark
    public void proxiedInsert(Blackhole bh) throws Exception {
        int id = idCounter.incrementAndGet();
        try (Statement stmt = proxiedConnection.createStatement()) {
            int rows = stmt.executeUpdate(
                    "INSERT INTO test_update (id, name, val) VALUES (" + id + ", 'test" + id + "', " + id + ")");
            bh.consume(rows);
        }
    }

    @Benchmark
    public void directPreparedInsert(Blackhole bh) throws Exception {
        int id = idCounter.incrementAndGet();
        directInsertPS.setInt(1, id);
        directInsertPS.setString(2, "test" + id);
        directInsertPS.setInt(3, id);
        int rows = directInsertPS.executeUpdate();
        bh.consume(rows);
    }

    @Benchmark
    public void proxiedPreparedInsert(Blackhole bh) throws Exception {
        int id = idCounter.incrementAndGet();
        proxiedInsertPS.setInt(1, id);
        proxiedInsertPS.setString(2, "test" + id);
        proxiedInsertPS.setInt(3, id);
        int rows = proxiedInsertPS.executeUpdate();
        bh.consume(rows);
    }

    @Benchmark
    public void directUpdate(Blackhole bh) throws Exception {
        int id = idCounter.get() % 1000;
        try (Statement stmt = directConnection.createStatement()) {
            int rows = stmt.executeUpdate("UPDATE test_update SET val = val + 1 WHERE id = " + id);
            bh.consume(rows);
        }
    }

    @Benchmark
    public void proxiedUpdate(Blackhole bh) throws Exception {
        int id = idCounter.get() % 1000;
        try (Statement stmt = proxiedConnection.createStatement()) {
            int rows = stmt.executeUpdate("UPDATE test_update SET val = val + 1 WHERE id = " + id);
            bh.consume(rows);
        }
    }

    @Benchmark
    public void directPreparedUpdate(Blackhole bh) throws Exception {
        int id = idCounter.incrementAndGet() % 1000;
        directUpdatePS.setInt(1, id);
        directUpdatePS.setInt(2, id);
        int rows = directUpdatePS.executeUpdate();
        bh.consume(rows);
    }

    @Benchmark
    public void proxiedPreparedUpdate(Blackhole bh) throws Exception {
        int id = idCounter.incrementAndGet() % 1000;
        proxiedUpdatePS.setInt(1, id);
        proxiedUpdatePS.setInt(2, id);
        int rows = proxiedUpdatePS.executeUpdate();
        bh.consume(rows);
    }
}
