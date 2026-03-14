package com.guicedee.client.services.config;

import com.guicedee.client.services.IDefaultService;
import io.github.classgraph.ResourceList;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Marks the class as a file scanner
 */
public interface IFileContentsPatternScanner<J extends IFileContentsPatternScanner<J>>
		extends IDefaultService<J>
{
	/**
	 * Returns a contents processor to run on match
	 *
	 * @return the maps of file identifiers and contents
	 */
	Map<Pattern, ResourceList.ByteArrayConsumer> onMatch();
}
