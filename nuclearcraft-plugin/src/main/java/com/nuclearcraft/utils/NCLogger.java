package com.nuclearcraft.utils;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized logger for NuclearCraft.
 * Supports INFO, WARNING, ERROR, and DEBUG levels.
 * Debug output is gated behind a configurable flag.
 */
public final class NCLogger {

    private static Logger logger;
    private static boolean debugMode = false;

    private NCLogger() {}

    public static void init(JavaPlugin plugin) {
        logger = plugin.getLogger();
    }

    public static void setDebugMode(boolean enabled) {
        debugMode = enabled;
    }

    public static boolean isDebugMode() {
        return debugMode;
    }

    public static void info(String message) {
        checkInit();
        logger.info(message);
    }

    public static void warn(String message) {
        checkInit();
        logger.warning(message);
    }

    public static void severe(String message) {
        checkInit();
        logger.severe(message);
    }

    public static void severe(String message, Throwable throwable) {
        checkInit();
        logger.log(Level.SEVERE, message, throwable);
    }

    public static void debug(String message) {
        if (debugMode) {
            checkInit();
            logger.info("[DEBUG] " + message);
        }
    }

    public static void debug(String format, Object... args) {
        if (debugMode) {
            checkInit();
            logger.info("[DEBUG] " + String.format(format, args));
        }
    }

    private static void checkInit() {
        if (logger == null) {
            throw new IllegalStateException("NCLogger has not been initialized. Call NCLogger.init(plugin) first.");
        }
    }
}
