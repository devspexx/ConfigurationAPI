[![Java CI with Maven](https://github.com/devspexx/ConfigurationAPI/actions/workflows/maven.yml/badge.svg)](https://github.com/devspexx/ConfigurationAPI/actions/workflows/maven.yml)
[![Code Factor](https://img.shields.io/codefactor/grade/github/devspexx/ConfigurationAPI?label=Code%20Factor&style=flat)](https://www.codefactor.io/repository/github/devspexx/ConfigurationAPI)
![Latest Release](https://img.shields.io/github/v/release/devspexx/ConfigurationAPI?label=Latest%20Release&style=flat)

## 1. Overview

ConfigurationAPI is a Paper-based plugin that handles configuration files for you.

It automatically watches, reloads, and synchronizes configs when they
change — no `/reload` commands needed. Designed to be fast, safe,
and developer-friendly.

> This plugin is Paper-only and will not work on Spigot.

### 1.1 Features

- No need for `/reload` commands — configs update instantly when files change.
- Efficiently monitors files at the system level with a single watcher thread.
- Reloads only when actual content changes — avoids unnecessary operations.
- Prevents duplicate reloads caused by rapid file system events.
- React to changes with built-in events:
    - `ConfigReloadedEvent`
    - `ConfigDeletedEvent`
    - `ConfigRegisteredEvent`
- Access everything through `ConfigurationProvider` — no boilerplate.
- Create, register, and manage configs at runtime — not just on startup.
- Normalized paths ensure reliable lookups across environments.
- Designed for concurrent environments with minimal overhead.
- Direct access to Bukkit’s `YamlConfiguration` for full control when needed.

### 1.2 Additional Features

- Load configs directly from your plugin JAR:
    - `registerFromJar(File target, String resourcePath, JavaPlugin plugin)`

- Create custom config files:
    - `register(File file)`

- Register configs with default values (only applied if missing):
    - `registerWithDefaults(File file, Map<String, Object> defaults)`

- Retrieve configs safely:
    - `get(File file)`
    - `getByPath(String path)` 
    - `isRegistered(File file)`


## 2. Installation

Add ConfigurationAPI to your project using one of the following methods:

### 2.1 JitPack (Recommended) 
Add this to your `pom.xml`:
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.devspexx</groupId>
    <artifactId>ConfigurationAPI</artifactId>
    <version>v1.3.2</version>
</dependency>
```

### 2.2 Manual JAR build
If you prefer building the project yourself:
```bash
git clone https://github.com/devspexx/ConfigurationAPI.git
cd ConfigurationAPI
mvn clean install
````
This installs the artifact into your local Maven repository. 
You can then add it as a dependency using the version defined in the project's `pom.xml` (e.g. `1.3.2`).

#### 2.2.1 Runtime
ConfigurationAPI must be available at runtime. You can choose one of the following:
- Shade it into your plugin (<b>recommended</b> for standalone plugins)
- Install it as a plugin on the server and depend on it

> 💡 If you do not shade it, make sure the ConfigurationAPI plugin is present on the server.

### 3. API Usage
How do I integrate ConfigurationAPI into my plugin?

#### 3.1 Initialize the API
You should initialize the API inside your plugin's `onEnable()` method.

```java
import dev.spexx.configurationAPI.api.ConfigurationProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class MyPlugin extends JavaPlugin {

    private final ConfigurationProvider configurationProvider;

    public @NotNull ConfigManager getConfigurationProvider() {
        return configurationProvider.api();
    }

    @Override
    public void onEnable() {
        setupConfigurationProvider();
    }

    public void setupConfigurationProvider() {

        // Initialize ConfigurationProvider for this plugin.
        // Passing "this" binds the provider to this plugin's lifecycle.
        configurationProvider = new ConfigurationProvider(this);
        configurationProvider.api().startFileWatcher();
    }
}
```

> 💡 You can register configs even after the file watcher has started. 
> <b>Newly registered configs will be picked up and tracked automatically</b>. 

#### 3.2 Working with Configs
Once the API is initialized, you can create, access, and modify configuration files.

##### 3.2.1 Register a custom config

```java
@NotNull YamlConfig dataConfig = getConfigurationProvider().register(
        new File(getDataFolder(), "data.yml")
);
```
- Creates the file if it does not exist
- Loads and registers it
- Automatically tracked by the watcher

#### 3.2.2 Register with default values
```java
Map<String, Object> defaults = Map.of(
    "settings.enabled", true,
    "motd.line1", "Hello world"
);

getConfigurationProvider().registerWithDefaults(
    new File(getDataFolder(), "config.yml"),
    defaults
);
```
- Only missing values are added
- Existing values are never overwritten

#### 3.2.3 Access a config

```java
import dev.spexx.configurationAPI.api.config.yaml.YamlConfig;

YamlConfig config = getConfigurationProvider().get(
        new File(getDataFolder(), "config.yml")
);

String value = config.get().getString("path.to.value");
```
> 💡 Prefer get(File) over getByPath(String) for better reliability.

#### 3.2.4 Modify and save
```java
config.get().set("path.to.value", "newValue");

// Always save after modifying
config.save();
```

#### 3.2.5 React to config changes

```java
import dev.spexx.configurationAPI.api.event.ConfigReloadedEvent;
import org.jetbrains.annotations.NotNull;

@EventHandler
public void onReload(@NotNull ConfigReloadedEvent event) {
    getLogger().info("Reloaded: " + event.getConfigName());
}
```
- Fired only when file content actually changes
- Runs on the main thread

##### 3.2.6 Recommended pattern (cache values)

```java
import dev.spexx.configurationAPI.api.event.ConfigReloadedEvent;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class MyListener implements Listener {

    private String cachedValue;

    public MyListener(MyPlugin plugin) {
        update(plugin);
    }

    public void update(@NotNull MyPlugin plugin) {
        YamlConfig config = plugin.getConfigurationProvider().get(
                new File(plugin.getDataFolder(), "config.yml")
        );

        cachedValue = config.get().getString("path.to.value");
    }

    @EventHandler
    public void onReload(ConfigReloadedEvent event) {
        update(plugin);
    }
}
```
- Avoid reading config on every event
- Cache values and refresh on reload

### 4 Events
ConfigurationAPI provides built-in events that allow you to react to configuration changes in real-time.
> 💡 All events are fired on the **main server thread**.

#### 4.1 ConfigReloadedEvent
Fired when a configuration file is modified and successfully reloaded.

| Method             | Description                                  |
|--------------------|----------------------------------------------|
| `getConfigName()`  | Name of the config file (e.g. `config.yml`)  |
| `getNewConfig()`   | Updated `FileConfiguration` instance         |
| `getOldChecksum()` | Checksum before reload (may be `null`)       |
| `getNewChecksum()` | Checksum after reload (may be `null`)        |

- Triggered only when file content actually changes (checksum-based).


#### 4.2 ConfigDeletedEvent
Fired when a tracked configuration file is deleted.

| Method             | Description                                  |
|--------------------|----------------------------------------------|
| `getConfigName()`  | Name of the deleted config file              |
| `getPath()`        | Absolute path of the deleted file            |

- Fired when the file is removed from disk
- Config is automatically untracked

#### 4.3 ConfigRegisteredEvent
Fired when a configuration is registered and loaded.

| Method            | Description                                  |
|-------------------|----------------------------------------------|
| `getConfigName()` | Name of the config file                      |
| `getConfig()`     | Loaded `FileConfiguration` instance          |
| `getChecksum()`   | Initial checksum (may be `null`)             |

- Fired after registration and initial load  
- Useful for initialization logic

#### 4.4 Example usage of events

```java
import dev.spexx.configurationAPI.api.event.ConfigDeletedEvent;
import dev.spexx.configurationAPI.api.event.ConfigRegisteredEvent;
import dev.spexx.configurationAPI.api.event.ConfigReloadedEvent;
import org.jetbrains.annotations.NotNull;

public class ConfigListener implements Listener {

    @EventHandler
    public void onReload(@NotNull ConfigReloadedEvent event) {
        Bukkit.getLogger().info("Reloaded: " + event.getConfigName());
    }

    @EventHandler
    public void onDelete(@NotNull ConfigDeletedEvent event) {
        Bukkit.getLogger().warning("Deleted: " + event.getConfigName());
    }

    @EventHandler
    public void onRegister(@NotNull ConfigRegisteredEvent event) {
        Bukkit.getLogger().info("Registered: " + event.getConfigName());
    }
}
```
- Events are synchronous (main thread)
- Reload events are checksum-based (no duplicate triggers)
- Delete events automatically stop tracking the file
- Register events fire once per config registration

### 5. Contributing

Pull requests are welcome!

If you find a bug or have a feature request, feel free to open an issue.

## 6. Final Notes

ConfigurationAPI is designed to remove boilerplate and let you focus on building features.

If you find it useful, consider ⭐ starring the repository. I would really appreciate it!