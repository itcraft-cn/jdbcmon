package cn.itcraft.jdbcmon;

import cn.itcraft.jdbcmon.config.ProxyConfig;
import cn.itcraft.jdbcmon.internal.MethodHandleInvoker;
import cn.itcraft.jdbcmon.spi.MethodInvoker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit tests for MethodInvoker")
class MethodInvokerTest {

    @Test
    @DisplayName("test_create_staticMethod")
    void test_create_staticMethod() throws Throwable {
        Method method = String.class.getMethod("valueOf", Object.class);
        MethodInvoker invoker = MethodHandleInvoker.create(method);

        Object result = invoker.invoke(null, 123);
        assertEquals("123", result);
    }

    @Test
    @DisplayName("test_create_instanceMethod")
    void test_create_instanceMethod() throws Throwable {
        Method method = String.class.getMethod("length");
        MethodInvoker invoker = MethodHandleInvoker.create(method);

        Object result = invoker.invoke("hello", new Object[0]);
        assertEquals(5, result);
    }

    @Test
    @DisplayName("test_create_withMultipleArgs")
    void test_create_withMultipleArgs() throws Throwable {
        Method method = String.class.getMethod("substring", int.class, int.class);
        MethodInvoker invoker = MethodHandleInvoker.create(method);

        Object result = invoker.invoke("hello world", 0, 5);
        assertEquals("hello", result);
    }

    @Test
    @DisplayName("test_getMethod_returnsOriginalMethod")
    void test_getMethod_returnsOriginalMethod() throws Exception {
        Method method = String.class.getMethod("length");
        MethodInvoker invoker = MethodHandleInvoker.create(method);

        assertEquals(method, invoker.getMethod());
    }

    @Test
    @DisplayName("test_invoke_nullTargetForInstance_throwsException")
    void test_invoke_nullTargetForInstance_throwsException() throws Exception {
        Method method = String.class.getMethod("length");
        MethodInvoker invoker = MethodHandleInvoker.create(method);

        assertThrows(NullPointerException.class, () -> {
            invoker.invoke(null, new Object[0]);
        });
    }
}