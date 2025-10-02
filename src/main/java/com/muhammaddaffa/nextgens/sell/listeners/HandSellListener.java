package com.muhammaddaffa.nextgens.sell.listeners;

import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.generators.action.InteractAction;
import com.muhammaddaffa.nextgens.sell.SellManager;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public record HandSellListener(SellManager sellManager) implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        FileConfiguration config = NextGens.DEFAULT_CONFIG.getConfig();

        if (!config.getBoolean("hand-sell.enabled")) return;

        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        Inventory inventory = player.getInventory();

        ItemStack stack = event.getItem();
        if (stack == null || stack.getType() == Material.AIR) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;

        // check if it's a drop from generator
        if (!meta.getPersistentDataContainer().has(NextGens.drop_value, PersistentDataType.DOUBLE)) {
            return;
        }

        // check if it's a valid generator
        if (!isValidGenerator(event, config)) return;
        // cancel the event
        event.setCancelled(true);
        // sell the item
        sellManager.performSell(player, null, inventory);
    }

    private boolean isValidGenerator(PlayerInteractEvent event, FileConfiguration config) {
        InteractAction action = InteractAction.find(event, InteractAction.RIGHT);
        InteractAction required = InteractAction.find(config.getString("hand-sell.interaction"), InteractAction.RIGHT);

        return action == required;
    }
}
