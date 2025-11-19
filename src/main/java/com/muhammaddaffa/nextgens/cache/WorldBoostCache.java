package com.muhammaddaffa.nextgens.cache;

import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.nextgens.NextGens;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WorldBoostCache {

    private static final Map<String, WorldBoostSettings> worldBoostSettingsMap = new ConcurrentHashMap<>();

    public static void init() {
        worldBoostSettingsMap.clear();

        // load world boost settings from config.yml
        FileConfiguration config = NextGens.DEFAULT_CONFIG.getConfig();
        if (!config.isConfigurationSection("world-multipliers")) {
            return;
        }

        for (String worldName : config.getConfigurationSection("world-multipliers").getKeys(false)) {
            double sellMultiplier = config.getDouble("world-multipliers." + worldName + ".sell-multiplier");
            double speedMultiplierPercentage = config.getDouble("world-multipliers." + worldName + ".speed-multiplier");
            int dropMultiplier = config.getInt("world-multipliers." + worldName + ".drop-multiplier");
            List<String> whitelistGeneratorIds = config.getStringList("world-multipliers." + worldName + ".whitelist-generator");
            WorldBoostSettings worldBoostSettings = new WorldBoostSettings(sellMultiplier, speedMultiplierPercentage, dropMultiplier, whitelistGeneratorIds);
            worldBoostSettingsMap.put(worldName, worldBoostSettings);
            Logger.info("Loaded world boost settings for world '" + worldName + "'");
        }
    }

    public static WorldBoostSettings getWorldBoostSettings(String worldName) {
        if (worldName == null) {
            Logger.warning("World name is null");
            return null;
        }
        return worldBoostSettingsMap.get(worldName);
    }
}
