package com.guicedee.client.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.inject.Key;
import com.guicedee.client.implementations.ObjectBinderKeys;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObjectBinderKeysTest {

    @Test
    void defaultObjectMapperKeyIsNotNull() {
        Key<ObjectMapper> key = ObjectBinderKeys.DefaultObjectMapper;
        assertNotNull(key);
        assertEquals(ObjectMapper.class, key.getTypeLiteral().getRawType());
    }

    @Test
    void jsonObjectWriterKeyIsNotNull() {
        Key<ObjectWriter> key = ObjectBinderKeys.JSONObjectWriter;
        assertNotNull(key);
        assertEquals(ObjectWriter.class, key.getTypeLiteral().getRawType());
    }

    @Test
    void jsonObjectWriterTinyKeyIsNotNull() {
        Key<ObjectWriter> key = ObjectBinderKeys.JSONObjectWriterTiny;
        assertNotNull(key);
        assertEquals(ObjectWriter.class, key.getTypeLiteral().getRawType());
    }

    @Test
    void jsonObjectReaderKeyIsNotNull() {
        Key<ObjectReader> key = ObjectBinderKeys.JSONObjectReader;
        assertNotNull(key);
        assertEquals(ObjectReader.class, key.getTypeLiteral().getRawType());
    }

    @Test
    void javascriptObjectMapperKeyIsNotNull() {
        Key<ObjectMapper> key = ObjectBinderKeys.JavascriptObjectMapper;
        assertNotNull(key);
        assertEquals(ObjectMapper.class, key.getTypeLiteral().getRawType());
    }

    @Test
    void javaScriptObjectWriterKeyIsNotNull() {
        Key<ObjectWriter> key = ObjectBinderKeys.JavaScriptObjectWriter;
        assertNotNull(key);
        assertEquals(ObjectWriter.class, key.getTypeLiteral().getRawType());
    }

    @Test
    void javaScriptObjectWriterTinyKeyIsNotNull() {
        Key<ObjectWriter> key = ObjectBinderKeys.JavaScriptObjectWriterTiny;
        assertNotNull(key);
        assertEquals(ObjectWriter.class, key.getTypeLiteral().getRawType());
    }

    @Test
    void javaScriptObjectReaderKeyIsNotNull() {
        Key<ObjectReader> key = ObjectBinderKeys.JavaScriptObjectReader;
        assertNotNull(key);
        assertEquals(ObjectReader.class, key.getTypeLiteral().getRawType());
    }

    @Test
    void defaultAndJavascriptMapperKeysAreDifferent() {
        assertNotEquals(ObjectBinderKeys.DefaultObjectMapper, ObjectBinderKeys.JavascriptObjectMapper);
    }

    @Test
    void jsonAndJavascriptWriterKeysAreDifferent() {
        assertNotEquals(ObjectBinderKeys.JSONObjectWriter, ObjectBinderKeys.JavaScriptObjectWriter);
    }
}

