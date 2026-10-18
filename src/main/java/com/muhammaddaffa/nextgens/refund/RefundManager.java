package com.muhammaddaffa.nextgens.refund;

import com.muhammaddaffa.mdlib.task.ExecutorManager;
import com.muhammaddaffa.mdlib.task.handleTask.HandleTask;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Config;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.generators.Generator;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;

public class RefundManager {

    private static final String DEFAULT_REMINDER = "&6[NextGens] &aYou have pending generators to be given to you: &f{generators}&r&a, free up at least &f{slots} &aslots in your inventory to receive the generators.";

    private final Map<UUID, List<String>> itemMap = new HashMap<>();
    private final Map<UUID, Long> nextReminder = new HashMap<>();
    private final Set<UUID> scheduled = new HashSet<>();
    private final Set<String> missingGenerators = new HashSet<>();
    private final GeneratorManager generatorManager;
    private final LongSupplier clock;
    private HandleTask task;
    private boolean stopped;

    public RefundManager(GeneratorManager generatorManager) {
        this(generatorManager, System::currentTimeMillis);
    }

    RefundManager(GeneratorManager generatorManager, LongSupplier clock) {
        this.generatorManager = generatorManager;
        this.clock = clock;
    }

    public void delayedGiveGeneratorItem(UUID uuid, String id) {
        refundGenerators(uuid, List.of(id));
    }

    public synchronized void refundGenerators(UUID uuid, List<String> ids) {
        if (ids.isEmpty()) {
            return;
        }
        itemMap.computeIfAbsent(uuid, key -> new ArrayList<>()).addAll(ids);
        savePlayerData(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            giveItemOnJoin(player);
        }
    }

    public synchronized void start() {
        if (task != null) {
            return;
        }
        stopped = false;
        task = ExecutorManager.getProvider().syncTimer(1L, 20L, this::checkOnlinePlayers);
    }

    public synchronized void stop() {
        stopped = true;
        if (task != null) {
            task.cancel();
            task = null;
        }
        scheduled.clear();
        nextReminder.clear();
    }

    private synchronized void checkOnlinePlayers() {
        for (UUID uuid : List.copyOf(itemMap.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                giveItemOnJoin(player);
            }
        }
    }

    public synchronized void giveItemOnJoin(Player player) {
        UUID uuid = player.getUniqueId();
        if (stopped || !itemMap.containsKey(uuid) || !scheduled.add(uuid)) {
            return;
        }
        Runnable delivery = () -> {
            try {
                deliver(player);
            } finally {
                finishDelivery(uuid);
            }
        };
        try {
            if (ExecutorManager.isFolia()) {
                Object scheduler = player.getClass().getMethod("getScheduler").invoke(player);
                boolean accepted = (boolean) Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler")
                        .getMethod("execute", Plugin.class, Runnable.class, Runnable.class, long.class)
                        .invoke(scheduler, NextGens.getInstance(), delivery, (Runnable) () -> finishDelivery(uuid), 1L);
                if (!accepted) {
                    finishDelivery(uuid);
                }
            } else {
                ExecutorManager.getProvider().sync(delivery);
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            finishDelivery(uuid);
            NextGens.getInstance().getLogger().warning("Could not schedule generator refunds for " + uuid + ": " + exception);
        }
    }

    private synchronized void finishDelivery(UUID uuid) {
        scheduled.remove(uuid);
    }

    private synchronized void deliver(Player player) {
        UUID uuid = player.getUniqueId();
        List<String> pending = itemMap.get(uuid);
        if (stopped || !player.isOnline() || pending == null || pending.isEmpty()) {
            return;
        }
        Map<String, Integer> remaining = new LinkedHashMap<>();
        pending.forEach(id -> remaining.merge(id, 1, Integer::sum));
        List<String> names = new ArrayList<>();
        int slots = 0;
        boolean changed = false;
        try {
            for (Map.Entry<String, Integer> entry : remaining.entrySet()) {
                String id = entry.getKey();
                Generator generator = generatorManager.getGenerator(id);
                if (generator == null) {
                    if (missingGenerators.add(id)) {
                        NextGens.getInstance().getLogger().warning("Generator with ID '" + id + "' not found; keeping its refunds pending.");
                    }
                    continue;
                }
                missingGenerators.remove(id);
                int amount = entry.getValue();
                ItemStack stack = generator.createItem(amount);
                int stackLimit = Math.max(1, Math.min(stack.getMaxStackSize(), player.getInventory().getMaxStackSize()));
                int leftover = player.getInventory().addItem(stack).values().stream()
                        .mapToInt(ItemStack::getAmount).sum();
                entry.setValue(leftover);
                changed |= leftover != amount;
                if (leftover > 0) {
                    names.add("&f" + leftover + " " + generator.displayName());
                    slots += (leftover - 1) / stackLimit + 1;
                }
            }
        } finally {
            if (changed) {
                List<String> retained = new ArrayList<>();
                remaining.forEach((id, amount) -> retained.addAll(Collections.nCopies(amount, id)));
                if (retained.isEmpty()) {
                    itemMap.remove(uuid);
                    nextReminder.remove(uuid);
                } else {
                    itemMap.put(uuid, retained);
                }
                savePlayerData(uuid);
            }
        }
        long now = clock.getAsLong();
        if (!names.isEmpty() && now >= nextReminder.getOrDefault(uuid, 0L)) {
            nextReminder.put(uuid, now + 60_000L);
            Placeholder placeholders = new Placeholder()
                    .add("{generators}", String.join("&r&f, ", names))
                    .add("{slots}", slots);
            if (NextGens.DEFAULT_CONFIG.getConfig().contains("messages.pending-refund")) {
                NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.pending-refund", placeholders);
            } else {
                Common.sendMessage(player, DEFAULT_REMINDER, placeholders);
            }
        }
    }

    public synchronized void load() {
        FileConfiguration config = NextGens.DATA_CONFIG.getConfig();
        if (!config.isConfigurationSection("items")) {
            return;
        }
        for (String uuidString : config.getConfigurationSection("items").getKeys(false)) {
            UUID uuid = UUID.fromString(uuidString);
            List<String> generators = config.getStringList("items." + uuidString);
            if (!generators.isEmpty()) {
                itemMap.put(uuid, new ArrayList<>(generators));
            }
        }
    }

    public synchronized void saveAll() {
        Config data = NextGens.DATA_CONFIG;
        FileConfiguration config = data.getConfig();
        config.set("items", null);
        itemMap.forEach((uuid, generators) -> config.set("items." + uuid, new ArrayList<>(generators)));
        data.saveConfig();
    }

    private void savePlayerData(UUID uuid) {
        Config data = NextGens.DATA_CONFIG;
        List<String> generators = itemMap.get(uuid);
        data.getConfig().set("items." + uuid, generators == null ? null : new ArrayList<>(generators));
        data.saveConfig();
    }
}
