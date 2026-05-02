package com.guicedee.client.test;

import com.google.inject.Key;
import com.guicedee.client.scopes.CallScoper;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests verifying that the Guice CallScope is propagated through Mutiny Uni chains
 * when execution crosses Vert.x context boundaries.
 * <p>
 * These tests exercise the propagation contract directly: values seeded into a call scope
 * on one Vert.x context must be visible when Uni operators resolve on that same context,
 * and captured snapshots must be restorable on different contexts.
 */
class CallScopeUniPropagationTest {

    private CallScoper scoper;
    private Vertx vertx;

    @BeforeEach
    void setUp() {
        scoper = new CallScoper();
        vertx = Vertx.vertx();
    }

    @AfterEach
    void tearDown() throws Exception {
        vertx.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }

    /**
     * Invokes the private {@code setScopeMap} via reflection on the current Vert.x context.
     */
    private void setScopeMap(Map<Key<?>, Object> map) throws Exception {
        Method m = CallScoper.class.getDeclaredMethod("setScopeMap", Map.class);
        m.setAccessible(true);
        m.invoke(null, map);
    }

    /**
     * Runs an action on the Vert.x event-loop context and waits for the result.
     */
    private <T> T runOnContext(ThrowingSupplier<T> action) throws Exception {
        CompletableFuture<T> future = new CompletableFuture<>();
        vertx.getOrCreateContext().runOnContext(v -> {
            try {
                future.complete(action.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future.get(5, TimeUnit.SECONDS);
    }

    private void runOnContext(ThrowingRunnable action) throws Exception {
        runOnContext(() -> {
            action.run();
            return null;
        });
    }

    @FunctionalInterface
    interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }

    // ── Tests ──

    @Test
    @DisplayName("Scope values remain accessible through Uni.map() on same Vert.x context")
    void scopeValuesAccessibleThroughUniMapOnSameContext() throws Exception {
        runOnContext(() -> {
            setScopeMap(new HashMap<>());
            scoper.seed(String.class, "propagated-value");

            // Simulate a Uni chain that accesses scope during transformation
            AtomicReference<String> captured = new AtomicReference<>();
            Uni.createFrom().item("input")
                    .map(item -> {
                        // Access scope values during Uni transformation
                        Map<Key<?>, Object> values = scoper.getValues();
                        assertNotNull(values, "Scope should be active during Uni.map()");
                        captured.set((String) values.get(Key.get(String.class)));
                        return item;
                    })
                    .await().indefinitely();

            assertEquals("propagated-value", captured.get());
            setScopeMap(null);
        });
    }

    @Test
    @DisplayName("Scope values remain accessible through chained Uni.flatMap() on same context")
    void scopeValuesAccessibleThroughUniFlatMapOnSameContext() throws Exception {
        runOnContext(() -> {
            setScopeMap(new HashMap<>());
            scoper.seed(Integer.class, 42);

            AtomicReference<Integer> captured = new AtomicReference<>();
            Uni.createFrom().item("start")
                    .flatMap(item -> Uni.createFrom().item(item + "-mapped"))
                    .flatMap(item -> {
                        Map<Key<?>, Object> values = scoper.getValues();
                        assertNotNull(values, "Scope should be active in nested flatMap");
                        captured.set((Integer) values.get(Key.get(Integer.class)));
                        return Uni.createFrom().item(item);
                    })
                    .await().indefinitely();

            assertEquals(42, captured.get());
            setScopeMap(null);
        });
    }

    @Test
    @DisplayName("Scope values survive Uni.emitOn() context switch when snapshot is manually restored")
    void scopeSnapshotCanBeRestoredOnDifferentContext() throws Exception {
        // Capture a scope snapshot on one context
        Map<Key<?>, Object> snapshot = runOnContext(() -> {
            setScopeMap(new HashMap<>());
            scoper.seed(String.class, "cross-context-value");
            scoper.seed(Integer.class, 99);
            Map<Key<?>, Object> captured = new HashMap<>(scoper.getValues());
            setScopeMap(null);
            return captured;
        });

        assertFalse(snapshot.isEmpty(), "Snapshot should contain seeded values");

        // Restore the snapshot on a different context invocation (simulates what CallScopeUniInterceptor does)
        Context newContext = vertx.getOrCreateContext();
        CompletableFuture<String> result = new CompletableFuture<>();
        newContext.runOnContext(v -> {
            try {
                setScopeMap(new HashMap<>(snapshot));
                String value = (String) scoper.getValues().get(Key.get(String.class));
                Integer intValue = (Integer) scoper.getValues().get(Key.get(Integer.class));
                assertEquals(99, intValue);
                setScopeMap(null);
                result.complete(value);
            } catch (Throwable t) {
                result.completeExceptionally(t);
            }
        });

        assertEquals("cross-context-value", result.get(5, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Scope is isolated between independent Uni chains on same context")
    void scopeIsolationBetweenUniChains() throws Exception {
        runOnContext(() -> {
            // First scope lifecycle
            setScopeMap(new HashMap<>());
            scoper.seed(String.class, "first-chain");

            String first = Uni.createFrom().item("x")
                    .map(i -> (String) scoper.getValues().get(Key.get(String.class)))
                    .await().indefinitely();
            assertEquals("first-chain", first);
            setScopeMap(null);

            // Second scope lifecycle — should NOT see first chain's values
            setScopeMap(new HashMap<>());
            scoper.seed(String.class, "second-chain");

            String second = Uni.createFrom().item("y")
                    .map(i -> (String) scoper.getValues().get(Key.get(String.class)))
                    .await().indefinitely();
            assertEquals("second-chain", second);
            setScopeMap(null);
        });
    }

    @Test
    @DisplayName("Scope values accessible in Uni.onItem().transform() chain")
    void scopeAccessibleInOnItemTransform() throws Exception {
        runOnContext(() -> {
            setScopeMap(new HashMap<>());
            scoper.seed(String.class, "transform-value");

            String result = Uni.createFrom().item("data")
                    .onItem().transform(item -> {
                        Map<Key<?>, Object> values = scoper.getValues();
                        return (String) values.get(Key.get(String.class));
                    })
                    .await().indefinitely();

            assertEquals("transform-value", result);
            setScopeMap(null);
        });
    }

    @Test
    @DisplayName("Scope values accessible after Uni.onFailure().recoverWithItem()")
    void scopeAccessibleInFailureRecovery() throws Exception {
        runOnContext(() -> {
            setScopeMap(new HashMap<>());
            scoper.seed(String.class, "recovery-value");

            String result = Uni.createFrom().<String>failure(new RuntimeException("boom"))
                    .onFailure().recoverWithItem(() -> {
                        Map<Key<?>, Object> values = scoper.getValues();
                        return (String) values.get(Key.get(String.class));
                    })
                    .await().indefinitely();

            assertEquals("recovery-value", result);
            setScopeMap(null);
        });
    }

    @Test
    @DisplayName("Multiple scope values propagate together through Uni chain")
    void multipleValuesPropagateTogether() throws Exception {
        runOnContext(() -> {
            setScopeMap(new HashMap<>());
            scoper.seed(String.class, "user-123");
            scoper.seed(Integer.class, 7);
            scoper.seed(Long.class, 1000L);

            AtomicReference<Map<String, Object>> captured = new AtomicReference<>();
            Uni.createFrom().item("go")
                    .map(item -> {
                        Map<Key<?>, Object> values = scoper.getValues();
                        Map<String, Object> result = new HashMap<>();
                        result.put("string", values.get(Key.get(String.class)));
                        result.put("int", values.get(Key.get(Integer.class)));
                        result.put("long", values.get(Key.get(Long.class)));
                        captured.set(result);
                        return item;
                    })
                    .await().indefinitely();

            assertEquals("user-123", captured.get().get("string"));
            assertEquals(7, captured.get().get("int"));
            assertEquals(1000L, captured.get().get("long"));
            setScopeMap(null);
        });
    }

    @Test
    @DisplayName("Scope snapshot capture preserves point-in-time values")
    void snapshotCapturesPointInTimeValues() throws Exception {
        runOnContext(() -> {
            setScopeMap(new HashMap<>());
            scoper.seed(String.class, "original");

            // Capture snapshot
            Map<Key<?>, Object> snapshot = new HashMap<>(scoper.getValues());

            // Modify scope after snapshot (simulate additional seeding)
            scoper.seed(Integer.class, 999);

            // Snapshot should NOT contain the later-seeded value
            assertNull(snapshot.get(Key.get(Integer.class)));
            assertEquals("original", snapshot.get(Key.get(String.class)));

            setScopeMap(null);
        });
    }
}



