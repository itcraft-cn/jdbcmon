package cn.itcraft.jdbcmon;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("jdbcmon Test Suite")
@SelectClasses({
    SqlMetricsTest.class,
    AdaptiveThresholdTest.class,
    IntegrationTest.class
})
public class TestSuite {
}