package dev.spexx.configurationAPI;

import dev.spexx.configurationAPI.configuration.yaml.YamlConfig;
import dev.spexx.configurationAPI.manager.ConfigManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class ConfigurationAPI extends JavaPlugin {

    @Override
    public void onEnable() {

        ConfigManager manager = new ConfigManager();

        File configFile = new File(getDataFolder(), "config.yml");
        YamlConfig config = manager.registerWithDefaults(
                configFile,
                "config.yml",
                this
        );

        manager.start();
        YamlConfiguration yaml = config.get();
        getLogger().info(yaml.getString("test", "default value"));
    }
}