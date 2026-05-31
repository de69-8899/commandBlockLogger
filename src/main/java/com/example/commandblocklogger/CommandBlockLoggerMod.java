package com.example.commandblocklogger;

import com.example.commandblocklogger.commands.CommandBlockLoggerCommands;
import com.example.commandblocklogger.config.CommandBlockLoggerConfig;
import com.example.commandblocklogger.logging.CommandBlockLogService;
import com.example.commandblocklogger.logging.CommandBlockStats;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CommandBlockLoggerMod implements ModInitializer {
    public static final String MOD_ID = "commandblocklogger";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static CommandBlockLoggerConfig config;
    private static CommandBlockLogService logService;
    private static final CommandBlockStats STATS = new CommandBlockStats();

    @Override
    public void onInitialize() {
        config = CommandBlockLoggerConfig.load();
        logService = new CommandBlockLogService(config, STATS);
        logService.start();

        CommandBlockLoggerCommands.register();

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("Stopping CommandBlockLogger...");
            logService.stop();
        });

        LOGGER.info("CommandBlockLogger initialized.");
    }

    public static CommandBlockLoggerConfig config() {
        return config;
    }

    public static CommandBlockLogService logService() {
        return logService;
    }

    public static CommandBlockStats stats() {
        return STATS;
    }

    public static void reloadConfig() {
        config = CommandBlockLoggerConfig.load();
        logService.updateConfig(config);
        LOGGER.info("CommandBlockLogger config reloaded.");
    }
}
