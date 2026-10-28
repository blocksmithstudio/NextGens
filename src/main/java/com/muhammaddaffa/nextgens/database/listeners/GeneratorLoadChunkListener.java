package com.muhammaddaffa.nextgens.database.listeners;

import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.ChunkLoadEvent;


public record GeneratorLoadChunkListener(
        GeneratorManager generatorManager
) implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    private void onChunkLoad(ChunkLoadEvent event) {
        handleChunkLoad(event.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) return;
        handleChunkLoad(event.getTo().getChunk());
    }

    private void handleChunkLoad(Chunk chunk) {
        this.generatorManager.requestChunkLoad(chunk);
    }

}
