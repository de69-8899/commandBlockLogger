package com.example.commandblocklogger.util;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

public final class CommandBlockTypeResolver {
    private CommandBlockTypeResolver() {}

    public static String resolve(BlockState state) {
        if (state == null) return "UNKNOWN";
        if (state.isOf(Blocks.CHAIN_COMMAND_BLOCK)) return "CHAIN";
        if (state.isOf(Blocks.REPEATING_COMMAND_BLOCK)) return "REPEAT";
        if (state.isOf(Blocks.COMMAND_BLOCK)) return "IMPULSE";
        return "UNKNOWN";
    }
}
