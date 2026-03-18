package cn.itcraft.jdbcmon.spring.autoconfigure;

import cn.itcraft.jdbcmon.config.ProxyConfig;
import cn.itcraft.jdbcmon.datasource.ProxyDataSource;
import cn.itcraft.jdbcmon.monitor.SqlMonitor;
import cn.itcraft.jdbcmon.spring.properties.JdbcMonProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@AutoConfiguration
@ConditionalOnClass({DataSource.class, ProxyDataSource.class})
@ConditionalOnProperty(prefix = "jdbcmon", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JdbcMonProperties.class)
public class JdbcMonAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(JdbcMonAutoConfiguration.class);

    private final JdbcMonProperties properties;

    public JdbcMonAutoConfiguration(JdbcMonProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    public ProxyConfig jdbcMonProxyConfig() {
        log.info("Initializing jdbcmon with slowQueryThresholdMs={}ms, adaptiveThreshold={}",
            properties.getSlowQueryThresholdMs(),
            properties.isUseAdaptiveThreshold());
        return properties.toProxyConfig();
    }

    @Bean
    @ConditionalOnMissingBean
    public SqlMonitor sqlMonitor(ProxyConfig config) {
        return new SqlMonitor(config);
    }

    @Bean
    public BeanPostProcessor dataSourceProxyPostProcessor(ProxyConfig config, SqlMonitor sqlMonitor) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource && !(bean instanceof ProxyDataSource)) {
                    log.info("Wrapping DataSource bean '{}' with jdbcmon proxy", beanName);
                    return new ProxyDataSource((DataSource) bean, config);
                }
                return bean;
            }
        };
    }
}