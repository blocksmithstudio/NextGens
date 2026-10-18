package com.muhammaddaffa.nextgens.hooks.bento;

import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.generators.ActiveGenerator;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import com.muhammaddaffa.nextgens.refund.RefundManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import world.bentobox.bentobox.api.events.island.*;
import world.bentobox.bentobox.api.events.team.TeamJoinEvent;
import world.bentobox.bentobox.api.events.team.TeamKickEvent;
import world.bentobox.bentobox.api.events.team.TeamLeaveEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record BentoListener(
        GeneratorManager generatorManager,
        RefundManager refundManager
) implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void onIslandDelete(IslandPreclearEvent event) {
        event.getOldIsland().getMemberSet().forEach(this::check);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void onTeamKick(TeamKickEvent event) {
        this.check(event.getPlayerUUID());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void onTeamJoin(TeamJoinEvent event) {
        this.check(event.getPlayerUUID());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void onTeamLeave(TeamLeaveEvent event) {
        this.check(event.getPlayerUUID());
    }

    private void check(UUID uuid) {
        List<ActiveGenerator> generators = this.generatorManager.getActiveGenerator(uuid);
        List<String> refunds = new ArrayList<>();
        for (ActiveGenerator active : generators) {
            this.generatorManager.unregisterGenerator(active.getLocation());
            active.getLocation().getBlock().setType(Material.AIR);
            if (NextGens.DEFAULT_CONFIG.getConfig().getBoolean("island-pickup")) {
                refunds.add(active.getGenerator().id());
            }
        }
        this.refundManager.refundGenerators(uuid, refunds);
    }

}
