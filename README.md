![Build](https://github.com/devspexx/ConfigurationAPI/actions/workflows/maven.yml/badge.svg)
[![CodeFactor](https://www.codefactor.io/repository/github/devspexx/configurationapi/badge)](https://www.codefactor.io/repository/github/devspexx/configurationapi)

## ConfigurationAPI

This is a simple yet somewhat robust plugin I wrote in my free time. 
It allows developers to not worry about config changes and implementing /reload commands in their java plugins.
It is a Paper based API, well, a Paper based plugin. It will only work on Paper servers. Don't try to install it on spigot 
servers, it won't work. I might make a fork of it in the near future, to also support spigot.
<br><br>
This plugin automatically tracks any configs you configure it to track, and reloads them, when 
they were manually edited (without code), and you can also write to them by code, of course. So it works
both ways.

## Features

- Simple API
- File watching (WatchService)
- checksum validation (no unnecessary reloads)
- Debounce protection
- Single watcher (no per-file threads)
- A cool sync event! (`ConfigReloadEvent`)

## Installation
Currently, it's only available as a jar. I'll see what I 
can do in the near future about that.

```xml
<dependency>
    <groupId>dev.spexx</groupId>
    <artifactId>ConfigurationAPI</artifactId>
    <version>1.3.0</version> 
</dependency>
```

## Or use jitpack
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
    <version>v1.3.0</version>
</dependency>
```

### Usage
#### Full sample (with comments)

```java
import dev.spexx.configurationAPI.manager.ConfigManager;

// ConfigManager takes a plugin instance, should only
// be initialized once!
ConfigManager configManager = new ConfigManager(this);

// Sample 1 - registering files from the plugin jar itself
// Which means you should NOT use saveDefaultConfig() / saveConfig() 
// anywhere in your plugin.

// This is where you want the config from the plugin resources to be saved.
File myPluginConfigDestination = new File(getDataFolder(), "config.yml");

// 2nd parameter, "config.yml" is the path inside the jar. Should really leave that as it is, only 
// change your file name. 3rd parameter is your plugin instance - so we know from which plugin we're 
// pulling the config.
configManager.registerFromJar(myPluginConfigDestination, "config.yml", this);

// I recommend putting all of this in your onEnable, at the end don't forget to
// actually start the watch service. Note that you can also dynamically create files during
// runtime, you don't have to do everything in onEnable.
manager.start();

// Sample 2 - registering custom yaml files, except not from plugin jar
// This is fairly simple!
YamlConfig myCustomConfig = manager.register(
        new File(getDataFolder(), "data.yml")
);
 
// What you're doing here is creating a file, in your plugin folder - getDataFolder(),
// and you're naming it 'data.yml', that's about it.
// You can write to it:
myCustomConfig.get().set("hello", "world");

// Read from it (get() - returns the latest internally cached config,
// basically, the latest config that matches the file on your disk)
// with get(), you get raw YamlConfiguration access, so you're able to do
// anything. If you do change anything in the file don't forget to
// save it in the end: myCustomConfig.save(), api will handle the rest.
@Nullable String myValue = myCustomConfig.get().getString("something.here");

// ---
// If you find this confusing, there is javadoc at every method / public variable. So you won't be lost!
```

#### Listen for reload (If you need to)

```java
import org.jetbrains.annotations.NotNull;

@EventHandler
public void onReload(@NotNull ConfigReloadEvent event) {
    getLogger().info("Reloaded: " + event.getConfigName());
}
```

#### What this event provides
```
## ConfigReloadEvent

| Method             | Description                              |
|--------------------|------------------------------------------|
| `getConfigName()`  | Full name of the file (e.g. `config.yml`)|
| `getNewConfig()`   | Updated configuration (latest state)     |
| `getOldChecksum()` | Checksum before reload                   |
| `getNewChecksum()` | Checksum after reload                    |
```

#### Event behavior
When file changes:
- Change is detected internally in watcher
- Checksum is computed 
  - If checksum is different, reload event is fired, and new file is cached 
  - New file overrides the latest file (if any) in the cache
  - If checksum isn't different, nothing happens, as there were no changes in the file
- Event is fired on main thread

---

### Thank you for reading!
