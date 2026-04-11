package dev.spexx.configurationAPI.api;

import dev.spexx.configurationAPI.api.manager.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Facade for accessing configuration API components.
 *
 * @since 1.3.2
 */
public class ConfigurationProvider {

    /**
     * The internal configuration manager instance.
     *
     * @since 1.3.2
     */
    private final @NotNull ConfigManager configManager;

    /**
     * Creates a new {@link dev.spexx.configurationAPI.ConfigurationAPI} instance.
     *
     * <p>The provided {@link JavaPlugin} is used internally for scheduling tasks
     * and interacting with the Bukkit API.</p>
     *
     * @param javaPlugin the owning plugin instance
     *
     * @since 1.3.2
     */
    public ConfigurationProvider(@NotNull JavaPlugin javaPlugin) {
        this.configManager = new ConfigManager(javaPlugin);
    }

    /**
     * Returns the core {@link ConfigManager} API.
     *
     * <p>This provides access to configuration registration, retrieval,
     * and file watching functionality.</p>
     *
     * @return the {@link ConfigManager} instance
     *
     * @since 1.3.2
     */
    public @NotNull ConfigManager api() {
        return this.configManager;
    }
}
