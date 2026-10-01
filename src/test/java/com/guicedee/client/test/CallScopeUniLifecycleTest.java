package com.guicedee.client.test;

import com.guicedee.client.scopes.CallScoper;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CallScopeUniLifecycleTest {
    private final Vertx vertx = Vertx.vertx();
    private final Context context = vertx.getOrCreateContext();

    @AfterEach
    void close() throws Exception {
        vertx.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }

    @Test
    void completedItemReleasesUpstreamWhileDownstreamRetainsHandle() throws Exception {
        Fixture fixture = onContext(() -> fixture(false));
        onContext(() -> { fixture.subscriber.onItem("done"); return null; });
        assertEquals("done", fixture.downstream.item);
        assertCollected(fixture.upstream);
        fixture.downstream.subscription.cancel();
        assertEquals(0, fixture.cancelled.get());
        assertEquals(0, fixture.scoper.exits.get(), "Caller-owned scope must remain open");
    }

    @Test
    void failureReleasesUpstreamAndExitsOwnedScopeOnce() throws Exception {
        Fixture fixture = onContext(() -> fixture(true));
        Throwable failure = new IllegalStateException("expected");
        onContext(() -> { fixture.subscriber.onFailure(failure); return null; });
        assertSame(failure, fixture.downstream.failure);
        assertCollected(fixture.upstream);
        fixture.downstream.subscription.cancel();
        assertEquals(1, fixture.scoper.exits.get());
        assertEquals(0, fixture.cancelled.get());
    }

    @Test
    void cancellationRunsOnceOnOriginalContextAndReleasesUpstream() throws Exception {
        Fixture fixture = onContext(() -> fixture(true));
        fixture.downstream.subscription.cancel();
        fixture.downstream.subscription.cancel();
        fixture.cancellation.get(5, TimeUnit.SECONDS);
        onContext(() -> null); // Wait for scope cleanup after upstream cancellation.
        assertEquals(1, fixture.cancelled.get());
        assertEquals(1, fixture.scoper.exits.get());
        assertSame(context, fixture.scoper.exitContext);
        assertCollected(fixture.upstream);
    }

    private Fixture fixture(boolean ownsScope) throws Exception {
        var constructor = Class.forName(
                "com.guicedee.client.scopes.mutiny.CallScopeUniInterceptor$ScopedUniSubscriber")
                .getDeclaredConstructor(UniSubscriber.class, CallScoper.class, boolean.class);
        constructor.setAccessible(true);
        Downstream downstream = new Downstream();
        CountingScoper scoper = new CountingScoper();
        @SuppressWarnings("unchecked")
        UniSubscriber<String> subscriber = (UniSubscriber<String>) constructor.newInstance(downstream, scoper, ownsScope);
        AtomicInteger cancelled = new AtomicInteger();
        CompletableFuture<Void> cancellation = new CompletableFuture<>();
        UniSubscription upstream = new UniSubscription() {
            @Override public void request(long n) { }
            @Override public void cancel() {
                assertSame(context, Vertx.currentContext());
                cancelled.incrementAndGet();
                cancellation.complete(null);
            }
        };
        subscriber.onSubscribe(upstream);
        return new Fixture(subscriber, downstream, scoper, new WeakReference<>(upstream), cancelled, cancellation);
    }

    private static void assertCollected(WeakReference<?> reference) throws Exception {
        for (int attempt = 0; attempt < 30 && reference.get() != null; attempt++) {
            System.gc();
            Thread.sleep(20);
        }
        assertNull(reference.get(), "A retained completed handle must not retain its database subscription");
    }

    private <T> T onContext(CheckedSupplier<T> action) throws Exception {
        CompletableFuture<T> result = new CompletableFuture<>();
        context.runOnContext(ignored -> {
            try { result.complete(action.get()); }
            catch (Throwable failure) { result.completeExceptionally(failure); }
        });
        return result.get(5, TimeUnit.SECONDS);
    }

    @FunctionalInterface private interface CheckedSupplier<T> { T get() throws Exception; }
    private record Fixture(UniSubscriber<String> subscriber, Downstream downstream, CountingScoper scoper,
                           WeakReference<UniSubscription> upstream, AtomicInteger cancelled,
                           CompletableFuture<Void> cancellation) { }

    private static final class Downstream implements UniSubscriber<String> {
        private UniSubscription subscription;
        private String item;
        private Throwable failure;
        @Override public void onSubscribe(UniSubscription subscription) { this.subscription = subscription; }
        @Override public void onItem(String item) { this.item = item; }
        @Override public void onFailure(Throwable failure) { this.failure = failure; }
    }

    private static final class CountingScoper extends CallScoper {
        private final AtomicInteger exits = new AtomicInteger();
        private Context exitContext;
        @Override public void exit() {
            exitContext = Vertx.currentContext();
            exits.incrementAndGet();
        }
    }
}
