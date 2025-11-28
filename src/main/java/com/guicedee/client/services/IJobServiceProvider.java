package com.guicedee.client.services;

/**
 * Internal use, provides the context
 */
public interface IJobServiceProvider
{
	/**
	 * Provides an instance of JobService
	 *
	 * @return An instance of JobService
	 */
	IJobService get();
}
