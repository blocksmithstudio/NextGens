package com.muhammaddaffa.nextgens.hologram.imp;

import com.muhammaddaffa.nextgens.hologram.HologramProvider;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Location;

import java.util.List;

public class DecentHologramsProvider implements HologramProvider {


    @Override
    public String id() {
        return "DecentHolograms";
    }

    @Override
    public String pluginName() {
        return "DecentHolograms";
    }

    @Override
    public void spawn(String name, Location location, List<String> lines) {
        DHAPI.createHologram(name, location, lines);
    }

    @Override
    public void destroy(String name, Location location) {
        Hologram hologram = DHAPI.getHologram(name);
        // destroy the hologram if exists
        if (hologram != null) {
            hologram.destroy();
        }
    }
}
