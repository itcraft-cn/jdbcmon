package cn.itcraft.jdbcmon.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.itcraft.jdbcmon.config.WrappedConfig;

/**
 * javaagent 配置解析单元测试
 */
class JdbcMonAgentConfigTest {

    @Test
    void applyArgs_plainKeys() {
        try {
            JdbcMonAgentConfig.applyArgs("sampleRate=10000;slowQueryThresholdMs=2500");
            WrappedConfig cfg = JdbcMonAgentConfig.getConfig();
            assertEquals(10000, cfg.getSampleRate());
            assertEquals(2500L, cfg.getSlowQueryThresholdMs());
            assertNotNull(JdbcMonAgentConfig.getMonitor());
        } finally {
            JdbcMonAgentConfig.reload();
        }
    }

    @Test
    void applyArgs_prefixedKeys() {
        try {
            JdbcMonAgentConfig.applyArgs("jdbcmon.sampleRate=5000");
            assertEquals(5000, JdbcMonAgentConfig.getConfig().getSampleRate());
        } finally {
            JdbcMonAgentConfig.reload();
        }
    }

    @Test
    void applyArgs_invalidFallsBackToDefault() {
        try {
            JdbcMonAgentConfig.applyArgs("sampleRate=999999");
            assertEquals(100, JdbcMonAgentConfig.getConfig().getSampleRate());
        } finally {
            JdbcMonAgentConfig.reload();
        }
    }

    @Test
    void setConfig_nullRejected() {
        assertThrows(IllegalArgumentException.class, () -> JdbcMonAgentConfig.setConfig(null));
    }
}
