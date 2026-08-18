package com.muhammaddaffa.nextgens.utils;

import com.muhammaddaffa.mdlib.hooks.VaultEconomy;
import com.muhammaddaffa.nextgens.NextGens;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import su.nightexpress.excellenteconomy.api.ExcellentEconomyAPI;
import su.nightexpress.excellenteconomy.api.currency.ExcellentCurrency;

public class EconomySelector {

    private static ExcellentEconomyAPI api;
    private static ExcellentCurrency currency;

    public static void init() {
        RegisteredServiceProvider<ExcellentEconomyAPI> provider = Bukkit.getServer().getServicesManager().getRegistration(ExcellentEconomyAPI.class);
        if (provider != null) {
            api = provider.getProvider();
            // Initialize the currency
            String id = currencyId();
            if (id != null) {
                currency = api.getCurrency(id);
            }
        }
    }

    public static void deposit(Player player, double amount) {
        if (currency != null) {
            api.deposit(player, currency, amount);
        } else {
            VaultEconomy.deposit(player, amount);
        }
    }

    public static void withdraw(Player player, double amount) {
        if (currency != null) {
            api.withdraw(player, currency, amount);
        } else {
            VaultEconomy.withdraw(player, amount);
        }
    }

    public static double getBalance(Player player) {
        if (currency != null) {
            return api.getBalance(player, currency);
        } else {
            return VaultEconomy.getBalance(player);
        }
    }

    private static String currencyId() {
        return NextGens.DEFAULT_CONFIG.getString("currency");
    }

}
