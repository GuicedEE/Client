package com.guicedee.client.services;

/**
 * Defines a service that can be enabled or disabled at runtime.
 *
 * @param <J> the implementing service type
 */
@SuppressWarnings("unused")
@FunctionalInterface
public interface IServiceEnablement<J extends IServiceEnablement<J>> {
	/**
	 * Indicates whether the service should run.
	 *
	 * @return true when the service is enabled
	 */
	boolean enabled();


}
