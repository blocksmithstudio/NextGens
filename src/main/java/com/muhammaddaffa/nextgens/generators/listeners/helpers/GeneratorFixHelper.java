package com.muhammaddaffa.nextgens.generators.listeners.helpers;

import com.muhammaddaffa.mdlib.task.ExecutorManager;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.generators.ActiveGenerator;
import com.muhammaddaffa.nextgens.generators.Generator;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import com.muhammaddaffa.nextgens.utils.Utils;
import com.muhammaddaffa.nextgens.utils.VisualAction;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class GeneratorFixHelper {

    public static void fixGenerator(Player player, ActiveGenerator active, Generator generator) {
        GeneratorManager manager = NextGens.getInstance().getGeneratorManager();
        synchronized (manager) {
            if (!GeneratorActionHelper.reserve(active)) return;
            try {
                if (!canRepair(player, manager, active, generator)) return;
                if (!GeneratorActionHelper.withdraw(player, generator.fixCost(),
                        () -> canRepair(player, manager, active, generator))) return;
                active.setCorrupted(false);
                manager.saveActiveGenerator(active);
            } finally {
                GeneratorActionHelper.release(active);
            }
        }
        Block block = active.getLocation().getBlock();
        // visual actions
        VisualAction.send(player, NextGens.DEFAULT_CONFIG.getConfig(), "corrupt-fix-options", new Placeholder()
                .add("{gen}", generator.displayName())
                .add("{cost}", Common.digits(generator.fixCost())));
        // play particle
        ExecutorManager.getProvider().async(() -> {
            if (NextGens.DEFAULT_CONFIG.getConfig().getBoolean("corrupt-fix-options.particles")) {
                GeneratorParticle.successParticle(block, generator);
            }
        });
        // give cashback to the player
        Utils.performCashback(player, NextGens.getInstance().getUserManager(), generator.fixCost());
    }

    public static void fixGenerators(Player player, GeneratorManager generatorManager) {
        synchronized (generatorManager) {
            if (!player.hasPermission("nextgens.repairgens") || NextGens.STOPPING) return;
            List<ActiveGenerator> corrupted = generatorManager.getActiveGenerator(player).stream()
                    .filter(ActiveGenerator::isCorrupted)
                    .toList();
            if (corrupted.isEmpty()) {
                NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.fix-empty");
                return;
            }
            List<Generator> tiers = corrupted.stream().map(ActiveGenerator::getGenerator).toList();
            List<ActiveGenerator> reserved = new ArrayList<>();
            try {
                for (ActiveGenerator active : corrupted) {
                    if (!GeneratorActionHelper.reserve(active)) return;
                    reserved.add(active);
                }
                double cost = 0;
                for (Generator generator : tiers) {
                    if (!Double.isFinite(generator.fixCost()) || generator.fixCost() < 0) {
                        NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.transaction-failed");
                        return;
                    }
                    cost += generator.fixCost();
                }
                if (!GeneratorActionHelper.withdraw(player, cost,
                        () -> canRepairAll(player, generatorManager, corrupted, tiers))) return;
                corrupted.forEach(active -> {
                    active.setCorrupted(false);
                    generatorManager.saveActiveGenerator(active);
                });
                NextGens.DEFAULT_CONFIG.sendMessage(player, "messages.fix-all");
                Utils.performCashback(player, NextGens.getInstance().getUserManager(), cost);
            } finally {
                reserved.forEach(GeneratorActionHelper::release);
            }
        }
    }

    private static boolean canRepair(Player player, GeneratorManager manager, ActiveGenerator active, Generator generator) {
        return GeneratorActionHelper.current(manager, active, generator, true)
                && GeneratorActionHelper.canRepair(player, active);
    }

    private static boolean canRepairAll(Player player, GeneratorManager manager, List<ActiveGenerator> active, List<Generator> tiers) {
        if (!player.hasPermission("nextgens.repairgens")) return false;
        for (int i = 0; i < active.size(); i++) {
            if (!active.get(i).getOwner().equals(player.getUniqueId())
                    || !canRepair(player, manager, active.get(i), tiers.get(i))) return false;
        }
        return true;
    }

}
