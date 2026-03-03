package com.guicedee.client.test;

import com.guicedee.client.scopes.CallScopeSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CallScopeSourceTest {

    @Test
    void allExpectedValuesExist() {
        CallScopeSource[] values = CallScopeSource.values();
        assertTrue(values.length > 0);

        // Verify all known sources
        assertNotNull(CallScopeSource.valueOf("Unknown"));
        assertNotNull(CallScopeSource.valueOf("Http"));
        assertNotNull(CallScopeSource.valueOf("WebSocket"));
        assertNotNull(CallScopeSource.valueOf("RabbitMQ"));
        assertNotNull(CallScopeSource.valueOf("Timer"));
        assertNotNull(CallScopeSource.valueOf("SerialPort"));
        assertNotNull(CallScopeSource.valueOf("Transaction"));
        assertNotNull(CallScopeSource.valueOf("Test"));
        assertNotNull(CallScopeSource.valueOf("Rest"));
        assertNotNull(CallScopeSource.valueOf("Persistence"));
        assertNotNull(CallScopeSource.valueOf("WebService"));
        assertNotNull(CallScopeSource.valueOf("Startup"));
        assertNotNull(CallScopeSource.valueOf("VertXConsumer"));
        assertNotNull(CallScopeSource.valueOf("VertXProducer"));
        assertNotNull(CallScopeSource.valueOf("Event"));
    }

    @Test
    void valueOfReturnsCorrectEnum() {
        assertEquals(CallScopeSource.Http, CallScopeSource.valueOf("Http"));
        assertEquals(CallScopeSource.Unknown, CallScopeSource.valueOf("Unknown"));
    }

    @Test
    void valueOfThrowsForInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> CallScopeSource.valueOf("InvalidSource"));
    }

    @Test
    void ordinalIsConsistent() {
        // Unknown should be first
        assertEquals(0, CallScopeSource.Unknown.ordinal());
        // All ordinals should be unique
        CallScopeSource[] values = CallScopeSource.values();
        for (int i = 0; i < values.length; i++) {
            assertEquals(i, values[i].ordinal());
        }
    }

    @Test
    void nameReturnsEnumConstantName() {
        assertEquals("Http", CallScopeSource.Http.name());
        assertEquals("WebSocket", CallScopeSource.WebSocket.name());
        assertEquals("RabbitMQ", CallScopeSource.RabbitMQ.name());
    }
}

