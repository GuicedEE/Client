package com.guicedee.client.test;

import com.guicedee.client.utils.OptionalPair;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OptionalPairTest {

    @Test
    void constructorSetsKeyAndValue() {
        OptionalPair<String, Integer> pair = new OptionalPair<>("k", 1);
        assertEquals("k", pair.getKey());
        assertEquals(1, pair.getValue());
    }

    @Test
    void defaultConstructorLeavesFieldsNull() {
        OptionalPair<String, String> pair = new OptionalPair<>();
        assertNull(pair.getKey());
        assertNull(pair.getValue());
    }

    @Test
    void getKeyOptionalPresent() {
        OptionalPair<String, Integer> pair = new OptionalPair<>("key", 1);
        Optional<String> opt = pair.getKeyOptional();
        assertTrue(opt.isPresent());
        assertEquals("key", opt.get());
    }

    @Test
    void getKeyOptionalEmpty() {
        OptionalPair<String, Integer> pair = new OptionalPair<>();
        Optional<String> opt = pair.getKeyOptional();
        assertTrue(opt.isEmpty());
    }

    @Test
    void getValueOptionalPresent() {
        OptionalPair<String, Integer> pair = new OptionalPair<>("k", 42);
        Optional<Integer> opt = pair.getValueOptional();
        assertTrue(opt.isPresent());
        assertEquals(42, opt.get());
    }

    @Test
    void getValueOptionalEmpty() {
        OptionalPair<String, Integer> pair = new OptionalPair<>("k", null);
        Optional<Integer> opt = pair.getValueOptional();
        assertTrue(opt.isEmpty());
    }

    @Test
    void setKeyReturnsOptionalPairInstance() {
        OptionalPair<String, String> pair = new OptionalPair<>();
        OptionalPair<String, String> result = pair.setKey("newKey");
        assertSame(pair, result);
        assertEquals("newKey", pair.getKey());
    }

    @Test
    void setValueReturnsOptionalPairInstance() {
        OptionalPair<String, String> pair = new OptionalPair<>();
        OptionalPair<String, String> result = pair.setValue("newValue");
        assertSame(pair, result);
        assertEquals("newValue", pair.getValue());
    }

    @Test
    void toStringContainsKeyAndValue() {
        OptionalPair<String, Integer> pair = new OptionalPair<>("name", 42);
        String s = pair.toString();
        assertTrue(s.contains("name"));
        assertTrue(s.contains("42"));
    }

    @Test
    void toStringWithNullValues() {
        OptionalPair<String, String> pair = new OptionalPair<>();
        String s = pair.toString();
        // Should not throw and should contain null representations
        assertNotNull(s);
    }

    @Test
    void chainingSetters() {
        OptionalPair<String, String> pair = new OptionalPair<String, String>()
                .setKey("k")
                .setValue("v");
        assertEquals("k", pair.getKey());
        assertEquals("v", pair.getValue());
    }

    @Test
    void inheritsCompareToFromPair() {
        OptionalPair<String, Integer> a = new OptionalPair<>("alpha", 1);
        OptionalPair<String, Integer> b = new OptionalPair<>("beta", 2);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
    }

    @Test
    void inheritsEqualsFromPair() {
        OptionalPair<String, Integer> a = new OptionalPair<>("x", 1);
        OptionalPair<String, Integer> b = new OptionalPair<>("x", 2);
        // Pair equality is by key only
        assertEquals(a, b);
    }

    @Test
    void inheritsIsEmptyFromPair() {
        OptionalPair<String, String> empty = new OptionalPair<>();
        assertTrue(empty.isEmpty());

        OptionalPair<String, String> notEmpty = new OptionalPair<>("k", null);
        assertFalse(notEmpty.isEmpty());
    }
}

