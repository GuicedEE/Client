package com.guicedee.client.services;

import org.apache.logging.log4j.core.config.Configuration;

/**
 * Hook for customizing Log4j2 configuration during startup.
 */
public interface Log4JConfigurator
{
    /**
     * Applies configuration changes to the Log4j2 {@link Configuration}.
     *
     * @param config the Log4j2 configuration to update
     */
    void configure(Configuration config);
}
