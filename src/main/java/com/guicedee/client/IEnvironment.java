package com.guicedee.client;

import java.util.Map;

/**
 * Defines a named environment backed by an enum with optional properties.
 *
 * @param <J> the concrete enum type
 */
public interface IEnvironment<J extends Enum<J> & IEnvironment<J>>
{
		/**
			* Returns the environment name (typically the enum constant name).
		 *
		 * @return the environment name
			*/
		String name();
		
		/**
			* Returns the enum ordinal for ordering or persistence.
		 *
		 * @return the ordinal value
			*/
		int ordinal();
		
		/**
			* Provides configuration properties associated with this environment.
		 *
		 * @return a map of environment properties
			*/
		Map<String, Object> properties();
}
