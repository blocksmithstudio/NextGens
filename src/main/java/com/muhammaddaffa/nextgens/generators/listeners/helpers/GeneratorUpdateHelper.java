package com.muhammaddaffa.nextgens.generators.listeners.helpers;

import com.muhammaddaffa.mdlib.task.ExecutorManager;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.api.events.generators.GeneratorUpgradeEvent;
import com.muhammaddaffa.nextgens.generators.ActiveGenerator;
import com.muhammaddaffa.nextgens.generators.Generator;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import com.muhammaddaffa.nextgens.utils.Utils;
import com.muhammaddaffa.nextgens.utils.VisualAction;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

public class GeneratorUpdateHelper {

    public static boolean upgradeGenerator(Player player, ActiveGenerator active) {
        return upgradeGenerator(player, active, false);
    }

    public static boolean upgradeGenerator(Player player, ActiveGenerator active, boolean silent) {
        if (active == null) return false;
        Generator generator = active.getGenerator();
        if (generator == null) return false;
        Generator nextGenerator = NextGens.getInstance().getGeneratorManager().getGenerator(generator.nextTier());
        // Try to upgrade
        return upgradeGenerator(player, active, generator, nextGenerator, silent);
    }

    public static boolean upgradeGenerator(Player player, ActiveGenerator active, Generator generator, Generator nextGenerator) {
        return upgradeGenerator(player, active, generator, nextGenerator, false);
    }

    public static boolean upgradeGenerator(Player player, ActiveGenerator active, Generator generator, Generator nextGenerator, boolean silent) {
        GeneratorManager manager = NextGens.getInstance().getGeneratorManager();
        // ponytail: serialize registry actions; use per-location locks if this becomes a bottleneck.
        synchronized (manager) {
            if (!GeneratorActionHelper.reserve(active)) return false;
            try {
                if (!canUpgrade(player, manager, active, generator, nextGenerator)) return false;
                if (nextGenerator == null) {
                    NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.no-upgrade");
                    Utils.bassSound(player);
                    return false;
                }
                List<String> requirementsNotPassed = generator.checkRequirements(player, generator.upgradeRequirements());
                if (!requirementsNotPassed.isEmpty()) {
                    Common.sendMessage(player, requirementsNotPassed);
                    Utils.bassSound(player);
                    return false;
                }
                GeneratorUpgradeEvent upgradeEvent = new GeneratorUpgradeEvent(generator, player, nextGenerator);
                Bukkit.getPluginManager().callEvent(upgradeEvent);
                if (upgradeEvent.isCancelled()
                        || !canUpgrade(player, manager, active, generator, nextGenerator)) return false;
                if (!generator.checkRequirements(player, generator.upgradeRequirements()).isEmpty()) return false;
                if (!GeneratorActionHelper.withdraw(player, generator.cost(),
                        () -> canUpgrade(player, manager, active, generator, nextGenerator))) return false;
                // Preserve the owner when a trusted member upgrades an existing generator.
                manager.registerGenerator(active.getOwner(), nextGenerator, active.getLocation().getBlock());
            } finally {
                GeneratorActionHelper.release(active);
            }
        }
        Block block = active.getLocation().getBlock();
        // visual actions
        FileConfiguration config = NextGens.DEFAULT_CONFIG.getConfig();
        if (!silent) {
            VisualAction.send(player, config, "generator-upgrade-options", new Placeholder()
                    .add("{previous}", generator.displayName())
                    .add("{current}", nextGenerator.displayName())
                    .add("{cost}", Common.digits(generator.cost())));
        }
        // play particle
        ExecutorManager.getProvider().async(() -> {
            if (NextGens.DEFAULT_CONFIG.getConfig().getBoolean("generator-upgrade-options.particles")) {
                GeneratorParticle.successParticle(block, generator);
            }
        });
        // give cashback to the player
        Utils.performCashback(player, NextGens.getInstance().getUserManager(), generator.cost());
        return true;
    }

    private static boolean canUpgrade(Player player, GeneratorManager manager, ActiveGenerator active,
                                      Generator generator, Generator nextGenerator) {
        return GeneratorActionHelper.current(manager, active, generator, false)
                && GeneratorActionHelper.hasAccess(player, active)
                && manager.getGenerator(generator.nextTier()) == nextGenerator;
    }

}
