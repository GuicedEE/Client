package com.guicedee.client.services.lifecycle;

import com.guicedee.client.services.IGuiceConfig;

/**
 * Configuration hook for customizing {@link IGuiceConfig} before injector creation.
 * <p>
 * Purpose: mutate configuration for scanning and injector behavior.
 * Trigger: invoked during context bootstrap before injector build.
 * Order: determined by the caller or service loader ordering.
 * Idempotency: implementations should avoid side effects beyond config mutation.
 */
@FunctionalInterface
public interface IGuiceConfigurator {
    /**
     * Configures the Guice context.
     *
     * @param config the configuration object to mutate
     * @return the configured Guice configuration
     */
    IGuiceConfig<?> configure(IGuiceConfig<?> config);


}
