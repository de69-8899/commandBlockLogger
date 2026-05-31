package com.example.commandblocklogger.mixin;

import com.example.commandblocklogger.CommandBlockLoggerMod;
import com.example.commandblocklogger.events.CommandBlockExecutionEvent;
import com.example.commandblocklogger.util.CommandBlockTypeResolver;
import net.minecraft.block.BlockState;
import net.minecraft.block.CommandBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.CommandBlockExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Mixin(CommandBlock.class)
public abstract class CommandBlockMixin {
    @Unique
    private static final DateTimeFormatter CBL_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Inject(method = "execute", at = @At("RETURN"))
    private void commandblocklogger$afterExecute(
            BlockState state,
            ServerWorld world,
            BlockPos pos,
            CommandBlockExecutor executor,
            boolean hasCommand,
            CallbackInfo ci
    ) {
        try {
            if (!hasCommand || executor == null) return;

            String command = executor.getCommand();
            Text outputText = executor.getLastOutput();
            String output = outputText == null ? "" : outputText.getString();

            int successCount = executor.getSuccessCount();
            boolean success = successCount > 0;

            CommandBlockExecutionEvent event = new CommandBlockExecutionEvent(
                    Instant.now(),
                    LocalTime.now().format(CBL_TIME_FORMAT),
                    world.getRegistryKey().getValue().toString(),
                    pos.toImmutable(),
                    CommandBlockTypeResolver.resolve(state),
                    command,
                    success ? "SUCCESS" : "FAILED",
                    successCount,
                    output,
                    "command_block",
                    success ? "" : output
            );

            CommandBlockLoggerMod.logService().submit(event);
        } catch (Throwable t) {
            CommandBlockLoggerMod.LOGGER.error("Failed to capture command block execution.", t);
        }
    }
}
