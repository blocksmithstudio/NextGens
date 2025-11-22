package com.muhammaddaffa.nextgens.generators.listeners;

import com.muhammaddaffa.mdlib.task.ExecutorManager;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.mdlib.xseries.particles.XParticle;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.api.events.generators.GeneratorBreakEvent;
import com.muhammaddaffa.nextgens.generators.ActiveGenerator;
import com.muhammaddaffa.nextgens.generators.Generator;
import com.muhammaddaffa.nextgens.generators.action.InteractAction;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import com.muhammaddaffa.nextgens.users.UserManager;
import com.muhammaddaffa.nextgens.users.models.User;
import com.muhammaddaffa.nextgens.utils.Utils;
import com.muhammaddaffa.nextgens.utils.VisualAction;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public record GeneratorBreakListener(
        GeneratorManager generatorManager,
        UserManager userManager
) implements Listener {

    private boolean getAnimationBreak() {
        return NextGens.DEFAULT_CONFIG.getConfig().getBoolean("animation-break");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void onBlockBreak(BlockBreakEvent event) {
        if (!getAnimationBreak() || NextGens.STOPPING) return;

        Player player = event.getPlayer();
        Block block = event.getBlock();
        ActiveGenerator active = this.generatorManager.getActiveGenerator(block);
        if (active == null) return;

        // Ownership/permission
        if (!isPlayerAllowedToBreak(active, player)) {
            notifyNotAllowed(player, active);
            event.setCancelled(true);
            return;
        }

        // Suppress vanilla drops/exp to avoid dupes
        event.setDropItems(false);
        event.setExpToDrop(0);

        // Unregister & custom drop next tick (ensures block is already gone)
        handleGeneratorBreak(active, player, block, NextGens.DEFAULT_CONFIG.getConfig(), event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void generatorBreak(PlayerInteractEvent event) {
        if (!isValidEvent(event) || getAnimationBreak()) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        FileConfiguration config = NextGens.DEFAULT_CONFIG.getConfig();

        ActiveGenerator active = this.generatorManager.getActiveGenerator(block);
        if (!isValidGenerator(active, event, config)) return;

        if (!isPlayerAllowedToBreak(active, player)) {
            notifyNotAllowed(player, active);
            return;
        }

        handleGeneratorBreak(active, player, block, config, event);
    }

    private boolean isValidEvent(PlayerInteractEvent event) {
        return event.getHand() == EquipmentSlot.HAND &&
                event.getClickedBlock() != null &&
                !NextGens.STOPPING;
    }

    private boolean isValidGenerator(ActiveGenerator active, PlayerInteractEvent event, FileConfiguration config) {
        if (active == null) return false;

        InteractAction action = InteractAction.find(event, InteractAction.LEFT);
        InteractAction required = InteractAction.find(config.getString("interaction.gens-pickup"), InteractAction.LEFT);

        return action == required;
    }

    private boolean isPlayerAllowedToBreak(ActiveGenerator active, Player player) {
        return active.getOwner().equals(player.getUniqueId()) ||
                this.userManager.getUser(active.getOwner()).isMember(player.getUniqueId()) ||
                player.hasPermission("nextgens.break.others");
    }

    private void notifyNotAllowed(Player player, ActiveGenerator active) {
        NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.not-owner");
        Utils.bassSound(player);
    }
    private void handleGeneratorBreak(ActiveGenerator active, Player player, Block block, FileConfiguration config, BlockBreakEvent event) {
        Generator generator = active.getGenerator();

        if (active.isCorrupted()) {
            notifyCorruptedGenerator(player);
            return;
        }

        GeneratorBreakEvent breakEvent = new GeneratorBreakEvent(generator, player);
        Bukkit.getPluginManager().callEvent(breakEvent);

        if (breakEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }

        unregisterAndDropGenerator(player, block, config, generator);
        playBreakVisuals(block, config, active, player);
    }

    private void handleGeneratorBreak(ActiveGenerator active, Player player, Block block, FileConfiguration config, PlayerInteractEvent event) {
        Generator generator = active.getGenerator();

        if (active.isCorrupted()) {
            notifyCorruptedGenerator(player);
            return;
        }

        GeneratorBreakEvent breakEvent = new GeneratorBreakEvent(generator, player);
        Bukkit.getPluginManager().callEvent(breakEvent);

        if (breakEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }

        unregisterAndDropGenerator(player, block, config, generator);
        playBreakVisuals(block, config, active, player);
    }

    private void notifyCorruptedGenerator(Player player) {
        NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.pickup-broken");
        Utils.bassSound(player);
    }

    private void unregisterAndDropGenerator(Player player, Block block, FileConfiguration config, Generator generator) {
        this.generatorManager.unregisterGenerator(block);
        block.setType(Material.AIR);

        if (config.getBoolean("drop-on-break")) {
            block.getWorld().dropItemNaturally(block.getLocation(), generator.createItem(1));
        } else {
            Common.addInventoryItem(player, generator.createItem(1));
        }
    }

    private void playBreakVisuals(Block block, FileConfiguration config, ActiveGenerator active, Player player) {
        Player owner = Bukkit.getPlayer(active.getOwner());
        if (owner != null) {
            VisualAction.send(owner, config, "generator-break-options", new Placeholder()
                    .add("{gen}", active.getGenerator().displayName())
                    .add("{current}", this.generatorManager.getGeneratorCount(owner))
                    .add("{max}", this.userManager.getMaxSlot(owner)));
        }

        if (config.getBoolean("generator-break-options.particles")) {
            ExecutorManager.getProvider().async(() -> block.getWorld().spawnParticle(XParticle.CLOUD.get(), block.getLocation().add(0.5, 0, 0.5), 30, 0.25, 0.25, 0.25, 3));
        }
    }

}
