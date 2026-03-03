package com.guicedee.client.test;

import com.guicedee.client.utils.Pair;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PairTest {

    @Test
    void constructorSetsKeyAndValue() {
        Pair<String, Integer> pair = new Pair<>("name", 42);
        assertEquals("name", pair.getKey());
        assertEquals(42, pair.getValue());
    }

    @Test
    void defaultConstructorLeavesFieldsNull() {
        Pair<String, String> pair = new Pair<>();
        assertNull(pair.getKey());
        assertNull(pair.getValue());
    }

    @Test
    void setKeyReturnsThis() {
        Pair<String, String> pair = new Pair<>();
        Pair<String, String> result = pair.setKey("k");
        assertSame(pair, result);
        assertEquals("k", pair.getKey());
    }

    @Test
    void setValueReturnsThis() {
        Pair<String, String> pair = new Pair<>();
        Pair<String, String> result = pair.setValue("v");
        assertSame(pair, result);
        assertEquals("v", pair.getValue());
    }

    @Test
    void ofFactoryCreatesPair() {
        Pair<String, Integer> pair = Pair.of("a", 1);
        assertEquals("a", pair.getKey());
        assertEquals(1, pair.getValue());
    }

    @Test
    void emptyReturnsPairWithNullKeyAndValue() {
        Pair<String, String> empty = Pair.empty();
        assertNull(empty.getKey());
        assertNull(empty.getValue());
    }

    @Test
    void isEmptyWhenKeyIsNull() {
        Pair<String, String> pair = new Pair<>();
        assertTrue(pair.isEmpty());
    }

    @Test
    void isNotEmptyWhenKeyIsSet() {
        Pair<String, String> pair = new Pair<>("k", null);
        assertFalse(pair.isEmpty());
    }

    @Test
    void compareToOrdersByKey() {
        Pair<String, Integer> a = Pair.of("alpha", 1);
        Pair<String, Integer> b = Pair.of("beta", 2);

        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(Pair.of("alpha", 99)));
    }

    @Test
    void equalsSameKeyDifferentValue() {
        Pair<String, Integer> a = Pair.of("x", 1);
        Pair<String, Integer> b = Pair.of("x", 2);
        assertEquals(a, b, "Pairs with the same key should be equal");
    }

    @Test
    void notEqualsDifferentKey() {
        Pair<String, Integer> a = Pair.of("x", 1);
        Pair<String, Integer> b = Pair.of("y", 1);
        assertNotEquals(a, b);
    }

    @Test
    void equalsNull() {
        Pair<String, Integer> a = Pair.of("x", 1);
        assertNotEquals(null, a);
    }

    @Test
    void equalsSameInstance() {
        Pair<String, Integer> a = Pair.of("x", 1);
        Pair<String, Integer> ref = a;
        assertEquals(a, ref);
    }

    @Test
    void hashCodeConsistentWithEquals() {
        Pair<String, Integer> a = Pair.of("x", 1);
        Pair<String, Integer> b = Pair.of("x", 2);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void toStringContainsKeyAndValue() {
        Pair<String, Integer> pair = Pair.of("name", 42);
        String s = pair.toString();
        assertTrue(s.contains("name"));
        assertTrue(s.contains("42"));
    }

    @Test
    void chainingSetters() {
        Pair<String, String> pair = new Pair<String, String>()
                .setKey("key")
                .setValue("value");
        assertEquals("key", pair.getKey());
        assertEquals("value", pair.getValue());
    }
}


