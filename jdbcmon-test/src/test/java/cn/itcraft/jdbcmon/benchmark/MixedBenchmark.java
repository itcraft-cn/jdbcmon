package cn.itcraft.jdbcmon.benchmark;

import cn.itcraft.jdbcmon.config.WrappedConfig;
import cn.itcraft.jdbcmon.wrap.WrappedDataSource;
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
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 50, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 20, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class MixedBenchmark {

    private DataSource directDataSource;
    private DataSource proxiedDataSource;

    private Connection directConnection;
    private Connection proxiedConnection;

    private PreparedStatement directQueryPS;
    private PreparedStatement proxiedQueryPS;
    private PreparedStatement directUpdatePS;
    private PreparedStatement proxiedUpdatePS;

    private AtomicInteger queryCounter = new AtomicInteger(0);
    private AtomicInteger updateCounter = new AtomicInteger(10000);

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

        directQueryPS = directConnection.prepareStatement(
                "SELECT id, name, val FROM test_mixed WHERE id = ?");
        proxiedQueryPS = proxiedConnection.prepareStatement(
                "SELECT id, name, val FROM test_mixed WHERE id = ?");
        directUpdatePS = directConnection.prepareStatement(
                "INSERT INTO test_mixed (id, name, val) VALUES (?, ?, ?)");
        proxiedUpdatePS = proxiedConnection.prepareStatement(
                "INSERT INTO test_mixed (id, name, val) VALUES (?, ?, ?)");
    }

    @TearDown(Level.Iteration)
    public void tearDownIteration() throws Exception {
        if (directQueryPS != null) {
            directQueryPS.close();
        }
        if (proxiedQueryPS != null) {
            proxiedQueryPS.close();
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
        ds.setURL("jdbc:h2:mem:direct_mixed;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    private DataSource createProxiedDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:proxied_mixed;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");

        WrappedConfig config = new WrappedConfig.Builder()
                .slowQueryThresholdMs(1000)
                .logSlowQueries(false)
                .enableLogging(false)
                .useAdaptiveThreshold(false)
                .monitorConnections(true)
                .monitorTransactions(true)
                .build();

        return new WrappedDataSource(ds, config);
    }

    private void initTestData(DataSource ds) throws Exception {
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS test_mixed");
            stmt.execute("CREATE TABLE test_mixed (id INT PRIMARY KEY, name VARCHAR(100), val INT)");
            for (int i = 0; i < 1000; i++) {
                stmt.execute("INSERT INTO test_mixed VALUES (" + i + ", 'name" + i + "', " + i + ")");
            }
        }
    }

    @Benchmark
    public void directMixed80Query20Update(Blackhole bh) throws Exception {
        int op = queryCounter.incrementAndGet();

        if (op % 5 == 0) {
            int id = updateCounter.incrementAndGet();
            directUpdatePS.setInt(1, id);
            directUpdatePS.setString(2, "new" + id);
            directUpdatePS.setInt(3, id);
            int rows = directUpdatePS.executeUpdate();
            bh.consume(rows);
        } else {
            int queryId = op % 1000;
            directQueryPS.setInt(1, queryId);
            try (ResultSet rs = directQueryPS.executeQuery()) {
                if (rs.next()) {
                    bh.consume(rs.getInt("id"));
                    bh.consume(rs.getString("name"));
                }
            }
        }
    }

    @Benchmark
    public void proxiedMixed80Query20Update(Blackhole bh) throws Exception {
        int op = queryCounter.incrementAndGet();

        if (op % 5 == 0) {
            int id = updateCounter.incrementAndGet();
            proxiedUpdatePS.setInt(1, id);
            proxiedUpdatePS.setString(2, "new" + id);
            proxiedUpdatePS.setInt(3, id);
            int rows = proxiedUpdatePS.executeUpdate();
            bh.consume(rows);
        } else {
            int queryId = op % 1000;
            proxiedQueryPS.setInt(1, queryId);
            try (ResultSet rs = proxiedQueryPS.executeQuery()) {
                if (rs.next()) {
                    bh.consume(rs.getInt("id"));
                    bh.consume(rs.getString("name"));
                }
            }
        }
    }

    @Benchmark
    public void directMixed50Query50Update(Blackhole bh) throws Exception {
        int op = queryCounter.incrementAndGet();

        if (op % 2 == 0) {
            int id = updateCounter.incrementAndGet();
            directUpdatePS.setInt(1, id);
            directUpdatePS.setString(2, "new" + id);
            directUpdatePS.setInt(3, id);
            int rows = directUpdatePS.executeUpdate();
            bh.consume(rows);
        } else {
            int queryId = op % 1000;
            directQueryPS.setInt(1, queryId);
            try (ResultSet rs = directQueryPS.executeQuery()) {
                if (rs.next()) {
                    bh.consume(rs.getInt("id"));
                }
            }
        }
    }

    @Benchmark
    public void proxiedMixed50Query50Update(Blackhole bh) throws Exception {
        int op = queryCounter.incrementAndGet();

        if (op % 2 == 0) {
            int id = updateCounter.incrementAndGet();
            proxiedUpdatePS.setInt(1, id);
            proxiedUpdatePS.setString(2, "new" + id);
            proxiedUpdatePS.setInt(3, id);
            int rows = proxiedUpdatePS.executeUpdate();
            bh.consume(rows);
        } else {
            int queryId = op % 1000;
            proxiedQueryPS.setInt(1, queryId);
            try (ResultSet rs = proxiedQueryPS.executeQuery()) {
                if (rs.next()) {
                    bh.consume(rs.getInt("id"));
                }
            }
        }
    }
}
