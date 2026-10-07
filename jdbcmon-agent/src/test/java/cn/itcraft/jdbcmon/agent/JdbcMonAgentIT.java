package cn.itcraft.jdbcmon.agent;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * javaagent 端到端集成测试
 * <p>
 * 以 {@code -javaagent} 启动子进程，且子进程 classpath 不包含 agent 自身类，
 * 验证仅凭 agent jar 即可完成连接包装（真正的零侵入接入）。
 */
class JdbcMonAgentIT {

    @Test
    void agent_wrapsConnectionInChildJvm() throws Exception {
        String agentJar = System.getProperty("agent.jar");
        assumeTrue(agentJar != null && new File(agentJar).isFile(),
            "agent jar not built yet: " + agentJar);

        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        String classpath = childClasspath();
        String url = "jdbc:h2:mem:agent_it_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";

        ProcessBuilder pb = new ProcessBuilder(
            javaBin,
            "-javaagent:" + agentJar,
            "-cp", classpath,
            AgentSmokeMain.class.getName(),
            url);
        pb.redirectErrorStream(true);

        Process process = pb.start();
        String output = readAll(process.getInputStream());
        int exit = process.waitFor();

        assertEquals(0, exit, "Child JVM failed:\n" + output);
        assertTrue(output.contains("SMOKE_DONE"), "Smoke did not complete:\n" + output);
        assertTrue(output.contains("CONN_CLASS=cn.itcraft.jdbcmon.wrap.MonitoredConnection"),
            "Connection was not wrapped by agent:\n" + output);
        assertTrue(output.contains("STMT_CLASS=cn.itcraft.jdbcmon.wrap.MonitoredStatement"),
            "Statement was not wrapped by agent:\n" + output);
    }

    private static String childClasspath() {
        String cp = System.getProperty("java.class.path");
        StringBuilder sb = new StringBuilder();
        for (String entry : cp.split(File.pathSeparator)) {
            if (entry.contains("h2") || entry.contains("test-classes") || entry.contains("slf4j")) {
                if (sb.length() > 0) {
                    sb.append(File.pathSeparator);
                }
                sb.append(entry);
            }
        }
        return sb.toString();
    }

    private static String readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
