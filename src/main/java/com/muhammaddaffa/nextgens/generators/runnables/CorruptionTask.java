package com.muhammaddaffa.nextgens.generators.runnables;

import com.muhammaddaffa.mdlib.task.ExecutorManager;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Config;
import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.api.events.generators.GeneratorCorruptedEvent;
import com.muhammaddaffa.nextgens.generators.ActiveGenerator;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import com.muhammaddaffa.nextgens.utils.GensRunnable;
import com.muhammaddaffa.nextgens.utils.Settings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CorruptionTask extends GensRunnable {

    private static CorruptionTask runnable;

    public static void start(GeneratorManager generatorManager) {
        if (runnable != null) {
            runnable.cancel();
            runnable = null;
        }
        // set back the runnable
        runnable = new CorruptionTask(generatorManager);
        // run the task
        runnable.runTaskTimerAsynchronously(NextGens.getInstance(), 20L, 20L);
    }

    public static int getTimeLeft() {
        if (runnable == null) {
            return -1;
        }
        return runnable.getCorruptionTime() - runnable.getTimer();
    }

    public static CorruptionTask getInstance() {
        return runnable;
    }

    private final GeneratorManager generatorManager;
    public CorruptionTask(GeneratorManager generatorManager) {
        this.generatorManager = generatorManager;
    }

    private int timer;

    @Override
    public void run() {
        // if corruption is disabled, skip this
        if (!Settings.CORRUPTION_ENABLED) {
            return;
        }
        // increase the timer
        this.timer++;
        // check if the timer exceed the interval
        if (this.timer >= this.getCorruptionTime()) {
            // set the timer back to 0
            this.timer = 0;
            // corrupt the generators
            this.corruptGenerators();
        }
    }

    public void corruptGenerators() {
        // get possibly infected generators
        AtomicInteger actuallyCorrupted = new AtomicInteger();
        for (ActiveGenerator active : this.getPossiblyInfectedGenerators()) {
            // check for chances
            if (ThreadLocalRandom.current().nextDouble(101) <= active.getGenerator().corruptChance()) {
                // must run in a sync task
                ExecutorManager.getProvider().sync(() -> {
                    // get Player
                    Player player = Bukkit.getPlayer(active.getOwner());
                    // check for online-only option
                    if (Settings.CORRUPTION_ONLINE_ONLY && player == null) return;
                    // call the event, and check for cancelled
                    GeneratorCorruptedEvent corruptedEvent = new GeneratorCorruptedEvent(active.getGenerator(), active);
                    Bukkit.getPluginManager().callEvent(corruptedEvent);
                    if (!corruptedEvent.isCancelled()) {
                        // actually set the generator to be corrupted
                        active.setCorrupted(true);
                        // increment the counter
                        actuallyCorrupted.getAndIncrement();
                        // Save the generator
                        this.generatorManager.saveActiveGenerator(active);
                    }
                });
            }
        }
        // broadcast the corrupt event
        if (actuallyCorrupted.get() > 0) {
            Settings.CORRUPTION_BROADCAST.broadcast(new Placeholder()
                    .add("{amount}", actuallyCorrupted.get()));
        }
    }

    private List<ActiveGenerator> getPossiblyInfectedGenerators() {
        List<ActiveGenerator> activeGenerators = this.generatorManager.getActiveGenerator()
                .stream()
                .filter(active -> !active.isCorrupted())
                .toList();
        int percentage = Math.max(0, Math.min(100, Settings.CORRUPTION_PERCENTAGE));
        int totalInfected = (int) ((long) activeGenerators.size() * percentage / 100);
        List<String> blacklisted = Settings.CORRUPTION_BLACKLISTED_GENERATORS;
        List<ActiveGenerator> eligible = new ArrayList<>();
        for (ActiveGenerator active : activeGenerators) {
            if (active.getGenerator() != null && !blacklisted.contains(active.getGenerator().id())) {
                eligible.add(active);
            }
        }
        Collections.shuffle(eligible);
        return eligible.subList(0, Math.min(totalInfected, eligible.size()));
    }

    public int getTimer() {
        return timer;
    }

    public int getCorruptionTime() {
        return (int) TimeUnit.MINUTES.toSeconds(Settings.CORRUPTION_INTERVAL);
    }

}
