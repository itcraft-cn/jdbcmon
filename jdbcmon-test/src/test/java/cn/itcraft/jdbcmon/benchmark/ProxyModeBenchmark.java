package cn.itcraft.jdbcmon.benchmark;

import cn.itcraft.jdbcmon.config.ProxyConfig;
import cn.itcraft.jdbcmon.config.ProxyMode;
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
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class ProxyModeBenchmark {

    private DataSource directDataSource;
    private DataSource wrapperDataSource;
    private DataSource reflectionDataSource;

    private Connection directConnection;
    private Connection wrapperConnection;
    private Connection reflectionConnection;

    private PreparedStatement directQueryPS;
    private PreparedStatement wrapperQueryPS;
    private PreparedStatement reflectionQueryPS;

    private PreparedStatement directUpdatePS;
    private PreparedStatement wrapperUpdatePS;
    private PreparedStatement reflectionUpdatePS;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        directDataSource = createDirectDataSource();
        wrapperDataSource = createProxiedDataSource(ProxyMode.WRAPPER);
        reflectionDataSource = createProxiedDataSource(ProxyMode.REFLECTION);

        initSchema(directDataSource);
        initSchema(wrapperDataSource);
        initSchema(reflectionDataSource);
    }

    @Setup(Level.Iteration)
    public void setupIteration() throws Exception {
        directConnection = directDataSource.getConnection();
        wrapperConnection = wrapperDataSource.getConnection();
        reflectionConnection = reflectionDataSource.getConnection();

        directQueryPS = directConnection.prepareStatement("SELECT id, name, val FROM test_perf WHERE id = ?");
        wrapperQueryPS = wrapperConnection.prepareStatement("SELECT id, name, val FROM test_perf WHERE id = ?");
        reflectionQueryPS = reflectionConnection.prepareStatement("SELECT id, name, val FROM test_perf WHERE id = ?");

        directUpdatePS = directConnection.prepareStatement("INSERT INTO test_perf (id, name, val) VALUES (?, ?, ?)");
        wrapperUpdatePS = wrapperConnection.prepareStatement("INSERT INTO test_perf (id, name, val) VALUES (?, ?, ?)");
        reflectionUpdatePS =
                reflectionConnection.prepareStatement("INSERT INTO test_perf (id, name, val) VALUES (?, ?, ?)");
    }

    @TearDown(Level.Iteration)
    public void tearDownIteration() throws Exception {
        closeQuietly(directQueryPS, wrapperQueryPS, reflectionQueryPS);
        closeQuietly(directUpdatePS, wrapperUpdatePS, reflectionUpdatePS);
        closeQuietly(directConnection, wrapperConnection, reflectionConnection);
    }

    private void closeQuietly(AutoCloseable... resources) {
        for (AutoCloseable r : resources) {
            if (r != null) {
                try {
                    r.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private DataSource createDirectDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:direct_mode;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    private DataSource createProxiedDataSource(ProxyMode mode) {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + mode.name().toLowerCase() + "_mode;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");

        ProxyConfig config = new ProxyConfig.Builder()
                .proxyMode(mode)
                .slowQueryThresholdMs(1000)
                .logSlowQueries(false)
                .enableLogging(false)
                .useAdaptiveThreshold(false)
                .build();

        return new ProxyDataSource(ds, config);
    }

    private void initSchema(DataSource ds) throws Exception {
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS test_perf");
            stmt.execute("CREATE TABLE test_perf (id BIGINT PRIMARY KEY, name VARCHAR(100), val BIGINT)");
            for (int i = 0; i < 1000; i++) {
                stmt.execute("INSERT INTO test_perf VALUES (" + i + ", 'name" + i + "', " + i + ")");
            }
        }
    }

    // ========== Query Benchmarks ==========

    @Benchmark
    public void directQuery(Blackhole bh) throws Exception {
        directQueryPS.setInt(1, 1);
        try (ResultSet rs = directQueryPS.executeQuery()) {
            if (rs.next()) {
                bh.consume(rs.getInt("id"));
                bh.consume(rs.getString("name"));
            }
        }
    }

    @Benchmark
    public void wrapperQuery(Blackhole bh) throws Exception {
        wrapperQueryPS.setInt(1, 1);
        try (ResultSet rs = wrapperQueryPS.executeQuery()) {
            if (rs.next()) {
                bh.consume(rs.getInt("id"));
                bh.consume(rs.getString("name"));
            }
        }
    }

    @Benchmark
    public void reflectionQuery(Blackhole bh) throws Exception {
        reflectionQueryPS.setInt(1, 1);
        try (ResultSet rs = reflectionQueryPS.executeQuery()) {
            if (rs.next()) {
                bh.consume(rs.getInt("id"));
                bh.consume(rs.getString("name"));
            }
        }
    }

    // ========== Update Benchmarks ==========

    @Benchmark
    public void directUpdate(Blackhole bh) throws Exception {
        long id = System.nanoTime();
        directUpdatePS.setLong(1, id);
        directUpdatePS.setString(2, "name" + id);
        directUpdatePS.setLong(3, id);
        int rows = directUpdatePS.executeUpdate();
        bh.consume(rows);
    }

    @Benchmark
    public void wrapperUpdate(Blackhole bh) throws Exception {
        long id = System.nanoTime();
        wrapperUpdatePS.setLong(1, id);
        wrapperUpdatePS.setString(2, "name" + id);
        wrapperUpdatePS.setLong(3, id);
        int rows = wrapperUpdatePS.executeUpdate();
        bh.consume(rows);
    }

    @Benchmark
    public void reflectionUpdate(Blackhole bh) throws Exception {
        long id = System.nanoTime();
        reflectionUpdatePS.setLong(1, id);
        reflectionUpdatePS.setString(2, "name" + id);
        reflectionUpdatePS.setLong(3, id);
        int rows = reflectionUpdatePS.executeUpdate();
        bh.consume(rows);
    }
}
