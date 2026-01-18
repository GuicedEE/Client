package com.guicedee.client.services.config;

import com.guicedee.client.services.IDefaultService;

import java.util.Set;

/**
 * Marks JAR files referenced from libraries to be excluded from all scans
 */
@FunctionalInterface
public interface IGuiceScanJarExclusions<J extends IGuiceScanJarExclusions<J>>
		extends IDefaultService<J>
{
	/**
	 * Returns the jar names to exclude from scanning.
	 *
	 * @return the excluded jar names
	 */
	Set<String> excludeJars();


}
