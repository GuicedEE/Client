package com.guicedee.client;

import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Convenience helpers for resolving environment variables and system properties.
 * <p>
 * Resolution order (highest priority wins):
 * <ol>
 *     <li>System properties ({@code -Dkey=value})</li>
 *     <li>OS environment variables</li>
 *     <li>{@code .env.local} file (local overrides, typically git-ignored)</li>
 *     <li>{@code .env} file (shared defaults, may be committed)</li>
 *     <li>Provided default value</li>
 * </ol>
 */
@Log4j2
public class Environment {

    /** Parsed entries from {@code .env} file. */
    private static volatile Map<String, String> dotEnvEntries = Collections.emptyMap();
    /** Parsed entries from {@code .env.local} file (overrides .env). */
    private static volatile Map<String, String> dotEnvLocalEntries = Collections.emptyMap();
    /** Whether dot-env files have been loaded. */
    private static final AtomicBoolean dotEnvLoaded = new AtomicBoolean(false);

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
        ensureDotEnvLoaded();

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
        }

        // Check .env.local (higher priority than .env)
        String dotEnvLocalValue = getDotEnvValue(name, dotEnvLocalEntries);
        if (dotEnvLocalValue != null) {
            log.debug("📄 Resolved from .env.local - Name: '{}', Value: '{}'", name, dotEnvLocalValue);
            try {
                System.setProperty(name, dotEnvLocalValue);
                return System.getProperty(name);
            } catch (Exception T) {
                return dotEnvLocalValue;
            }
        }

        // Check .env
        String dotEnvValue = getDotEnvValue(name, dotEnvEntries);
        if (dotEnvValue != null) {
            log.debug("📄 Resolved from .env - Name: '{}', Value: '{}'", name, dotEnvValue);
            try {
                System.setProperty(name, dotEnvValue);
                return System.getProperty(name);
            } catch (Exception T) {
                return dotEnvValue;
            }
        }

        if (defaultValue == null) {
            return "";
        }
        String resolvedDefault = resolvePlaceholders(defaultValue);
        log.trace("📋 Using default value for property - Name: '{}', Value: '{}'", name, resolvedDefault);
        try {
            System.setProperty(name, resolvedDefault);
            return System.getProperty(name);
        } catch (Exception T) {
            log.debug("⚠️ Couldn't set system property to default value - Name: '{}', Value: '{}'",
                    name, resolvedDefault, T);
            return resolvedDefault;
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
        ensureDotEnvLoaded();

        if (System.getProperty(name) != null) {
            return System.getProperty(name);
        }
        if (System.getenv(name) != null) {
            return System.getenv(name);
        }
        String envName = name.toUpperCase().replace('.', '_').replace('-', '_');
        if (System.getenv(envName) != null) {
            return System.getenv(envName);
        }
        // Check .env.local then .env
        String localValue = getDotEnvValue(name, dotEnvLocalEntries);
        if (localValue != null) {
            return localValue;
        }
        return getDotEnvValue(name, dotEnvEntries);
    }

    // ─── Dot-env file support ────────────────────────────────────────────────────

    /**
     * Looks up a property name in a dot-env map, trying exact match first,
     * then the uppercase/underscored variant.
     */
    private static String getDotEnvValue(String name, Map<String, String> entries) {
        if (entries.isEmpty()) {
            return null;
        }
        String value = entries.get(name);
        if (value != null) {
            return value;
        }
        // Try uppercase/underscored variant
        String envName = name.toUpperCase().replace('.', '_').replace('-', '_');
        return entries.get(envName);
    }

    /**
     * Lazily loads {@code .env} and {@code .env.local} files from the working directory.
     * Also checks the classpath root for dot-env files (supports IDE runs where user.dir
     * may differ from the module directory).
     * Thread-safe — files are loaded at most once.
     */
    private static void ensureDotEnvLoaded() {
        if (dotEnvLoaded.get()) {
            return;
        }
        if (dotEnvLoaded.compareAndSet(false, true)) {
            Path cwd = Path.of(System.getProperty("user.dir", "."));

            // Load from working directory
            Map<String, String> envFile = loadDotEnvFile(cwd.resolve(".env"));
            Map<String, String> envLocalFile = loadDotEnvFile(cwd.resolve(".env.local"));

            // If not found in cwd, walk up parent directories (max 5 levels)
            if (envFile.isEmpty() || envLocalFile.isEmpty()) {
                Path parent = cwd.getParent();
                int depth = 0;
                while (parent != null && depth < 5) {
                    if (envFile.isEmpty()) {
                        envFile = loadDotEnvFile(parent.resolve(".env"));
                    }
                    if (envLocalFile.isEmpty()) {
                        envLocalFile = loadDotEnvFile(parent.resolve(".env.local"));
                    }
                    if (!envFile.isEmpty() && !envLocalFile.isEmpty()) {
                        break;
                    }
                    parent = parent.getParent();
                    depth++;
                }
            }

            // Final fallback: classpath root (supports IDE runs)
            if (envFile.isEmpty()) {
                try {
                    var resource = Environment.class.getClassLoader().getResource(".env");
                    if (resource != null && "file".equals(resource.getProtocol())) {
                        envFile = loadDotEnvFile(Path.of(resource.toURI()));
                    }
                } catch (Exception ignored) {}
            }
            if (envLocalFile.isEmpty()) {
                try {
                    var resource = Environment.class.getClassLoader().getResource(".env.local");
                    if (resource != null && "file".equals(resource.toURI().getScheme())) {
                        envLocalFile = loadDotEnvFile(Path.of(resource.toURI()));
                    }
                } catch (Exception ignored) {}
            }

            dotEnvEntries = envFile;
            dotEnvLocalEntries = envLocalFile;

            int total = dotEnvEntries.size() + dotEnvLocalEntries.size();
            if (total > 0) {
                log.info("📄 Loaded dot-env files — .env: {} entries, .env.local: {} entries (cwd: {})",
                        dotEnvEntries.size(), dotEnvLocalEntries.size(), cwd);
            }
        }
    }

    /**
     * Parses a dot-env file into a map.
     * Supports:
     * <ul>
     *     <li>Comments ({@code #})</li>
     *     <li>Empty lines</li>
     *     <li>{@code KEY=VALUE} and {@code KEY="VALUE"} and {@code KEY='VALUE'}</li>
     *     <li>{@code export KEY=VALUE} prefix</li>
     *     <li>Inline comments after unquoted values</li>
     * </ul>
     */
    private static Map<String, String> loadDotEnvFile(Path path) {
        if (!Files.isRegularFile(path)) {
            return Collections.emptyMap();
        }
        Map<String, String> entries = new LinkedHashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                // Strip optional 'export ' prefix
                if (line.startsWith("export ")) {
                    line = line.substring(7).trim();
                }
                int eqIdx = line.indexOf('=');
                if (eqIdx <= 0) {
                    continue;
                }
                String key = line.substring(0, eqIdx).trim();
                String value = line.substring(eqIdx + 1).trim();

                // Unquote if wrapped in matching quotes
                if (value.length() >= 2) {
                    char first = value.charAt(0);
                    char last = value.charAt(value.length() - 1);
                    if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                        value = value.substring(1, value.length() - 1);
                    } else {
                        // Strip inline comments for unquoted values
                        int commentIdx = value.indexOf(" #");
                        if (commentIdx > 0) {
                            value = value.substring(0, commentIdx).trim();
                        }
                    }
                }
                entries.put(key, value);
            }
            log.debug("📄 Parsed {} entries from {}", entries.size(), path.getFileName());
        } catch (IOException e) {
            log.debug("⚠️ Could not read dot-env file '{}': {}", path, e.getMessage());
        }
        return Collections.unmodifiableMap(entries);
    }

    /**
     * Forces a reload of dot-env files. Useful after changing the working directory or in tests.
     */
    public static void reloadDotEnv() {
        dotEnvLoaded.set(false);
        dotEnvEntries = Collections.emptyMap();
        dotEnvLocalEntries = Collections.emptyMap();
        ensureDotEnvLoaded();
    }

    /**
     * Returns all entries loaded from the {@code .env} file (unmodifiable).
     */
    public static Map<String, String> getDotEnvEntries() {
        ensureDotEnvLoaded();
        return dotEnvEntries;
    }

    /**
     * Returns all entries loaded from the {@code .env.local} file (unmodifiable).
     */
    public static Map<String, String> getDotEnvLocalEntries() {
        ensureDotEnvLoaded();
        return dotEnvLocalEntries;
    }
}
