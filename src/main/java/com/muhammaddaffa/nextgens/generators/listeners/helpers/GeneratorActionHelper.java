package com.muhammaddaffa.nextgens.generators.listeners.helpers;

import com.muhammaddaffa.mdlib.hooks.VaultEconomy;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.generators.ActiveGenerator;
import com.muhammaddaffa.nextgens.generators.Generator;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import com.muhammaddaffa.nextgens.users.models.User;
import com.muhammaddaffa.nextgens.utils.Utils;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

final class GeneratorActionHelper {

    // Economy and event callbacks may recursively invoke another action on the same generator.
    private static final Set<ActiveGenerator> pending = ConcurrentHashMap.newKeySet();

    static boolean reserve(ActiveGenerator active) {
        return active != null && pending.add(active);
    }

    static void release(ActiveGenerator active) {
        pending.remove(active);
    }

    static boolean current(GeneratorManager manager, ActiveGenerator active, Generator generator, boolean corrupted) {
        return !NextGens.STOPPING && active != null && generator != null
                && manager.getActiveGenerator(active.getLocation()) == active
                && active.getGenerator() == generator && active.isCorrupted() == corrupted;
    }

    static boolean hasAccess(Player player, ActiveGenerator active) {
        if (active.getOwner().equals(player.getUniqueId())) return true;
        User owner = NextGens.getInstance().getUserManager().getUser(active.getOwner());
        return owner != null && owner.isMember(player.getUniqueId());
    }

    static boolean canRepair(Player player, ActiveGenerator active) {
        return !NextGens.DEFAULT_CONFIG.getConfig().getBoolean("repair-owner-only") || hasAccess(player, active);
    }

    static boolean withdraw(Player player, double cost, BooleanSupplier current) {
        if (!Double.isFinite(cost) || cost < 0) return failed(player);
        try {
            double balance = VaultEconomy.getBalance(player);
            if (!Double.isFinite(balance)) return failed(player);
            if (balance < cost) {
                NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.not-enough-money", new Placeholder()
                        .add("{money}", Common.digits(balance))
                        .add("{upgradecost}", Common.digits(cost))
                        .add("{remaining}", Common.digits(balance - cost)));
                Utils.bassSound(player);
                return false;
            }
            if (!current.getAsBoolean()) return false;
            if (cost == 0) return true;
            EconomyResponse response = VaultEconomy.withdraw(player, cost);
            if (response == null || !response.transactionSuccess()) return failed(player);
        } catch (RuntimeException exception) {
            return failed(player);
        }
        // A provider callback can remove the generator or revoke access while taking payment.
        if (current.getAsBoolean()) return true;
        try {
            EconomyResponse refund = VaultEconomy.deposit(player, cost);
            if (refund == null || !refund.transactionSuccess()) {
                NextGens.getInstance().getLogger().severe("A stale generator action could not refund its payment.");
            }
        } catch (RuntimeException exception) {
            NextGens.getInstance().getLogger().severe("A stale generator action could not refund its payment.");
        }
        return failed(player);
    }

    private static boolean failed(Player player) {
        NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.transaction-failed");
        return false;
    }
}
