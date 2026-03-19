package cn.itcraft.jdbcmon.benchmark;

import cn.itcraft.jdbcmon.config.ProxyConfig;
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

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 50, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 20, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class QueryBenchmark {

    private DataSource directDataSource;
    private DataSource proxiedDataSource;

    private Connection directConnection;
    private Connection proxiedConnection;

    private PreparedStatement directPreparedStatement;
    private PreparedStatement proxiedPreparedStatement;

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

        directPreparedStatement = directConnection.prepareStatement(
                "SELECT id, name, val FROM test_query WHERE id = ?");
        proxiedPreparedStatement = proxiedConnection.prepareStatement(
                "SELECT id, name, val FROM test_query WHERE id = ?");
    }

    @TearDown(Level.Iteration)
    public void tearDownIteration() throws Exception {
        if (directPreparedStatement != null) {
            directPreparedStatement.close();
        }
        if (proxiedPreparedStatement != null) {
            proxiedPreparedStatement.close();
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
        ds.setURL("jdbc:h2:mem:direct_query;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    private DataSource createProxiedDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:proxied_query;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");

        ProxyConfig config = new ProxyConfig.Builder()
                .slowQueryThresholdMs(1000)
                .logSlowQueries(false)
                .enableLogging(false)
                .useAdaptiveThreshold(false)
                .build();

        return new WrappedDataSource(ds, config);
    }

    private void initTestData(DataSource ds) throws Exception {
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS test_query");
            stmt.execute("CREATE TABLE test_query (id INT PRIMARY KEY, name VARCHAR(100), val INT)");
            for (int i = 0; i < 1000; i++) {
                stmt.execute("INSERT INTO test_query VALUES (" + i + ", 'name" + i + "', " + i + ")");
            }
        }
    }

    @Benchmark
    public void directSimpleQuery(Blackhole bh) throws Exception {
        try (Statement stmt = directConnection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM test_query WHERE id = 1")) {
            if (rs.next()) {
                bh.consume(rs.getInt("id"));
                bh.consume(rs.getString("name"));
                bh.consume(rs.getInt("val"));
            }
        }
    }

    @Benchmark
    public void proxiedSimpleQuery(Blackhole bh) throws Exception {
        try (Statement stmt = proxiedConnection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM test_query WHERE id = 1")) {
            if (rs.next()) {
                bh.consume(rs.getInt("id"));
                bh.consume(rs.getString("name"));
                bh.consume(rs.getInt("val"));
            }
        }
    }

    @Benchmark
    public void directPreparedQuery(Blackhole bh) throws Exception {
        directPreparedStatement.setInt(1, 1);
        try (ResultSet rs = directPreparedStatement.executeQuery()) {
            if (rs.next()) {
                bh.consume(rs.getInt("id"));
                bh.consume(rs.getString("name"));
                bh.consume(rs.getInt("val"));
            }
        }
    }

    @Benchmark
    public void proxiedPreparedQuery(Blackhole bh) throws Exception {
        proxiedPreparedStatement.setInt(1, 1);
        try (ResultSet rs = proxiedPreparedStatement.executeQuery()) {
            if (rs.next()) {
                bh.consume(rs.getInt("id"));
                bh.consume(rs.getString("name"));
                bh.consume(rs.getInt("val"));
            }
        }
    }

    @Benchmark
    public void directMultiRowQuery(Blackhole bh) throws Exception {
        try (Statement stmt = directConnection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM test_query WHERE id < 100")) {
            while (rs.next()) {
                bh.consume(rs.getInt("id"));
            }
        }
    }

    @Benchmark
    public void proxiedMultiRowQuery(Blackhole bh) throws Exception {
        try (Statement stmt = proxiedConnection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM test_query WHERE id < 100")) {
            while (rs.next()) {
                bh.consume(rs.getInt("id"));
            }
        }
    }
}
