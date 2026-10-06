package cn.itcraft.jdbcmon.driver;

/**
 * jdbcmon URL 前缀解析工具
 * <p>
 * 约定以 {@link #URL_PREFIX} 作为监控代理标识前缀，真实 URL 通过剥离该前缀还原：
 * <pre>
 *   jdbc:jdbcmon:mysql://host:3306/db  ->  jdbc:mysql://host:3306/db
 *   jdbc:jdbcmon:h2:mem:test           ->  jdbc:h2:mem:test
 *   jdbc:jdbcmon:oracle:thin:@h:1521:X ->  jdbc:oracle:thin:@h:1521:X
 * </pre>
 *
 * <p>注意事项：前缀匹配大小写不敏感，但还原后的真实 URL 统一使用小写 {@code jdbc:} 前缀。
 */
public final class DriverUrlParser {

    /**
     * 监控代理 URL 前缀
     */
    public static final String URL_PREFIX = "jdbc:jdbcmon:";

    private static final String JDBC_PREFIX = "jdbc:";

    private DriverUrlParser() {
    }

    /**
     * 判断是否为 jdbcmon 代理 URL
     *
     * @param url JDBC URL，可为 null
     * @return true 表示以 {@link #URL_PREFIX} 开头
     */
    public static boolean isMonitoredUrl(String url) {
        return url != null && url.regionMatches(true, 0, URL_PREFIX, 0, URL_PREFIX.length());
    }

    /**
     * 剥离监控前缀，还原真实 JDBC URL
     *
     * @param url 代理 URL
     * @return 真实 JDBC URL
     * @throws IllegalArgumentException url 不是 jdbcmon 代理 URL
     */
    public static String toRealUrl(String url) {
        if (!isMonitoredUrl(url)) {
            throw new IllegalArgumentException("Not a jdbcmon URL: " + url);
        }
        String rest = url.substring(URL_PREFIX.length());
        return JDBC_PREFIX + rest;
    }
}
