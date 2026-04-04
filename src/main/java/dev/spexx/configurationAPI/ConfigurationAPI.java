package dev.spexx.configurationAPI;

import dev.spexx.configurationAPI.config.YamlConfig;
import dev.spexx.configurationAPI.difference.ConfigLineDiff;
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

    /**
     * Constructs the plugin instance.
     *
     * @since 1.0
     */
    public ConfigurationAPI() { }

    private ConfigManager configManager;

    @Override
    public void onEnable() {

        this.configManager = new ConfigManager(this);

        // load test config
        YamlConfig config = configManager.getOrLoadResource("config.yml");

        getLogger().info("Loaded config value: " +
                config.config().getInt("value"));

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

        for (ConfigLineDiff diff : event.getDiffs()) {

            int delta = diff.getCharDelta();
            String sign = delta > 0 ? "+" : "";

            String type =
                    diff.getOldLine().isEmpty() ? "ADDED" :
                            diff.getNewLine().isEmpty() ? "REMOVED" :
                                    diff.isOnlyWhitespaceChange() ? "WHITESPACE" :
                                            "MODIFIED";

            getLogger().info(() ->
                    "[ConfigWatcher] file=" + event.getConfig().file().getName()
                            + " line=" + diff.getLineNumber()
                            + " type=" + type
                            + " delta=" + sign + delta
            );
        }

        getLogger().info("Config reloaded: " +
                event.getConfig().file().getName());

        getLogger().info("New value: " +
                event.getConfig().config().getInt("value"));
    }
}