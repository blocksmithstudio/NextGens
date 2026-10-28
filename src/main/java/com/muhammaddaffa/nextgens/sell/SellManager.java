package com.muhammaddaffa.nextgens.sell;

import com.muhammaddaffa.mdlib.hooks.VaultEconomy;
import com.muhammaddaffa.mdlib.task.ExecutorManager;
import com.muhammaddaffa.mdlib.utils.Common;
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
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import net.milkbowl.vault.economy.EconomyResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SellManager {

    private final UserManager userManager;
    private final EventManager eventManager;
    private final Set<Inventory> activeSales = ConcurrentHashMap.newKeySet();

    public SellManager(UserManager userManager, EventManager eventManager) {
        this.userManager = userManager;
        this.eventManager = eventManager;
    }

    public boolean sell(Player player, ItemStack stack) {
        GeneratorAPI api = NextGens.getApi();
        User user = userManager.getUser(player);
        Double value = api.getWorth(stack);
        if (value == null || !Double.isFinite(value) || value <= 0) return false;
        // Leave the drop available when the economy provider rejects the payment.
        try {
            EconomyResponse response = VaultEconomy.deposit(player, value);
            if (response == null || !response.transactionSuccess()) return false;
        } catch (RuntimeException exception) {
            return false;
        }
        // Update statistics
        user.addEarnings(value);
        user.addItemsSold(stack.getAmount());
        // Remove the item
        stack.setAmount(0);
        return true;
    }

    public SellData performSell(Player player, SellwandData sellwand, Inventory... inventories) {
        return performSell(player, sellwand, false, null, inventories);
    }

    public SellData performSell(Player player, SellwandData sellwand, boolean silent, Inventory... inventories) {
        return performSell(player, sellwand, silent, null, inventories);
    }

    public SellData performSell(Player player, SellwandData sellwand, @Nullable Block block, Inventory... inventories) {
        return performSell(player, sellwand, false, block, inventories);
    }

    public SellData performSell(Player player, SellwandData sellwand, boolean silent, @Nullable Block block, Inventory... inventories) {
        List<Inventory> reserved = new ArrayList<>();
        try {
            // Event and economy callbacks must not sell the same inventory recursively.
            for (Inventory inventory : new LinkedHashSet<>(Arrays.asList(inventories))) {
                if (inventory == null || !activeSales.add(inventory)) return null;
                reserved.add(inventory);
            }
            return sellInventories(player, sellwand, silent, block, reserved);
        } finally {
            reserved.forEach(activeSales::remove);
        }
    }

    private SellData sellInventories(Player player, SellwandData sellwand, boolean silent, @Nullable Block block, List<Inventory> inventories) {
        GeneratorAPI api = NextGens.getApi();
        List<SaleItem> items = new ArrayList<>();
        double totalValue = 0.0;
        int totalItems = 0;

        // Aggregate sellable items
        for (Inventory inventory : inventories) {
            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                Double worth = api.getWorth(stack);
                // If item is worthless, just skip it
                if (worth == null || !Double.isFinite(worth) || worth <= 0) {
                    continue;
                }
                // Otherwise, we should add it
                totalItems += stack.getAmount();
                totalValue += worth;
                items.add(new SaleItem(inventory, i, stack.clone()));
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

        SellEvent sellEvent = (sellwand != null) ? new SellwandUseEvent(player, user, sellData, block)
                : new SellCommandUseEvent(player, user, sellData);

        // Call the event
        Bukkit.getPluginManager().callEvent(sellEvent);
        if (sellEvent.isCancelled()) return null;

        SellData data = sellEvent.getSellData();
        if (data == null || !Double.isFinite(data.getTotalValue()) || data.getTotalValue() < 0) return null;
        // A listener may have changed the inventory while handling the event.
        for (SaleItem item : items) {
            if (!item.stack().equals(item.inventory().getItem(item.slot()))) return null;
        }

        boolean paid = false;
        try {
            for (SaleItem item : items) {
                item.inventory().setItem(item.slot(), null);
            }
            EconomyResponse response = VaultEconomy.deposit(player, data.getTotalValue());
            paid = response != null && response.transactionSuccess();
            if (!paid) {
                if (!silent) NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.transaction-failed");
                return null;
            }
        } catch (RuntimeException exception) {
            if (!silent) NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.transaction-failed");
            return null;
        } finally {
            if (!paid) {
                for (SaleItem item : items) {
                    item.inventory().setItem(item.slot(), item.stack());
                }
            }
        }

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
        ExecutorManager.getProvider().async(() -> NextGens.getInstance().getUserRepository().saveUser(user));

        return data;
    }

    private record SaleItem(Inventory inventory, int slot, ItemStack stack) { }

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
