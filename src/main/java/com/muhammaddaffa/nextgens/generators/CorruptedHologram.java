package com.muhammaddaffa.nextgens.generators;

import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.LocationUtils;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.hologram.HologramManager;
import com.muhammaddaffa.nextgens.utils.Settings;
import org.bukkit.Location;

import java.util.List;

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
        HologramManager manager = NextGens.getInstance().getHologramManager();
        // no hologram plugin is installed; nothing to spawn
        if (!manager.isEnabled()) {
            return;
        }
        // get the hologram lines
        List<String> lines = Common.color(Settings.CORRUPTION_HOLOGRAM_LINES);
        manager.getProvider().spawn(this.name, this.hologramLocation, lines);
    }

    public void destroy() {
        HologramManager manager = NextGens.getInstance().getHologramManager();
        if (!manager.isEnabled()) {
            return;
        }
        manager.getProvider().destroy(this.name, this.hologramLocation);
    }

    private String getCleanNames(String text) {
        return text.replace(",", "")
                .replace(".", "")
                .replace(";", "")
                .replace("-", "_");
    }

}
