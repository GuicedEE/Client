package com.guicedee.client.test;

import com.guicedee.client.utils.GlobalProperties;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalPropertiesTest {

    @Test
    void newInstanceHasNoKeys() {
        GlobalProperties gp = new GlobalProperties();
        assertNull(gp.getKey("nonexistent"));
    }

    @Test
    void addKeyAndRetrieve() {
        GlobalProperties gp = new GlobalProperties();
        Map<Object, Object> props = new HashMap<>();
        props.put("host", "localhost");
        props.put("port", 8080);
        gp.addKey("db", props);

        Map<String, Object> retrieved = gp.getKey("db");
        assertNotNull(retrieved);
        assertEquals("localhost", retrieved.get("host"));
        assertEquals(8080, retrieved.get("port"));
    }

    @Test
    void addPropertyCreatesKeyMapIfAbsent() {
        GlobalProperties gp = new GlobalProperties();
        gp.addProperty("server", "host", "localhost");

        Map<String, String> map = gp.getKey("server");
        assertNotNull(map);
        assertEquals("localhost", map.get("host"));
    }

    @Test
    void addPropertyAppendsToExistingKey() {
        GlobalProperties gp = new GlobalProperties();
        gp.addProperty("server", "host", "localhost");
        gp.addProperty("server", "port", "8080");

        Map<String, String> map = gp.getKey("server");
        assertEquals("localhost", map.get("host"));
        assertEquals("8080", map.get("port"));
    }

    @Test
    void addPropertyObjectValue() {
        GlobalProperties gp = new GlobalProperties();
        gp.addProperty("config", "count", 42);

        Integer val = gp.getProperty("config", "count");
        assertEquals(42, val);
    }

    @Test
    void addPropertyObjectCreatesKeyMapIfAbsent() {
        GlobalProperties gp = new GlobalProperties();
        gp.addProperty("newkey", "prop", "value");

        String val = gp.getProperty("newkey", "prop");
        assertEquals("value", val);
    }

    @Test
    void getPropertyReturnsCorrectValue() {
        GlobalProperties gp = new GlobalProperties();
        gp.addProperty("app", "name", "MyApp");
        assertEquals("MyApp", gp.<String>getProperty("app", "name"));
    }

    @Test
    void removePropertyRemovesFromMap() {
        GlobalProperties gp = new GlobalProperties();
        gp.addProperty("app", "name", "MyApp");
        gp.addProperty("app", "version", "1.0");

        gp.removeProperty("app", "name");

        Map<String, String> map = gp.getKey("app");
        assertNull(map.get("name"));
        assertEquals("1.0", map.get("version"));
    }

    @Test
    void removePropertyIgnoresNonexistentKey() {
        GlobalProperties gp = new GlobalProperties();
        // Should not throw
        assertDoesNotThrow(() -> gp.removeProperty("nonexistent", "prop"));
    }

    @Test
    void emptyPropertySetsValueToEmptyString() {
        GlobalProperties gp = new GlobalProperties();
        gp.addProperty("app", "name", "MyApp");
        gp.emptyProperty("app", "name");

        String val = gp.getProperty("app", "name");
        assertEquals("", val);
    }

    @Test
    void emptyPropertyIgnoresNonexistentKey() {
        GlobalProperties gp = new GlobalProperties();
        // Should not throw
        assertDoesNotThrow(() -> gp.emptyProperty("nonexistent", "prop"));
    }

    @Test
    void addKeyOverwritesExisting() {
        GlobalProperties gp = new GlobalProperties();
        Map<Object, Object> props1 = new HashMap<>();
        props1.put("a", "1");
        gp.addKey("k", props1);

        Map<Object, Object> props2 = new HashMap<>();
        props2.put("b", "2");
        gp.addKey("k", props2);

        Map<String, String> map = gp.getKey("k");
        assertNull(map.get("a"));
        assertEquals("2", map.get("b"));
    }

    @Test
    void multipleKeysAreIndependent() {
        GlobalProperties gp = new GlobalProperties();
        gp.addProperty("key1", "prop", "val1");
        gp.addProperty("key2", "prop", "val2");

        assertEquals("val1", gp.<String>getProperty("key1", "prop"));
        assertEquals("val2", gp.<String>getProperty("key2", "prop"));
    }
}



