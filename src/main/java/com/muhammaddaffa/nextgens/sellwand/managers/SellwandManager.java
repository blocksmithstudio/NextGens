package com.muhammaddaffa.nextgens.sellwand.managers;

import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.ItemBuilder;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.mdlib.xseries.XSound;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.sellwand.models.SellwandData;
import com.muhammaddaffa.nextgens.users.UserManager;
import com.muhammaddaffa.nextgens.utils.SellData;
import com.muhammaddaffa.nextgens.utils.Utils;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class SellwandManager {

    public String getUsesPlaceholder(int uses) {
        if (uses == -1) {
            return NextGens.DEFAULT_CONFIG.getConfig().getString("sellwand.unlimited-placeholder");
        }
        return Common.digits(uses);
    }

    public boolean isSellwand(ItemStack stack) {
        if (stack == null || stack.getItemMeta() == null) return false;
        return stack.getItemMeta().getPersistentDataContainer().has(NextGens.sellwand_global, PersistentDataType.STRING);
    }

    public boolean action(Player player, ItemStack stack, Inventory... inventories) {
        return action(player, stack, null, inventories);
    }

    public boolean action(Player player, ItemStack stack, @Nullable Block block, Inventory... inventories) {
        // Extract data from the item
        ItemBuilder builder = new ItemBuilder(stack);
        ItemMeta meta = builder.getItemMeta();

        Double multiplier = meta.getPersistentDataContainer().get(NextGens.sellwand_multiplier, PersistentDataType.DOUBLE);
        Integer uses = meta.getPersistentDataContainer().get(NextGens.sellwand_uses, PersistentDataType.INTEGER);
        Double totalSold = meta.getPersistentDataContainer().get(NextGens.sellwand_total_sold, PersistentDataType.DOUBLE);
        Integer totalItemsSold = meta.getPersistentDataContainer().get(NextGens.sellwand_total_items, PersistentDataType.INTEGER);

        // Validate data
        if (multiplier == null || uses == null || totalSold == null || totalItemsSold == null) {
            return false;
        }

        // Perform the sell
        SellwandData sellwandData = new SellwandData(stack, multiplier);
        SellData data = NextGens.getInstance().getSellManager().performSell(player, sellwandData, block, inventories);
        if (data == null) return true;

        // Update uses
        int finalUses = uses - 1;

        if (finalUses == 0) {
            // Destroy the item
            stack.setAmount(0);
            NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.sellwand-broke");
            player.playSound(player.getLocation(), XSound.ENTITY_ITEM_BREAK.get(), 1.0f, 1.0f);
            return true;
        }

        // Update the Persistent Data Container
        if (uses > 0) {
            builder.pdc(NextGens.sellwand_uses, finalUses);
        }
        builder.pdc(NextGens.sellwand_total_sold, totalSold + data.getTotalValue());
        builder.pdc(NextGens.sellwand_total_items, totalItemsSold + data.getTotalItems());

        // Update the item
        update(builder.build());
        return true;
    }

    public void update(ItemStack stack) {
        if (stack == null) return;

        FileConfiguration config = NextGens.DEFAULT_CONFIG.getConfig();
        ItemBuilder builder = new ItemBuilder(stack);
        ItemMeta meta = builder.getItemMeta();

        Double multiplier = meta.getPersistentDataContainer().get(NextGens.sellwand_multiplier, PersistentDataType.DOUBLE);
        Integer uses = meta.getPersistentDataContainer().get(NextGens.sellwand_uses, PersistentDataType.INTEGER);
        Double totalSold = meta.getPersistentDataContainer().get(NextGens.sellwand_total_sold, PersistentDataType.DOUBLE);
        Integer totalItemsSold = meta.getPersistentDataContainer().get(NextGens.sellwand_total_items, PersistentDataType.INTEGER);

        if (multiplier == null || uses == null || totalSold == null || totalItemsSold == null) {
            return;
        }

        // Set display name and lore
        String displayName = config.getString("sellwand.item.display-name");
        List<String> lore = config.getStringList("sellwand.item.lore");

        // Parse the placeholder api
        displayName = PlaceholderAPI.setPlaceholders(null, displayName);
        lore = PlaceholderAPI.setPlaceholders(null, lore);

        builder.name(displayName);
        builder.lore(lore);
        builder.placeholder(new Placeholder()
                .add("{multiplier}", Common.digits(multiplier))
                .add("{uses}", getUsesPlaceholder(uses))
                .add("{total_sold}", Common.digits(totalSold))
                .add("{total_sold_formatted}", Utils.formatBalance(totalSold.longValue()))
                .add("{total_items}", Common.digits(totalItemsSold))
                .add("{total_items_formatted}", Utils.formatBalance(totalItemsSold)));

        builder.build();
    }

    public ItemStack create(double multiplier, int uses) {
        return ItemBuilder.fromConfig(NextGens.DEFAULT_CONFIG.getConfig(), "sellwand.item")
                .pdc(NextGens.sellwand_global, UUID.randomUUID().toString())
                .pdc(NextGens.sellwand_multiplier, multiplier)
                .pdc(NextGens.sellwand_uses, uses)
                .pdc(NextGens.sellwand_total_sold, 0D)
                .pdc(NextGens.sellwand_total_items, 0)
                .placeholder(new Placeholder()
                        .add("{multiplier}", Common.digits(multiplier))
                        .add("{uses}", getUsesPlaceholder(uses))
                        .add("{total_sold}", Common.digits(0))
                        .add("{total_items}", Common.digits(0)))
                .build();
    }

}
