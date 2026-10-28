package com.muhammaddaffa.nextgens.generators.managers;

import com.muhammaddaffa.mdlib.task.ExecutorManager;
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
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
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

    private final Map<String, Generator> generatorMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ActiveGenerator> activeGenerators = new ConcurrentHashMap<>();

    private final Map<ChunkCoord, List<String>> generatorsByChunk = new ConcurrentHashMap<>();

    private final Map<UUID, Set<String>> generatorCount = new ConcurrentHashMap<>();
    private final Set<ChunkCoord> loadingChunks = ConcurrentHashMap.newKeySet();
    private volatile boolean chunkIndexReady;

    private final DatabaseManager dbm;
    public GeneratorManager(DatabaseManager dbm) {
        this.dbm = dbm;
    }

    @Nullable
    public Generator getGenerator(String id) {
        return id == null ? null : this.generatorMap.get(id);
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

    public synchronized ActiveGenerator registerGenerator(UUID owner, @NotNull Generator generator, @NotNull Block block) {
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
            ActiveGenerator expected = active;
            ExecutorManager.getProvider().region(block.getWorld(), block.getX() >> 4, block.getZ() >> 4, () -> {
                synchronized (this) {
                    if (this.getActiveGenerator(block) == expected && expected.getGenerator() == generator
                            && expected.isChunkLoaded()) {
                        block.setType(generator.item().getType());
                    }
                }
            });
        }
        ActiveGenerator finalActive = active;
        // Add generator to the chunk coord
        addChunkCoord(finalActive);
        // save the generator on the database
        this.dbm.saveGenerator(finalActive);
        return active;
    }

    public void unregisterGenerator(@Nullable Block block) {
        if (block == null) return;
        this.unregisterGenerator(block.getLocation());
    }

    public void unregisterGenerator(Location location) {
        this.unregisterGenerator(LocationUtils.serialize(location));
    }

    public synchronized void unregisterGenerator(String serialized) {
        ActiveGenerator removed = this.activeGenerators.remove(serialized);
        if (removed != null) {
            onGeneratorRemoved(removed);
        }
    }

    public synchronized boolean unregisterGenerator(ActiveGenerator expected) {
        if (expected == null || !this.activeGenerators.remove(LocationUtils.serialize(expected.getLocation()), expected)) {
            return false;
        }
        onGeneratorRemoved(expected);
        return true;
    }

    private void onGeneratorRemoved(ActiveGenerator removed) {
        removed.setCorrupted(false);
        GeneratorTask.destroy(removed);
        this.removeGeneratorCount(removed.getOwner(), removed);
        removeChunkCoord(removed);
        this.dbm.deleteGenerator(removed);
    }

    public void removeAllGenerator(Player player) {
        this.removeAllGenerator(player.getUniqueId());
    }

    public void removeAllGenerator(UUID uuid) {
        this.getActiveGenerator(uuid).forEach(active -> {
            // Get the block
            Block block = active.getLocation().getBlock();
            // Unregister the generator and set it to air
            if (this.unregisterGenerator(active)) {
                block.setType(Material.AIR);
            }
        });
    }

    public void requestChunkLoad(Chunk chunk) {
        ChunkCoord coord = new ChunkCoord(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        if (!this.chunkIndexReady || NextGens.STOPPING || NextGens.LOADED_CHUNKS.contains(coord)
                || !this.loadingChunks.add(coord)) return;
        List<String> locations = this.generatorsByChunk.getOrDefault(coord, List.of());
        if (locations.isEmpty()) {
            NextGens.LOADED_CHUNKS.add(coord);
            this.loadingChunks.remove(coord);
            return;
        }
        try {
            ExecutorManager.getProvider().async(() -> this.loadActiveGenerator(coord, locations));
        } catch (RuntimeException ex) {
            this.loadingChunks.remove(coord);
            throw ex;
        }
    }

    private record StoredGenerator(UUID owner, String location, String generatorId,
                                   double timer, boolean corrupted) {}

    public void loadActiveGenerator(ChunkCoord coord, List<String> serializedLocations) {
        if (serializedLocations.isEmpty()) {
            this.loadingChunks.remove(coord);
            return;
        }
        String placeholders = serializedLocations.stream().map(s -> "?").collect(Collectors.joining(","));
        String sql = "SELECT owner, location, generator_id, timer, is_corrupted FROM "
                + DatabaseManager.GENERATOR_TABLE + " WHERE location IN (" + placeholders + ")";
        List<StoredGenerator> rows = new ArrayList<>();
        try (Connection conn = this.dbm.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < serializedLocations.size(); i++) {
                ps.setString(i + 1, serializedLocations.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        String location = rs.getString("location");
                        if (!coord.equals(ChunkCoord.fromSerializedLocation(location))) continue;
                        String owner = rs.getString("owner");
                        double timer = rs.getDouble("timer");
                        if (owner == null || !Double.isFinite(timer)) throw new IllegalArgumentException("Invalid generator data");
                        rows.add(new StoredGenerator(UUID.fromString(owner), location,
                                rs.getString("generator_id"), timer, rs.getBoolean("is_corrupted")));
                    } catch (IllegalArgumentException ex) {
                        Logger.warning("Skipped an invalid stored generator while loading a chunk");
                    }
                }
            }
            // Resolve worlds on the server scheduler, and touch chunk state on its region scheduler.
            ExecutorManager.getProvider().sync(() -> {
                try {
                    World world = Bukkit.getWorld(coord.world());
                    if (world == null || NextGens.STOPPING) {
                        this.loadingChunks.remove(coord);
                        return;
                    }
                    ExecutorManager.getProvider().region(world, coord.x(), coord.z(), () -> {
                        try {
                            if (NextGens.STOPPING || !world.isChunkLoaded(coord.x(), coord.z())) return;
                            synchronized (this) {
                                List<String> indexed = this.generatorsByChunk.getOrDefault(coord, List.of());
                                for (StoredGenerator row : rows) {
                                    if (!indexed.contains(row.location())) continue;
                                    Generator generator = this.getGenerator(row.generatorId());
                                    if (generator == null) continue;
                                    Location location = LocationUtils.deserialize(row.location());
                                    if (location.getWorld() != world) continue;
                                    // A query started before a placement, upgrade or removal must not replace live state.
                                    this.activeGenerators.putIfAbsent(row.location(), new ActiveGenerator(row.owner(),
                                            location, generator, row.timer(), row.corrupted()));
                                }
                                NextGens.LOADED_CHUNKS.add(coord);
                            }
                        } finally {
                            this.loadingChunks.remove(coord);
                        }
                    });
                } catch (RuntimeException ex) {
                    this.loadingChunks.remove(coord);
                    Logger.warning("Failed to schedule generator loading; the chunk can be retried");
                }
            });
        } catch (SQLException | RuntimeException ex) {
            this.loadingChunks.remove(coord);
            Logger.warning("Failed to load generators; the chunk can be retried");
        }
    }

    public void loadChunkCoords() {
        Map<ChunkCoord, List<String>> index = new HashMap<>();
        Map<UUID, Set<String>> counts = new HashMap<>();
        int skipped = 0;
        String query = "SELECT owner, location FROM " + DatabaseManager.GENERATOR_TABLE;
        try (Connection connection = this.dbm.getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                try {
                    String ownerString = result.getString("owner");
                    if (ownerString == null) throw new IllegalArgumentException("Missing generator owner");
                    UUID owner = UUID.fromString(ownerString);
                    String serialized = result.getString("location");
                    ChunkCoord coord = ChunkCoord.fromSerializedLocation(serialized);
                    counts.computeIfAbsent(owner, k -> new HashSet<>()).add(serialized);
                    index.computeIfAbsent(coord, k -> new ArrayList<>()).add(serialized);
                } catch (IllegalArgumentException ex) {
                    skipped++;
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load the generator index; startup aborted", ex);
        }
        synchronized (this) {
            index.forEach((coord, locations) -> this.generatorsByChunk.put(coord, List.copyOf(locations)));
            counts.forEach((owner, locations) -> locations.forEach(location -> addGeneratorCount(owner, location)));
            this.chunkIndexReady = true;
        }
        if (skipped != 0) Logger.warning("Skipped " + skipped + " invalid generator index rows");
        Logger.info("Successfully indexed generators in " + index.size() + " chunks!");
    }

    public void addChunkCoord(ActiveGenerator active) {
        addChunkCoord(active.getLocation());
    }

    public synchronized void addChunkCoord(Location location) {
        if (location == null) return;

        ChunkCoord coord = ChunkCoord.fromLocation(location);
        String serialized = LocationUtils.serialize(location);
        this.generatorsByChunk.compute(coord, (key, locations) -> {
            if (locations != null && locations.contains(serialized)) return locations;
            List<String> updated = new ArrayList<>(locations == null ? List.of() : locations);
            updated.add(serialized);
            return List.copyOf(updated);
        });
    }

    public void removeChunkCoord(ActiveGenerator active) {
        removeChunkCoord(active.getLocation());
    }

    public synchronized void removeChunkCoord(Location location) {
        if (location == null) return;
        ChunkCoord coord = ChunkCoord.fromLocation(location);
        String serialized = LocationUtils.serialize(location);
        this.generatorsByChunk.computeIfPresent(coord, (key, locations) -> {
            List<String> updated = new ArrayList<>(locations);
            updated.removeIf(serialized::equals);
            return updated.isEmpty() ? null : List.copyOf(updated);
        });
    }

    public Map<ChunkCoord, List<String>> getGeneratorsByChunk() {
        return this.generatorsByChunk;
    }

    public void saveActiveGenerator(ActiveGenerator active) {
        if (active != null && this.getActiveGenerator(active.getLocation()) == active) {
            this.dbm.saveGenerator(active);
        }
    }

    public void saveActiveGenerator() {
        this.dbm.saveGenerator(this.activeGenerators.values());
    }

    public synchronized void loadGenerators() {
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
