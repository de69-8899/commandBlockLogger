package com.example.commandblocklogger.logging;

import com.example.commandblocklogger.CommandBlockLoggerMod;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.stream.Stream;

public final class DailyRotatingFileWriter implements Closeable {
    private final Path logDirectory;
    private final String baseName;
    private final int retentionDays;

    private LocalDate currentDate;
    private BufferedWriter writer;

    public DailyRotatingFileWriter(Path logDirectory, String baseName, int retentionDays) {
        this.logDirectory = logDirectory;
        this.baseName = baseName;
        this.retentionDays = retentionDays;
    }

    public synchronized void writeLine(String line) throws IOException {
        rotateIfNeeded();
        writer.write(line);
        writer.newLine();
    }

    public synchronized void flush() throws IOException {
        if (writer != null) writer.flush();
    }

    private void rotateIfNeeded() throws IOException {
        LocalDate today = LocalDate.now();
        if (writer != null && today.equals(currentDate)) return;

        close();

        Files.createDirectories(logDirectory);
        currentDate = today;

        Path active = logDirectory.resolve(baseName);
        Path dated = logDirectory.resolve(today + "-" + baseName);
        writer = Files.newBufferedWriter(active, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        writer.write("# mirrored daily file: " + dated.getFileName());
        writer.newLine();

        cleanupOldFiles();
    }

    private void cleanupOldFiles() {
        try (Stream<Path> files = Files.list(logDirectory)) {
            files.filter(p -> p.getFileName().toString().endsWith("-" + baseName))
                    .sorted(Comparator.comparing(Path::getFileName).reversed())
                    .skip(retentionDays)
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            CommandBlockLoggerMod.LOGGER.warn("Failed to delete old command block log {}", p, e);
                        }
                    });
        } catch (IOException e) {
            CommandBlockLoggerMod.LOGGER.warn("Failed to clean old command block logs.", e);
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if (writer != null) {
            writer.flush();
            writer.close();
            writer = null;
        }
    }
}
