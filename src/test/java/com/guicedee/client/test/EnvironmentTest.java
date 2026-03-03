package com.guicedee.client.test;
import com.guicedee.client.Environment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
class EnvironmentTest {
    private final Set<String> propsToClean = new HashSet<>();
    private void setProperty(String key, String value) {
        propsToClean.add(key);
        System.setProperty(key, value);
    }
    @AfterEach
    void cleanUp() {
        for (String key : propsToClean) {
            System.clearProperty(key);
        }
        propsToClean.clear();
    }
    @Test
    void getPropertyReturnsSystemProperty() {
        setProperty("env.test.prop1", "sys-value");
        String result = Environment.getProperty("env.test.prop1", "default");
        assertEquals("sys-value", result);
    }
    @Test
    void getPropertyFallsBackToDefault() {
        System.clearProperty("env.test.nonexistent.prop");
        String result = Environment.getProperty("env.test.nonexistent.prop", "fallback");
        assertEquals("fallback", result);
        propsToClean.add("env.test.nonexistent.prop");
    }
    @Test
    void getPropertyResolvesPlaceholdersInDefault() {
        setProperty("ENVTEST_INNER", "resolved");
        System.clearProperty("env.test.placeholder.prop");
        propsToClean.add("env.test.placeholder.prop");
        String result = Environment.getProperty("env.test.placeholder.prop", "${ENVTEST_INNER}");
        assertEquals("resolved", result);
    }
    @Test
    void getSystemPropertyOrEnvironmentSystemPropertyTakesPrecedence() {
        setProperty("env.test.sysprop", "from-system");
        String result = Environment.getSystemPropertyOrEnvironment("env.test.sysprop", "default-val");
        assertEquals("from-system", result);
    }
    @Test
    void getSystemPropertyOrEnvironmentNullDefaultReturnsEmpty() {
        System.clearProperty("env.test.nulldefault");
        propsToClean.add("env.test.nulldefault");
        String result = Environment.getSystemPropertyOrEnvironment("env.test.nulldefault", null);
        assertEquals("", result);
    }
    @Test
    void getSystemPropertyOrEnvironmentResolvedDefaultIsPersisted() {
        setProperty("ENVTEST_RESOLVE_INNER", "inner-val");
        System.clearProperty("env.test.resolve.outer");
        propsToClean.add("env.test.resolve.outer");
        String result = Environment.getSystemPropertyOrEnvironment("env.test.resolve.outer", "${ENVTEST_RESOLVE_INNER}");
        assertEquals("inner-val", result);
        assertEquals("inner-val", System.getProperty("env.test.resolve.outer"));
    }
    @Test
    void resolvePlaceholdersNullInput() {
        assertNull(Environment.resolvePlaceholders(null));
    }
    @Test
    void resolvePlaceholdersNoPlaceholders() {
        assertEquals("plain text", Environment.resolvePlaceholders("plain text"));
    }
    @Test
    void resolvePlaceholdersEmptyString() {
        assertEquals("", Environment.resolvePlaceholders(""));
    }
    @Test
    void resolvePlaceholdersMissingVarRetainsPlaceholder() {
        System.clearProperty("DEFINITELY_NOT_SET_XYZ");
        String result = Environment.resolvePlaceholders("${DEFINITELY_NOT_SET_XYZ}");
        assertEquals("${DEFINITELY_NOT_SET_XYZ}", result);
    }
    @Test
    void resolvePlaceholdersWithColonDefault() {
        System.clearProperty("UNSET_VAR_COLON");
        String result = Environment.resolvePlaceholders("${UNSET_VAR_COLON:default-val}");
        assertEquals("default-val", result);
    }
    @Test
    void resolvePlaceholdersWithColonDashDefault() {
        System.clearProperty("UNSET_VAR_COLON_DASH");
        String result = Environment.resolvePlaceholders("${UNSET_VAR_COLON_DASH:-dash-default}");
        assertEquals("dash-default", result);
    }
    @Test
    void resolvePlaceholdersEnvDotPrefix() {
        setProperty("MY_ENV_VAR", "env-val");
        String result = Environment.resolvePlaceholders("${env.MY_ENV_VAR}");
        assertEquals("env-val", result);
    }
    @Test
    void resolvePlaceholdersMultipleInOneLine() {
        setProperty("ENVTEST_A", "aa");
        setProperty("ENVTEST_B", "bb");
        String result = Environment.resolvePlaceholders("${ENVTEST_A}-${ENVTEST_B}");
        assertEquals("aa-bb", result);
    }
    @Test
    void resolvePlaceholdersNestedTwoLevels() {
        setProperty("ENVTEST_L1", "final-value");
        setProperty("ENVTEST_L2", "${ENVTEST_L1}");
        String result = Environment.resolvePlaceholders("${ENVTEST_L2}");
        assertEquals("final-value", result);
    }
    @Test
    void resolvePlaceholdersNestedDefault() {
        System.clearProperty("OUTER_UNSET");
        System.clearProperty("INNER_UNSET");
        String result = Environment.resolvePlaceholders("${OUTER_UNSET:-${INNER_UNSET:-deepfallback}}");
        assertEquals("deepfallback", result);
    }
    @Test
    void resolvePlaceholdersPartialText() {
        setProperty("ENVTEST_PARTIAL", "world");
        String result = Environment.resolvePlaceholders("hello ${ENVTEST_PARTIAL}!");
        assertEquals("hello world!", result);
    }
    @Test
    void resolvePlaceholdersUnclosedBrace() {
        String result = Environment.resolvePlaceholders("${UNCLOSED");
        assertEquals("${UNCLOSED", result);
    }
    @Test
    void resolvePlaceholdersEmptyPlaceholder() {
        assertThrows(IllegalArgumentException.class, () -> Environment.resolvePlaceholders("${}"));
    }
    @Test
    void resolvePlaceholdersRecursionLimit() {
        setProperty("ENVTEST_LOOP_A", "${ENVTEST_LOOP_B}");
        setProperty("ENVTEST_LOOP_B", "${ENVTEST_LOOP_A}");
        String result = Environment.resolvePlaceholders("${ENVTEST_LOOP_A}");
        assertNotNull(result);
    }
}
