package com.guicedee.client.services;

import com.google.inject.Provider;
import com.guicedee.client.IGuiceContext;

/**
 * Provides an {@link IGuiceContext} instance for bootstrap.
 */
public interface IGuiceProvider extends Provider<IGuiceContext>
{
	/**
	 * Provides an instance of {@link IGuiceContext}.
	 *
	 * @return an initialized Guice context
	 */
	IGuiceContext get();
}
