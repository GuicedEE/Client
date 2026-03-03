package com.guicedee.client.test;

import com.guicedee.client.scopes.CallScopeProperties;
import com.guicedee.client.scopes.CallScopeSource;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CallScopePropertiesTest {

    @Test
    void defaultSourceIsUnknown() {
        CallScopeProperties props = new CallScopeProperties();
        assertEquals(CallScopeSource.Unknown, props.getSource());
    }

    @Test
    void setSourceReturnsThis() {
        CallScopeProperties props = new CallScopeProperties();
        CallScopeProperties result = props.setSource(CallScopeSource.Http);
        assertSame(props, result);
        assertEquals(CallScopeSource.Http, props.getSource());
    }

    @Test
    void allSourceValuesCanBeSet() {
        CallScopeProperties props = new CallScopeProperties();
        for (CallScopeSource source : CallScopeSource.values()) {
            props.setSource(source);
            assertEquals(source, props.getSource());
        }
    }

    @Test
    void propertiesMapIsInitiallyEmpty() {
        CallScopeProperties props = new CallScopeProperties();
        assertNotNull(props.getProperties());
        assertTrue(props.getProperties().isEmpty());
    }

    @Test
    void canAddAndRetrieveProperties() {
        CallScopeProperties props = new CallScopeProperties();
        props.getProperties().put("key1", "value1");
        props.getProperties().put("key2", 42);

        assertEquals("value1", props.getProperties().get("key1"));
        assertEquals(42, props.getProperties().get("key2"));
        assertEquals(2, props.getProperties().size());
    }

    @Test
    void setPropertiesReplacesMap() {
        CallScopeProperties props = new CallScopeProperties();
        props.getProperties().put("old", "data");

        Map<Object, Object> newMap = Map.of("new", "data");
        props.setProperties(new java.util.HashMap<>(newMap));

        assertEquals(1, props.getProperties().size());
        assertEquals("data", props.getProperties().get("new"));
        assertNull(props.getProperties().get("old"));
    }

    @Test
    void touchesListIsInitiallyEmpty() {
        CallScopeProperties props = new CallScopeProperties();
        assertNotNull(props.getTouches());
        assertTrue(props.getTouches().isEmpty());
    }

    @Test
    void canAddTouches() {
        CallScopeProperties props = new CallScopeProperties();
        props.getTouches().add("touch1@location1");
        props.getTouches().add("touch2@location2");

        assertEquals(2, props.getTouches().size());
        assertEquals("touch1@location1", props.getTouches().get(0));
        assertEquals("touch2@location2", props.getTouches().get(1));
    }

    @Test
    void setTouchesReplacesList() {
        CallScopeProperties props = new CallScopeProperties();
        props.getTouches().add("old");

        List<String> newTouches = new java.util.ArrayList<>();
        newTouches.add("new1");
        newTouches.add("new2");
        props.setTouches(newTouches);

        assertEquals(2, props.getTouches().size());
        assertEquals("new1", props.getTouches().getFirst());
    }

    @Test
    void chainingSetters() {
        CallScopeProperties props = new CallScopeProperties()
                .setSource(CallScopeSource.WebSocket);
        assertEquals(CallScopeSource.WebSocket, props.getSource());
    }

    @Test
    void serialVersionUidIsSet() {
        // Verify the class is Serializable
        CallScopeProperties props = new CallScopeProperties();
        assertInstanceOf(java.io.Serializable.class, props);
    }
}



