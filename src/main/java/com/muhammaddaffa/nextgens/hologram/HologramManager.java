package com.muhammaddaffa.nextgens.hologram;

import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.hologram.imp.DecentHologramsProvider;
import com.muhammaddaffa.nextgens.hologram.imp.FancyHologramProvider;
import com.muhammaddaffa.nextgens.hologram.imp.HolographicDisplaysProvider;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class HologramManager {

    private HologramProvider provider;

    private static final Map<String, HologramProvider> HOLOGRAM_PROVIDER_LIST = new ConcurrentHashMap<>();

    /**
     * The providers shipped with the plugin. They are only instantiated once we know their backing
     * plugin is installed: every provider compiles against classes that only exist when that plugin
     * is present, so touching the class without it would throw a {@link NoClassDefFoundError}.
     * The factories are lambdas on purpose: a constructor reference would resolve the provider class
     * right here in the static initializer, which is exactly what we are avoiding.
     */
    private static final List<BuiltinProvider> BUILTIN_PROVIDERS = List.of(
            new BuiltinProvider("DecentHolograms", () -> new DecentHologramsProvider()),
            new BuiltinProvider("FancyHolograms", () -> new FancyHologramProvider()),
            new BuiltinProvider("HolographicDisplays", () -> new HolographicDisplaysProvider())
    );

    private record BuiltinProvider(String pluginName, Supplier<HologramProvider> factory) {
    }

    public static void register(HologramProvider hologramProvider) {
        if (hologramProvider == null) {
            Logger.info("Invalid Holograms Provider! Shutting down the plugin...");
            Bukkit.getPluginManager().disablePlugins();
            return;
        }

        String id = hologramProvider.id();
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    hologramProvider.getClass().getName() + " returned a null or blank id()"
            );
        }

        HologramProvider previous = HOLOGRAM_PROVIDER_LIST.putIfAbsent(id.toLowerCase(Locale.ROOT), hologramProvider);
        if (previous != null) {
            throw new IllegalStateException("A hologram with id " + id + " is already registered by " + previous.getClass().getName());
        }
    }

    public static boolean unregister(String id) {
        if (id == null || id.isBlank()) return false;

        return HOLOGRAM_PROVIDER_LIST.remove(id.toLowerCase(Locale.ROOT)) != null;
    }

    /**
     * Registers every built-in provider whose backing plugin is actually installed and enabled.
     * A provider that fails to load (e.g. an incompatible plugin version) is skipped instead of
     * taking the whole plugin down with it.
     */
    private static void registerBuiltinProviders() {
        for (BuiltinProvider builtin : BUILTIN_PROVIDERS) {
            if (!Bukkit.getPluginManager().isPluginEnabled(builtin.pluginName()) || isRegistered(builtin.pluginName())) {
                continue;
            }
            try {
                register(builtin.factory().get());
            } catch (Throwable ex) {
                Logger.warning("Failed to hook into " + builtin.pluginName() + " (" +
                        ex.getClass().getSimpleName() + ": " + ex.getMessage() + "), skipping it.");
            }
        }
    }

    private static boolean isRegistered(String pluginName) {
        for (HologramProvider registered : HOLOGRAM_PROVIDER_LIST.values()) {
            if (registered.pluginName().equalsIgnoreCase(pluginName)) {
                return true;
            }
        }
        return false;
    }

    public void load() {
        registerBuiltinProviders();

        FileConfiguration config = NextGens.DEFAULT_CONFIG.getConfig();
        String configId = config.getString("holograms.type");

        // 1. Try to honour the configured provider first.
        if (configId != null && !configId.isBlank()) {
            HologramProvider configured = findByConfigId(configId);
            if (configured != null && configured.isAvailable()) {
                this.provider = configured;
                Logger.info("Successfully hooked into hologram provider: " + configured.id());
                return;
            }
            Logger.info("Configured hologram provider '" + configId + "' is not available, falling back to any existing hologram...");
        } else {
            Logger.info("No hologram type configured, falling back to any existing hologram...");
        }

        // 2. Fallback: use the first registered provider whose plugin is installed.
        for (HologramProvider candidate : HOLOGRAM_PROVIDER_LIST.values()) {
            if (candidate.isAvailable()) {
                this.provider = candidate;
                Logger.info("Falling back to hologram provider: " + candidate.id());
                return;
            }
        }

        // 3. Nothing usable was found; holograms stay disabled.
        this.provider = null;
        Logger.warning("No supported hologram plugin found! Generator holograms will be disabled.");
    }

    /**
     * Finds a provider whose id() or backing plugin name matches the config value.
     * This lets the config accept either spelling (e.g. "FancyHologram" or "FancyHolograms").
     */
    private HologramProvider findByConfigId(String configId) {
        // fast path: direct match against the registered id
        HologramProvider byId = HOLOGRAM_PROVIDER_LIST.get(configId.toLowerCase(Locale.ROOT));
        if (byId != null) {
            return byId;
        }
        // fallback: match against the backing plugin name
        for (HologramProvider candidate : HOLOGRAM_PROVIDER_LIST.values()) {
            if (candidate.pluginName().equalsIgnoreCase(configId)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * The active hologram provider, or null if no supported plugin is installed.
     */
    public HologramProvider getProvider() {
        return provider;
    }

    /**
     * Whether a usable hologram provider is currently active.
     */
    public boolean isEnabled() {
        return provider != null;
    }
}
