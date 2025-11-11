package com.muhammaddaffa.nextgens.generators.listeners;

import com.muhammaddaffa.nextgens.api.events.generators.GeneratorBreakEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GeneratorDupeFixListener implements Listener {

    public static Map<UUID, Long> delayMap = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBreak(GeneratorBreakEvent event) {
        Player player = event.getPlayer();
        Long duration = delayMap.get(player.getUniqueId());
        // Check if player has opened the upgrade gui previously
        if (duration != null) {
            long elapsed = System.currentTimeMillis() - duration;
            if (elapsed < 1500) {
                event.setCancelled(true);
            }
        }
    }

}
