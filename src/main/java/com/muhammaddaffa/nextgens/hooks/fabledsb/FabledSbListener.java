package com.muhammaddaffa.nextgens.hooks.fabledsb;

import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.generators.ActiveGenerator;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import com.muhammaddaffa.nextgens.refund.RefundManager;
import com.songoda.skyblock.api.event.island.IslandDeleteEvent;
import com.songoda.skyblock.api.event.island.IslandKickEvent;
import com.songoda.skyblock.api.event.player.PlayerIslandJoinEvent;
import com.songoda.skyblock.api.event.player.PlayerIslandLeaveEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.UUID;

public record FabledSbListener(
        GeneratorManager generatorManager,
        RefundManager refundManager
) implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onIslandKick(IslandKickEvent event) {
        this.check(event.getKicked().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onIslandDelete(IslandDeleteEvent event) {
        event.getIsland().getIsland().getWhitelistedPlayers().forEach(this::check);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onIslandLeave(PlayerIslandLeaveEvent event) {
        this.check(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onIslandJoin(PlayerIslandJoinEvent event) {
        this.check(event.getPlayer().getUniqueId());
    }

    private void check(UUID uuid) {
        // get all variables we need
        Player player = Bukkit.getPlayer(uuid);
        List<ActiveGenerator> generators = this.generatorManager.getActiveGenerator(uuid);
        // loop through them all
        for (ActiveGenerator active : generators) {
            // unregister the generator
            this.generatorManager.unregisterGenerator(active.getLocation());
            // set the block to air
            active.getLocation().getBlock().setType(Material.AIR);
            // check for island pickup option
            if (NextGens.DEFAULT_CONFIG.getConfig().getBoolean("island-pickup")) {
                // give the generator back
                if (player == null) {
                    // if player not online, register it to item join
                    this.refundManager.delayedGiveGeneratorItem(uuid, active.getGenerator().id());
                } else {
                    // if player is online, give them the generators
                    Common.addInventoryItem(player, active.getGenerator().createItem(1));
                }
            }

        }
    }

}
