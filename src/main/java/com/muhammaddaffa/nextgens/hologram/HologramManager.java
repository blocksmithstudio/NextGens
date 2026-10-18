package com.muhammaddaffa.nextgens.hologram;

import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.hologram.imp.DecentHologramsProvider;
import com.muhammaddaffa.nextgens.hologram.imp.FancyHologramProvider;
import com.muhammaddaffa.nextgens.hologram.imp.HolographicDisplaysProvider;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

public class HologramManager {

    private HologramProvider provider;

    private static final Map<String, HologramProvider> HOLOGRAM_PROVIDER_LIST =
            Collections.synchronizedMap(new LinkedHashMap<>());

    /**
     * The providers shipped with the plugin. They are only instantiated once we know their backing
     * plugin is installed: every provider compiles against classes that only exist when that plugin
     * is present, so touching the class without it would throw a {@link NoClassDefFoundError}.
     * The factories are lambdas on purpose: a constructor reference would resolve the provider class
     * right here in the static initializer, which is exactly what we are avoiding.
     */
    private static final List<BuiltinProvider> BUILTIN_PROVIDERS = List.of(
            new BuiltinProvider("DecentHolograms", "DecentHolograms", () -> new DecentHologramsProvider()),
            new BuiltinProvider("FancyHologram", "FancyHolograms", () -> new FancyHologramProvider()),
            new BuiltinProvider("HolographicDisplays", "HolographicDisplays", () -> new HolographicDisplaysProvider())
    );

    private record BuiltinProvider(String id, String pluginName, Supplier<HologramProvider> factory) {
    }

    public static void register(HologramProvider hologramProvider) {
        if (hologramProvider == null) {
            Logger.warning("Ignoring a null hologram provider.");
            return;
        }

        String id = hologramProvider.id();
        if (id == null || id.isBlank()) {
            Logger.warning("Ignoring hologram provider " + hologramProvider.getClass().getName() + " with a null or blank id().");
            return;
        }

        HologramProvider previous = HOLOGRAM_PROVIDER_LIST.putIfAbsent(id.toLowerCase(Locale.ROOT), hologramProvider);
        if (previous != null) {
            Logger.warning("Hologram provider " + id + " is already registered; keeping the existing provider.");
        }
    }

    public static boolean unregister(String id) {
        if (id == null || id.isBlank()) return false;

        return HOLOGRAM_PROVIDER_LIST.remove(id.toLowerCase(Locale.ROOT)) != null;
    }

    private static HologramProvider loadBuiltinProvider(BuiltinProvider builtin) {
        if (!Bukkit.getPluginManager().isPluginEnabled(builtin.pluginName())) {
            return null;
        }
        try {
            HologramProvider candidate = findByConfigId(builtin.pluginName());
            if (candidate == null) {
                register(builtin.factory().get());
                candidate = findByConfigId(builtin.pluginName());
            }
            return candidate != null && isAvailable(candidate) ? candidate : null;
        } catch (LinkageError | RuntimeException ex) {
            Logger.warning("Failed to hook into " + builtin.pluginName() + " (" +
                    ex.getClass().getSimpleName() + ": " + ex.getMessage() + "), skipping it.");
            return null;
        }
    }

    private static boolean isAvailable(HologramProvider candidate) {
        try {
            return candidate.isAvailable();
        } catch (LinkageError | RuntimeException ex) {
            Logger.warning("Failed to check hologram provider " + candidate.id() + " (" +
                    ex.getClass().getSimpleName() + ": " + ex.getMessage() + "), skipping it.");
            return false;
        }
    }

    private static List<HologramProvider> registeredProviders() {
        synchronized (HOLOGRAM_PROVIDER_LIST) {
            return List.copyOf(HOLOGRAM_PROVIDER_LIST.values());
        }
    }

    public void load() {
        FileConfiguration config = NextGens.DEFAULT_CONFIG.getConfig();
        String configId = config.getString("holograms.type");

        // 1. Try to honour the configured provider first.
        if (configId != null && !configId.isBlank()) {
            HologramProvider configured = findByConfigId(configId);
            if (configured == null) {
                for (BuiltinProvider builtin : BUILTIN_PROVIDERS) {
                    if (builtin.id().equalsIgnoreCase(configId) || builtin.pluginName().equalsIgnoreCase(configId)) {
                        configured = loadBuiltinProvider(builtin);
                        break;
                    }
                }
            }
            if (configured != null && isAvailable(configured)) {
                this.provider = configured;
                Logger.info("Successfully hooked into hologram provider: " + configured.id());
                return;
            }
            Logger.info("Configured hologram provider '" + configId + "' is not available, falling back to any existing hologram...");
        } else {
            Logger.info("No hologram type configured, falling back to any existing hologram...");
        }

        // 2. Fallback: use the first registered provider whose plugin is installed.
        for (HologramProvider candidate : registeredProviders()) {
            if (isAvailable(candidate)) {
                this.provider = candidate;
                Logger.info("Falling back to hologram provider: " + candidate.id());
                return;
            }
        }

        for (BuiltinProvider builtin : BUILTIN_PROVIDERS) {
            if (builtin.id().equalsIgnoreCase(configId) || builtin.pluginName().equalsIgnoreCase(configId)) {
                continue;
            }
            HologramProvider candidate = loadBuiltinProvider(builtin);
            if (candidate != null) {
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
    private static HologramProvider findByConfigId(String configId) {
        // fast path: direct match against the registered id
        HologramProvider byId = HOLOGRAM_PROVIDER_LIST.get(configId.toLowerCase(Locale.ROOT));
        if (byId != null) {
            return byId;
        }
        // fallback: match against the backing plugin name
        for (HologramProvider candidate : registeredProviders()) {
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
