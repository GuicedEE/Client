package com.guicedee.client.services.config;

import io.github.classgraph.ResourceList;

import java.util.Map;

/**
 * Marks the class as a file scanner
 */
@FunctionalInterface
public interface IFileContentsScanner
{
	/**
	 * Returns a contents processor to run on match
	 *
	 * @return the maps of file identifiers and contents
	 */
	Map<String, ResourceList.ByteArrayConsumer> onMatch();
}
