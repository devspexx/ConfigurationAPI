package dev.spexx.configurationAPI;

import dev.spexx.configurationAPI.manager.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Entry point for the ConfigurationAPI plugin.
 *
 * <p>This class acts as a minimal bootstrap when the API is deployed as a Bukkit plugin.</p>
 *
 * <p>In most cases, consumers should interact directly with API components such as
 * {@link ConfigManager} rather than relying on plugin lifecycle behavior.</p>
 *
 * <h2>Lifecycle</h2>
 * <ul>
 *     <li>{@link #onLoad()} &mdash; called when the plugin is loaded</li>
 *     <li>{@link #onEnable()} &mdash; called when the plugin is enabled</li>
 *     <li>{@link #onDisable()} &mdash; called during shutdown</li>
 * </ul>
 *
 * <p>This implementation does not perform any automatic configuration setup.
 * It exists primarily to allow the API to function as a standalone plugin if required.</p>
 *
 * @since 1.0.0
 */
public final class ConfigurationAPI extends JavaPlugin {

    /**
     * Constructs the ConfigurationAPI plugin instance.
     *
     * <p>No initialization logic is performed in the constructor.</p>
     *
     * @since 1.0.4
     */
    public ConfigurationAPI() {
    }

    /**
     * Called when the plugin is loaded.
     *
     * <p>This method is invoked before {@link #onEnable()} and can be used for
     * early initialization logic if required.</p>
     *
     * @since 1.1.0
     */
    @Override
    public void onLoad() {
        // No-op
    }

    /**
     * Called when the plugin is enabled.
     *
     * <p>This implementation does not automatically initialize any components.
     * Consumers are expected to create and manage {@link ConfigManager} instances
     * within their own plugins.</p>
     *
     * @since 1.1.0
     */
    @Override
    public void onEnable() {
        // No-op
    }

    /**
     * Called when the plugin is disabled.
     *
     * <p>No shutdown logic is required at this level. Individual components
     * such as {@link ConfigManager} should be properly shut down by their
     * owning plugins.</p>
     *
     * @since 1.1.0
     */
    @Override
    public void onDisable() {
        // No-op
    }
}