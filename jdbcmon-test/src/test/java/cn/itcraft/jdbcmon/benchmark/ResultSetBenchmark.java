package cn.itcraft.jdbcmon.benchmark;

import cn.itcraft.jdbcmon.config.HugeResultSetAction;
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
import org.openjdk.jmh.annotations.Param;
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
@Warmup(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class ResultSetBenchmark {

    private DataSource directDataSource;
    private DataSource proxiedDataSource;

    private Connection directConnection;
    private Connection proxiedConnection;

    private PreparedStatement directPreparedStatement;
    private PreparedStatement proxiedPreparedStatement;

    @Param({"10", "100", "1000", "10000"})
    private int rowCount;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        directDataSource = createDirectDataSource();
        proxiedDataSource = createProxiedDataSource();

        initTestData(directDataSource, rowCount);
        initTestData(proxiedDataSource, rowCount);
    }

    @Setup(Level.Iteration)
    public void setupIteration() throws Exception {
        directConnection = directDataSource.getConnection();
        proxiedConnection = proxiedDataSource.getConnection();

        directPreparedStatement = directConnection.prepareStatement(
            "SELECT id, name, value FROM rs_bench WHERE id < ?");
        proxiedPreparedStatement = proxiedConnection.prepareStatement(
            "SELECT id, name, value FROM rs_bench WHERE id < ?");
    }

    @TearDown(Level.Iteration)
    public void tearDownIteration() throws Exception {
        if (directPreparedStatement != null) directPreparedStatement.close();
        if (proxiedPreparedStatement != null) proxiedPreparedStatement.close();
        if (directConnection != null) directConnection.close();
        if (proxiedConnection != null) proxiedConnection.close();
    }

    private DataSource createDirectDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:direct_rs;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    private DataSource createProxiedDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:proxied_rs;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");

        WrappedConfig config = new WrappedConfig.Builder()
            .slowQueryThresholdMs(1000)
            .logSlowQueries(false)
            .enableLogging(false)
            .hugeResultSetThreshold(100000)
            .hugeResultSetAction(HugeResultSetAction.NOTIFY_IMMEDIATE)
            .build();

        return new WrappedDataSource(ds, config);
    }

    private void initTestData(DataSource ds, int rows) throws Exception {
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS rs_bench");
            stmt.execute("CREATE TABLE rs_bench (id INT PRIMARY KEY, name VARCHAR(100), value INT)");
            for (int i = 0; i < rows; i++) {
                stmt.execute("INSERT INTO rs_bench VALUES (" + i + ", 'name" + i + "', " + i + ")");
            }
        }
    }

    @Benchmark
    public void directStatementResultSet(Blackhole bh) throws Exception {
        try (Statement stmt = directConnection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM rs_bench")) {
            while (rs.next()) {
                bh.consume(rs.getInt("id"));
                bh.consume(rs.getString("name"));
                bh.consume(rs.getInt("value"));
            }
        }
    }

    @Benchmark
    public void proxiedStatementResultSet(Blackhole bh) throws Exception {
        try (Statement stmt = proxiedConnection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM rs_bench")) {
            while (rs.next()) {
                bh.consume(rs.getInt("id"));
                bh.consume(rs.getString("name"));
                bh.consume(rs.getInt("value"));
            }
        }
    }

    @Benchmark
    public void directPreparedStatementResultSet(Blackhole bh) throws Exception {
        directPreparedStatement.setInt(1, rowCount);
        try (ResultSet rs = directPreparedStatement.executeQuery()) {
            while (rs.next()) {
                bh.consume(rs.getInt("id"));
            }
        }
    }

    @Benchmark
    public void proxiedPreparedStatementResultSet(Blackhole bh) throws Exception {
        proxiedPreparedStatement.setInt(1, rowCount);
        try (ResultSet rs = proxiedPreparedStatement.executeQuery()) {
            while (rs.next()) {
                bh.consume(rs.getInt("id"));
            }
        }
    }

    @Benchmark
    public void directPartialRead(Blackhole bh) throws Exception {
        try (Statement stmt = directConnection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM rs_bench")) {
            int count = 0;
            while (rs.next() && count < 100) {
                bh.consume(rs.getInt("id"));
                count++;
            }
        }
    }

    @Benchmark
    public void proxiedPartialRead(Blackhole bh) throws Exception {
        try (Statement stmt = proxiedConnection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM rs_bench")) {
            int count = 0;
            while (rs.next() && count < 100) {
                bh.consume(rs.getInt("id"));
                count++;
            }
        }
    }

    @Benchmark
    public void directSingleRowRead(Blackhole bh) throws Exception {
        try (Statement stmt = directConnection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM rs_bench WHERE id = 1")) {
            if (rs.next()) {
                bh.consume(rs.getInt("id"));
                bh.consume(rs.getString("name"));
                bh.consume(rs.getInt("value"));
            }
        }
    }

    @Benchmark
    public void proxiedSingleRowRead(Blackhole bh) throws Exception {
        try (Statement stmt = proxiedConnection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM rs_bench WHERE id = 1")) {
            if (rs.next()) {
                bh.consume(rs.getInt("id"));
                bh.consume(rs.getString("name"));
                bh.consume(rs.getInt("value"));
            }
        }
    }
}