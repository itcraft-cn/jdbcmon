package cn.itcraft.jdbcmon.datasource;

import cn.itcraft.jdbcmon.config.ProxyConfig;

import javax.sql.DataSource;
import java.util.Objects;

public final class ProxyDataSourceBuilder {

    private DataSource target;
    private ProxyConfig config;
    private String name;

    private ProxyDataSourceBuilder() {
    }

    public static ProxyDataSourceBuilder create() {
        return new ProxyDataSourceBuilder();
    }

    public static ProxyDataSourceBuilder create(DataSource target) {
        return new ProxyDataSourceBuilder().target(target);
    }

    public ProxyDataSourceBuilder target(DataSource target) {
        this.target = Objects.requireNonNull(target, "target cannot be null");
        return this;
    }

    public ProxyDataSourceBuilder config(ProxyConfig config) {
        this.config = config;
        return this;
    }

    public ProxyDataSourceBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ProxyDataSource build() {
        Objects.requireNonNull(target, "target DataSource cannot be null");

        if (config == null) {
            config = new ProxyConfig.Builder().build();
        }

        return new ProxyDataSource(target, config);
    }
}