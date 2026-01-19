package com.guicedee.client.test;

import com.guicedee.client.Environment;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class EnvironmentPlaceholderTest {

    @Test
    public void testPlaceholderResolution() {
        System.setProperty("TEST_VAR", "test-value");
        String resolved = Environment.getSystemPropertyOrEnvironment("MY_PROP", "${TEST_VAR}");
        assertEquals("test-value", resolved, "Should resolve ${TEST_VAR} from system properties");
    }

    @Test
    public void testPlaceholderWithDefault() {
        String resolved = Environment.getSystemPropertyOrEnvironment("MY_PROP_DEFAULT", "${NON_EXISTENT_VAR:-default-value}");
        assertEquals("default-value", resolved, "Should resolve to default-value when VAR is missing");
    }

    @Test
    public void testPlaceholderWithEnvPrefix() {
        // We can't easily set actual ENV vars in a running JVM, but we can check if it tries to resolve them
        // Environment.getSystemPropertyOrEnvironment already checks both Sys Props and Env
        System.setProperty("DB_URL", "jdbc:mysql://localhost:3306/db");
        String resolved = Environment.getSystemPropertyOrEnvironment("DATA_SOURCE", "${env.DB_URL:-xxxx}");
        assertEquals("jdbc:mysql://localhost:3306/db", resolved, "Should resolve ${env.DB_URL:-xxxx} when DB_URL is in Sys Props");
    }

    @Test
    public void testMultiplePlaceholders() {
        System.setProperty("HOST", "localhost");
        System.setProperty("PORT", "8080");
        String resolved = Environment.resolvePlaceholders("http://${HOST}:${PORT}/api");
        assertEquals("http://localhost:8080/api", resolved);
    }

    @Test
    public void testNestedPlaceholders() {
        System.setProperty("INNER", "value");
        System.setProperty("OUTER", "${INNER}");
        String resolved = Environment.resolvePlaceholders("${OUTER}");
        assertEquals("value", resolved, "Should resolve nested placeholders");
    }

    @Test
    public void testDeeplyNestedPlaceholders() {
        System.setProperty("VAR1", "final");
        System.setProperty("VAR2", "${VAR1}");
        System.setProperty("VAR3", "${VAR2}");
        String resolved = Environment.resolvePlaceholders("The result is ${VAR3}");
        assertEquals("The result is final", resolved);
    }

    @Test
    public void testInValueDefault() {
        String resolved = Environment.resolvePlaceholders("${NON_EXISTENT:-${ANOTHER_NON_EXISTENT:-fallback}}");
        assertEquals("fallback", resolved);
    }
}
