package com.muhammaddaffa.nextgens.generators.listeners;

import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.generators.ActiveGenerator;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import com.muhammaddaffa.nextgens.utils.Utils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public record GeneratorPreventionListener(
        GeneratorManager generatorManager
) implements Listener {

    @EventHandler
    private void onBucketPlace(PlayerBucketEmptyEvent event) {
        Block clicked = event.getBlockClicked();
        Block target = event.getBlock();
        Block relative = clicked.getRelative(event.getBlockFace());

        if (isGeneratorBlock(clicked) ||
        isGeneratorBlock(target) ||
        isGeneratorBlock(relative)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onWaterFlow(BlockFromToEvent event) {
        if (event.getBlock().getType() != Material.WATER && event.getBlock().getType() != Material.WATER_CAULDRON) return;

        if (this.generatorManager.getActiveGenerator(event.getToBlock()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPhysicBlock(BlockPhysicsEvent event) {
        if (this.generatorManager.getActiveGenerator(event.getBlock()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onFallingBlock(EntityChangeBlockEvent event) {
        // check if it's not falling block then return
        if (!(event.getEntity() instanceof FallingBlock)) return;
        // if it's generator then cancel
        if (this.generatorManager.getActiveGenerator(event.getBlock()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onDecay(LeavesDecayEvent event) {
        if (this.generatorManager.getActiveGenerator(event.getBlock()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onBlockFade(BlockFadeEvent event) {
        if (this.generatorManager.getActiveGenerator(event.getBlock()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onGrindstoneUse(PrepareGrindstoneEvent event) {
        // Check if one of the item is a generator item
        for (ItemStack stack : event.getInventory().getContents()) {
            if (this.generatorManager.isGeneratorItem(stack)) {
                // Set the result to null
                event.setResult(null);
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void prevention(CraftItemEvent event) {
        FileConfiguration config = NextGens.DEFAULT_CONFIG.getConfig();
        // if it's not enabled, skip it
        if (!config.getBoolean("disable-crafting.enabled")) {
            return;
        }
        String type = config.getString("disable-crafting.type", "ALL");
        switch (type.toUpperCase()) {
            case "ALL" -> {
                // Check if one of the item is a generator item
                for (ItemStack stack : event.getInventory().getContents()) {
                    if (this.generatorManager.isGeneratorItem(stack) || this.generatorManager.isDropItem(stack)) {
                        event.setCancelled(true);
                        // send message
                        NextGens.DEFAULT_CONFIG.sendMessage(event.getWhoClicked(), "disable-crafting.message");
                        return;
                    }
                }
            }
            case "DROP" -> {
                // Check if one of the item is a generator item
                for (ItemStack stack : event.getInventory().getContents()) {
                    if (this.generatorManager.isDropItem(stack)) {
                        event.setCancelled(true);
                        // send message
                        NextGens.DEFAULT_CONFIG.sendMessage(event.getWhoClicked(), "disable-crafting.message");
                        return;
                    }
                }
            }
            case "GENERATOR" -> {
                // Check if one of the item is a generator item
                for (ItemStack stack : event.getInventory().getContents()) {
                    if (this.generatorManager.isGeneratorItem(stack)) {
                        event.setCancelled(true);
                        // send message
                        NextGens.DEFAULT_CONFIG.sendMessage(event.getWhoClicked(), "disable-crafting.message");
                        return;
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void prevention(BlockBreakEvent event) {
        // get all variables we want
        Block block = event.getBlock();
        ActiveGenerator active = this.generatorManager.getActiveGenerator(block);
        // cancel if the block is active generator
        if (active != null) {
            event.setCancelled(true);
            // play bass sound
            Utils.bassSound(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void prevention(EntityExplodeEvent event) {
        this.checkExplosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void prevention(BlockExplodeEvent event) {
        this.checkExplosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void prevention(BlockPlaceEvent event) {
        ItemStack hand = event.getItemInHand();
        // check if requirements are correct
        if (NextGens.DEFAULT_CONFIG.getConfig().getBoolean("disable-drop-place") && this.generatorManager.isDropItem(hand)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void prevention(BlockPistonExtendEvent event) {
        this.checkPiston(event, event.getBlocks());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void prevention(BlockPistonRetractEvent event) {
        this.checkPiston(event, event.getBlocks());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void prevention(SpongeAbsorbEvent event) {
        ActiveGenerator active = this.generatorManager.getActiveGenerator(event.getBlock());
        if (active != null) event.setCancelled(true);
    }

    private void checkPiston(BlockPistonEvent event, List<Block> blocks) {
        for (Block block : blocks) {
            // get active generator from block
            ActiveGenerator active = this.generatorManager.getActiveGenerator(block);
            if (active != null) {
                event.setCancelled(true);
            }
        }
    }

    private void checkExplosion(List<Block> blocks) {
        if (NextGens.DEFAULT_CONFIG.getConfig().getBoolean("anti-explosion")) {
            // remove generator blocks
            blocks.removeIf(block -> this.generatorManager.getActiveGenerator(block) != null);
        } else {
            // if option is disabled, unregister active generator
            for (Block block : blocks) {
                ActiveGenerator active = this.generatorManager.getActiveGenerator(block);
                if (active != null) {
                    this.generatorManager.unregisterGenerator(active.getLocation());
                }
            }
        }
    }

    /**
     * In API versions 1.20.6 and earlier, InventoryView is a class.
     * In versions 1.21 and later, it is an interface.
     * This method uses reflection to get the top Inventory object from the
     * InventoryView associated with an InventoryEvent, to avoid runtime errors.
     * @param event The generic InventoryEvent with an InventoryView to inspect.
     * @return The top Inventory object from the event's InventoryView.
     */
    private Inventory getTopInventory(InventoryEvent event) {
        try {
            Object view = event.getView();
            Method getTopInventory = view.getClass().getMethod("getTopInventory");
            getTopInventory.setAccessible(true);
            return (Inventory) getTopInventory.invoke(view);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isGeneratorBlock(Block block) {
        return block != null && this.generatorManager.getActiveGenerator(block) != null;
    }

}
