package com.example.commandblocklogger.logging;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

public final class CommandBlockStats {
    private final LongAdder totalCommands = new LongAdder();
    private final LongAdder failedCommands = new LongAdder();
    private final ConcurrentHashMap<String, LongAdder> commandCounts = new ConcurrentHashMap<>();

    public void record(String command, boolean failed) {
        totalCommands.increment();
        if (failed) failedCommands.increment();

        String key = normalizeCommand(command);
        commandCounts.computeIfAbsent(key, ignored -> new LongAdder()).increment();
    }

    public long totalCommands() {
        return totalCommands.sum();
    }

    public long failedCommands() {
        return failedCommands.sum();
    }

    public double failureRate() {
        long total = totalCommands();
        return total == 0 ? 0.0 : (failedCommands() * 100.0) / total;
    }

    public Map<String, Long> topCommands(int limit) {
        return commandCounts.entrySet().stream()
                .sorted(Comparator.comparingLong((Map.Entry<String, LongAdder> e) -> e.getValue().sum()).reversed())
                .limit(Math.max(1, limit))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().sum(),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    private String normalizeCommand(String command) {
        if (command == null || command.isBlank()) return "<empty>";
        String trimmed = command.trim();
        int firstSpace = trimmed.indexOf(' ');
        return firstSpace < 0 ? trimmed : trimmed.substring(0, firstSpace);
    }
}
