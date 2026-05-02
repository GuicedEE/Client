package com.guicedee.client.test;

import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.guicedee.client.scopes.CallScoper;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CallScoper} verifying Vert.x context-local scope storage.
 * <p>
 * {@code enter()} and {@code exit()} invoke {@code IGuiceContext.loaderToSet()}
 * for lifecycle listeners which requires a full Guice bootstrap.  These unit tests
 * exercise the lower-level scope/seed/provider mechanics directly via reflection
 * on the private {@code setScopeMap} helper.
 */
class CallScoperTest {

    private CallScoper scoper;
    private Vertx vertx;

    @BeforeEach
    void setUp() throws Exception {
        scoper = new CallScoper();
        vertx = Vertx.vertx();
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clean up scope on the Vert.x context if still active
        CompletableFuture<Void> cleanup = new CompletableFuture<>();
        vertx.getOrCreateContext().runOnContext(v -> {
            try {
                if (scoper.isStartedScope()) {
                    invokeScopeMapSetter(null);
                }
                cleanup.complete(null);
            } catch (Throwable t) {
                cleanup.complete(null); // best-effort
            }
        });
        cleanup.get(5, TimeUnit.SECONDS);
        vertx.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }

    /**
     * Invokes the private {@code setScopeMap} via reflection.
     * Must be called on a thread with a Vert.x context.
     */
    private void invokeScopeMapSetter(Map<Key<?>, Object> map) throws Exception {
        var m = CallScoper.class.getDeclaredMethod("setScopeMap", Map.class);
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

    // ── No Vert.x context → should fail ──

    @Test
    void noVertxContextThrowsOnEnter() {
        // No Vert.x context on the test thread
        assertNull(Vertx.currentContext());
        var ex = assertThrows(java.lang.reflect.InvocationTargetException.class,
                () -> invokeScopeMapSetter(new java.util.HashMap<>()));
        assertInstanceOf(IllegalStateException.class, ex.getCause());
    }

    @Test
    void isStartedScopeReturnsFalseWithoutVertxContext() {
        assertNull(Vertx.currentContext());
        assertFalse(scoper.isStartedScope());
    }

    @Test
    void seedOutsideScopeThrows() {
        assertThrows(OutOfScopeException.class, () -> scoper.seed(String.class, "value"));
    }

    @Test
    void scopeProviderOutsideScopeThrows() {
        Provider<String> scoped = scoper.scope(Key.get(String.class), () -> "fail");
        assertThrows(OutOfScopeException.class, scoped::get);
    }

    // ── Vert.x context tests ──

    @Test
    void enterCreatesScope() throws Exception {
        runOnContext(() -> {
            invokeScopeMapSetter(new java.util.HashMap<>());
            assertTrue(scoper.isStartedScope());
            assertNotNull(scoper.getValues());
        });
    }

    @Test
    void exitClearsScope() throws Exception {
        runOnContext(() -> {
            invokeScopeMapSetter(new java.util.HashMap<>());
            invokeScopeMapSetter(null);
            assertFalse(scoper.isStartedScope());
            assertNull(scoper.getValues());
        });
    }

    @Test
    void seedAndRetrieveValue() throws Exception {
        runOnContext(() -> {
            invokeScopeMapSetter(new java.util.HashMap<>());
            scoper.seed(String.class, "hello");
            assertEquals("hello", scoper.getValues().get(Key.get(String.class)));
        });
    }

    @Test
    void seedWithKeyAndRetrieve() throws Exception {
        runOnContext(() -> {
            invokeScopeMapSetter(new java.util.HashMap<>());
            Key<Integer> key = Key.get(Integer.class);
            scoper.seed(key, 42);
            assertEquals(42, scoper.getValues().get(key));
        });
    }

    @Test
    void seedDuplicateKeyThrows() throws Exception {
        runOnContext(() -> {
            invokeScopeMapSetter(new java.util.HashMap<>());
            scoper.seed(String.class, "first");
            assertThrows(IllegalStateException.class, () -> scoper.seed(String.class, "second"));
        });
    }

    @Test
    void setValuesMergesIntoCurrentScope() throws Exception {
        runOnContext(() -> {
            invokeScopeMapSetter(new java.util.HashMap<>());
            scoper.seed(String.class, "existing");
            scoper.setValues(Map.of(Key.get(Integer.class), 42));
            assertEquals(42, scoper.getValues().get(Key.get(Integer.class)));
            assertEquals("existing", scoper.getValues().get(Key.get(String.class)));
        });
    }

    @Test
    void scopeProviderCachesInstance() throws Exception {
        runOnContext(() -> {
            invokeScopeMapSetter(new java.util.HashMap<>());
            int[] callCount = {0};
            Provider<String> unscoped = () -> {
                callCount[0]++;
                return "created";
            };
            Provider<String> scoped = scoper.scope(Key.get(String.class), unscoped);
            assertEquals("created", scoped.get());
            assertEquals("created", scoped.get());
            assertEquals(1, callCount[0], "Unscoped provider should only be called once");
        });
    }

    @Test
    void scopeIsolationBetweenCycles() throws Exception {
        runOnContext(() -> {
            invokeScopeMapSetter(new java.util.HashMap<>());
            scoper.seed(String.class, "first");
            invokeScopeMapSetter(null);

            invokeScopeMapSetter(new java.util.HashMap<>());
            assertNull(scoper.getValues().get(Key.get(String.class)));
        });
    }

    @Test
    void fullLifecycleOnVertxContext() throws Exception {
        runOnContext(() -> {
            assertNotNull(Vertx.currentContext(), "Should have a Vert.x context");
            assertFalse(scoper.isStartedScope());

            invokeScopeMapSetter(new java.util.HashMap<>());
            assertTrue(scoper.isStartedScope());
            scoper.seed(String.class, "vertx-value");
            assertEquals("vertx-value", scoper.getValues().get(Key.get(String.class)));

            invokeScopeMapSetter(null);
            assertFalse(scoper.isStartedScope());
        });
    }
}

