package com.guicedee.client;

import lombok.extern.log4j.Log4j2;

/**
 * Convenience helpers for resolving environment variables and system properties.
 */
@Log4j2
public class Environment
{
		/**
		 * Resolves a property from system properties or environment variables, falling back to a default.
		 *
		 * @param key the system property or environment variable name
		 * @param defaultValue the value to use when neither source is set
		 * @return the resolved value from system properties, environment variables, or the default
		 */
		public static String getProperty(String key, String defaultValue)
		{
				if (System.getProperty(key) == null)
				{
						if (System.getenv(key) == null)
						{
								System.setProperty(key, defaultValue);
						}
						else
						{
								System.setProperty(key, System.getenv(key));
						}
				}
				return System.getProperty(key);
		}
		
		/**
		 * Returns an environment or system defined property with a default value.
		 * <p>
		 * System defined properties (-Dxxx=xxx) override environment variables.
		 *
		 * @param name the name of the variable
		 * @param defaultValue the default value to always return
		 * @return the required value from the environment or system properties
		 */
		public static String getSystemPropertyOrEnvironment(String name, String defaultValue)
		{
				if (System.getProperty(name) != null)
				{
						return System.getProperty(name);
				}
				if (System.getenv(name) != null)
				{
						try
						{
								System.setProperty(name, System.getenv(name));
								return System.getProperty(name);
						}
						catch (Exception T)
						{
								log.debug("⚠️ Couldn't set system property value from environment - Name: '{}', Default: '{}'",
																		name, defaultValue, T);
								return System.getenv(name);
						}
				}
				else
				{
						if (defaultValue == null)
						{
								return "";
						}
						log.debug("📋 Using default value for property - Name: '{}', Value: '{}'", name, defaultValue);
						try
						{
								System.setProperty(name, defaultValue);
								return System.getProperty(name);
						}
						catch (Exception T)
						{
								log.debug("⚠️ Couldn't set system property to default value - Name: '{}', Value: '{}'",
																		name, defaultValue, T);
								return defaultValue;
						}
				}
		}
		
}
