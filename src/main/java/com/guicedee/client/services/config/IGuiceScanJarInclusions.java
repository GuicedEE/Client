package com.guicedee.client.services.config;

import com.guicedee.client.services.IDefaultService;

import java.util.Set;

/**
 * Supplies jar names to include when scanning.
 *
 * @param <J> the implementing inclusions type
 */
@FunctionalInterface
public interface IGuiceScanJarInclusions<J extends IGuiceScanJarInclusions<J>>
		extends IDefaultService<J>
{
	/**
	 * Returns the jar names to include in scanning.
	 *
	 * @return the included jar names
	 */
	Set<String> includeJars();


}
