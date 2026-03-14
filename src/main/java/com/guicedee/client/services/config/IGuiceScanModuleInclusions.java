package com.guicedee.client.services.config;

import com.guicedee.client.services.IDefaultService;

import java.util.Set;

/**
 * Supplies module names to include when scanning.
 *
 * @param <J> the implementing inclusions type
 */
@FunctionalInterface
public interface IGuiceScanModuleInclusions<J extends IGuiceScanModuleInclusions<J>>
		extends IDefaultService<J>
{
	/**
	 * Returns the module names to include in scanning.
	 *
	 * @return the included module names
	 */
	Set<String> includeModules();

}
