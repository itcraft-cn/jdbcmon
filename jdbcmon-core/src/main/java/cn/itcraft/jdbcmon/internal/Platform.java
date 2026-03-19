package cn.itcraft.jdbcmon.internal;

import cn.itcraft.jdbcmon.config.ProxyConfig;
import cn.itcraft.jdbcmon.spi.AsyncExecutor;

public final class Platform {

    private Platform() {
    }

    public static AsyncExecutor createAsyncExecutor(ProxyConfig config) {
        return new PlatformThreadExecutor(config);
    }
}