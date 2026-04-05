## ConfigurationAPI

A lightweight, high-performance YAML configuration API for Paper/Spigot plugins, featuring automatic file watching, atomic reloads, and a clean event-driven design.

### Features
- Automatic config reload on file changes (WatchService-based)
- Single global watcher (no per-file overhead)
- Atomic, thread-safe configuration replacement
- SHA-256 checksum-based change detection
- Debounce protection against duplicate reloads
- Synchronous reload events (ConfigReloadedEvent)
- Clean, minimal, and predictable API
- Designed for high-performance plugin environments

### Why ConfigurationAPI?
- Eliminates manual reload logic
- Guarantees consistent configuration state
- Prevents unsafe concurrent access
- Centralized architecture (single source of truth)
- Minimal overhead and easy integration

### Installation
#### Maven
```xml
<dependency>
    <groupId>dev.spexx</groupId>
    <artifactId>ConfigurationAPI</artifactId>
    <version>1.2.0</version>
</dependency>
```

### Usage

#### 1. Create ConfigManager
```java
ConfigManager configManager = new ConfigManager(plugin);
```
#### 2. Load a configuration file
```java
File file = new File(plugin.getDataFolder(), "config.yml");

YamlConfig config = configManager.getOrLoad(file);
```
#### 2.1 Load default resource (e.g. config.yml)
```java
YamlConfig config = configManager.getOrLoadResource("config.yml");

# Copies file from JAR if it does not exist
# Automatically registers it with the watcher

```
#### 3. Access configuration
```java
YamlConfig config = configManager.get(file);
String value = config.config().getString("path.to.value");

# Always returns the latest snapshot
# Safe for concurrent access
```

#### 4. Automatic Reloads
Configuration files are automatically monitored.

When a file changes:
- The file is reloaded
- A new `YamlConfig` instance is created
- The old instance is atomically replaced
- A `ConfigReloadedEvent` is fired


#### 5. Listening to Reload Events
```java
@EventHandler
public void onReload(ConfigReloadedEvent event) {

    plugin.getLogger().info(() ->
        "[Config] Reloaded: " +
        event.getNewConfig().file().getName() +
        " (" + event.getReloadTimeMs() + "ms)"
    );

    plugin.getLogger().info(() ->
        "Checksum: " +
        event.getOldChecksum() +
        " -> " +
        event.getNewChecksum()
    );
}
```

#### 6. Event Data
`ConfigReloadedEvent` provides:
- `getOldConfig()` → previous snapshot
- `getNewConfig()` → updated snapshot
- `getOldChecksum()` → previous file hash
- `getNewChecksum()` → new file hash
- `getReloadTimeMs()` → reload duration

#### 7. Architecture Overview
```yml
ConfigManager (API layer)
 ├── delegates to GlobalConfigWatcher
 └── provides access to configs

GlobalConfigWatcher (core)
 ├── owns config state (Map<Path, YamlConfig>)
 ├── owns checksums
 ├── handles file watching (WatchService)
 ├── performs reloads
 └── fires events

YamlConfig
 └── immutable snapshot of configuration
```

#### 8. Threading Model
- Watcher runs on a dedicated async thread
- File changes are processed asynchronously
- Events are dispatched synchronously on the main thread
- `YamlConfig` instances are immutable

#### 9. Best Practices
- Always access configs via `ConfigManager`
- Do not store `YamlConfig` instances long-term
- Use `ConfigReloadedEvent` for reactive updates
- Avoid modifying `FileConfiguration` directly

#### 10. Guarantees
- No duplicated configuration state
- Atomic updates (no partial reads)
- No reload if file content is unchanged
- Safe under concurrent access
