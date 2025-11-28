package com.guicedee.client;

import java.util.Map;

/**
	* A definition of an environment
	*
	* @param <J>
	*/
public interface IEnvironment<J extends Enum<J> & IEnvironment<J>>
{
		/**
			* @return A name of an environment
			*/
		String name();
		
		/**
			* @return The ordinal of the enum
			*/
		int ordinal();
		
		/**
			* @return Any properties that are programmatically required through an enum
			*/
		Map<String, Object> properties();
}
