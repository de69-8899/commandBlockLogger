# CommandBlockLogger

Production-style Fabric mod for clean, structured, searchable command block logs.

## Target

- Minecraft Java Edition 1.21.11
- Fabric Loader 0.16+
- Java 21
- Dedicated servers and integrated singleplayer server
- Server-side logging mod

## Features

- Logs every command block execution.
- Captures timestamp, world, position, command block type, command, result, status, output, and trigger source.
- Writes to:
  - `logs/commandblocks.log`
  - `logs/commandblocks-error.log`
- Supports JSON, compact, and human-readable formats.
- Async queue + buffered writer to avoid blocking the server tick.
- Filters by dimension, exact coordinate, command pattern, failure-only, and successful-command ignoring.
- Runtime commands:
  - `/commandblocklogger reload`
  - `/commandblocklogger stats`
  - `/commandblocklogger enable`
  - `/commandblocklogger disable`

## Build

```bash
./gradlew build
```

Jar output:

```text
build/libs/commandblocklogger-1.0.0.jar
```

## Install

1. Install Fabric Loader for Minecraft 1.21.11.
2. Put Fabric API in the server `mods/` folder.
3. Put `commandblocklogger-1.0.0.jar` in `mods/`.
4. Start the server once.
5. Edit `config/commandblocklogger.json`.
6. Run `/commandblocklogger reload`.

## Example config

```json
{
  "enabled": true,
  "logFailuresOnly": false,
  "ignoreSuccessfulExecutions": false,
  "writeJsonLogs": true,
  "writeConsoleLogs": true,
  "writeHumanReadableLogs": true,
  "writeCompactLogs": false,
  "maxLogFiles": 30,
  "queueCapacity": 16384,
  "writerFlushIntervalMs": 1000,
  "includeOutput": true,
  "includeStackTraces": false,
  "allowedDimensions": [],
  "allowedCoordinates": [],
  "ignoreCommands": [
    "execute if",
    "execute unless"
  ]
}
```

## Example human output

```text
[CommandBlockLogger]

[12:41:22]
WORLD: minecraft:overworld
POS: 123 64 -45
TYPE: CHAIN
STATUS: SUCCESS
RESULT: 1
SOURCE: command_block

COMMAND:
execute as @a run say Hello

OUTPUT:
Hello
```

## Design notes

- The mixin layer is tiny and isolated because Minecraft internals change often.
- The event model is plain Java records, so NeoForge porting can reuse config, formatting, filtering, stats, and writers.
- The server tick path only builds a small event and queues it. File IO runs on a daemon writer thread.
- Failed commands are duplicated to `commandblocks-error.log` for quick debugging.
- JSON logs are newline-delimited JSON, making them easy to grep, ingest into Loki/ELK, or parse with scripts.
