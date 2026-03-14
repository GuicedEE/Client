package com.guicedee.client.services.lifecycle;

import com.guicedee.client.services.IDefaultService;
import com.guicedee.client.services.IGuiceConfig;

/**
 * Configuration hook for customizing {@link IGuiceConfig} before injector creation.
 * <p>
 * Purpose: mutate configuration for scanning and injector behavior.
 * Trigger: invoked during context bootstrap before injector build.
 * Order: ascending {@link #sortOrder()}, default 100.
 * Idempotency: implementations should avoid side effects beyond config mutation.
 *
 * @param <J> the implementing configurator type
 */
public interface IGuiceConfigurator<J extends IGuiceConfigurator<J>>
		extends IDefaultService<J> {
    /**
     * Configures the Guice context.
     *
     * @param config the configuration object to mutate
     * @return the configured Guice configuration
     */
    IGuiceConfig<?> configure(IGuiceConfig<?> config);


}
