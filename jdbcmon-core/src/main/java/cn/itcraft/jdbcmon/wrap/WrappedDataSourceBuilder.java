package cn.itcraft.jdbcmon.wrap;

import cn.itcraft.jdbcmon.config.ProxyConfig;

import javax.sql.DataSource;
import java.util.Objects;

public final class WrappedDataSourceBuilder {

    private DataSource target;
    private ProxyConfig config;
    private String name;

    private WrappedDataSourceBuilder() {
    }

    public static WrappedDataSourceBuilder create() {
        return new WrappedDataSourceBuilder();
    }

    public static WrappedDataSourceBuilder create(DataSource target) {
        return new WrappedDataSourceBuilder().target(target);
    }

    public WrappedDataSourceBuilder target(DataSource target) {
        this.target = Objects.requireNonNull(target, "target cannot be null");
        return this;
    }

    public WrappedDataSourceBuilder config(ProxyConfig config) {
        this.config = config;
        return this;
    }

    public WrappedDataSourceBuilder name(String name) {
        this.name = name;
        return this;
    }

    public WrappedDataSource build() {
        Objects.requireNonNull(target, "target DataSource cannot be null");

        if (config == null) {
            config = new ProxyConfig.Builder().build();
        }

        return new WrappedDataSource(target, config);
    }
}