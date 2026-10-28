package com.muhammaddaffa.nextgens.hooks.ssb2;

import com.bgsoftware.superiorskyblock.api.events.*;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.generators.ActiveGenerator;
import com.muhammaddaffa.nextgens.generators.Generator;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import com.muhammaddaffa.nextgens.refund.RefundManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

public record SSB2Listener(
        GeneratorManager generatorManager,
        RefundManager refundManager
) implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onIslandDisband(IslandDisbandEvent event) {
        event.getIsland().getIslandMembers(true).forEach(this::check);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onIslandKick(IslandKickEvent event) {
        this.check(event.getTarget());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onIslandLeave(IslandQuitEvent event) {
        this.check(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onIslandBan(IslandBanEvent event) {
        SuperiorPlayer player = event.getTarget();
        Island island = event.getIsland();
        Island playerIsland = player.getIsland();
        if (playerIsland == null) return;

        if (island.getUniqueId().equals(playerIsland.getUniqueId())) {
            this.check(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerPlace(BlockStackEvent event) {
        if (this.generatorManager.getActiveGenerator(event.getBlock()) != null) {
            event.setCancelled(true);
        }
    }

    private void check(SuperiorPlayer superiorPlayer) {
        List<ActiveGenerator> generators = this.generatorManager.getActiveGenerator(superiorPlayer.getUniqueId());
        List<String> refunds = new ArrayList<>();
        for (ActiveGenerator active : generators) {
            Generator generator = active.getGenerator();
            if (!this.generatorManager.unregisterGenerator(active)) continue;
            active.getLocation().getBlock().setType(Material.AIR);
            if (NextGens.DEFAULT_CONFIG.getConfig().getBoolean("island-pickup")) {
                refunds.add(generator.id());
            }
        }
        this.refundManager.refundGenerators(superiorPlayer.getUniqueId(), refunds);
    }

}
