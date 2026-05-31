package com.example.commandblocklogger.util;

public final class OutputCapture {
    private final StringBuilder output = new StringBuilder(256);

    public void append(String text) {
        if (text == null || text.isBlank()) return;
        if (!output.isEmpty()) output.append("\n");
        output.append(text);
    }

    public String output() {
        return output.toString();
    }
}
