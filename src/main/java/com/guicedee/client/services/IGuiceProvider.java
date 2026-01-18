package com.guicedee.client.services;

import com.guicedee.client.*;

/**
 * Provides an {@link IGuiceContext} instance for bootstrap.
 */
public interface IGuiceProvider
{
	/**
	 * Provides an instance of {@link IGuiceContext}.
	 *
	 * @return an initialized Guice context
	 */
	IGuiceContext get();
}
