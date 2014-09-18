// Copyright (c) 2014 Project Iris. All rights reserved.
//
// The current language binding is an official support library of the Iris
// cloud messaging framework, and as such, the same licensing terms apply.
// For details please see http://iris.karalabe.com/downloads#License
package com.karalabe.iris.common;

import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides helper methods to enter MDC contextual log messages.
 */
public class ContextualLogger {
    private static final int TRUNCATE_CAP = 256; // Number of bytes to truncate a binary blob to before logging.

    private final Logger              logger;  // Logger through which to pass the entries.
    private final Map<String, String> context; // Context assigned to this particular logger.

    /**
     * Wraps an SLF4J logger with flexible context handling.
     * @param logger  logger to wrap with some MDC context
     * @param context list of key-value pairs to add as the context
     */
    public ContextualLogger(final Logger logger, String... context) {
        this.logger = logger;

        this.context = new HashMap<>();
        for (int i = 0; i < context.length; i += 2) {
            this.context.put(context[i], context[i + 1]);
        }
    }

    /**
     * Creates a new contextual logger, by further specializing an existing one.
     * @param logger  logger of which to further extend the context
     * @param context list of additional key-value pairs to insert into the context
     */
    public ContextualLogger(final ContextualLogger logger, String... context) {
        this.logger = logger.logger;

        this.context = new HashMap<>();
        for (Map.Entry<String, String> entry : logger.context.entrySet()) {
            this.context.put(entry.getKey(), entry.getValue());
        }
        for (int i = 0; i < context.length; i += 2) {
            this.context.put(context[i], context[i + 1]);
        }
    }

    /**
     * Loads the context into the current thread's MDC.
     */
    public void loadContext() {
        for (Map.Entry<String, String> entry : context.entrySet()) {
            MDC.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Unloads the context from the current thread's MDC.
     */
    public void unloadContext() {
        for (String key : context.keySet()) {
            MDC.remove(key);
        }
    }

    // Adds a temporary context to the current thread's MDC.
    private static void loadTemporaryContext(String... context) {
        for (int i = 0; i < context.length; i += 2) {
            MDC.put(context[i], context[i + 1]);
        }
    }

    // Unloads a temporary context from the current thread's MDC.
    private static void unloadTemporaryContext(String... context) {
        for (int i = 0; i < context.length; i += 2) {
            MDC.remove(context[i]);
        }
    }

    /**
     * Checks whether this logger should report debug levels.
     *
     * Mainly used for lazy evaluation (i.e. don't load/unload the context and evaluate
     * additional log variables unless there's actually a need for them).
     * @return true if yes, false otherwise
     */
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    /**
     * Enters an MDC context tagged debug entry.
     * @param message textual data of the log entry
     * @param context list of additional key-value pairs to insert into the log entry
     */
    public void debug(final String message, String... context) {
        if (logger.isDebugEnabled()) {
            loadTemporaryContext(context);
            logger.debug(message);
            unloadTemporaryContext(context);
        }
    }

    /**
     * Checks whether this logger should report info levels.
     *
     * Mainly used for lazy evaluation (i.e. don't load/unload the context and evaluate
     * additional log variables unless there's actually a need for them).
     * @return true if yes, false otherwise
     */
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    /**
     * Enters an MDC context tagged info entry.
     * @param message textual data of the log entry
     * @param context list of additional key-value pairs to insert into the log entry
     */
    public void info(final String message, String... context) {
        if (logger.isInfoEnabled()) {
            loadTemporaryContext(context);
            logger.info(message);
            unloadTemporaryContext(context);
        }
    }

    /**
     * Checks whether this logger should report warning levels.
     *
     * Mainly used for lazy evaluation (i.e. don't load/unload the context and evaluate
     * additional log variables unless there's actually a need for them).
     * @return true if yes, false otherwise
     */
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    /**
     * Enters an MDC context tagged warning entry.
     * @param message textual data of the log entry
     * @param context list of additional key-value pairs to insert into the log entry
     */
    public void warn(final String message, String... context) {
        if (logger.isWarnEnabled()) {
            loadTemporaryContext(context);
            logger.warn(message);
            unloadTemporaryContext(context);
        }
    }

    /**
     * Checks whether this logger should report error levels.
     *
     * Mainly used for lazy evaluation (i.e. don't load/unload the context and evaluate
     * additional log variables unless there's actually a need for them).
     * @return true if yes, false otherwise
     */
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    /**
     * Enters an MDC context tagged error entry.
     * @param message textual data of the log entry
     * @param context list of additional key-value pairs to insert into the log entry
     */
    public void error(final String message, String... context) {
        if (logger.isErrorEnabled()) {
            loadTemporaryContext(context);
            logger.error(message);
            unloadTemporaryContext(context);
        }
    }

    /**
     * Truncates a binary data blob to a capped size to prevent overloading the loggers.
     * @param data binary blob to truncate if too large
     * @return byte array with a capped maximum length
     */
    public byte[] truncate(final byte[] data) {
        return (data.length < TRUNCATE_CAP) ? data : Arrays.copyOfRange(data, 0, TRUNCATE_CAP);
    }
}
