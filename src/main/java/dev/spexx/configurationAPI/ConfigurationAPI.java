package dev.spexx.configurationAPI;

import dev.spexx.configurationAPI.config.YamlConfig;
import dev.spexx.configurationAPI.difference.ConfigLineDifference;
import dev.spexx.configurationAPI.events.ConfigReloadedEvent;
import dev.spexx.configurationAPI.manager.ConfigManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Entry point for the ConfigurationAPI plugin.
 *
 * <p>This class is responsible for initializing core components and
 * registering listeners required for configuration monitoring.</p>
 *
 * @apiNote
 * This plugin primarily serves as a runtime host for the configuration API.
 *
 * @implSpec
 * Lifecycle methods are invoked by the Bukkit framework.
 *
 * @since 1.0
 */
public final class ConfigurationAPI extends JavaPlugin implements Listener {

    private ConfigurationAPI() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onEnable() {

        ConfigManager configManager = new ConfigManager(this);

        // load test config
        YamlConfig config = configManager.getOrLoadResource("config.yml");

        // register event listener
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() { }

    /**
     * Handles configuration reload events.
     *
     * @param event the reload event
     *
     * @since 1.0
     */
    @EventHandler
    public void onReload(@NotNull ConfigReloadedEvent event) {

        getLogger().info("Config reloaded: " +
                event.getConfig().file().getName());

        getLogger().info("New value: " +
                event.getConfig().config().getInt("value"));
    }
}