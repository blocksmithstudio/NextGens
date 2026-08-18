package com.muhammaddaffa.nextgens.utils;

import com.muhammaddaffa.mdlib.hooks.VaultEconomy;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.mdlib.xseries.XSound;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.api.events.PlayerCashbackEvent;
import com.muhammaddaffa.nextgens.users.UserManager;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.List;

public class Utils {

    private static final FormatBalance formatBalance = new FormatBalance();

    public static ItemStack parsePlaceholderAPI(ItemStack stack) {
        if (stack == null || stack.getItemMeta() == null) {
            return stack;
        }
        ItemMeta meta = stack.getItemMeta();
        // Parse the display name
        if (meta.getDisplayName() != null && !meta.getDisplayName().isEmpty()) {
            meta.setDisplayName(PlaceholderAPI.setPlaceholders(null, meta.getDisplayName()));
        }
        if (meta.getLore() != null && !meta.getLore().isEmpty()) {
            meta.setLore(PlaceholderAPI.setPlaceholders(null, meta.getLore()));
        }
        // Set the item meta
        stack.setItemMeta(meta);
        // Return the item
        return stack;
    }

    public static int[] convertListToIntArray(List<Integer> list) {
        // Create an int array of the same size as the list
        int[] array = new int[list.size()];

        // Populate the int array with values from the list
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i); // Auto-unboxing from Integer to int
        }

        return array;
    }

    public static Inventory replicateInventoryRemoveLastRow(Inventory inventory) {
        // create another inventory
        Inventory cloned = Bukkit.createInventory(null, inventory.getSize() - 9);
        for (int i = 0; i < cloned.getSize(); i++) {
            // clone the item
            cloned.setItem(i, inventory.getItem(i));
        }
        return cloned;
    }

    public static boolean isSimilar(ItemStack one, ItemStack two) {
        if (one == null || two == null) return false;
        ItemMeta oneMeta = one.getItemMeta();
        ItemMeta twoMeta = two.getItemMeta();

        if (one.getType() == two.getType() &&
                (oneMeta != null && twoMeta != null) &&
                oneMeta.getDisplayName().equals(twoMeta.getDisplayName())) {

            if (oneMeta.getLore() == null && twoMeta.getLore() == null) {
                return true;
            }
            return oneMeta.getLore().equals(twoMeta.getLore());
        }
        return false;
    }

    public static void performCashback(Player player, UserManager userManager, double amount) {
        // get the cashback for player
        int cashback = Utils.getCashback(player);
        // if cashback is 0 or below, skip
        if (cashback <= 0) {
            return;
        }
        // call the custom event
        PlayerCashbackEvent cashbackEvent = new PlayerCashbackEvent(player, userManager.getUser(player), cashback);
        Bukkit.getPluginManager().callEvent(cashbackEvent);
        // check if event is cancelled
        if (cashbackEvent.isCancelled()) {
            return;
        }
        // get the cashback amount
        double refund = ((amount * cashbackEvent.getPercentage()) / 100);
        // give back the money to the player
        EconomySelector.deposit(player, refund);
        // send the message only if player has notify on
        if (userManager.getUser(player).isToggleCashback()) {
            NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.cashback", new Placeholder()
                    .add("{amount}", Common.digits(refund))
                    .add("{amount_formatted}", Utils.formatBalance((long) refund))
                    .add("{percentage}", Common.digits(cashback)));
        }
    }

    public static int getCashback(Player player) {
        int cashback = 0;
        for (PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
            String permission = pai.getPermission();
            if (!permission.startsWith("nextgens.cashback.")) {
                continue;
            }
            int current = Integer.parseInt(permission.split("\\.")[2]);
            if (current > cashback) {
                cashback = current;
            }
        }
        return cashback;
    }

    public static String formatBalance(long value) {
        return formatBalance.format(value);
    }

    public static void bassSound(Player player) {
        player.playSound(player.getLocation(), XSound.BLOCK_NOTE_BLOCK_BASS.get(), 1.0f, 2.0f);
    }

}
