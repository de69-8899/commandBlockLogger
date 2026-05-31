package com.example.commandblocklogger.events;

import net.minecraft.util.math.BlockPos;

import java.time.Instant;

public record CommandBlockExecutionEvent(
        Instant timestamp,
        String time,
        String world,
        BlockPos pos,
        String commandBlockType,
        String command,
        String status,
        int result,
        String output,
        String triggerSource,
        String error
) {
    public boolean failed() {
        return "FAILED".equalsIgnoreCase(status);
    }
}
