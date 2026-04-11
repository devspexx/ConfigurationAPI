package dev.spexx.configurationAPI.loader;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import org.jetbrains.annotations.NotNull;

/**
 * Classpath loader for the ConfigurationProvider plugin.
 *
 * <p>This class is responsible for modifying the plugin's classpath
 * during the loading phase.</p>
 *
 * <p>It can be used to add external libraries or dependencies to the
 * plugin classloader before the plugin is initialized.</p>
 *
 * <p>Currently, no additional libraries are injected.</p>
 *
 * @since 1.3.0
 */
public class ConfigurationLoader implements PluginLoader {

    /**
     * Creates a new ConfigurationLoader instance.
     *
     * @since 1.3.0
     */
    public ConfigurationLoader() {
    }

    /**
     * Called when the plugin classloader is being constructed.
     *
     * @param builder the classpath builder used to modify the plugin classpath
     */
    @Override
    public void classloader(@NotNull PluginClasspathBuilder builder) {
        // no-op (reserved for future use)
    }
}