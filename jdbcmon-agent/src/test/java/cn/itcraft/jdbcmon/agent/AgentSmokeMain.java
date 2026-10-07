package cn.itcraft.jdbcmon.agent;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * javaagent 端到端冒烟入口
 * <p>
 * 仅依赖 JDK 与具体数据库驱动，用于在 {@code -javaagent} 启动的子进程中验证连接/语句是否被自动包装。
 * 输出形如 {@code CONN_CLASS=...}、{@code STMT_CLASS=...}，由测试断言。
 */
public final class AgentSmokeMain {

    private AgentSmokeMain() {
    }

    public static void main(String[] args) throws Exception {
        String url = args[0];
        try (Connection conn = DriverManager.getConnection(url)) {
            System.out.println("CONN_CLASS=" + conn.getClass().getName());
            try (Statement stmt = conn.createStatement()) {
                System.out.println("STMT_CLASS=" + stmt.getClass().getName());
                try (ResultSet rs = stmt.executeQuery("SELECT 1")) {
                    rs.next();
                }
            }
        }
        System.out.println("SMOKE_DONE");
    }
}
