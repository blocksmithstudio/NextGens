package com.muhammaddaffa.nextgens.hologram;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.List;

public interface HologramProvider {

    /**
     * The internal id used to match against the config value (holograms.type).
     */
    String id();

    /**
     * The Bukkit plugin name this provider hooks into.
     * Used to detect whether the provider can actually be used.
     */
    String pluginName();

    /**
     * Whether the backing plugin is installed and enabled on this server.
     * Only available providers are eligible for the fallback.
     */
    default boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled(pluginName());
    }

    void spawn(String name, Location location, List<String> lines);

    void destroy(String name, Location location);
}