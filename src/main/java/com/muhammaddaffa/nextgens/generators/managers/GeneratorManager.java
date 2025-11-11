package com.muhammaddaffa.nextgens.generators.managers;

import com.muhammaddaffa.mdlib.utils.*;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.api.events.generators.GeneratorLoadEvent;
import com.muhammaddaffa.nextgens.database.ChunkCoord;
import com.muhammaddaffa.nextgens.database.DatabaseManager;
import com.muhammaddaffa.nextgens.generators.ActiveGenerator;
import com.muhammaddaffa.nextgens.generators.Drop;
import com.muhammaddaffa.nextgens.generators.Generator;
import com.muhammaddaffa.nextgens.generators.runnables.GeneratorTask;
import com.muhammaddaffa.nextgens.requirements.GensRequirement;
import com.muhammaddaffa.nextgens.requirements.impl.PermissionRequirement;
import com.muhammaddaffa.nextgens.requirements.impl.PlaceholderRequirement;
import com.muhammaddaffa.nextgens.utils.*;
import me.clip.placeholderapi.PlaceholderAPI;
import net.brcdev.shopgui.core.BConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class GeneratorManager {

    private final Map<String, Generator> generatorMap = new HashMap<>();
    private final ConcurrentMap<String, ActiveGenerator> activeGenerators = new ConcurrentHashMap<>();

    private final Map<ChunkCoord, List<String>> generatorsByChunk = new ConcurrentHashMap<>();

    private final Map<UUID, Set<String>> generatorCount = new HashMap<>();

    private final DatabaseManager dbm;
    public GeneratorManager(DatabaseManager dbm) {
        this.dbm = dbm;
    }

    @Nullable
    public Generator getGenerator(String id) {
        return this.generatorMap.get(id);
    }

    @Nullable
    public Generator getGenerator(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || stack.getItemMeta() == null) {
            return null;
        }
        // scrap the id from the item
        String id = stack.getItemMeta().getPersistentDataContainer().get(NextGens.generator_id, PersistentDataType.STRING);
        if (id == null) {
            return null;
        }
        return this.getGenerator(id);
    }

    @NotNull
    public Generator getRandomGenerator() {
        // turn the key into list
        List<String> generators = this.generatorMap.keySet().stream().toList();
        return this.generatorMap.get(generators.get(ThreadLocalRandom.current().nextInt(generators.size())));
    }

    public Set<String> getGeneratorIDs() {
        return this.generatorMap.keySet();
    }

    public boolean isGeneratorItem(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || stack.getItemMeta() == null) {
            return false;
        }
        return stack.getItemMeta().getPersistentDataContainer().has(NextGens.generator_id, PersistentDataType.STRING);
    }

    public boolean isDropItem(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || stack.getItemMeta() == null) {
            return false;
        }
        return stack.getItemMeta().getPersistentDataContainer().has(NextGens.drop_value, PersistentDataType.DOUBLE);
    }

    public Collection<Generator> getGenerators() {
        return this.generatorMap.values();
    }

    public int getGeneratorCount(Player player) {
        return this.getGeneratorCount(player.getUniqueId());
    }

    public int getGeneratorCount(UUID uuid) {
        return this.generatorCount.getOrDefault(uuid, new HashSet<>()).size();
    }

    public void addGeneratorCount(UUID uuid, ActiveGenerator active) {
        addGeneratorCount(uuid, active.getLocation());
    }

    public void addGeneratorCount(UUID uuid, Location location) {
        if (location == null) return;
        addGeneratorCount(uuid, LocationUtils.serialize(location));
    }

    public void addGeneratorCount(UUID uuid, String serializedLocation) {
        this.generatorCount.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).add(serializedLocation);
    }

    public void removeGeneratorCount(UUID uuid, ActiveGenerator active) {
        removeGeneratorCount(uuid, active.getLocation());
    }

    public void removeGeneratorCount(UUID uuid, Location location) {
        this.removeGeneratorCount(uuid, LocationUtils.serialize(location));
    }

    public void removeGeneratorCount(UUID uuid, String serializedLocation) {
        this.generatorCount.getOrDefault(uuid, new HashSet<>()).remove(serializedLocation);
    }

    public Collection<ActiveGenerator> getActiveGenerator() {
        return this.activeGenerators.values();
    }

    @NotNull
    public List<ActiveGenerator> getActiveGenerator(Player player) {
        return this.getActiveGenerator(player.getUniqueId());
    }

    @NotNull
    public List<ActiveGenerator> getActiveGenerator(UUID uuid) {
        return this.getActiveGenerator().stream()
                .filter(active -> active.getOwner().equals(uuid))
                .collect(Collectors.toList());
    }

    @Nullable
    public ActiveGenerator getActiveGenerator(@Nullable Block block) {
        if (block == null) return null;
        return this.getActiveGenerator(block.getLocation());
    }

    @Nullable
    public ActiveGenerator getActiveGenerator(@NotNull Location location) {
        return this.getActiveGenerator(LocationUtils.serialize(location));
    }

    @Nullable
    public ActiveGenerator getActiveGenerator(String serialized) {
        return this.activeGenerators.get(serialized);
    }

    public ActiveGenerator registerGenerator(Player owner, @NotNull Generator generator, @NotNull Block block) {
        return this.registerGenerator(owner.getUniqueId(), generator, block);
    }

    public ActiveGenerator registerGenerator(UUID owner, @NotNull Generator generator, @NotNull Block block) {
        ActiveGenerator active = this.getActiveGenerator(block);
        // check if block is already active generator or not
        if (active == null) {
            // register the new one
            String serialized = LocationUtils.serialize(block.getLocation());
            active = new ActiveGenerator(owner, block.getLocation(), generator);
            this.activeGenerators.put(serialized, active);
            // add generator count
            this.addGeneratorCount(owner, serialized);
        } else {
            // change the generator id
            active.setGenerator(generator);
            // set the block
            Executor.syncLater(2L, () -> block.setType(generator.item().getType()));
        }
        ActiveGenerator finalActive = active;
        // Add generator to the chunk coord
        addChunkCoord(finalActive);
        // save the generator on the database
        Executor.async(() -> this.dbm.saveGenerator(finalActive));
        return active;
    }

    public void unregisterGenerator(@Nullable Block block) {
        if (block == null) return;
        this.unregisterGenerator(block.getLocation());
    }

    public void unregisterGenerator(Location location) {
        this.unregisterGenerator(LocationUtils.serialize(location));
    }

    public void unregisterGenerator(String serialized) {
        ActiveGenerator removed = this.activeGenerators.remove(serialized);
        // check if the remove is successful
        if (removed != null) {
            // remove the corrupt status
            removed.setCorrupted(false);
            // force remove
            GeneratorTask.destroy(removed);
            // remove the generator count
            this.removeGeneratorCount(removed.getOwner(), removed);
            // Remove from the chunk coord
            removeChunkCoord(removed);
            // remove the generator from the database
            Executor.async(() -> this.dbm.deleteGenerator(removed));
        }
    }

    public void removeAllGenerator(Player player) {
        this.removeAllGenerator(player.getUniqueId());
    }

    public void removeAllGenerator(UUID uuid) {
        this.getActiveGenerator(uuid).forEach(active -> {
            // Get the block
            Block block = active.getLocation().getBlock();
            // Unregister the generator and set it to air
            this.unregisterGenerator(block);
            block.setType(Material.AIR);
        });
    }

    public void loadActiveGenerator(ChunkCoord coord, List<String> serializedLocations) {
        // 1) early-out if empty
        if (serializedLocations.isEmpty()) return;

        // 2) build placeholders "(?,?,…)"
        String placeholders = serializedLocations.stream()
                .map(s -> "?")
                .collect(Collectors.joining(","));

        String sql = "SELECT owner, location, generator_id, timer, is_corrupted " +
                "FROM " + DatabaseManager.GENERATOR_TABLE +
                " WHERE location IN (" + placeholders + ")";

        try (Connection conn = this.dbm.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // 3) bind all of them at once
            for (int i = 0; i < serializedLocations.size(); i++) {
                ps.setString(i + 1, serializedLocations.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                List<ActiveGenerator> toRegister = new ArrayList<>();

                while (rs.next()) {
                    UUID owner = UUID.fromString(rs.getString("owner"));
                    String serialized = rs.getString("location");
                    Location loc = LocationUtils.deserialize(serialized);
                    String genId = rs.getString("generator_id");
                    double timer = rs.getDouble("timer");
                    boolean corrupted = rs.getBoolean("is_corrupted");

                    Generator gen = this.getGenerator(genId);
                    if (gen == null || loc.getWorld() == null) continue;

                    toRegister.add(new ActiveGenerator(owner, loc, gen, timer, corrupted));
                    count++;
                }

                // 4) switch back to the main thread to actually register them
                int finalCount = count;
                Executor.sync(() -> {
                    for (ActiveGenerator ag : toRegister) {
                        String key = LocationUtils.serialize(ag.getLocation());
                        activeGenerators.put(key, ag);
                    }
                    Logger.info("Successfully loaded " + finalCount
                            + " active generators in chunk [" + coord.x() + "," + coord.z() + "]");
                });
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed loading generators for chunk ["
                    + coord.x() + "," + coord.z() + "]", ex);

        }
    }

    public void loadChunkCoords() {
        String query = "SELECT * FROM " + DatabaseManager.GENERATOR_TABLE;
        this.dbm.executeQuery(query, result -> {
            while (result.next()) {

                // get the uuid string
                String uuidString = result.getString(1);
                // if the uuid string is null, skip the iteration
                if (uuidString == null) {
                    continue;
                }
                // Load the chunk coords
                String serialized = result.getString(2);
                Location location = LocationUtils.deserialize(serialized);
                ChunkCoord coord = ChunkCoord.fromLocation(location);
                // Add the generator count
                addGeneratorCount(UUID.fromString(uuidString), serialized);
                // Store the data into the map
                this.generatorsByChunk
                        .computeIfAbsent(coord, k -> new ArrayList<>())
                        .add(serialized);
            }
            // send log message
            Logger.info("Successfully stored chunk coords for " + this.generatorsByChunk.size() + " active generators!");
        });
    }

    public void addChunkCoord(ActiveGenerator active) {
        addChunkCoord(active.getLocation());
    }

    public void addChunkCoord(Location location) {
        if (location == null) return;

        ChunkCoord coord = ChunkCoord.fromLocation(location);
        this.generatorsByChunk
                .computeIfAbsent(coord, k -> new ArrayList<>())
                .add(LocationUtils.serialize(location));
    }

    public void removeChunkCoord(ActiveGenerator active) {
        removeChunkCoord(active.getLocation());
    }

    public void removeChunkCoord(Location location) {
        if (location == null) return;

        ChunkCoord coord = ChunkCoord.fromLocation(location);
        this.generatorsByChunk
                .computeIfAbsent(coord, k -> new ArrayList<>())
                .remove(LocationUtils.serialize(location));
    }

    public Map<ChunkCoord, List<String>> getGeneratorsByChunk() {
        return generatorsByChunk;
    }

    public void saveActiveGenerator(ActiveGenerator active) {
        this.dbm.saveGenerator(active);
    }

    public void saveActiveGenerator() {
        this.dbm.saveGenerator(this.activeGenerators.values());
    }

    public void loadGenerators() {
        // Clear the generators map
        this.generatorMap.clear();
        // log message
        Logger.info("Starting to load all generators...");
        // load all generators files inside the 'generators' directory
        File directory = this.getMainDirectory();
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    this.loadGenerators(YamlConfiguration.loadConfiguration(file));
                }
            }
            // load generator from 'generators.yml'
            this.loadGenerators(NextGens.GENERATORS_CONFIG.getConfig());
            // send log message
            Logger.info("Successfully loaded " + this.generatorMap.size() + " generators!");
        } else {
            directory.mkdirs();
            // generate default files
            Logger.info("Creating default files for per-file generators system...");
            NextGens.getInstance().saveResource("generators/elemental_generators.yml", true);
            Logger.info("Successfully created 'generators/elemental_generators.yml' file");
            NextGens.getInstance().saveResource("generators/orion_generators.yml", true);
            Logger.info("Successfully created 'generators/orion_generators.yml' file");
            // load back the generator
            this.loadGenerators();
        }
    }

    private void loadGenerators(FileConfiguration config) {
        // get all sections on the config
        for (String id : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            // load the generator
            this.loadGenerators(id, section);
        }
    }

    private void loadGenerators(String id, ConfigurationSection section) {
        // get all data
        String displayName = section.getString("display-name");
        int interval = section.getInt("interval");
        boolean corrupted = section.getBoolean("corrupted.enabled");
        double fixCost = section.getDouble("corrupted.cost");
        double corruptChance = section.getDouble("corrupted.chance");
        String nextTier = section.getString("upgrade.next-generator");
        double upgradeCost = section.getDouble("upgrade.upgrade-cost");
        // online only options
        Boolean onlineOnly;
        if (section.get("online-only") == null) {
            onlineOnly = null;
        } else {
            onlineOnly = section.getBoolean("online-only");
        }

        ConfigurationSection itemSection = section.getConfigurationSection("item");
        if (itemSection == null) {
            Logger.warning("Failed to load generator '" + id + "'");
            Logger.warning("Reason: There is no generator item configuration!");
            return;
        }
        ItemBuilder builder = ItemBuilder.fromConfig(itemSection);
        if (builder == null) {
            Logger.warning("Failed to load generator '" + id + "'");
            Logger.warning("Reason: Failed to load the generator item configuration!");
            return;
        }
        ItemStack item = builder.build();

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        String modelItemString = section.getString("item.item-model");
        if (modelItemString != null) {
            String[] parts = modelItemString.split(":", 2);
            if (parts.length == 2) {
                NamespacedKey modelItem = new NamespacedKey(parts[0], parts[1]);
                meta.setItemModel(modelItem);
                item.setItemMeta(meta);
            } else {
                Logger.warning("Invalid model item format for generator " + id);
            }
        }

        List<Drop> drops = new ArrayList<>();

        if (section.isConfigurationSection("drops")) {
            for (String key : section.getConfigurationSection("drops").getKeys(false)) {
                ConfigurationSection dropSection = section.getConfigurationSection("drops." + key);
                if (dropSection == null) {
                    continue;
                }
                try {
                    Drop drop = Drop.fromConfig(id, key, dropSection);
                    if (drop == null) {
                        Logger.warning("Failed to load drop " + key + " for generator " + id);
                        continue;
                    }
                    drops.add(drop);
                } catch (Exception e) {
                    Logger.warning("Exception while loading drop '" + key + "' for generator '" + id + "': " + e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        }

        List<GensRequirement> placeRequirements = this.loadRequirement(section, "place-requirements");
        List<GensRequirement> upgradeRequirements = this.loadRequirement(section, "upgrade-requirements");

        Generator generator = new Generator(id, displayName, interval, item, drops, nextTier, upgradeCost,
                corrupted, fixCost, corruptChance, onlineOnly, placeRequirements, upgradeRequirements);

        // call the custom event
        GeneratorLoadEvent loadEvent = new GeneratorLoadEvent(generator);
        Bukkit.getPluginManager().callEvent(loadEvent);
        if (loadEvent.isCancelled()) {
            return;
        }
        // store it on the map
        this.generatorMap.put(id, generator);
        // send log message
        Logger.info("Loaded generator '" + id + "'");
    }

    private List<GensRequirement> loadRequirement(ConfigurationSection section, String path) {
        List<GensRequirement> requirements = new ArrayList<>();
        if (section.isConfigurationSection(path)) {
            for (String key : section.getConfigurationSection(path).getKeys(false)) {
                String type = section.getString(path + "." + key + ".type", "DUMMY");
                String message = section.getString(path + "." + key + ".message", "&cYou don't have the requirement to do this!");
                switch (type.toUpperCase()) {
                    case "PERMISSION" -> {
                        String permission = section.getString(path + "." + key + ".permission");
                        requirements.add(new PermissionRequirement(message, permission));
                    }
                    case "PLACEHOLDER" -> {
                        String placeholder = section.getString(path + "." + key + ".placeholder");
                        String value = section.getString(path + "." + key + ".value");
                        requirements.add(new PlaceholderRequirement(message, placeholder, value));
                    }
                }
            }
        }
        return requirements;
    }

    public void refreshActiveGenerator() {
        for (ActiveGenerator active : this.activeGenerators.values()) {
            if (active.getGenerator() == null) {
                this.fixGenerator(active);
                continue;
            }
            // get the new generator
            Generator refreshed = this.getGenerator(active.getGenerator().id());
            // if the refreshed is not null
            if (refreshed != null) {
                // refresh it
                active.setGenerator(refreshed);
            }
        }
    }

    private void fixGenerator(ActiveGenerator active) {
        if (active.getGenerator() != null) {
            return;
        }
        for (Generator generator : this.generatorMap.values()) {
            if (active.getLocation().getBlock().getType() == generator.item().getType()) {
                active.setGenerator(generator);
                break;
            }
        }
    }

    private File getMainDirectory() {
        return new File(NextGens.getInstance().getDataFolder() + File.separator + "generators");
    }

}
