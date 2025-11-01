package com.muhammaddaffa.nextgens.sell;

import com.muhammaddaffa.mdlib.hooks.VaultEconomy;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.api.GeneratorAPI;
import com.muhammaddaffa.nextgens.api.events.sell.SellCommandUseEvent;
import com.muhammaddaffa.nextgens.api.events.sell.SellEvent;
import com.muhammaddaffa.nextgens.api.events.sell.SellwandUseEvent;
import com.muhammaddaffa.nextgens.events.managers.EventManager;
import com.muhammaddaffa.nextgens.sellwand.models.SellwandData;
import com.muhammaddaffa.nextgens.users.models.User;
import com.muhammaddaffa.nextgens.users.UserManager;
import com.muhammaddaffa.nextgens.utils.SellData;
import com.muhammaddaffa.nextgens.utils.Utils;
import com.muhammaddaffa.nextgens.utils.VisualAction;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class SellManager {

    private final UserManager userManager;
    private final EventManager eventManager;

    public SellManager(UserManager userManager, EventManager eventManager) {
        this.userManager = userManager;
        this.eventManager = eventManager;
    }

    public boolean sell(Player player, ItemStack stack) {
        GeneratorAPI api = NextGens.getApi();
        User user = userManager.getUser(player);
        Double value = api.getWorth(stack);
        if (value == null) return false;
        // Sell the item
        VaultEconomy.deposit(player, value);
        // Update statistics
        user.addEarnings(value);
        user.addItemsSold(stack.getAmount());
        // Remove the item
        stack.setAmount(0);
        return true;
    }

    public SellData performSell(Player player, SellwandData sellwand, Inventory... inventories) {
        return performSell(player, sellwand, false, inventories);
    }

    public SellData performSell(Player player, SellwandData sellwand, boolean silent, Inventory... inventories) {
        GeneratorAPI api = NextGens.getApi();
        double totalValue = 0.0;
        int totalItems = 0;

        // Aggregate sellable items
        for (Inventory inventory : inventories) {
            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                Double worth = api.getWorth(stack);
                // If item is worthless, just skip it
                if (worth == null || worth <= 0) {
                    continue;
                }
                // Otherwise, we should add it
                totalItems += stack.getAmount();
                totalValue += worth;
                // Remove the item completely, double remove to make sure
                stack.setAmount(0);
                inventory.setItem(i, null);
            }
        }

        if (totalItems == 0) {
            if (!silent) {
                NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.no-sell");
                Utils.bassSound(player);
            }
            return null;
        }

        User user = userManager.getUser(player);
        SellData sellData = SellDataCalculator.calculateSellData(player, user, sellwand, totalValue, totalItems);

        SellEvent sellEvent = (sellwand != null) ? new SellwandUseEvent(player, user, sellData)
                : new SellCommandUseEvent(player, user, sellData);

        // Call the event
        Bukkit.getPluginManager().callEvent(sellEvent);
        if (sellEvent.isCancelled()) return null;

        SellData data = sellEvent.getSellData();
        VaultEconomy.deposit(player, data.getTotalValue());

        if (!silent) {
            sendSellVisual(player, data);
        }

        // Update user statistics
        user.addEarnings(data.getTotalValue());
        user.addItemsSold(data.getTotalItems());
        if (sellwand != null) {
            user.addSellwandSell(1);
        } else {
            user.addNormalSell(1);
        }

        // Save user data asynchronously
        Executor.async(() -> NextGens.getInstance().getUserRepository().saveUser(user));

        return data;
    }

    private void sendSellVisual(Player player, SellData data) {
        FileConfiguration config = NextGens.DEFAULT_CONFIG.getConfig();
        VisualAction.send(player, config, "sell-options", new Placeholder()
                .add("{amount}", Common.digits(data.getTotalItems()))
                .add("{amount_formatted}", Utils.formatBalance(data.getTotalItems()))
                .add("{value}", Common.digits(data.getTotalValue()))
                .add("{value_formatted}", Utils.formatBalance((long) data.getTotalValue()))
                .add("{multiplier}", Common.digits(data.getMultiplier())));
    }

}
