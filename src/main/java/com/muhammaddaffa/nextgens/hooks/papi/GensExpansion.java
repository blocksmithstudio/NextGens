package com.muhammaddaffa.nextgens.hooks.papi;

import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.TimeUtils;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.events.Event;
import com.muhammaddaffa.nextgens.events.managers.EventManager;
import com.muhammaddaffa.nextgens.generators.ActiveGenerator;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import com.muhammaddaffa.nextgens.generators.runnables.CorruptionTask;
import com.muhammaddaffa.nextgens.sell.multipliers.SellMultiplierProvider;
import com.muhammaddaffa.nextgens.sell.multipliers.SellMultiplierRegistry;
import com.muhammaddaffa.nextgens.users.models.User;
import com.muhammaddaffa.nextgens.users.UserManager;
import com.muhammaddaffa.nextgens.utils.Utils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GensExpansion extends PlaceholderExpansion {

    private final GeneratorManager generatorManager;
    private final UserManager userManager;
    private final EventManager eventManager;
    private final SellMultiplierRegistry registry;

    public GensExpansion(GeneratorManager generatorManager, UserManager userManager, EventManager eventManager, SellMultiplierRegistry registry) {
        this.generatorManager = generatorManager;
        this.userManager = userManager;
        this.eventManager = eventManager;
        this.registry = registry;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "nextgens";
    }

    @Override
    public @NotNull String getAuthor() {
        return "aglerr";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true; // This is required or else PlaceholderAPI will unregister the Expansion on reload
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {

        final User user = this.userManager.getUser(player);


        if (params.equalsIgnoreCase("cashback")) {
            return Common.digits(Utils.getCashback(player));
        }
        if (params.equalsIgnoreCase("statistics_totalsell_formatted")) {
            return Utils.formatBalance(user.getTotalSell());
        }
        if (params.equalsIgnoreCase("statistics_totalsell")) {
            return Common.digits(user.getTotalSell());
        }
        if (params.equalsIgnoreCase("statistics_sellwandsell_formatted")) {
            return Utils.formatBalance(user.getSellwandSell());
        }
        if (params.equalsIgnoreCase("statistics_sellwandsell")) {
            return Common.digits(user.getSellwandSell());
        }
        if (params.equalsIgnoreCase("statistics_commandsell_formatted")) {
            return Utils.formatBalance(user.getNormalSell());
        }
        if (params.equalsIgnoreCase("statistics_commandsell")) {
            return Common.digits(user.getNormalSell());
        }
        if (params.equalsIgnoreCase("statistics_itemsold_formatted")) {
            return Utils.formatBalance(user.getItemsSold());
        }
        if (params.equalsIgnoreCase("statistics_itemsold")) {
            return Common.digits(user.getItemsSold());
        }
        if (params.equalsIgnoreCase("statistics_earnings_formatted")) {
            return Utils.formatBalance((long) user.getEarnings());
        }
        if (params.equalsIgnoreCase("statistics_earnings_string")) {
            try {
                String earnings = Common.digits(user.getEarnings());
                earnings = earnings.replace(",", "").replace(".", ",");
                return String.valueOf((int) Double.parseDouble(earnings));
            } catch (NumberFormatException e) {
                // Handle the exception or return a default value, e.g., "0"
                return "0";
            }
        }
        if (params.equalsIgnoreCase("statistics_earnings")) {
            return Common.digits(user.getEarnings());
        }
        if (params.equalsIgnoreCase("multiplier")) {
            double multiplier = 0.0;

            for (SellMultiplierProvider provider : registry.getMultipliers()) {
                multiplier += provider.getMultiplier(player, user, null);
            }

            return Common.digits(multiplier);
        }
        if (params.equalsIgnoreCase("currentplaced")) {
            return Common.digits(this.generatorManager.getGeneratorCount(player));
        }
        if (params.equalsIgnoreCase("max")) {
            return Common.digits(this.userManager.getMaxSlot(player));
        }
        if (params.equalsIgnoreCase("total_generator")) {
            return Common.digits(this.generatorManager.getActiveGenerator().size());
        }
        if (params.equalsIgnoreCase("corrupt_time")) {
            return TimeUtils.format(CorruptionTask.getTimeLeft());
        }
        if (params.equalsIgnoreCase("event_name")) {
            Event event = this.eventManager.getActiveEvent();
            if (event == null) {
                return NextGens.EVENTS_CONFIG.getConfig().getString("events.placeholders.no-event");
            }
            return NextGens.EVENTS_CONFIG.getConfig().getString("events.placeholders.active-event")
                    .replace("{display_name}", event.getDisplayName());
        }
        if (params.equalsIgnoreCase("event_time")) {
            Event event = this.eventManager.getActiveEvent();
            if (event == null) {
                return NextGens.EVENTS_CONFIG.getConfig().getString("events.placeholders.no-event-timer")
                        .replace("{timer}", TimeUtils.format((long) this.eventManager.getWaitTime()));
            }
            return NextGens.EVENTS_CONFIG.getConfig().getString("events.placeholders.active-event-timer")
                    .replace("{timer}", TimeUtils.format((long) event.getDuration()));
        }

        if (params.equalsIgnoreCase("event_isactive")) {
            return this.eventManager.getActiveEvent() == null ?
                    "false" : "true";
        }

        if (params.startsWith("statistics_gens_")) {
            String generatorName = params.substring("statistics_gens_".length());

            List<ActiveGenerator> generators = new ArrayList<>();
            for (ActiveGenerator active : this.generatorManager.getActiveGenerator(player)) {
                if (active.getGenerator().id().equalsIgnoreCase(generatorName)) {
                    generators.add(active);
                }
            }

            return Common.digits(generators.size());
        }

        return null; // Placeholder is unknown by the Expansion
    }

}
