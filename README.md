# ConfigurationAPI

A lightweight, high-performance YAML configuration API with automatic file watching, diff tracking, and atomic reloads for Paper/Spigot plugins.

---

## Features

- Automatic config reload on file changes (WatchService-based)
- Atomic, thread-safe configuration swapping
- Line-level diff tracking (changed / added / removed)
- Change summary abstraction (`ConfigChangeSummary`)
- Robust against partial writes and filesystem edge cases
- Optimized (no regex, O(n) diff, minimal allocations)
- Clean integration with Bukkit/Paper event system

---

## Installation

### Maven

```xml
<dependency>
    <groupId>dev.spexx</groupId>
    <artifactId>ConfigurationAPI</artifactId>
    <version>1.0.4</version>
</dependency>
```

### 1. Create ConfigManager
```java
ConfigManager configManager = new ConfigManager(plugin);
```

### 2. Load a configuration file
```java
File file = new File(plugin.getDataFolder(), "config.yml");

YamlConfig config = configManager.getOrLoad(file);
```

### 2.1 Load default resource (eg.: config.yml)
```java
YamlConfig config = configManager.getOrLoadResource("config.yml");

# Copies from JAR if missing
# Automatically registers watcher
```

## 3. Automatic Reloads
Configuration files are automatically monitored.

When a file changes:
- It is reloaded atomically
- A `ConfigReloadedEvent` is fired
- Diff + summary are provided

### 4. Listening to Reload Events
```java
@EventHandler
public void onReload(ConfigReloadedEvent event) {

    ConfigChangeSummary summary = event.getSummary();

    plugin.getLogger().info(() ->
        "[Config] Reloaded: " + summary
    );

    for (ConfigLineDifference diff : event.getDiffs()) {
        plugin.getLogger().info(() ->
            "[ConfigWatcher] file=" +
            event.getConfig().file().getName() +
            " line=" + diff.getLineNumber() +
            " delta=" + diff.getCharDelta()
        );
    }
}
```

### 5. Change Summary
```java
ConfigChangeSummary summary = event.getSummary();

summary.changedLines();
summary.addedLines();
summary.removedLines();
summary.getTotalChanges();
summary.hasChanges();
```

### 6. Line Differences
```java
for (ConfigLineDifference diff : event.getDiffs()) {

    int line = diff.getLineNumber();
    String oldLine = diff.getOldLine();
    String newLine = diff.getNewLine();
    int delta = diff.getCharDelta();

    if (diff.isOnlyWhitespaceChange()) {
        // ignore formatting changes if needed
    }
}
```

### 7. Architecture Overview
```yml
ConfigManager
 ├── YamlConfig (immutable snapshot)
 ├── YamlConfigWatcher (file watcher)
 │     └── Diff engine (O(n))
 └── Events (ConfigReloadedEvent)
```

### 8. Thread Safety
- `YamlConfig` is immutable
- Reloads are atomic
- Watcher runs async, events fire sync (main thread)
- Safe for concurrent reads

### 9. Best Practices
- Always fetch config via ConfigManager
- Avoid storing long-lived YamlConfig references
- Use events for reactive updates
