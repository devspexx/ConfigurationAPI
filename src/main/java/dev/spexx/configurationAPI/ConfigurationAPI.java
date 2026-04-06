package dev.spexx.configurationAPI;

import dev.spexx.configurationAPI.configuration.yaml.YamlConfig;
import dev.spexx.configurationAPI.manager.ConfigManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Main plugin entry point for the ConfigurationAPI.
 *
 * <p>Initializes and demonstrates usage of the configuration system.</p>
 *
 * @since 1.0.0
 */
public final class ConfigurationAPI extends JavaPlugin {

    /**
     * Creates a new plugin instance.
     *
     * @since 1.0.0
     */
    public ConfigurationAPI() {
    }

    @Override
    public void onEnable() {

        ConfigManager manager = new ConfigManager(this);

        File configFile = new File(new File(getDataFolder(), "sampleJar"), "config.yml");
        manager.registerFromJar(
                configFile,
                "config.yml",
                this
        );

        File customFile = new File(new File(getDataFolder(), "sampleCustom"), "data.yml");
        YamlConfig customConfig = manager.register(customFile);
        YamlConfiguration customYaml = customConfig.get();
        customYaml.set("hello", "world");
        customConfig.save();

        manager.start();
    }
}