package com.guicedee.client.implementations;

import com.guicedee.client.IGuiceContext;
import com.guicedee.client.services.lifecycle.IGuicePreStartup;
import io.vertx.core.Future;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import lombok.extern.log4j.Log4j2;

import java.util.List;

/**
 * Pre-startup hook that configures scanning defaults and reloads Mutiny interceptors.
 * <p>
 * Purpose: ensure the client context has sensible scanning defaults and Mutiny interceptors are active.
 * Trigger: executed during {@link com.guicedee.client.services.lifecycle.IGuicePreStartup#onStartup()}.
 * Order: {@link #sortOrder()} returns the earliest possible priority.
 * Idempotency: safe to run once; repeated runs reapply configuration and reload interceptors.
 */
@Log4j2
public class GuicedEEClientStartup implements IGuicePreStartup<GuicedEEClientStartup> {
    /**
     * Creates a new pre-startup hook for client initialization.
     */
    public GuicedEEClientStartup() {
    }

    /**
     * Configures the GuicedEE client scanning defaults and reloads Mutiny interceptors.
     *
     * @return a completed future indicating startup success
     */
    @Override
    public List<Future<Boolean>> onStartup() {
        log.trace("🚀 Starting GuicedEE Client initialization");
        try {
            log.trace("📋 Configuring GuicedEE scanning options");
            IGuiceContext
                    .instance()
                    .getConfig()
                    .setFieldScanning(true)
                    .setMethodInfo(true)
                    .setIgnoreClassVisibility(true)
                    .setIgnoreMethodVisibility(true)
                    .setIgnoreFieldVisibility(true)
                    .setAnnotationScanning(true)
            ;
            log.debug("✅ GuicedEE scanning options configured successfully");
            log.trace("🔄 Reloading Mutiny Uni interceptors");
            Infrastructure.reloadUniInterceptors();
            log.trace("✅ GuicedEE Client initialized successfully");
        } catch (Throwable T) {
            log.error("❌ No Guice Client Instantiation Found: {}", T.getMessage(), T);
            log.error("💥 Please add guiced-injection to the classpath to resolve this issue");
        }
        log.trace("📤 Returning startup result");
        return List.of(Future.succeededFuture(true));
    }

    /**
     * Ensures this startup hook runs before other services.
     *
     * @return the sort order
     */
    @Override
    public Integer sortOrder() {
        return Integer.MIN_VALUE + 1;
    }
}
