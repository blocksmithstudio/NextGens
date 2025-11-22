package com.muhammaddaffa.nextgens.hooks.ssb2;

import com.bgsoftware.superiorskyblock.api.events.*;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.muhammaddaffa.mdlib.task.ExecutorManager;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Config;
import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.generators.ActiveGenerator;
import com.muhammaddaffa.nextgens.generators.Generator;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import com.muhammaddaffa.nextgens.refund.RefundManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;

public record SSB2Listener(
        GeneratorManager generatorManager,
        RefundManager refundManager
) implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    private void onIslandDisband(IslandDisbandEvent event) {
        event.getIsland().getIslandMembers(true).forEach(this::check);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onIslandKick(IslandKickEvent event) {
        this.check(event.getTarget());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onIslandLeave(IslandQuitEvent event) {
        this.check(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
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
        // get all variables we need
        Player player = Bukkit.getPlayer(superiorPlayer.getUniqueId());
        List<ActiveGenerator> generators = this.generatorManager.getActiveGenerator(superiorPlayer.getUniqueId());
        // loop through them all
        for (ActiveGenerator active : generators) {
            Generator generator = active.getGenerator();
            // unregister the generator
            this.generatorManager.unregisterGenerator(active.getLocation());
            // set the block to air
            active.getLocation().getBlock().setType(Material.AIR);
            // check for island pickup option
            if (NextGens.DEFAULT_CONFIG.getConfig().getBoolean("island-pickup")) {
                // give the generator back
                if (player == null) {
                    // if player not online, register it to item join
                    this.refundManager.delayedGiveGeneratorItem(superiorPlayer.getUniqueId(), generator.id());
                } else {
                    // if player is online, give them the generators
                    ExecutorManager.getProvider().sync(() -> Common.addInventoryItem(player, generator.createItem(1)));
                }
            }

        }
    }

}
