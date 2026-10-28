package com.muhammaddaffa.nextgens;

import com.muhammaddaffa.mdlib.MDLib;
import com.muhammaddaffa.mdlib.task.ExecutorManager;
import com.muhammaddaffa.mdlib.task.ExecutorProvider;
import com.muhammaddaffa.mdlib.utils.LocationUtils;
import com.muhammaddaffa.mdlib.utils.Config;
import com.muhammaddaffa.nextgens.database.ChunkCoord;
import com.muhammaddaffa.nextgens.database.DatabaseManager;
import com.muhammaddaffa.nextgens.generators.ActiveGenerator;
import com.muhammaddaffa.nextgens.generators.Generator;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import com.muhammaddaffa.nextgens.generators.listeners.GeneratorPreventionListener;
import com.muhammaddaffa.nextgens.generators.listeners.GeneratorPlaceListener;
import com.muhammaddaffa.nextgens.users.UserManager;
import com.muhammaddaffa.nextgens.users.UserRepository;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLogger;
import org.bukkit.plugin.PluginManager;
import sun.misc.Unsafe;

import java.lang.reflect.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

/** Plain Java regression check; run with compiled plugin classes and the Maven dependency classpath. */
public final class StartupLoadingRegression {
    private static int checks;
    private static final Deque<Runnable> async = new ArrayDeque<>();
    private static final Deque<Runnable> sync = new ArrayDeque<>();
    private static final Deque<Runnable> region = new ArrayDeque<>();
    private static boolean worldAvailable = true;
    private static boolean chunkLoaded = true;
    private static World world;
    private static final UUID owner = UUID.randomUUID();
    private static final String serialized = "1.0;64.0;1.0;0.0;0.0;fixture";

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++;
    }
    private static void field(Class<?> type, Object owner, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(owner, value);
    }
    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0D;
        if (type == float.class) return 0F;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return (char) 0;
        return null;
    }
    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
    @SuppressWarnings("unchecked")
    private static <T> T value(Class<?> type, Object owner, String name) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); return (T) field.get(owner);
    }
    private static void invoke(Object target, String name, Class<?> type, Object argument) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, type);
        method.setAccessible(true);
        method.invoke(target, argument);
    }
    private static void drain(Deque<Runnable> tasks) {
        while (!tasks.isEmpty()) tasks.removeFirst().run();
    }
    private static void finishLoad() { drain(async); drain(sync); drain(region); }
    private static Generator generator(String id) {
        return new Generator(id, id, 20, null, List.of(), null, 0, true, 0, 100, false, List.of(), List.of());
    }
    private static final class FixtureDatabase extends DatabaseManager {
        final String url;
        boolean fail;
        int saved, deleted;
        @Override public void saveGenerator(ActiveGenerator active) { saved++; }
        @Override public void deleteGenerator(ActiveGenerator active) { deleted++; }
        FixtureDatabase(Path file) { url = "jdbc:sqlite:" + file; }
        @Override public Connection getConnection() throws SQLException {
            if (fail) throw new SQLException("Fixture query failure");
            return DriverManager.getConnection(url);
        }
    }
    public static void main(String[] args) throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe"); unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);
        NextGens plugin = (NextGens) unsafe.allocateInstance(NextGens.class);
        field(MDLib.class, null, "instance", plugin);
        world = proxy(World.class, (p, m, a) -> switch (m.getName()) {
            case "getName" -> "fixture";
            case "getUID" -> owner;
            case "isChunkLoaded" -> chunkLoaded;
            case "equals" -> p == a[0];
            case "hashCode" -> System.identityHashCode(p);
            default -> defaultValue(m.getReturnType());
        });
        int[] disabled = {0};
        PluginManager plugins = proxy(PluginManager.class, (p, m, a) -> {
            if (m.getName().equals("disablePlugin")) {
                check(a[0] == plugin, "startup failure disables the affected plugin");
                disabled[0]++;
                field(JavaPlugin.class, plugin, "isEnabled", false);
            }
            return defaultValue(m.getReturnType());
        });
        Server server = proxy(Server.class, (p, m, a) -> switch (m.getName()) {
            case "getPluginManager" -> plugins;
            case "getLogger" -> Logger.getLogger("startup-loading-regression");
            case "getWorld" -> worldAvailable && "fixture".equals(a[0]) ? world : null;
            case "getName" -> "fixture";
            case "getVersion" -> "git-Fixture (MC: 1.21.11)";
            case "getBukkitVersion" -> "1.21.11-R0.1-SNAPSHOT";
            default -> defaultValue(m.getReturnType());
        });
        field(Bukkit.class, null, "server", server);
        field(JavaPlugin.class, plugin, "server", server);
        field(JavaPlugin.class, plugin, "description", new PluginDescriptionFile("LoadingRegression", "1.0", "fixture.Main"));
        field(JavaPlugin.class, plugin, "logger", new PluginLogger(plugin));
        ExecutorProvider provider = proxy(ExecutorProvider.class, (p, m, a) -> {
            switch (m.getName()) {
                case "async" -> async.add((Runnable) a[0]);
                case "sync" -> sync.add((Runnable) a[0]);
                case "region" -> region.add((Runnable) a[3]);
                default -> throw new AssertionError("Unexpected scheduler method " + m.getName());
            }
            return null;
        });
        field(ExecutorManager.class, null, "provider", provider);
        NextGens.STOPPING = false;
        NextGens.LOADED_CHUNKS.clear();
        activationFailures(plugin, unsafe, disabled);
        parser();
        Path file = Files.createTempFile(Files.createDirectories(Path.of("target")), "nextgens-loading-regression-", ".db");
        try {
            FixtureDatabase database = new FixtureDatabase(file);
            try (Connection connection = database.getConnection(); Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE nextgens_generator (owner TEXT, location TEXT, generator_id TEXT, timer REAL, is_corrupted INTEGER)");
                statement.execute("CREATE TABLE nextgens_user (uuid TEXT, bonus INTEGER, multiplier REAL, earnings REAL, items_sold INTEGER, normal_sell INTEGER, sellwand_sell INTEGER, toggle_cashback INTEGER, toggle_inventory_sell INTEGER, toggle_gens_sell INTEGER, member_set TEXT)");
                try (PreparedStatement insert = connection.prepareStatement("INSERT INTO nextgens_generator VALUES (?, ?, 'stored', 12, 0)")) {
                    insert.setString(1, owner.toString()); insert.setString(2, serialized); insert.executeUpdate();
                    insert.setString(1, null); insert.setString(2, "16;64;0;0;0;fixture"); insert.executeUpdate();
                    insert.setString(1, owner.toString()); insert.setString(2, "NaN;64;0;0;0;fixture"); insert.executeUpdate();
                }
                statement.execute("INSERT INTO nextgens_user VALUES ('" + owner + "', 9, 1.5, 20, 1, 1, 0, 1, 0, 0, '')");
            }
            users(database);
            generators(database);
            conditionalMutations(database);
        } finally {
            Files.deleteIfExists(file);
        }
        System.out.println("PASS: " + checks + " startup/loading regression checks");
    }
    private static final class FailingConfig extends Config {
        boolean linkage;
        private FailingConfig() { super("fixture.yml", null, false); }
        @Override public String getString(String path) {
            if (linkage) throw new LinkageError("Fixture integration linkage failure");
            throw new IllegalStateException("Fixture command registration failure");
        }
    }
    private static void activationFailures(NextGens plugin, Unsafe unsafe, int[] disabled) throws Exception {
        FailingConfig config = (FailingConfig) unsafe.allocateInstance(FailingConfig.class);
        NextGens.DEFAULT_CONFIG = config;
        Method startup = NextGens.class.getDeclaredMethod("finishStartup"); startup.setAccessible(true);
        for (boolean linkage : List.of(false, true)) {
            config.linkage = linkage;
            field(JavaPlugin.class, plugin, "isEnabled", true);
            int before = disabled[0];
            startup.invoke(plugin);
            check(disabled[0] == before + 1 && !plugin.isEnabled(),
                    "scheduled activation failure disables the plugin instead of leaving partial handlers active");
        }
        NextGens.DEFAULT_CONFIG = null;
    }
    private static void parser() {
        check(ChunkCoord.fromSerializedLocation("-0.1;64;-16.1;0;0;not_loaded")
                .equals(new ChunkCoord("not_loaded", -1, -2)), "indexing absent worlds preserves negative chunk coordinates");
        for (String bad : Arrays.asList(null, "broken", "NaN;64;0;0;0;fixture", "0;64;0;0;0;", "0;64;0;Infinity;0;fixture")) {
            boolean rejected = false;
            try { ChunkCoord.fromSerializedLocation(bad); } catch (IllegalArgumentException expected) { rejected = true; }
            check(rejected, "malformed location rejected");
        }
    }
    private static void users(FixtureDatabase database) throws Exception {
        UserManager users = new UserManager();
        users.beginLoading();
        boolean defaultsBlocked = false;
        try { users.getUser(owner); } catch (IllegalStateException expected) { defaultsBlocked = true; }
        check(defaultsBlocked && users.getUsers().isEmpty(), "API cannot create defaults before persisted users load");
        UserRepository repository = new UserRepository(database, users);
        database.fail = true;
        boolean failed = false;
        try { repository.loadUsers(); } catch (IllegalStateException expected) { failed = true; }
        check(failed && users.getUsers().isEmpty(), "user query failure propagates without installing defaults");
        database.fail = false;
        try (Connection connection = database.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO nextgens_user VALUES ('invalid', 0, 0, 0, 0, 0, 0, 1, 0, 0, '')");
        }
        failed = false;
        try { repository.loadUsers(); } catch (IllegalArgumentException expected) { failed = true; }
        check(failed && users.getUsers().isEmpty(), "invalid later user row cannot publish a partial user load");
        try (Connection connection = database.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DELETE FROM nextgens_user WHERE uuid='invalid'");
        }
        repository.loadUsers();
        check(users.getUser(owner).getBonus() == 9, "persisted user bonus loads before gameplay");
    }
    private static void generators(FixtureDatabase database) throws Exception {
        GeneratorManager manager = new GeneratorManager(database);
        check(manager.getGenerator((String) null) == null, "terminal tier lookup remains nullable");
        Map<String, Generator> definitions = value(GeneratorManager.class, manager, "generatorMap");
        Generator stored = generator("stored"), replacement = generator("replacement");
        definitions.put("stored", stored); definitions.put("replacement", replacement);
        Map<String, ActiveGenerator> active = value(GeneratorManager.class, manager, "activeGenerators");
        Chunk chunk = proxy(Chunk.class, (p, m, a) -> switch (m.getName()) {
            case "getWorld" -> world;
            case "getX", "getZ" -> 0;
            default -> defaultValue(m.getReturnType());
        });
        ChunkCoord coord = new ChunkCoord("fixture", 0, 0);
        manager.requestChunkLoad(chunk);
        check(async.isEmpty() && !NextGens.LOADED_CHUNKS.contains(coord), "pre-index chunk event cannot mark a chunk loaded");
        database.fail = true;
        boolean failed = false;
        try { manager.loadChunkCoords(); } catch (IllegalStateException expected) { failed = true; }
        manager.requestChunkLoad(chunk);
        check(failed && async.isEmpty(), "failed index query leaves loading unavailable");
        database.fail = false; worldAvailable = false;
        manager.loadChunkCoords();
        check(manager.getGeneratorCount(owner) == 1, "absent world is indexed and counted while invalid rows are skipped");
        check(manager.getGeneratorsByChunk().get(coord).equals(List.of(serialized)), "index contains valid stored location");
        protection(manager);
        manager.requestChunkLoad(chunk); manager.requestChunkLoad(chunk);
        check(async.size() == 1, "duplicate chunk events share one in-flight query");
        finishLoad();
        check(!NextGens.LOADED_CHUNKS.contains(coord), "missing world remains retryable");
        worldAvailable = true; database.fail = true;
        manager.requestChunkLoad(chunk); finishLoad();
        check(!NextGens.LOADED_CHUNKS.contains(coord), "query failure does not falsely mark success");
        database.fail = false; chunkLoaded = false;
        manager.requestChunkLoad(chunk); finishLoad();
        check(active.isEmpty() && !NextGens.LOADED_CHUNKS.contains(coord), "unloaded chunk remains retryable without adding generators");
        chunkLoaded = true;
        manager.requestChunkLoad(chunk); finishLoad();
        check(active.get(serialized).getGenerator() == stored && NextGens.LOADED_CHUNKS.contains(coord), "retry installs stored generator and marks success");
        NextGens.LOADED_CHUNKS.clear();
        manager.requestChunkLoad(chunk); drain(async); drain(sync);
        ActiveGenerator newer = new ActiveGenerator(UUID.randomUUID(), LocationUtils.deserialize(serialized), replacement);
        active.put(serialized, newer);
        drain(region);
        check(active.get(serialized) == newer, "stale query cannot overwrite a newly registered owner or tier");
        List<String> snapshot = manager.getGeneratorsByChunk().get(coord);
        manager.addChunkCoord(newer); manager.addChunkCoord(newer);
        check(manager.getGeneratorsByChunk().get(coord).size() == 1, "upgrades cannot duplicate indexed locations");
        NextGens.LOADED_CHUNKS.clear();
        manager.requestChunkLoad(chunk); drain(async); drain(sync);
        manager.removeChunkCoord(newer); active.remove(serialized); drain(region);
        check(active.isEmpty(), "removed indexed location cannot be resurrected by stale query completion");
        check(snapshot.equals(List.of(serialized)), "chunk requests retain a stable snapshot while index changes");
    }
    private static void protection(GeneratorManager manager) throws Exception {
        Location location = new Location(world, 1, 64, 1);
        Block block = proxy(Block.class, (p, m, a) -> switch (m.getName()) {
            case "getLocation" -> location;
            case "getWorld" -> world;
            case "getType" -> Material.STONE;
            default -> defaultValue(m.getReturnType());
        });
        check(manager.getActiveGenerator(block) == null && manager.isKnownGenerator(block),
                "unhydrated indexed block is known without inventing an active generator");
        check(!manager.isKnownGenerator(new Location(world, 2, 64, 1)), "ordinary nearby block remains unprotected");
        GeneratorPreventionListener prevention = new GeneratorPreventionListener(manager);
        BlockBreakEvent breaking = new BlockBreakEvent(block, null);
        invoke(prevention, "prevention", BlockBreakEvent.class, breaking);
        check(breaking.isCancelled(), "ordinary block break cannot drop an unhydrated indexed generator");
        BlockPhysicsEvent physics = new BlockPhysicsEvent(block, null);
        invoke(prevention, "onPhysicBlock", BlockPhysicsEvent.class, physics);
        check(physics.isCancelled(), "physics cannot destroy an unhydrated generator");
        BlockPistonExtendEvent piston = new BlockPistonExtendEvent(block, List.of(block), BlockFace.EAST);
        invoke(prevention, "prevention", BlockPistonExtendEvent.class, piston);
        check(piston.isCancelled(), "piston cannot move an unhydrated generator");
        BlockPlaceEvent placement = new BlockPlaceEvent(block, null, block, null, null, true);
        invoke(new GeneratorPlaceListener(manager, new UserManager()), "generatorPlace", BlockPlaceEvent.class, placement);
        check(placement.isCancelled(), "placement cannot overwrite an unhydrated indexed location");
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe"); unsafeField.setAccessible(true);
        Config config = (Config) ((Unsafe) unsafeField.get(null)).allocateInstance(Config.class);
        YamlConfiguration yaml = new YamlConfiguration(); field(Config.class, config, "config", yaml);
        NextGens.DEFAULT_CONFIG = config;
        for (boolean enabled : List.of(true, false)) {
            yaml.set("anti-explosion", enabled);
            List<Block> destroyed = new ArrayList<>(List.of(block));
            invoke(prevention, "checkExplosion", List.class, destroyed);
            check(destroyed.isEmpty(), "pending indexed generator survives explosions until its state has loaded");
        }
    }

    private static void conditionalMutations(FixtureDatabase database) throws Exception {
        GeneratorManager manager = new GeneratorManager(database);
        Map<String, ActiveGenerator> active = value(GeneratorManager.class, manager, "activeGenerators");
        Location location = new Location(world, 1, 64, 1);
        int[] blockWrites = {0};
        Block block = proxy(Block.class, (p, m, a) -> switch (m.getName()) {
            case "getLocation" -> location;
            case "getWorld" -> world;
            case "setType" -> { blockWrites[0]++; yield null; }
            default -> defaultValue(m.getReturnType());
        });
        ActiveGenerator first = manager.registerGenerator(owner, generator("first"), block);
        manager.registerGenerator(owner, generator("upgrade"), block);
        ActiveGenerator replacement = new ActiveGenerator(owner, location, generator("replacement"));
        active.put(serialized, replacement);
        int deletedBefore = database.deleted;
        check(!manager.unregisterGenerator(first) && active.get(serialized) == replacement
                && database.deleted == deletedBefore, "stale expected removal cannot delete a replacement or queue its deletion");
        drain(region);
        check(blockWrites[0] == 0, "queued block update cannot mutate a replacement generator");
        check(manager.unregisterGenerator(replacement) && !manager.isKnownGenerator(block)
                && manager.getGeneratorCount(owner) == 0, "expected removal atomically clears active registry, index and owner count");
        check(!manager.unregisterGenerator(replacement) && database.deleted == deletedBefore + 1,
                "repeated expected removal queues exactly one deletion");
        ActiveGenerator removed = manager.registerGenerator(owner, generator("another"), block);
        manager.registerGenerator(owner, generator("another-upgrade"), block);
        manager.unregisterGenerator(removed);
        drain(region);
        check(blockWrites[0] == 0, "queued block update cannot restore a removed generator block");
    }

}
