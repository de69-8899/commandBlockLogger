package com.example.commandblocklogger.logging;

import com.example.commandblocklogger.events.CommandBlockExecutionEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CommandBlockLogFormatter {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private CommandBlockLogFormatter() {}

    public static String human(CommandBlockExecutionEvent e) {
        return human(e, 1);
    }

    public static String human(CommandBlockExecutionEvent e, int count) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("[CommandBlockLogger]\n\n");
        sb.append("[").append(e.time()).append("]\n");
        sb.append("WORLD: ").append(e.world()).append('\n');
        sb.append("POS: ")
                .append(e.pos().getX()).append(' ')
                .append(e.pos().getY()).append(' ')
                .append(e.pos().getZ()).append('\n');
        sb.append("TYPE: ").append(e.commandBlockType()).append('\n');
        sb.append("STATUS: ").append(e.status()).append('\n');
        sb.append("RESULT: ").append(e.result()).append('\n');
        if (count > 1) sb.append("COUNT: x").append(count).append('\n');
        sb.append("SOURCE: ").append(e.triggerSource()).append("\n\n");
        sb.append("COMMAND:\n").append(e.command()).append("\n\n");

        if (e.failed()) {
            sb.append("ERROR:\n").append(emptySafe(e.error())).append('\n');
        } else {
            sb.append("OUTPUT:\n").append(emptySafe(e.output())).append('\n');
        }

        return sb.toString();
    }

    public static String compact(CommandBlockExecutionEvent e) {
        return compact(e, 1);
    }

    public static String compact(CommandBlockExecutionEvent e, int count) {
        return "%s world=%s pos=%d,%d,%d type=%s status=%s result=%d count=x%d command=\"%s\" output=\"%s\""
                .formatted(
                        e.time(),
                        e.world(),
                        e.pos().getX(),
                        e.pos().getY(),
                        e.pos().getZ(),
                        e.commandBlockType(),
                        e.status(),
                        e.result(),
                        count,
                        escapeOneLine(e.command()),
                        escapeOneLine(e.failed() ? e.error() : e.output())
                );
    }

    public static String json(CommandBlockExecutionEvent e) {
        return json(e, 1);
    }

    public static String json(CommandBlockExecutionEvent e, int count) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("timestamp", e.timestamp().toString());
        map.put("time", e.time());
        map.put("world", e.world());
        map.put("x", e.pos().getX());
        map.put("y", e.pos().getY());
        map.put("z", e.pos().getZ());
        map.put("type", e.commandBlockType());
        map.put("command", e.command());
        map.put("status", e.status());
        map.put("result", e.result());
        map.put("count", count);
        map.put("output", e.output());
        map.put("error", e.error());
        map.put("triggerSource", e.triggerSource());
        return GSON.toJson(map);
    }

    private static String emptySafe(String value) {
        return value == null || value.isBlank() ? "<empty>" : value;
    }

    private static String escapeOneLine(String value) {
        return emptySafe(value).replace("\n", "\\n").replace("\r", "");
    }
}
