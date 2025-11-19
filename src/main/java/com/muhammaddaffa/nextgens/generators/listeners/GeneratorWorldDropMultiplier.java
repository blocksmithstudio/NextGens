package com.muhammaddaffa.nextgens.generators.listeners;

import com.muhammaddaffa.nextgens.api.events.generators.GeneratorGenerateItemEvent;
import com.muhammaddaffa.nextgens.cache.WorldBoostCache;
import com.muhammaddaffa.nextgens.cache.WorldBoostSettings;
import com.muhammaddaffa.nextgens.generators.ActiveGenerator;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

public class GeneratorWorldDropMultiplier implements Listener {

    @EventHandler
    private void onGenerateItem(GeneratorGenerateItemEvent event) {
        ActiveGenerator active = event.getActiveGenerator();
        String worldName = active.getLocation().getWorld().getName();
        // Get the world boost settings
        WorldBoostSettings worldBoostSettings = WorldBoostCache.getWorldBoostSettings(worldName);
        int dropAmount = worldBoostSettings.getDropMultiplier();
        List<String> whitelistWorld = worldBoostSettings.getWhitelistGeneratorIds();
        // Set the drop amount if it's greater than zero
        if (dropAmount > 0 && (whitelistWorld.isEmpty() || whitelistWorld.contains(active.getGenerator().id()))) {
            event.setDropAmount(event.getDropAmount() + dropAmount);
        }
    }

}
