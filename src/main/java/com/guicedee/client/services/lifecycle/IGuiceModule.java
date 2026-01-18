package com.guicedee.client.services.lifecycle;

import com.guicedee.client.services.IDefaultService;

/**
 * Module contribution hook for Guice injector creation.
 * <p>
 * Purpose: register bindings and scopes via Guice modules.
 * Trigger: invoked during injector creation.
 * Order: ascending {@link #sortOrder()}, default 100.
 * Idempotency: module bindings should be deterministic and safe to register once.
 */
public interface IGuiceModule<J extends IGuiceModule<J>>
		extends IDefaultService<J>, com.google.inject.Module
{
	/**
	 * Indicates whether this module should be applied.
	 *
	 * @return true when the module is enabled
	 */
	default boolean enabled()
	{
		return true;
	}
}
