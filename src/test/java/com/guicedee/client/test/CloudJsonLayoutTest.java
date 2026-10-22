package com.guicedee.client.test;

import com.guicedee.client.utils.LogUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class CloudJsonLayoutTest {
    @Test
    void cloudLayoutWritesEscapedJsonAndExceptionsWithoutJackson2() {
        var configuration = new DefaultConfiguration();
        var layout = LogUtils.jsonLayout(configuration);
        var event = Log4jLogEvent.newBuilder()
                .setLoggerName("cloud.jackson3")
                .setLevel(Level.ERROR)
                .setMessage(new SimpleMessage("quote \" and newline\nnext"))
                .setThrown(new IllegalArgumentException("test failure"))
                .build();
        String encoded = new String(layout.toByteArray(event), StandardCharsets.UTF_8);
        var json = JsonMapper.builder().build().readTree(encoded);
        assertEquals("cloud.jackson3", json.path("loggerName").asText());
        assertEquals("ERROR", json.path("level").asText());
        assertEquals("quote \" and newline\nnext", json.path("message").asText());
        assertTrue(encoded.contains("IllegalArgumentException"));
        assertTrue(encoded.endsWith(System.lineSeparator()));
        assertFalse(encoded.substring(0, encoded.length() - System.lineSeparator().length()).contains("\n"));
        assertEquals("org.apache.logging.log4j.layout.template.json.JsonTemplateLayout", layout.getClass().getName());
    }
}
