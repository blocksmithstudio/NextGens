package com.muhammaddaffa.nextgens.refund;

import com.muhammaddaffa.mdlib.task.ExecutorManager;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Config;
import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.generators.Generator;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class RefundManager {

    private final Map<UUID, List<String>> itemMap = new HashMap<>();
    private final GeneratorManager generatorManager;

    public RefundManager(GeneratorManager generatorManager) {
        this.generatorManager = generatorManager;
    }

    /**
     * Adds a generator ID to a player's refund list.
     *
     * @param uuid Player's UUID
     * @param id   Generator ID
     */
    public void delayedGiveGeneratorItem(UUID uuid, String id) {
        List<String> generators = itemMap.computeIfAbsent(uuid, k -> new ArrayList<>());
        generators.add(id);
        // Update the configuration immediately
        savePlayerData(uuid);
    }

    /**
     * Gives the refunded items to the player upon joining and removes their data.
     *
     * @param player The player who joined
     */
    public void giveItemOnJoin(Player player) {
        UUID playerUUID = player.getUniqueId();
        List<String> generators = itemMap.remove(playerUUID);

        if (generators == null || generators.isEmpty()) {
            return;
        }

        // Give the generators to the player
        for (String id : generators) {
            Generator generator = generatorManager.getGenerator(id);
            if (generator != null) {
                Common.addInventoryItem(player, generator.createItem(1));
            } else {
                NextGens.getInstance().getLogger().warning(
                        "Generator with ID '" + id + "' not found for player " + player.getName()
                );
            }
        }

        // Remove player's data from config and save
        ExecutorManager.getProvider().async(() -> removePlayerData(playerUUID));
    }

    /**
     * Loads refund data from the configuration file into memory.
     */
    public void load() {
        FileConfiguration config = NextGens.DATA_CONFIG.getConfig();

        if (!config.isConfigurationSection("items")) {
            return;
        }

        for (String uuidString : config.getConfigurationSection("items").getKeys(false)) {
            UUID uuid = UUID.fromString(uuidString);
            List<String> generators = config.getStringList("items." + uuidString);

            itemMap.put(uuid, generators);
        }
    }

    /**
     * Saves all refund data from memory to the configuration file.
     */
    public void saveAll() {
        Config data = NextGens.DATA_CONFIG;
        FileConfiguration config = data.getConfig();

        config.set("items", null); // Clear existing data

        itemMap.forEach((uuid, generators) -> config.set("items." + uuid.toString(), generators));

        data.saveConfig();
    }

    /**
     * Saves a specific player's refund data to the configuration file.
     *
     * @param uuid Player's UUID
     */
    private void savePlayerData(UUID uuid) {
        Config data = NextGens.DATA_CONFIG;
        FileConfiguration config = data.getConfig();

        config.set("items." + uuid.toString(), itemMap.get(uuid));

        data.saveConfig();
    }

    /**
     * Removes a specific player's refund data from the configuration file.
     *
     * @param uuid Player's UUID
     */
    private void removePlayerData(UUID uuid) {
        Config data = NextGens.DATA_CONFIG;
        FileConfiguration config = data.getConfig();

        config.set("items." + uuid.toString(), null);

        data.saveConfig();
    }

}
