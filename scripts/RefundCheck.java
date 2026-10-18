import javax.tools.ToolProvider;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RefundCheck {
    public static void main(String[] args) throws Exception {
        Path output = Files.createTempDirectory(Files.createDirectories(Path.of("target")), "refund-check-");
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("org.bukkit.plugin.Plugin", "package org.bukkit.plugin; public interface Plugin {}");
        sources.put("org.bukkit.Material", "package org.bukkit; public enum Material { AIR, STONE }");
        sources.put("org.bukkit.block.Block", """
                package org.bukkit.block;
                public class Block {
                    public org.bukkit.Material type = org.bukkit.Material.STONE;
                    public void setType(org.bukkit.Material value) { type = value; }
                }
                """);
        sources.put("org.bukkit.Location", """
                package org.bukkit;
                public class Location {
                    private final org.bukkit.block.Block block = new org.bukkit.block.Block();
                    public org.bukkit.block.Block getBlock() { return block; }
                }
                """);
        sources.put("org.bukkit.Bukkit", """
                package org.bukkit;
                public class Bukkit {
                    public static final java.util.Map<java.util.UUID, org.bukkit.entity.Player> players = new java.util.HashMap<>();
                    public static org.bukkit.entity.Player getPlayer(java.util.UUID id) {
                        var player = players.get(id);
                        return player != null && player.online ? player : null;
                    }
                }
                """);
        sources.put("org.bukkit.inventory.ItemStack", """
                package org.bukkit.inventory;
                public class ItemStack {
                    public final String id;
                    public final int limit;
                    private int amount;
                    public ItemStack(String id, int amount, int limit) { this.id = id; this.amount = amount; this.limit = limit; }
                    public int getAmount() { return amount; }
                    public void setAmount(int value) { amount = value; }
                    public int getMaxStackSize() { return limit; }
                    public boolean isSimilar(ItemStack other) { return other != null && id.equals(other.id); }
                    public ItemStack clone() { return new ItemStack(id, amount, limit); }
                }
                """);
        sources.put("org.bukkit.inventory.PlayerInventory", """
                package org.bukkit.inventory;
                public class PlayerInventory {
                    public final ItemStack[] storage = new ItemStack[36];
                    public int max = 64;
                    public boolean requireOwner;
                    public int getMaxStackSize() { return max; }
                    public java.util.HashMap<Integer, ItemStack> addItem(ItemStack... items) {
                        if (requireOwner && !io.papermc.paper.threadedregions.scheduler.EntityScheduler.onOwner) {
                            throw new AssertionError("Inventory accessed off the owning region");
                        }
                        var leftover = new java.util.HashMap<Integer, ItemStack>();
                        for (int n = 0; n < items.length; n++) {
                            ItemStack input = items[n];
                            int amount = input.getAmount();
                            int limit = Math.min(input.limit, max);
                            for (ItemStack stack : storage) {
                                if (stack != null && stack.isSimilar(input)) {
                                    int moved = Math.min(amount, Math.max(0, limit - stack.getAmount()));
                                    stack.setAmount(stack.getAmount() + moved);
                                    amount -= moved;
                                }
                            }
                            for (int i = 0; i < storage.length && amount > 0; i++) {
                                if (storage[i] == null) {
                                    int moved = Math.min(amount, limit);
                                    storage[i] = new ItemStack(input.id, moved, input.limit);
                                    amount -= moved;
                                }
                            }
                            input.setAmount(amount);
                            if (amount > 0) { leftover.put(n, input.clone()); }
                        }
                        return leftover;
                    }
                    public int count(String id) {
                        return java.util.Arrays.stream(storage).filter(s -> s != null && s.id.equals(id)).mapToInt(ItemStack::getAmount).sum();
                    }
                    public void full() {
                        for (int i = 0; i < storage.length; i++) { storage[i] = new ItemStack("stone", 64, 64); }
                    }
                }
                """);
        sources.put("org.bukkit.entity.Player", """
                package org.bukkit.entity;
                public class Player {
                    public final java.util.UUID uuid = java.util.UUID.randomUUID();
                    public final org.bukkit.inventory.PlayerInventory inventory = new org.bukkit.inventory.PlayerInventory();
                    public final java.util.List<String> messages = new java.util.ArrayList<>();
                    public final io.papermc.paper.threadedregions.scheduler.EntityScheduler scheduler = new io.papermc.paper.threadedregions.scheduler.EntityScheduler();
                    public boolean online = true;
                    public java.util.UUID getUniqueId() { return uuid; }
                    public boolean isOnline() { return online; }
                    public String getName() { return "RefundPlayer"; }
                    public org.bukkit.inventory.PlayerInventory getInventory() { return inventory; }
                    public io.papermc.paper.threadedregions.scheduler.EntityScheduler getScheduler() { return scheduler; }
                }
                """);
        sources.put("io.papermc.paper.threadedregions.scheduler.EntityScheduler", """
                package io.papermc.paper.threadedregions.scheduler;
                public class EntityScheduler {
                    public static boolean onOwner;
                    public boolean reject, retire;
                    public boolean execute(org.bukkit.plugin.Plugin plugin, Runnable run, Runnable retired, long delay) {
                        if (reject) { return false; }
                        com.muhammaddaffa.mdlib.task.ExecutorManager.queue.add(() -> {
                            if (retire) { retired.run(); return; }
                            onOwner = true;
                            try { run.run(); } finally { onOwner = false; }
                        });
                        return true;
                    }
                }
                """);
        sources.put("com.muhammaddaffa.mdlib.task.handleTask.HandleTask", """
                package com.muhammaddaffa.mdlib.task.handleTask;
                public class HandleTask {
                    public boolean cancelled;
                    public void cancel() { cancelled = true; }
                }
                """);
        sources.put("com.muhammaddaffa.mdlib.task.ExecutorManager", """
                package com.muhammaddaffa.mdlib.task;
                public class ExecutorManager {
                    public static final java.util.Queue<Runnable> queue = new java.util.concurrent.ConcurrentLinkedQueue<>();
                    public static final Provider provider = new Provider();
                    public static boolean folia;
                    public static Provider getProvider() { return provider; }
                    public static boolean isFolia() { return folia; }
                    public static void drain() { Runnable run; while ((run = queue.poll()) != null) { run.run(); } }
                    public static class Provider {
                        public Runnable timer;
                        public long period;
                        public com.muhammaddaffa.mdlib.task.handleTask.HandleTask handle;
                        public void sync(Runnable run) { queue.add(run); }
                        public com.muhammaddaffa.mdlib.task.handleTask.HandleTask syncTimer(long delay, long period, Runnable run) {
                            this.timer = run; this.period = period;
                            return handle = new com.muhammaddaffa.mdlib.task.handleTask.HandleTask();
                        }
                        public void tick() { if (!handle.cancelled) { timer.run(); drain(); } }
                    }
                }
                """);
        sources.put("org.bukkit.configuration.file.FileConfiguration", """
                package org.bukkit.configuration.file;
                public class FileConfiguration {
                    public final java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
                    public void set(String path, Object value) {
                        if (value == null) { values.keySet().removeIf(key -> key.equals(path) || key.startsWith(path + ".")); }
                        else { values.put(path, value); }
                    }
                    public boolean isConfigurationSection(String path) { return values.keySet().stream().anyMatch(key -> key.startsWith(path + ".")); }
                    public FileConfiguration getConfigurationSection(String path) { return this; }
                    public java.util.Set<String> getKeys(boolean deep) {
                        var keys = new java.util.LinkedHashSet<String>();
                        values.keySet().stream().filter(key -> key.startsWith("items.")).forEach(key -> keys.add(key.substring(6)));
                        return keys;
                    }
                    public java.util.List<String> getStringList(String path) {
                        Object value = values.get(path);
                        return value instanceof java.util.List<?> list ? new java.util.ArrayList<>((java.util.List<String>) list) : new java.util.ArrayList<>();
                    }
                    public boolean getBoolean(String path) { return !Boolean.FALSE.equals(values.get(path)); }
                    public boolean contains(String path) { return values.containsKey(path); }
                }
                """);
        sources.put("com.muhammaddaffa.mdlib.utils.Placeholder", """
                package com.muhammaddaffa.mdlib.utils;
                public class Placeholder {
                    public final java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
                    public Placeholder add(String key, Object value) { values.put(key, String.valueOf(value)); return this; }
                }
                """);
        sources.put("com.muhammaddaffa.mdlib.utils.Config", """
                package com.muhammaddaffa.mdlib.utils;
                public class Config {
                    public final org.bukkit.configuration.file.FileConfiguration config = new org.bukkit.configuration.file.FileConfiguration();
                    public java.util.Map<String, Object> saved = new java.util.HashMap<>();
                    public String message = "{generators} | slots={slots}";
                    public int saves;
                    public Config() { config.set("messages.pending-refund", message); }
                    public org.bukkit.configuration.file.FileConfiguration getConfig() { return config; }
                    public void saveConfig() { saved = new java.util.HashMap<>(config.values); saves++; }
                    public void sendMessage(org.bukkit.entity.Player player, String path, Placeholder placeholders) {
                        if (!path.equals("messages.pending-refund")) { throw new AssertionError("Wrong message key"); }
                        String text = message;
                        for (var entry : placeholders.values.entrySet()) { text = text.replace(entry.getKey(), entry.getValue()); }
                        player.messages.add(text);
                    }
                }
                """);
        sources.put("com.muhammaddaffa.mdlib.utils.Common", """
                package com.muhammaddaffa.mdlib.utils;
                public class Common {
                    public static void sendMessage(org.bukkit.entity.Player player, String text, Placeholder placeholders) {
                        for (var entry : placeholders.values.entrySet()) { text = text.replace(entry.getKey(), entry.getValue()); }
                        player.messages.add(text);
                    }
                }
                """);
        sources.put("com.muhammaddaffa.nextgens.NextGens", """
                package com.muhammaddaffa.nextgens;
                public class NextGens implements org.bukkit.plugin.Plugin {
                    public static com.muhammaddaffa.mdlib.utils.Config DATA_CONFIG = new com.muhammaddaffa.mdlib.utils.Config();
                    public static com.muhammaddaffa.mdlib.utils.Config DEFAULT_CONFIG = new com.muhammaddaffa.mdlib.utils.Config();
                    private static final NextGens INSTANCE = new NextGens();
                    public static NextGens getInstance() { return INSTANCE; }
                    public java.util.logging.Logger getLogger() { return java.util.logging.Logger.getLogger("RefundCheck"); }
                }
                """);
        sources.put("com.muhammaddaffa.nextgens.generators.Generator", """
                package com.muhammaddaffa.nextgens.generators;
                public record Generator(String id, String displayName, int limit) {
                    public org.bukkit.inventory.ItemStack createItem(int amount) { return new org.bukkit.inventory.ItemStack(id, amount, limit); }
                }
                """);
        sources.put("com.muhammaddaffa.nextgens.generators.ActiveGenerator", """
                package com.muhammaddaffa.nextgens.generators;
                public record ActiveGenerator(org.bukkit.Location getLocation, Generator getGenerator) {}
                """);
        sources.put("com.muhammaddaffa.nextgens.generators.managers.GeneratorManager", """
                package com.muhammaddaffa.nextgens.generators.managers;
                public class GeneratorManager {
                    public final java.util.Map<String, com.muhammaddaffa.nextgens.generators.Generator> types = new java.util.HashMap<>();
                    public final java.util.Map<java.util.UUID, java.util.List<com.muhammaddaffa.nextgens.generators.ActiveGenerator>> placed = new java.util.HashMap<>();
                    public com.muhammaddaffa.nextgens.generators.Generator getGenerator(String id) { return types.get(id); }
                    public java.util.List<com.muhammaddaffa.nextgens.generators.ActiveGenerator> getActiveGenerator(java.util.UUID id) {
                        return new java.util.ArrayList<>(placed.getOrDefault(id, java.util.List.of()));
                    }
                    public Object getActiveGenerator(org.bukkit.block.Block block) { return null; }
                    public void unregisterGenerator(org.bukkit.Location location) { placed.values().forEach(list -> list.removeIf(g -> g.getLocation() == location)); }
                }
                """);
        sources.put("org.bukkit.event.Listener", "package org.bukkit.event; public interface Listener {}");
        sources.put("org.bukkit.event.EventPriority", "package org.bukkit.event; public enum EventPriority { NORMAL, HIGHEST, MONITOR }");
        sources.put("org.bukkit.event.EventHandler", """
                package org.bukkit.event;
                @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
                public @interface EventHandler {
                    boolean ignoreCancelled() default false;
                    EventPriority priority() default EventPriority.NORMAL;
                }
                """);
        sources.put("org.bukkit.event.player.PlayerJoinEvent", """
                package org.bukkit.event.player;
                public record PlayerJoinEvent(org.bukkit.entity.Player getPlayer) {}
                """);
        sources.put("com.bgsoftware.superiorskyblock.api.island.Island", """
                package com.bgsoftware.superiorskyblock.api.island;
                public class Island {
                    public java.util.List<com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer> members = new java.util.ArrayList<>();
                    public final java.util.UUID id = java.util.UUID.randomUUID();
                    public java.util.UUID getUniqueId() { return id; }
                    public java.util.List<com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer> getIslandMembers(boolean owner) { return members; }
                }
                """);
        sources.put("com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer", """
                package com.bgsoftware.superiorskyblock.api.wrappers;
                public record SuperiorPlayer(java.util.UUID getUniqueId, com.bgsoftware.superiorskyblock.api.island.Island getIsland) {}
                """);
        for (String name : List.of("IslandKickEvent", "IslandQuitEvent", "IslandBanEvent", "IslandDisbandEvent")) {
            String pkg = "com.bgsoftware.superiorskyblock.api.events";
            sources.put(pkg + "." + name, "package " + pkg + "; public record " + name
                    + "(com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer player, com.bgsoftware.superiorskyblock.api.island.Island getIsland) {"
                    + "public com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer getPlayer() { return player; }"
                    + "public com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer getTarget() { return player; }}");
        }
        sources.put("com.bgsoftware.superiorskyblock.api.events.BlockStackEvent", """
                package com.bgsoftware.superiorskyblock.api.events;
                public class BlockStackEvent {
                    public org.bukkit.block.Block getBlock() { return new org.bukkit.block.Block(); }
                    public void setCancelled(boolean value) {}
                }
                """);
        sources.put("world.bentobox.bentobox.api.events.island.IslandPreclearEvent", """
                package world.bentobox.bentobox.api.events.island;
                public record IslandPreclearEvent(OldIsland getOldIsland) {
                    public record OldIsland(java.util.Set<java.util.UUID> getMemberSet) {}
                }
                """);
        for (String name : List.of("TeamKickEvent", "TeamJoinEvent", "TeamLeaveEvent")) {
            String pkg = "world.bentobox.bentobox.api.events.team";
            sources.put(pkg + "." + name, "package " + pkg + "; public record " + name + "(java.util.UUID getPlayerUUID) {}");
        }
        sources.put("com.muhammaddaffa.nextgens.refund.Check", """
                package com.muhammaddaffa.nextgens.refund;
                import com.muhammaddaffa.nextgens.NextGens;
                import com.muhammaddaffa.nextgens.generators.*;
                import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
                import com.muhammaddaffa.mdlib.task.ExecutorManager;
                import org.bukkit.Bukkit;
                import org.bukkit.entity.Player;
                import org.bukkit.inventory.ItemStack;
                import java.util.*;

                public class Check {
                    static int assertions, scenarios;
                    static long now;
                    static GeneratorManager types;
                    static RefundManager manager;
                    static Player player;
                    static void setup() {
                        ExecutorManager.queue.clear();
                        ExecutorManager.folia = false;
                        Bukkit.players.clear();
                        NextGens.DATA_CONFIG = new com.muhammaddaffa.mdlib.utils.Config();
                        NextGens.DEFAULT_CONFIG = new com.muhammaddaffa.mdlib.utils.Config();
                        types = new GeneratorManager();
                        types.types.put("wheat", new Generator("wheat", "&eWheat Generator", 64));
                        types.types.put("pumpkin", new Generator("pumpkin", "&6Pumpkin Generator", 64));
                        manager = new RefundManager(types, () -> now);
                        player = new Player();
                        Bukkit.players.put(player.uuid, player);
                        now = 100_000L;
                        manager.start();
                        scenarios++;
                    }
                    static void require(boolean value, String reason) {
                        assertions++;
                        if (!value) { throw new AssertionError(reason); }
                    }
                    static List<String> pending() { return NextGens.DATA_CONFIG.config.getStringList("items." + player.uuid); }
                    static void refund(String... ids) { manager.refundGenerators(player.uuid, List.of(ids)); ExecutorManager.drain(); }
                    static void tick() { ExecutorManager.provider.tick(); }
                    static String message() { return player.messages.getLast(); }
                    static void event(Object listener, String method, Object event) throws Exception {
                        var handler = listener.getClass().getDeclaredMethod(method, event.getClass());
                        handler.setAccessible(true);
                        handler.invoke(listener, event);
                        ExecutorManager.drain();
                    }
                    static void place() {
                        types.placed.put(player.uuid, new ArrayList<>(List.of(
                                new ActiveGenerator(new org.bukkit.Location(), types.getGenerator("wheat")),
                                new ActiveGenerator(new org.bukkit.Location(), types.getGenerator("wheat")),
                                new ActiveGenerator(new org.bukkit.Location(), types.getGenerator("pumpkin")))));
                    }
                    public static void main(String[] args) throws Exception {
                        setup();
                        player.inventory.full();
                        refund("wheat", "wheat", "pumpkin");
                        require(pending().size() == 3, "Full inventory must preserve all refunds");
                        require(message().equals("&f2 &eWheat Generator&r&f, &f1 &6Pumpkin Generator | slots=2"), "Grouped names, colors and slots");
                        int saves = NextGens.DATA_CONFIG.saves;
                        for (int i = 0; i < 59; i++) { now += 1_000; tick(); }
                        require(player.messages.size() == 1, "No reminder spam before a minute");
                        require(NextGens.DATA_CONFIG.saves == saves, "No repeated disk writes while blocked");
                        now += 1_000; tick();
                        require(player.messages.size() == 2, "Reminder repeats after one minute");
                        require(ExecutorManager.provider.period == 20L, "Retry every second");
                        player.inventory.storage[0] = null; tick();
                        require(player.inventory.count("wheat") == 2 && pending().equals(List.of("pumpkin")), "Partial delivery retains only leftovers");
                        player.inventory.storage[1] = null; tick();
                        require(player.inventory.count("pumpkin") == 1 && pending().isEmpty(), "Automatic delivery after freeing space");
                        tick(); manager.giveItemOnJoin(player); ExecutorManager.drain();
                        require(player.inventory.count("wheat") == 2 && player.inventory.count("pumpkin") == 1, "No repeat delivery");

                        setup();
                        player.inventory.full();
                        player.inventory.storage[0] = new ItemStack("wheat", 62, 64);
                        refund("wheat", "wheat", "pumpkin");
                        require(player.inventory.count("wheat") == 64, "Use partial matching stacks even without an empty slot");
                        require(pending().equals(List.of("pumpkin")) && message().endsWith("slots=1"), "Only remaining pumpkin needs a slot");

                        setup();
                        player.inventory.full();
                        player.inventory.storage[0] = new ItemStack("ordinary-wheat-without-generator-tag", 1, 64);
                        refund("wheat");
                        require(pending().size() == 1, "Do not merge different item metadata");

                        setup();
                        player.inventory.full();
                        manager.refundGenerators(player.uuid, Collections.nCopies(65, "wheat")); ExecutorManager.drain();
                        require(message().endsWith("slots=2"), "Sixty-five wheat require two stacks");
                        player.inventory.storage[0] = null; tick();
                        require(player.inventory.count("wheat") == 64 && pending().size() == 1, "Stack boundary partial delivery");

                        setup();
                        types.types.put("wheat", new Generator("wheat", "&eSmall Stack", 16));
                        player.inventory.full();
                        manager.refundGenerators(player.uuid, Collections.nCopies(17, "wheat")); ExecutorManager.drain();
                        require(message().endsWith("slots=2"), "Respect item custom stack limit");
                        player.inventory.storage[0] = null; tick();
                        require(player.inventory.count("wheat") == 16 && pending().size() == 1, "Custom stack limit delivery");

                        setup();
                        player.inventory.max = 1; player.inventory.full();
                        refund("wheat", "wheat", "pumpkin");
                        require(message().endsWith("slots=3"), "Respect inventory stack limit");

                        setup();
                        player.online = false;
                        manager.delayedGiveGeneratorItem(player.uuid, "wheat");
                        require(ExecutorManager.queue.isEmpty() && pending().size() == 1, "Offline API refund stays queued");
                        manager.stop(); manager.saveAll();
                        require(NextGens.DATA_CONFIG.saved.get("items." + player.uuid).equals(List.of("wheat")), "Old data format persisted");
                        NextGens.DATA_CONFIG.config.values.clear();
                        NextGens.DATA_CONFIG.config.values.putAll(NextGens.DATA_CONFIG.saved);
                        manager = new RefundManager(types, () -> now); manager.load(); manager.start();
                        player.online = true; player.inventory.full();
                        new com.muhammaddaffa.nextgens.refund.listeners.RefundListener(manager).onPlayerJoin(new org.bukkit.event.player.PlayerJoinEvent(player));
                        ExecutorManager.drain();
                        require(pending().size() == 1 && message().endsWith("slots=1"), "Login with full inventory retains saved refund");
                        player.inventory.storage[0] = null; tick();
                        require(player.inventory.count("wheat") == 1 && pending().isEmpty(), "Restored refund auto delivered");

                        setup();
                        manager.delayedGiveGeneratorItem(player.uuid, "wheat");
                        manager.giveItemOnJoin(player); manager.giveItemOnJoin(player);
                        require(ExecutorManager.queue.size() == 1, "Coalesce competing join and periodic checks");
                        player.online = false; ExecutorManager.drain();
                        require(pending().size() == 1 && player.inventory.count("wheat") == 0, "Disconnect before delivery retains refund");
                        player.online = true; tick();
                        require(player.inventory.count("wheat") == 1 && pending().isEmpty(), "Reconnect resumes delivery");

                        setup();
                        refund("missing", "wheat");
                        require(pending().equals(List.of("missing")) && player.inventory.count("wheat") == 1, "Missing generator definition does not erase refunds or block others");
                        types.types.put("missing", new Generator("missing", "&bRestored", 64)); tick();
                        require(pending().isEmpty() && player.inventory.count("missing") == 1, "Restored definitions resume delivery");

                        setup();
                        player.inventory.full(); refund("wheat");
                        NextGens.DEFAULT_CONFIG.message = "Pending: {generators}; free={slots}";
                        now += 60_000; tick();
                        require(message().equals("Pending: &f1 &eWheat Generator; free=1"), "Read reloaded reminder config");

                        setup(); player.inventory.full();
                        NextGens.DEFAULT_CONFIG.config.set("messages.pending-refund", null);
                        refund("wheat", "wheat", "pumpkin");
                        require(message().contains("2 &eWheat Generator") && message().contains("at least &f2"), "Old config with auto-update disabled uses default reminder");

                        setup();
                        manager.refundGenerators(player.uuid, Collections.nCopies(36 * 64 + 2, "wheat")); ExecutorManager.drain();
                        require(player.inventory.count("wheat") == 36 * 64 && pending().size() == 2, "Refund larger than inventory can progress");
                        player.inventory.storage[0] = null; tick();
                        require(pending().isEmpty(), "Large refund completes in batches");

                        setup();
                        manager.delayedGiveGeneratorItem(player.uuid, "wheat");
                        manager.stop(); ExecutorManager.drain();
                        require(pending().size() == 1 && player.inventory.count("wheat") == 0, "Stop fences queued deliveries");
                        require(ExecutorManager.provider.handle.cancelled, "Stop cancels poll task");

                        setup();
                        ExecutorManager.folia = true; player.inventory.requireOwner = true;
                        player.scheduler.reject = true; refund("wheat");
                        require(pending().size() == 1, "Retired scheduler rejection preserves refund");
                        player.scheduler.reject = false; player.scheduler.retire = true; tick();
                        require(pending().size() == 1, "Retirement callback releases pending task without delivery");
                        player.scheduler.retire = false; tick();
                        require(pending().isEmpty() && player.inventory.count("wheat") == 1, "Folia delivery uses owner scheduler and retries after retirement");

                        setup(); place(); player.online = false;
                        event(new com.muhammaddaffa.nextgens.hooks.bento.BentoListener(types, manager), "onTeamKick",
                                new world.bentobox.bentobox.api.events.team.TeamKickEvent(player.uuid));
                        require(pending().size() == 3, "Offline island refund is saved");
                        player.online = true; tick();
                        require(player.inventory.count("wheat") == 2 && player.inventory.count("pumpkin") == 1 && pending().isEmpty(), "Online recovery of offline island refund");
                        require(player.messages.isEmpty(), "No pending reminder after full delivery");

                        for (String method : List.of("onIslandKick", "onIslandLeave", "onIslandBan", "onIslandDisband")) {
                            setup(); player.inventory.full(); place();
                            var island = new com.bgsoftware.superiorskyblock.api.island.Island();
                            var superior = new com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer(player.uuid, island);
                            island.members.add(superior);
                            String eventName = switch (method) {
                                case "onIslandKick" -> "IslandKickEvent";
                                case "onIslandLeave" -> "IslandQuitEvent";
                                case "onIslandBan" -> "IslandBanEvent";
                                default -> "IslandDisbandEvent";
                            };
                            Object action = Class.forName("com.bgsoftware.superiorskyblock.api.events." + eventName)
                                    .getConstructors()[0].newInstance(superior, island);
                            var listener = new com.muhammaddaffa.nextgens.hooks.ssb2.SSB2Listener(types, manager);
                            event(listener, method, action);
                            require(pending().size() == 3 && types.getActiveGenerator(player.uuid).isEmpty(), "SSB2 " + method + " queues removed generators");
                            var annotation = listener.getClass().getDeclaredMethod(method, action.getClass()).getAnnotation(org.bukkit.event.EventHandler.class);
                            require(annotation.ignoreCancelled(), "Cancelled SSB2 action must be ignored");
                        }
                        for (String method : List.of("onTeamKick", "onTeamJoin", "onTeamLeave", "onIslandDelete")) {
                            setup(); player.inventory.full(); place();
                            Object action;
                            if (method.equals("onIslandDelete")) {
                                action = new world.bentobox.bentobox.api.events.island.IslandPreclearEvent(
                                        new world.bentobox.bentobox.api.events.island.IslandPreclearEvent.OldIsland(Set.of(player.uuid)));
                            } else {
                                action = Class.forName("world.bentobox.bentobox.api.events.team." + method.substring(2) + "Event")
                                        .getConstructors()[0].newInstance(player.uuid);
                            }
                            event(new com.muhammaddaffa.nextgens.hooks.bento.BentoListener(types, manager), method, action);
                            require(pending().size() == 3 && types.getActiveGenerator(player.uuid).isEmpty(), "Bento " + method + " queues removed generators");
                        }

                        setup(); place();
                        NextGens.DEFAULT_CONFIG.config.set("island-pickup", false);
                        event(new com.muhammaddaffa.nextgens.hooks.bento.BentoListener(types, manager), "onTeamKick",
                                new world.bentobox.bentobox.api.events.team.TeamKickEvent(player.uuid));
                        require(pending().isEmpty() && player.inventory.count("wheat") == 0, "Respect island-pickup false");

                        setup(); player.online = false;
                        List<Thread> threads = new ArrayList<>();
                        for (int i = 0; i < 8; i++) {
                            threads.add(Thread.startVirtualThread(() -> {
                                for (int j = 0; j < 10; j++) { manager.delayedGiveGeneratorItem(player.uuid, "wheat"); }
                            }));
                        }
                        for (Thread thread : threads) { thread.join(); }
                        require(pending().size() == 80, "Concurrent API refunds are not lost");
                        player.online = true; tick();
                        require(player.inventory.count("wheat") == 80 && pending().isEmpty(), "Concurrent refund batch delivered once");
                        manager.delayedGiveGeneratorItem(player.uuid, "pumpkin"); ExecutorManager.drain();
                        require(player.inventory.count("pumpkin") == 1 && pending().isEmpty(), "No old async deletion erases a new refund");
                        System.out.println("Refund checks passed: " + scenarios + " scenarios, " + assertions + " assertions.");
                    }
                }
                """);
        List<String> compilerArgs = new ArrayList<>(List.of("-d", output.toString()));
        for (var source : sources.entrySet()) {
            Path path = output.resolve(source.getKey().replace('.', '/') + ".java");
            Files.createDirectories(path.getParent());
            Files.writeString(path, source.getValue());
            compilerArgs.add(path.toString());
        }
        for (String source : List.of("refund/RefundManager.java", "refund/listeners/RefundListener.java",
                "hooks/ssb2/SSB2Listener.java", "hooks/bento/BentoListener.java")) {
            compilerArgs.add("src/main/java/com/muhammaddaffa/nextgens/" + source);
        }
        if (ToolProvider.getSystemJavaCompiler().run(null, null, null, compilerArgs.toArray(String[]::new)) != 0) {
            throw new AssertionError("Refund regression sources failed to compile");
        }
        try (var loader = new URLClassLoader(new java.net.URL[]{output.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            loader.loadClass("com.muhammaddaffa.nextgens.refund.Check").getMethod("main", String[].class).invoke(null, (Object) args);
        }
    }
}
