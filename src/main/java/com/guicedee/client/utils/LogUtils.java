package com.guicedee.client.utils;

import com.google.common.base.Strings;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.CompositeTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.JsonLayout;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.Appender;

import java.io.File;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Logging helpers for configuring Log4j2 appenders in client applications.
 */
public class LogUtils
{
    private static final Set<String> names = new HashSet<>();

    private static boolean isCloud()
    {
        // Lazy access to Environment to avoid circular init; refer by FQN
        String v;
        try
        {
            v = com.guicedee.client.Environment.getSystemPropertyOrEnvironment("CLOUD", "false");
        }
        catch (Throwable t)
        {
            v = "false";
        }
        if (v == null)
            return false;
        String s = v.trim().toLowerCase();
        return s.equals("true") || s.equals("1") || s.equals("yes") || s.equals("y") || s.equals("on");
    }

    private static Layout<? extends Serializable> buildLayout(boolean highlighted)
    {
        if (isCloud())
        {
            // JSON output for cloud-friendly ingestion
            return JsonLayout.newBuilder()
                    .setComplete(false)
                    .setCompact(true)
                    .setEventEol(true)
                    .build();
        }
        if (highlighted)
        {
            String pattern = "%highlight{[%d{yyyy-MM-dd HH:mm:ss.SSS}] [%25.25C{3}] [%t] [%-5level]} - %msg%n";
            return PatternLayout.newBuilder()
                    .withPattern(pattern)
                    .withAlwaysWriteExceptions(true)
                    .build();
        }
        return PatternLayout.newBuilder()
                .withPattern("[%d{yyyy-MM-dd HH:mm:ss.SSS}] [%25.25C{3}] [%t] [%-5level] - [%msg]%n")
                .build();
    }

    // Sanitize file base names for Windows reserved names and illegal characters.
    // This does NOT change the logger name, only the generated file name.
    private static String sanitizeForWindows(String baseName)
    {
        if (baseName == null || baseName.isEmpty())
        {
            return "_log";
        }
        String s = baseName.trim();
        // Replace illegal characters
        s = s.replaceAll("[\\\\/:*?\"<>|]", "_");
        // Trim trailing dots and spaces
        while (s.endsWith(" ") || s.endsWith("."))
        {
            s = s.substring(0, s.length() - 1);
        }
        if (s.isEmpty())
        {
            s = "_log";
        }
        // Windows reserved device names
        String up = s.toUpperCase();
        // Special case: COM0 should use the shared cerial log, not a per-port file
        if (up.equals("COM0"))
        {
            return "cerial";
        }
        if (up.equals("CON") || up.equals("PRN") || up.equals("AUX") || up.equals("NUL"))
        {
            return "_" + s;
        }
        if ((up.startsWith("COM") || up.startsWith("LPT")) && up.length() == 4)
        {
            char d = up.charAt(3);
            if (d >= '1' && d <= '9')
            {
                return "_" + s;
            }
        }
        return s;
    }

    /**
     * Adds a ConsoleAppender that logs to System.out with a sensible default pattern.
     * If already added in this JVM, this is a no-op.
     */
    public static void addConsoleLogger()
    {
        addConsoleLogger(Level.DEBUG);
    }

    /**
     * Adds a ConsoleAppender that logs to System.out at the given level.
     * If already added in this JVM, this is a no-op.
     */
    public static void addConsoleLogger(Level level)
    {
        String key = "console_stdout";
        if (names.contains(key))
            return;
        names.add(key);

        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();

        Layout<? extends Serializable> layout = buildLayout(false);

        ConsoleAppender consoleAppender = ConsoleAppender.newBuilder()
                .setName("ConsoleStdOut")
                .setTarget(ConsoleAppender.Target.SYSTEM_OUT)
                .setLayout(layout)
                .setFollow(true)
                .build();
        consoleAppender.start();

        config.addAppender(consoleAppender);
        config.getRootLogger().addAppender(consoleAppender, level == null ? Level.DEBUG : level, null);
    }

    /**
     * Adds a highlighted ConsoleAppender (ANSI colors) to System.out using Log4j2 %highlight pattern converter.
     * If already added in this JVM, this is a no-op.
     */
    public static void addHighlightedConsoleLogger()
    {
        addHighlightedConsoleLogger(Level.DEBUG);
    }

    /**
     * Adds a highlighted ConsoleAppender (ANSI colors) to System.out at the given level.
     * If already added in this JVM, this is a no-op.
     */
    public static void addHighlightedConsoleLogger(Level level)
    {
        String key = "console_stdout_highlight";
        if (names.contains(key))
            return;
        names.add(key);

        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();

        Layout<? extends Serializable> layout = buildLayout(true);

        ConsoleAppender consoleAppender = ConsoleAppender.newBuilder()
                .setName("ConsoleStdOutHighlight")
                .setTarget(ConsoleAppender.Target.SYSTEM_OUT)
                .setLayout(layout)
                .setFollow(true)
                .build();
        consoleAppender.start();

        config.addAppender(consoleAppender);
        config.getRootLogger().addAppender(consoleAppender, level == null ? Level.DEBUG : level, null);
    }

    /**
     * Adds a rolling file logger for the given name and base directory.
     *
     * @param name the logger name and base file name
     * @param baseLogFolder the base directory for log files; defaults to {@code logs}
     */
    public static void addFileRollingLogger(String name, String baseLogFolder)
    {
        if (names.contains(name))
            return;
        names.add(name);

        LoggerContext context = (LoggerContext) LogManager.getContext(false); // Don't reinitialize
        Configuration config = context.getConfiguration();

        String logFolderPath = Strings.isNullOrEmpty(baseLogFolder) ? "logs" : baseLogFolder; // Base folder for logs
        String safeBase = sanitizeForWindows(name);
        String logFileName = safeBase + ".log";

        Layout<? extends Serializable> layout = buildLayout(false);

        RollingFileAppender rollingFileAppender = RollingFileAppender.newBuilder()
                .withFileName(logFolderPath + "/" + logFileName)
                .withFilePattern(logFolderPath + "/%d{yyyy-MM-dd}/" + logFileName + ".%i.gz")
                .withPolicy(CompositeTriggeringPolicy.createPolicy(
                        TimeBasedTriggeringPolicy.newBuilder().withInterval(1).withModulate(true).build(),
                        SizeBasedTriggeringPolicy.createPolicy("100MB")
                ))
                .withStrategy(DefaultRolloverStrategy.newBuilder()
                        //.withMax("7") // Keep 7 rollovers
                        .build())
                .withLayout(layout)
                .withName("RollingFile")
                .withAppend(true)
                .build();
        rollingFileAppender.start();

        // Rollover on startup: back up existing active file once at initialization
        try {
            File activeFile = new File(logFolderPath + "/" + logFileName);
            if (activeFile.exists() && activeFile.length() > 0) {
                rollingFileAppender.getManager().rollover();
            }
        } catch (Throwable ignored) {
            // Best-effort; ignore if rollover manager not available
        }

        // Add the rolling file appender to the configuration
        config.addAppender(rollingFileAppender);
        config.getRootLogger().addAppender(rollingFileAppender, Level.DEBUG, null);
    }

    /**
     * Adds a rolling file logger using a minimal configuration and default log directory.
     *
     * @param name the logger name and base file name
     */
    public static void addMinimalFileRollingLogger(String name)
    {
        if (names.contains(name))
            return;
        names.add(name);

        LoggerContext context = (LoggerContext) LogManager.getContext(false); // Don't reinitialize
        Configuration config = context.getConfiguration();

        String logFolderPath = "logs"; // Base folder for logs
        String safeBase = sanitizeForWindows(name);
        String logFileName = safeBase + ".log";

        Layout<? extends Serializable> layout = buildLayout(false);

        RollingFileAppender rollingFileAppender = RollingFileAppender.newBuilder()
                .withFileName(logFolderPath + "/" + logFileName)
                .withFilePattern(logFolderPath + "/%d{yyyy-MM-dd}/" + logFileName + ".%d{HH-mm-ss}.%i.gz")
                .withPolicy(CompositeTriggeringPolicy.createPolicy(
                        TimeBasedTriggeringPolicy.newBuilder().withInterval(1).withModulate(true).build(),
                        SizeBasedTriggeringPolicy.createPolicy("100MB")
                ))
                .withStrategy(DefaultRolloverStrategy.newBuilder()
                        //.withMax("7") // Keep 7 rollovers
                        .build())
                .withLayout(layout)
                .withName("RollingFile")
                .withAppend(true)
                .build();
        rollingFileAppender.start();

        // Rollover on startup: back up existing active file once at initialization
        try {
            File activeFile = new File(logFolderPath + "/" + logFileName);
            if (activeFile.exists() && activeFile.length() > 0) {
                rollingFileAppender.getManager().rollover();
            }
        } catch (Throwable ignored) {
            // Best-effort; ignore if rollover manager not available
        }

        // Add the rolling file appender to the configuration
        config.addAppender(rollingFileAppender);
        config.getRootLogger().addAppender(rollingFileAppender, Level.DEBUG, null);
    }


    /**
     * Creates or returns a logger with a dedicated rolling file appender.
     *
     * @param name the logger name and base file name
     * @param baseLogFolder the base directory for log files; defaults to {@code logs}
     * @param pattern an optional Log4j2 pattern layout
     * @param logToRoot whether this logger should be additive to the root logger
     * @return the configured logger instance
     */
    public static Logger getSpecificRollingLogger(String name, String baseLogFolder, String pattern, boolean logToRoot)
    {
        LoggerContext context = (LoggerContext) LogManager.getContext(false); // Don't reinitialize
        if (names.contains(name))
            return context.getLogger(name);

        names.add(name);
        Configuration config = context.getConfiguration();

        PatternLayout layout = PatternLayout.newBuilder()
                .withPattern(Strings.isNullOrEmpty(pattern) ? "[%d{yyyy-MM-dd HH:mm:ss.SSS}] [%25.25C{3}] [%t] [%-5level] - [%msg]%n" : pattern)
                .build();

        String logFolderPath = Strings.isNullOrEmpty(baseLogFolder) ? "logs" : baseLogFolder; // Base folder for logs
        String safeBase = sanitizeForWindows(name);
        String logFileName = safeBase + ".log";

        RollingFileAppender rollingFileAppender = RollingFileAppender.newBuilder()
                .withFileName(logFolderPath + "/" + logFileName)
                .withFilePattern(logFolderPath + "/%d{yyyy-MM-dd}/" + logFileName + "%i.gz")
                .withPolicy(CompositeTriggeringPolicy.createPolicy(
                        TimeBasedTriggeringPolicy.newBuilder().withInterval(1).withModulate(true).build(),
                        SizeBasedTriggeringPolicy.createPolicy("100MB")
                ))
                .withStrategy(DefaultRolloverStrategy.newBuilder()
                        //.withMax("7") // Keep 7 rollovers
                        .build())
                .withLayout(layout)
                .withName("RollingFile" + name)
                .withAppend(true)
                .build();
        rollingFileAppender.start();

        // Rollover on startup: back up existing active file once at initialization
        try {
            File activeFile = new File(logFolderPath + "/" + logFileName);
            if (activeFile.exists() && activeFile.length() > 0) {
                rollingFileAppender.getManager().rollover();
            }
        } catch (Throwable ignored) {
            // Best-effort; ignore if rollover manager not available
        }

        // Add the rolling file appender to the configuration
        config.addAppender(rollingFileAppender);

        // Create a logger config for the specific logger
        LoggerConfig specificLoggerConfig = LoggerConfig.createLogger(
                logToRoot,                            // Additivity (false means it will only use its own appenders, not root logger's)
                Level.DEBUG,                       // Logging level for this specific logger
                name,   // Logger name
                "true",                           // Include location for stack traces
                new AppenderRef[]{                 // Appender references
                        AppenderRef.createAppenderRef("RollingFile" + name, Level.DEBUG, null) // Attach appender
                },
                null,                             // Properties
                config,                           // Configuration object
                null                              // Filter
        );
        specificLoggerConfig.addAppender(rollingFileAppender, Level.INFO, null);

        // Add the specific logger configuration to the Log4j2 configuration
        config.addLogger(name, specificLoggerConfig);

        return context.getLogger(name);
    }

    /**
     * Programmatically adds an appender to the root logger.
     *
     * @param appender The appender to add.
     * @param level    The level to use for the appender.
     */
    public static void addAppender(Appender appender, Level level)
    {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();
        appender.start();
        config.addAppender(appender);
        config.getRootLogger().addAppender(appender, level == null ? Level.DEBUG : level, null);
    }
}
