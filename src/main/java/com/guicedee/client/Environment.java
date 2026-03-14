package com.guicedee.client;

import lombok.extern.log4j.Log4j2;

/**
 * Convenience helpers for resolving environment variables and system properties.
 */
@Log4j2
public class Environment {
    /**
     * Utility class — not instantiable.
     */
    private Environment() {
    }

    /**
     * Resolves a property from system properties or environment variables, falling back to a default.
     *
     * @param key          the system property or environment variable name
     * @param defaultValue the value to use when neither source is set
     * @return the resolved value from system properties, environment variables, or the default
     */
    public static String getProperty(String key, String defaultValue) {
        if (System.getProperty(key) == null) {
            if (System.getenv(key) == null) {
                System.setProperty(key, resolvePlaceholders(defaultValue));
            } else {
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
     * @param name         the name of the variable
     * @param defaultValue the default value to always return
     * @return the required value from the environment or system properties
     */
    public static String getSystemPropertyOrEnvironment(String name, String defaultValue) {
        if (System.getProperty(name) != null) {
            return System.getProperty(name);
        }
        if (System.getenv(name) != null) {
            try {
                System.setProperty(name, System.getenv(name));
                return System.getProperty(name);
            } catch (Exception T) {
                log.debug("⚠️ Couldn't set system property value from environment - Name: '{}', Default: '{}'",
                        name, defaultValue, T);
                return System.getenv(name);
            }
        }
        String envName = name.toUpperCase().replace('.', '_').replace('-', '_');
        if (System.getenv(envName) != null) {
            try {
                System.setProperty(name, System.getenv(envName));
                return System.getProperty(name);
            } catch (Exception T) {
                log.debug("⚠️ Couldn't set system property value from environment - Name: '{}', EnvName: '{}', Default: '{}'",
                        name, envName, defaultValue, T);
                return System.getenv(envName);
            }
        } else {
            if (defaultValue == null) {
                return "";
            }
            String resolvedDefault = resolvePlaceholders(defaultValue);
            log.debug("📋 Using default value for property - Name: '{}', Value: '{}'", name, resolvedDefault);
            try {
                System.setProperty(name, resolvedDefault);
                return System.getProperty(name);
            } catch (Exception T) {
                log.debug("⚠️ Couldn't set system property to default value - Name: '{}', Value: '{}'",
                        name, resolvedDefault, T);
                return resolvedDefault;
            }
        }
    }

    /**
     * Resolves placeholders in the form of ${env.VAR:-default} or ${VAR:-default} or ${VAR}.
     *
     * @param value The value containing placeholders.
     * @return The resolved value.
     */
    public static String resolvePlaceholders(String value) {
        if (value == null || !value.contains("${")) {
            return value;
        }

        String previous;
        String current = value;
        int iterations = 0;
        do {
            previous = current;
            current = resolvePlaceholdersOnce(current);
            iterations++;
        } while (!current.equals(previous) && iterations < 10); // Limit recursion to 10 levels

        return current;
    }

    private static String resolvePlaceholdersOnce(String value) {
        if (value == null || !value.contains("${")) {
            return value;
        }

        StringBuilder result = new StringBuilder();
        int lastPos = 0;
        int start = value.indexOf("${");

        while (start != -1) {
            result.append(value, lastPos, start);
            int end = findMatchingEnd(value, start);
            if (end == -1) {
                break;
            }

            String placeholder = value.substring(start + 2, end);
            String defaultValue = null;

            int defaultIdx = placeholder.indexOf(":-");
            int separatorLength = 2;
            if (defaultIdx == -1) {
                defaultIdx = placeholder.indexOf(":");
                separatorLength = 1;
            }
            if (defaultIdx != -1) {
                defaultValue = placeholder.substring(defaultIdx + separatorLength);
                placeholder = placeholder.substring(0, defaultIdx);
            }

            if (placeholder.startsWith("env.")) {
                placeholder = placeholder.substring(4);
            }

            String resolvedValue = getRawSystemPropertyOrEnvironment(placeholder);
            if (resolvedValue == null || resolvedValue.isEmpty()) {
                resolvedValue = defaultValue != null ? defaultValue : "${" + placeholder + "}";
            }

            result.append(resolvedValue);
            lastPos = end + 1;
            start = value.indexOf("${", lastPos);
        }

        result.append(value.substring(lastPos));
        return result.toString();
    }

    private static int findMatchingEnd(String value, int start) {
        int depth = 0;
        for (int i = start; i < value.length(); i++) {
            if (value.startsWith("${", i)) {
                depth++;
                i++; // skip {
            } else if (value.charAt(i) == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String getRawSystemPropertyOrEnvironment(String name) {
        if (System.getProperty(name) != null) {
            return System.getProperty(name);
        }
        if (System.getenv(name) != null) {
            return System.getenv(name);
        }
        String envName = name.toUpperCase().replace('.', '_').replace('-', '_');
        return System.getenv(envName);
    }

}
