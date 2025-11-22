package com.muhammaddaffa.nextgens.generators.runnables;

import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Config;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.mdlib.xseries.XSound;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.generators.ActiveGenerator;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import com.muhammaddaffa.nextgens.utils.GensRunnable;
import com.muhammaddaffa.nextgens.utils.Settings;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class NotifyTask extends GensRunnable {

    private static NotifyTask currentTask;
    private final GeneratorManager generatorManager;

    private NotifyTask(GeneratorManager generatorManager) {
        this.generatorManager = generatorManager;
    }

    public static void start(GeneratorManager generatorManager) {
        if (currentTask != null) {
            currentTask.cancel();
        }

        currentTask = new NotifyTask(generatorManager);
        long intervalTicks = TimeUnit.MINUTES.toSeconds(Settings.CORRUPTION_NOTIFY_INTERVAL) * 20L;
        currentTask.runTaskTimerAsynchronously(NextGens.getInstance(), intervalTicks, intervalTicks);
    }

    @Override
    public void run() {
        if (!Settings.CORRUPTION_ENABLED) {
            return;
        }

        Map<UUID, Integer> corruptedGeneratorCount = collectCorruptedGenerators();
        notifyPlayers(corruptedGeneratorCount);
    }

    private Map<UUID, Integer> collectCorruptedGenerators() {
        Map<UUID, Integer> corruptedCount = new HashMap<>();

        for (ActiveGenerator generator : generatorManager.getActiveGenerator()) {
            if (generator.isCorrupted()) {
                corruptedCount.merge(generator.getOwner(), 1, Integer::sum);
            }
        }

        return corruptedCount;
    }

    private void notifyPlayers(Map<UUID, Integer> corruptedGeneratorCount) {
        corruptedGeneratorCount.forEach((owner, count) -> {
            Player player = Bukkit.getPlayer(owner);

            if (player != null) {
                sendNotification(player, count);
            }
        });
    }

    private void sendNotification(Player player, int corruptedCount) {
        Settings.CORRUPTION_NOTIFY_MESSAGE.send(player, new Placeholder().add("{amount}", Common.digits(corruptedCount)));
        player.playSound(player.getLocation(), XSound.BLOCK_NOTE_BLOCK_PLING.get(), 1.0f, 1.0f);
    }

}
