package com.example.commandblocklogger.config;

import com.example.commandblocklogger.CommandBlockLoggerMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CommandBlockLoggerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("commandblocklogger.json");

    public boolean enabled = true;
    public boolean logFailuresOnly = false;
    public boolean ignoreSuccessfulExecutions = false;
    public boolean writeJsonLogs = true;
    public boolean writeConsoleLogs = true;
    public boolean compressedConsoleLogs = true;
    public boolean writeHumanReadableLogs = true;
    public boolean writeCompactLogs = false;
    public int maxLogFiles = 30;
    public int queueCapacity = 16384;
    public int writerFlushIntervalMs = 1000;
    public boolean includeOutput = true;
    public boolean includeStackTraces = false;
    public boolean mergeRepeatedLogs = true;
    public int mergeWindowMs = 1500;

    public List<String> allowedDimensions = new ArrayList<>();
    public List<CoordinateFilter> allowedCoordinates = new ArrayList<>();
    public List<String> ignoreCommands = new ArrayList<>();

    public static CommandBlockLoggerConfig load() {
        try {
            if (Files.notExists(CONFIG_PATH)) {
                CommandBlockLoggerConfig cfg = defaults();
                cfg.save();
                return cfg;
            }

            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                CommandBlockLoggerConfig cfg = GSON.fromJson(reader, CommandBlockLoggerConfig.class);
                if (cfg == null) cfg = defaults();
                cfg.normalize();
                cfg.save();
                return cfg;
            }
        } catch (Exception e) {
            CommandBlockLoggerMod.LOGGER.error("Failed to load commandblocklogger.json, using defaults.", e);
            return defaults();
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            CommandBlockLoggerMod.LOGGER.error("Failed to save commandblocklogger.json", e);
        }
    }

    public boolean dimensionAllowed(String worldId) {
        return allowedDimensions == null || allowedDimensions.isEmpty() || allowedDimensions.contains(worldId);
    }

    public boolean coordinatesAllowed(BlockPos pos) {
        if (allowedCoordinates == null || allowedCoordinates.isEmpty()) return true;
        for (CoordinateFilter filter : allowedCoordinates) {
            if (filter.matches(pos)) return true;
        }
        return false;
    }

    public boolean commandIgnored(String command) {
        if (ignoreCommands == null || ignoreCommands.isEmpty()) return false;
        String normalized = command == null ? "" : command.toLowerCase();
        for (String pattern : ignoreCommands) {
            if (pattern != null && !pattern.isBlank() && normalized.contains(pattern.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public void normalize() {
        if (maxLogFiles < 1) maxLogFiles = 1;
        if (queueCapacity < 1024) queueCapacity = 1024;
        if (writerFlushIntervalMs < 100) writerFlushIntervalMs = 100;
        if (mergeWindowMs < 100) mergeWindowMs = 100;
        if (allowedDimensions == null) allowedDimensions = new ArrayList<>();
        if (allowedCoordinates == null) allowedCoordinates = new ArrayList<>();
        if (ignoreCommands == null) ignoreCommands = new ArrayList<>();
    }

    public static CommandBlockLoggerConfig defaults() {
        CommandBlockLoggerConfig cfg = new CommandBlockLoggerConfig();
        cfg.ignoreCommands.add("execute if");
        cfg.ignoreCommands.add("execute unless");
        return cfg;
    }

    public static final class CoordinateFilter {
        public int x;
        public int y;
        public int z;

        public boolean matches(BlockPos pos) {
            return pos.getX() == x && pos.getY() == y && pos.getZ() == z;
        }
    }
}
