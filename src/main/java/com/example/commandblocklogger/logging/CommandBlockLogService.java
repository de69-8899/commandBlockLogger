package com.example.commandblocklogger.logging;

import com.example.commandblocklogger.CommandBlockLoggerMod;
import com.example.commandblocklogger.config.CommandBlockLoggerConfig;
import com.example.commandblocklogger.events.CommandBlockExecutionEvent;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CommandBlockLogService {
    private final CommandBlockStats stats;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile CommandBlockLoggerConfig config;
    private volatile ArrayBlockingQueue<CommandBlockExecutionEvent> queue;
    private Thread worker;

    private DailyRotatingFileWriter mainWriter;
    private DailyRotatingFileWriter errorWriter;

    private PendingLog pending;

    public CommandBlockLogService(CommandBlockLoggerConfig config, CommandBlockStats stats) {
        this.config = config;
        this.stats = stats;
        this.queue = new ArrayBlockingQueue<>(config.queueCapacity);
    }

    public void start() {
        if (!running.compareAndSet(false, true)) return;

        Path logs = FabricLoader.getInstance().getGameDir().resolve("logs");
        mainWriter = new DailyRotatingFileWriter(logs, "commandblocks.log", config.maxLogFiles);
        errorWriter = new DailyRotatingFileWriter(logs, "commandblocks-error.log", config.maxLogFiles);

        worker = new Thread(this::runWriterLoop, "CommandBlockLogger-AsyncWriter");
        worker.setDaemon(true);
        worker.start();
    }

    public void stop() {
        running.set(false);
        if (worker != null) {
            try {
                worker.join(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        flushPending();
        closeWriters();
    }

    public void updateConfig(CommandBlockLoggerConfig newConfig) {
        newConfig.normalize();
        this.config = newConfig;
    }

    public void submit(CommandBlockExecutionEvent event) {
        CommandBlockLoggerConfig cfg = config;
        if (cfg == null || !cfg.enabled) return;
        if (cfg.logFailuresOnly && !event.failed()) return;
        if (cfg.ignoreSuccessfulExecutions && !event.failed()) return;
        if (!cfg.dimensionAllowed(event.world())) return;
        if (!cfg.coordinatesAllowed(event.pos())) return;
        if (cfg.commandIgnored(event.command())) return;

        stats.record(event.command(), event.failed());

        if (!queue.offer(event)) {
            CommandBlockLoggerMod.LOGGER.warn("CommandBlockLogger queue full; dropped command block log at {} {}", event.world(), event.pos());
        }
    }

    private void runWriterLoop() {
        long lastFlush = System.currentTimeMillis();

        while (running.get() || !queue.isEmpty()) {
            try {
                CommandBlockExecutionEvent event = queue.poll(250, TimeUnit.MILLISECONDS);
                if (event != null) {
                    acceptForAggregation(event);
                }

                long now = System.currentTimeMillis();

                if (pending != null && now - pending.lastSeenMs >= config.mergeWindowMs) {
                    flushPending();
                }

                if (now - lastFlush >= config.writerFlushIntervalMs) {
                    flushWriters();
                    lastFlush = now;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                CommandBlockLoggerMod.LOGGER.error("CommandBlockLogger async writer failed.", e);
            }
        }

        flushPending();
        flushWriters();
    }

    private void acceptForAggregation(CommandBlockExecutionEvent event) throws Exception {
        CommandBlockLoggerConfig cfg = config;

        if (!cfg.mergeRepeatedLogs) {
            writeEvent(event, 1);
            return;
        }

        long now = System.currentTimeMillis();

        if (pending == null) {
            pending = new PendingLog(event, now);
            return;
        }

        if (sameLog(pending.event, event)) {
            pending.count++;
            pending.lastSeenMs = now;

            if (now - pending.firstSeenMs >= 3000) {
                flushPending();
            }

            return;
        }

        flushPending();
        pending = new PendingLog(event, now);
    }

    private boolean sameLog(CommandBlockExecutionEvent a, CommandBlockExecutionEvent b) {
        return Objects.equals(a.world(), b.world())
                && Objects.equals(a.pos(), b.pos())
                && Objects.equals(a.commandBlockType(), b.commandBlockType())
                && Objects.equals(a.command(), b.command())
                && Objects.equals(a.status(), b.status())
                && Objects.equals(normalizeMessage(a.output()), normalizeMessage(b.output()))
                && Objects.equals(normalizeMessage(a.error()), normalizeMessage(b.error()));
    }

    private String normalizeMessage(String value) {
        if (value == null) return "";
        return value.replaceFirst("^\\[\\d{2}:\\d{2}:\\d{2}\\]\\s*", "").trim();
    }

    private void flushPending() {
        if (pending == null) return;

        try {
            writeEvent(pending.event, pending.count);
        } catch (Exception e) {
            CommandBlockLoggerMod.LOGGER.error("Failed to flush aggregated command block log.", e);
        } finally {
            pending = null;
        }
    }

    private void writeEvent(CommandBlockExecutionEvent event, int count) throws Exception {
        CommandBlockLoggerConfig cfg = config;

        String line;
        if (cfg.writeJsonLogs) {
            line = CommandBlockLogFormatter.json(event, count);
        } else if (cfg.writeCompactLogs) {
            line = CommandBlockLogFormatter.compact(event, count);
        } else {
            line = CommandBlockLogFormatter.human(event, count);
        }

        if (cfg.writeHumanReadableLogs && cfg.writeJsonLogs) {
            mainWriter.writeLine(CommandBlockLogFormatter.human(event, count));
            mainWriter.writeLine(CommandBlockLogFormatter.json(event, count));
        } else {
            mainWriter.writeLine(line);
        }

        if (event.failed()) {
            errorWriter.writeLine(CommandBlockLogFormatter.human(event, count));
            if (cfg.writeJsonLogs) errorWriter.writeLine(CommandBlockLogFormatter.json(event, count));
        }

        if (cfg.writeConsoleLogs) {
            String consoleLine = cfg.compressedConsoleLogs
                    ? CommandBlockLogFormatter.consoleSummary(event, count)
                    : "\n" + CommandBlockLogFormatter.human(event, count);

            if (event.failed()) {
                LoggerFactory.getLogger(CommandBlockLoggerMod.MOD_ID).warn("{}", consoleLine);
            } else {
                LoggerFactory.getLogger(CommandBlockLoggerMod.MOD_ID).info("{}", consoleLine);
            }
        }
    }

    private void flushWriters() {
        try {
            if (mainWriter != null) mainWriter.flush();
            if (errorWriter != null) errorWriter.flush();
        } catch (Exception e) {
            CommandBlockLoggerMod.LOGGER.warn("Failed to flush command block logs.", e);
        }
    }

    private void closeWriters() {
        try {
            if (mainWriter != null) mainWriter.close();
            if (errorWriter != null) errorWriter.close();
        } catch (Exception e) {
            CommandBlockLoggerMod.LOGGER.warn("Failed to close command block log writers.", e);
        }
    }

    private static final class PendingLog {
        final CommandBlockExecutionEvent event;
        int count = 1;
        final long firstSeenMs;
        long lastSeenMs;

        PendingLog(CommandBlockExecutionEvent event, long now) {
            this.event = event;
            this.firstSeenMs = now;
            this.lastSeenMs = now;
        }
    }
}
