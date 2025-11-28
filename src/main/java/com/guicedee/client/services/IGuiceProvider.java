package com.guicedee.client.services;

import com.guicedee.client.*;

/**
 * Internal use, provides the context
 */
public interface IGuiceProvider
{
	/**
	 * Provides an instance of GuiceContext
	 *
	 * @return An instance of GuiceContext
	 */
	IGuiceContext get();
}
