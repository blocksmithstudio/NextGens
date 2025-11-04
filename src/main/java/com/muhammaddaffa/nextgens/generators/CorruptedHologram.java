package com.muhammaddaffa.nextgens.generators;

import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.mdlib.utils.LocationUtils;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.utils.Settings;
import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.data.TextHologramData;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import me.filoghost.holographicdisplays.api.HolographicDisplaysAPI;
import me.filoghost.holographicdisplays.api.Position;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;

import java.util.List;
import java.util.Optional;

public class CorruptedHologram {

    private final ActiveGenerator active;
    private final Location hologramLocation;
    private final String name;
    public CorruptedHologram(ActiveGenerator active) {
        this.active = active;
        this.hologramLocation = active.getLocation().clone().add(0.5, Settings.CORRUPTION_HOLOGRAM_HEIGHT, 0.5);
        this.name = this.getCleanNames(LocationUtils.serialize(this.hologramLocation));
    }

    public void spawn() {
        // get the hologram lines
        List<String> lines = Common.color(Settings.CORRUPTION_HOLOGRAM_LINES);
        // Holographic Displays
        if (Bukkit.getPluginManager().getPlugin("HolographicDisplays") != null) {
            // execute it in sync task
            Executor.sync(() -> {
                // get the holographic api
                HolographicDisplaysAPI api = HolographicDisplaysAPI.get(NextGens.getInstance());
                // get the hologram position
                Position position = Position.of(this.hologramLocation);
                // spawn the hologram at the location
                me.filoghost.holographicdisplays.api.hologram.Hologram hologram = api.createHologram(position);
                lines.forEach(line -> hologram.getLines().appendText(line));
            });
            return;
        }
        // Decent Holograms
        if (Bukkit.getPluginManager().getPlugin("DecentHolograms") != null) {
            DHAPI.createHologram(this.name, this.hologramLocation, lines);
        }
        // FancyHolograms
        if (Bukkit.getPluginManager().getPlugin("FancyHolograms") != null) {
            HologramManager manager = FancyHologramsPlugin.get().getHologramManager();
            TextHologramData hologram = new TextHologramData(this.name, this.hologramLocation);
            hologram.setPersistent(false);
            hologram.setBillboard(Display.Billboard.CENTER);
            hologram.setText(lines);

            de.oliver.fancyholograms.api.hologram.Hologram holograms =
                    manager.create(hologram);
            manager.addHologram(holograms);
        }
    }

    public void destroy() {
        // Holographic Displays
        if (Bukkit.getPluginManager().getPlugin("HolographicDisplays") != null) {
            // execute in sync task
            Executor.sync(() -> {
                // get the holographic api
                HolographicDisplaysAPI api = HolographicDisplaysAPI.get(NextGens.getInstance());
                // get the position of the hologram
                Position position = Position.of(this.hologramLocation);
                // get the hologram based on the position
                for (me.filoghost.holographicdisplays.api.hologram.Hologram hologram : api.getHolograms()) {
                    if (hologram.getPosition().toLocation().equals(position.toLocation())) {
                        hologram.delete();
                    }
                }
            });
            return;
        }
        // Decent Holograms
        if (Bukkit.getPluginManager().getPlugin("DecentHolograms") != null) {
            Hologram hologram = DHAPI.getHologram(this.name);
            // destroy the hologram if exists
            if (hologram != null) {
                hologram.destroy();
            }
        }
        if (Bukkit.getPluginManager().getPlugin("FancyHolograms") != null) {
            HologramManager manager = FancyHologramsPlugin.get().getHologramManager();
            Optional<de.oliver.fancyholograms.api.hologram.Hologram> hologram = manager.getHologram(this.name);
            // Remove hologram if present
            hologram.ifPresent(manager::removeHologram);
        }
    }

    private String getCleanNames(String text) {
        return text.replace(",", "")
                .replace(".", "")
                .replace(";", "")
                .replace("-", "_");
    }

}
