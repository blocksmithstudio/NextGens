package com.muhammaddaffa.nextgens.generators.listeners;

import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import io.github.thebusybiscuit.slimefun4.api.events.ExplosiveToolBreakBlocksEvent;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

public record GeneratorSlimefunPreventionListener(
        GeneratorManager generatorManager
) implements Listener {

    @EventHandler
    private void onPickaxeExplosion(ExplosiveToolBreakBlocksEvent event) {
        List<Block> BLOCK = new ArrayList<>(event.getAdditionalBlocks());
        BLOCK.add(event.getPrimaryBlock());

        for (Block block : BLOCK) {
            if (this.generatorManager.getActiveGenerator(block) != null) {
                event.setCancelled(true);
            }
        }
    }
}
