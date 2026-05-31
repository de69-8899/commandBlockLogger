package com.example.commandblocklogger.commands;

import com.example.commandblocklogger.CommandBlockLoggerMod;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

public final class CommandBlockLoggerCommands {
    private CommandBlockLoggerCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("commandblocklogger")
                .requires(source -> true)
                .then(literal("reload").executes(ctx -> {
                    CommandBlockLoggerMod.reloadConfig();
                    ctx.getSource().sendFeedback(() -> Text.literal("CommandBlockLogger config reloaded."), false);
                    return 1;
                }))
                .then(literal("stats").executes(ctx -> {
                    var stats = CommandBlockLoggerMod.stats();
                    ctx.getSource().sendFeedback(() -> Text.literal(
                            "CommandBlockLogger stats: total=" + stats.totalCommands()
                                    + ", failed=" + stats.failedCommands()
                                    + ", failureRate=" + String.format("%.2f", stats.failureRate()) + "%"
                    ), false);
                    stats.topCommands(8).forEach((cmd, count) ->
                            ctx.getSource().sendFeedback(() -> Text.literal(" - " + cmd + ": " + count), false)
                    );
                    return 1;
                }))
                .then(literal("enable").executes(ctx -> {
                    var cfg = CommandBlockLoggerMod.config();
                    cfg.enabled = true;
                    cfg.save();
                    CommandBlockLoggerMod.reloadConfig();
                    ctx.getSource().sendFeedback(() -> Text.literal("CommandBlockLogger enabled."), false);
                    return 1;
                }))
                .then(literal("disable").executes(ctx -> {
                    var cfg = CommandBlockLoggerMod.config();
                    cfg.enabled = false;
                    cfg.save();
                    CommandBlockLoggerMod.reloadConfig();
                    ctx.getSource().sendFeedback(() -> Text.literal("CommandBlockLogger disabled."), false);
                    return 1;
                }))
        );
    }
}
