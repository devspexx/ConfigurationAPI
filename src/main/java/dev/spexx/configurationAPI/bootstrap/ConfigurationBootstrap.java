package dev.spexx.configurationAPI.bootstrap;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import org.jetbrains.annotations.NotNull;

/**
 * Bootstrap entry point for the ConfigurationProvider plugin.
 *
 * <p>This class is invoked during the early plugin bootstrap phase,
 * before the plugin is fully loaded and enabled.</p>
 *
 * <p>It can be used to perform early initialization logic such as
 * preparing resources, validating environment state, or influencing
 * plugin loading behavior.</p>
 *
 * <p>Note that the Bukkit API is not fully available at this stage.</p>
 *
 * @since 1.3.0
 */
public class ConfigurationBootstrap implements PluginBootstrap {

    /**
     * Creates a new ConfigurationBootstrap instance.
     *
     * @since 1.3.0
     */
    public ConfigurationBootstrap() {
    }

    /**
     * Called during the bootstrap phase of plugin initialization.
     *
     * @param context the bootstrap context providing access to plugin metadata and logging
     */
    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        // no-op (reserved for future use)
    }
}