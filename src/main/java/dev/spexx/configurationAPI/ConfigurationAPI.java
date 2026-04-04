package dev.spexx.configurationAPI;

/**
 * Entry point for the ConfigurationAPI plugin.
 *
 * <p>This class serves as a minimal bootstrap for the API when used as a plugin.
 * In most cases, consumers will interact with the API components directly
 * (e.g., {@link dev.spexx.configurationAPI.manager.ConfigManager})
 * rather than relying on plugin lifecycle behavior.</p>
 *
 * @since 1.0.0
 */
public final class ConfigurationAPI extends org.bukkit.plugin.java.JavaPlugin {

    /**
     * Default constructor.
     *
     * <p>Constructs the ConfigurationAPI plugin instance.</p>
     *
     * @since 1.0.4
     */
    public ConfigurationAPI() {
    }
}