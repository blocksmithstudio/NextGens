package com.muhammaddaffa.nextgens.gui;

import com.muhammaddaffa.mdlib.fastinv.FastInv;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.ItemBuilder;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.generators.ActiveGenerator;
import com.muhammaddaffa.nextgens.generators.Generator;
import com.muhammaddaffa.nextgens.generators.listeners.helpers.GeneratorUpdateHelper;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import com.muhammaddaffa.nextgens.gui.helpers.Pagination;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class UpgradeGensInventory extends FastInv {

    private static final Map<UUID, UpgradeTask> UPGRADE_RUNNERS = new ConcurrentHashMap<>();

    private final Player player;

    private Pagination<MergedGenerator> pagination;

    public UpgradeGensInventory(Player player) {
        super(NextGens.UPGRADE_GENS_GUI_CONFIG.getInt("size"),
                Common.color(NextGens.UPGRADE_GENS_GUI_CONFIG.getString("title")));
        this.player = player;

        // Stop the task on close
        this.addCloseHandler(event -> {
            this.cancelTask();
        });

        this.setAllItems();
    }

    private void setAdditionalItems() {
        FileConfiguration config = NextGens.UPGRADE_GENS_GUI_CONFIG.getConfig();
        if (!config.isConfigurationSection("items")) {
            return;
        }
        for (String key : config.getConfigurationSection("items").getKeys(false)) {
            String type = config.getString("items." + key + ".type", "DUMMY");
            List<Integer> slots = config.getIntegerList("items." + key + ".slots");
            ItemStack stack = ItemBuilder.fromConfig(config, "items." + key)
                    .flags(ItemFlag.values())
                    .build();
            // Set the items
            this.setItems(slots, stack, event -> {
                if (type.equalsIgnoreCase("next_page")) {
                    if (this.pagination.hasNextPage()) {
                        this.pagination.currentPage++;
                        this.setAllItems();
                    }
                }

                if (type.equalsIgnoreCase("previous_page")) {
                    if (this.pagination.hasPreviousPage()) {
                        this.pagination.currentPage--;
                        this.setAllItems();
                    }
                }
            });
        }
    }

    public void setAllItems() {
        // Clear the gui
        this.clearItems();

        // Set additional items
        this.setAdditionalItems();

        // Get the merged generator
        List<MergedGenerator> generated = MergedGenerator.generate(player);
        List<Integer> slots = NextGens.UPGRADE_GENS_GUI_CONFIG.getIntegerList("item-slots");
        int currentPage = this.pagination != null ? this.pagination.currentPage : 1;

        this.pagination = new Pagination<>(generated, slots);
        if (this.pagination.getItems(currentPage) == null) {
            this.pagination.currentPage = 1;
        }

        List<MergedGenerator> items = this.pagination.getItems();
        if (items != null && !items.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                MergedGenerator merged = items.get(i);
                Integer slot = slots.get(i);

                this.setItem(slot, merged.item(), event -> {
                    switch (event.getClick()) {
                        case LEFT -> this.leftClick(merged);
                        case RIGHT -> this.rightClick(merged, slot);
                    }
                });
            }
        }
    }

    private void leftClick(MergedGenerator merged) {
        // Upgrade only one generator
        ActiveGenerator active = merged.list().get(0);
        if (GeneratorUpdateHelper.upgradeGenerator(player, active, true)) {
            // Update the menu if upgrade successful
            this.setAllItems();
        }
    }

    private void rightClick(MergedGenerator merged, int slot) {
        // Toggle off if user right-clicks again
        this.cancelTaskAndRefresh();

        if (merged == null || merged.list() == null || merged.list().isEmpty()) return;

        final int upgradeSpeed = Math.max(1, NextGens.UPGRADE_GENS_GUI_CONFIG.getInt("upgrade-speed", 5));
        final boolean stopWhenFinished = NextGens.UPGRADE_GENS_GUI_CONFIG.getBoolean("stop-when-finished");

        // Create and start the task
        UpgradeTask task = new UpgradeTask(player, merged, stopWhenFinished, slot, this);
        task.runTaskTimer(NextGens.getInstance(), 0, upgradeSpeed);
        // Put the task on the hash map
        UPGRADE_RUNNERS.put(player.getUniqueId(), task);
    }

    public void cancelTaskAndRefresh() {
        cancelTask();
        // Update the UI
        setAllItems();
    }

    public void cancelTask() {
        UpgradeTask task = UPGRADE_RUNNERS.remove(player.getUniqueId());
        if (task != null) {
            // Cancel the task
            task.cancel();
            // Send the total spent message

            double totalSpent = task.getTotalSpent();
            int successfulUpgrade = task.getSuccessfulUpgrade();
            Generator generator = task.getMergedGenerator().generator();
            Generator nextGenerator = NextGens.getInstance().getGeneratorManager().getGenerator(generator.nextTier());
            String generatorDisplayName = nextGenerator == null ? "&cUnknown" : nextGenerator.displayName();

            if (totalSpent > 0) {
                if (player.isOnline()) {
                    NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.upgrade-total-spent", new Placeholder()
                            .add("{amount}", Common.digits(totalSpent))
                            .add("{amount_short}", Common.format(totalSpent))
                            .add("{successful_upgrade}", Common.digits(successfulUpgrade))
                            .add("{successful_upgrade_short}", Common.format(successfulUpgrade))
                            .add("{generator}", generatorDisplayName));
                }
            }
        }
    }

    public ItemStack getProgressItem(int upgraded, int total) {
        return ItemBuilder.fromConfig(NextGens.UPGRADE_GENS_GUI_CONFIG, "upgrade-progress")
                .placeholder(new Placeholder()
                        .add("{upgraded}", Common.digits(upgraded))
                        .add("{total}", Common.digits(total)))
                .flags(ItemFlag.values())
                .build();
    }

    private static class UpgradeTask extends BukkitRunnable {

        final List<ActiveGenerator> targets;
        final int total;
        final int[] attempts = {0};   // how many generators we've attempted this session
        final int[] successes = {0};
        final int[] idx = {0};

        private final Player player;
        private final MergedGenerator merged;
        private final boolean stopWhenFinished;
        private final UpgradeGensInventory gui;
        private final int slot;

        private double totalSpent;

        private UpgradeTask(Player player, MergedGenerator merged, boolean stopWhenFinished,int slot, UpgradeGensInventory gui) {
            this.targets = merged.list();
            this.total = targets.size();

            this.player = player;
            this.merged = merged;
            this.stopWhenFinished = stopWhenFinished;
            this.slot = slot;
            this.gui = gui;
        }

        @Override
        public void run() {
            // Player offline/invalid => stop
            if (!player.isOnline() || !player.isValid()) {
                gui.cancelTask();
                return;
            }

            if (targets.isEmpty()) {
                gui.cancelTaskAndRefresh();
                return;
            }

            ActiveGenerator target = targets.get(idx[0] % targets.size());
            double cost = target.getGenerator().cost();

            idx[0]++;
            attempts[0]++;

            // Attempt upgrade
            boolean upgraded = GeneratorUpdateHelper.upgradeGenerator(player, target, true);
            if (upgraded) {
                totalSpent += cost;
            } else {
                // Stop immediately when upgrade says "no" (e.g., maxed or ran out of money)
                gui.cancelTaskAndRefresh();
                return;
            }

            // Successful attempt
            successes[0]++;

            // Update the progress
            gui.setItem(slot, gui.getProgressItem(successes[0], total));

            // Stop after we've processed each generator once
            if (stopWhenFinished && attempts[0] >= total) {
                gui.cancelTaskAndRefresh();
            }
        }

        public MergedGenerator getMergedGenerator() {
            return merged;
        }

        public double getTotalSpent() {
            return totalSpent;
        }

        public int getSuccessfulUpgrade() {
            return successes[0];
        }

    }

    private record MergedGenerator(
            Generator generator,
            List<ActiveGenerator> list
    ) {

        public ItemStack item() {
            // Create the placeholder
            Placeholder placeholder = new Placeholder()
                    .add("{amount}", Common.digits(amount()))
                    .add("{amount_short}", Common.format(amount()))
                    .add("{cost}", Common.digits(cost()))
                    .add("{cost_short}", Common.format(cost()))
                    .add("{total_cost}", Common.digits(totalCost()))
                    .add("{total_cost_short}", Common.format(totalCost()));

            int amount = Math.min(amount(), 64);

            // Create the item set up the lore
            return new ItemBuilder(generator.item().clone())
                    .amount(amount)
                    .lore(NextGens.UPGRADE_GENS_GUI_CONFIG.getStringList("lore"))
                    .placeholder(placeholder)
                    .flags(ItemFlag.values())
                    .build();
        }

        public double cost() {
            return generator.cost();
        }

        public double totalCost() {
            return cost() * amount();
        }

        public int amount() {
            return list.size();
        }

        // ------------- STATIC METHODS ------------- //

        @NotNull
        public static List<MergedGenerator> generate(Player player) {
            GeneratorManager manager = NextGens.getInstance().getGeneratorManager();
            // Let's group the active generator based on the generator type.
            List<ActiveGenerator> actives = manager.getActiveGenerator(player);

            Map<String, List<ActiveGenerator>> grouped = new LinkedHashMap<>();

            for (ActiveGenerator active : actives) {
                // Skip if generator is invalid
                if (active == null || active.getLocation() == null) continue;
                if (active.isCorrupted()) continue;

                Generator generator = active.getGenerator();
                if (generator != null) {
                    grouped.computeIfAbsent(generator.id(), k -> new ArrayList<>()).add(active);
                }
            }

            if (grouped.isEmpty()) {
                return List.of();
            }

            return grouped.values().stream()
                    .filter(list -> !list.isEmpty())
                    .map(list -> new MergedGenerator(list.get(0).getGenerator(), List.copyOf(list)))
                    .collect(Collectors.toList());
        }

    }

}
