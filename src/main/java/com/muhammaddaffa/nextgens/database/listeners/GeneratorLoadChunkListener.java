package com.muhammaddaffa.nextgens.database.listeners;

import com.muhammaddaffa.mdlib.task.ExecutorManager;
import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.database.ChunkCoord;
import com.muhammaddaffa.nextgens.generators.managers.GeneratorManager;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.List;

public record GeneratorLoadChunkListener(
        GeneratorManager generatorManager
) implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    private void onChunkLoad(ChunkLoadEvent event) {
        handleChunkLoad(event.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) return;
        handleChunkLoad(event.getTo().getChunk());
    }

    private void handleChunkLoad(Chunk chunk) {
        ChunkCoord key = new ChunkCoord(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        // If the chunk is already loaded, skip this
        if (!NextGens.LOADED_CHUNKS.add(key)) return;

        List<String> list = this.generatorManager.getGeneratorsByChunk().get(key);
        if (list == null || list.isEmpty()) return;

        // Proceed to load the active generators
        ExecutorManager.getProvider().async(() -> this.generatorManager.loadActiveGenerator(key, list));
    }

}
