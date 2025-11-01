package com.muhammaddaffa.nextgens.sellwand.listeners;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;
import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.sellwand.managers.SellwandManager;
import com.muhammaddaffa.nextgens.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.popcraft.bolt.BoltAPI;
import us.lynuxcraft.deadsilenceiv.advancedchests.AdvancedChestsAPI;
import us.lynuxcraft.deadsilenceiv.advancedchests.chest.AdvancedChest;
import us.lynuxcraft.deadsilenceiv.advancedchests.utils.inventory.InteractiveInventory;
import world.bentobox.bentobox.BentoBox;

import java.util.List;
import java.util.Optional;

public class SellwandListener implements Listener {

    private final SellwandManager sellwandManager;

    public SellwandListener(SellwandManager sellwandManager) {
        this.sellwandManager = sellwandManager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        ItemStack stack = event.getItem();

        if (block == null || !sellwandManager.isSellwand(stack)) {
            return;
        }

        event.setCancelled(true);

        // Check if player is in the same chunk
        if (NextGens.DEFAULT_CONFIG.getBoolean("sellwand.force-same-chunk")) {
            Chunk playerChunk = player.getLocation().getChunk();
            Chunk blockChunk = block.getChunk();
            // If it's the same we should return
            if (!playerChunk.equals(blockChunk)) {
                NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.sellwand-same-chunk");
                return;
            }
        }

        // Advanced Chests API Hook
        if (Bukkit.getPluginManager().isPluginEnabled("AdvancedChests")) {
            AdvancedChest<?, ?> advancedChest = AdvancedChestsAPI.getChestManager().getAdvancedChest(block.getLocation());
            if (advancedChest != null) {
                if (!advancedChest.getWhoPlaced().equals(player.getUniqueId())) {
                    boolean hasAccess = this.hasAccess(player, block);
                    boolean memberSell = NextGens.DEFAULT_CONFIG.getBoolean("advancedchests-member-sell");

                    // If the player doesn't have access or member selling is disallowed, abort
                    if (!hasAccess || !memberSell) {
                        NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.sellwand-failed");
                        Utils.bassSound(player);
                        return;
                    }
                }

                List<Inventory> inventories = advancedChest.getPages().values()
                        .stream()
                        .map(InteractiveInventory::getBukkitInventory)
                        .toList();

                sellwandManager.action(player, stack, inventories.toArray(new Inventory[0]));
                return;
            }
        }

        // Normal Container
        if (block.getState() instanceof Container container) {
            if (!hasAccess(player, block)) {
                NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.sellwand-failed");
                Utils.bassSound(player);
                return;
            }
            sellwandManager.action(player, stack, container.getInventory());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLoseDurability(PlayerItemDamageEvent event) {
        if (!sellwandManager.isSellwand(event.getItem())) return;
        event.setCancelled(true);
    }

    private boolean hasAccess(Player player, Block block) {
        // Bolt Check
        if (Bukkit.getPluginManager().isPluginEnabled("Bolt")) {
            BoltAPI bolt = NextGens.getInstance().getBoltAPI();
            return bolt.canAccess(block, player, "interact");
        }

        // LWC Check
        if (Bukkit.getPluginManager().isPluginEnabled("LWC")) {
            LWC lwc = ((LWCPlugin) Bukkit.getPluginManager().getPlugin("LWC")).getLWC();
            if (!lwc.canAccessProtection(player, block)) {
                return false;
            }
        }

        // SuperiorSkyblock Check
        if (Bukkit.getPluginManager().isPluginEnabled("SuperiorSkyblock2")) {
            SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(player);
            Island playerIsland = superiorPlayer.getIsland();
            Island islandAt = SuperiorSkyblockAPI.getIslandAt(block.getLocation());

            if (playerIsland == null || islandAt == null) {
                return false;
            }
            return playerIsland.getUniqueId().equals(islandAt.getUniqueId());
        }

        // BentoBox Check
        if (Bukkit.getPluginManager().isPluginEnabled("BentoBox")) {
            Optional<world.bentobox.bentobox.database.objects.Island> islandAt =
                    BentoBox.getInstance().getIslandsManager().getIslandAt(block.getLocation());

            if (islandAt.isEmpty()) {
                return false;
            }
            world.bentobox.bentobox.database.objects.Island island = islandAt.get();
            return island.getMemberSet().contains(player.getUniqueId());
        }

        return true;
    }

}
