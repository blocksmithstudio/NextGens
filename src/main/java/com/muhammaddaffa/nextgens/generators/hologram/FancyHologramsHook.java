package com.muhammaddaffa.nextgens.generators.hologram;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import org.bukkit.Location;
import org.bukkit.entity.Display;

import java.util.List;
import java.util.Optional;

public class FancyHologramsHook {

    public static void spawn(String name, Location location, List<String> lines) {
        HologramManager manager = FancyHologramsPlugin.get().getHologramManager();
        TextHologramData hologram = new TextHologramData(name, location);
        hologram.setPersistent(false);
        hologram.setBillboard(Display.Billboard.CENTER);
        hologram.setText(lines);

        de.oliver.fancyholograms.api.hologram.Hologram holograms =
                manager.create(hologram);
        manager.addHologram(holograms);
    }

    public static void destroy(String name) {
        HologramManager manager = FancyHologramsPlugin.get().getHologramManager();
        Optional<Hologram> hologram = manager.getHologram(name);
        // Remove hologram if present
        hologram.ifPresent(manager::removeHologram);
    }

}
