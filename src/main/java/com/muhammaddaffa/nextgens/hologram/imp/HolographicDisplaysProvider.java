package com.muhammaddaffa.nextgens.hologram.imp;

import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.hologram.HologramProvider;
import me.filoghost.holographicdisplays.api.HolographicDisplaysAPI;
import me.filoghost.holographicdisplays.api.Position;
import me.filoghost.holographicdisplays.api.hologram.Hologram;
import org.bukkit.Location;

import java.util.List;

public class HolographicDisplaysProvider implements HologramProvider {


    @Override
    public String id() {
        return "HolographicDisplays";
    }

    @Override
    public String pluginName() {
        return "HolographicDisplays";
    }

    @Override
    public void spawn(String name, Location location, List<String> lines) {
        // get the holographic api
        HolographicDisplaysAPI api = HolographicDisplaysAPI.get(NextGens.getInstance());
        // get the hologram position
        Position position = Position.of(location);
        // spawn the hologram at the location
        Hologram hologram = api.createHologram(position);
        lines.forEach(line -> hologram.getLines().appendText(line));
    }

    @Override
    public void destroy(String name, Location location) {
        // get the holographic api
        HolographicDisplaysAPI api = HolographicDisplaysAPI.get(NextGens.getInstance());
        // get the position of the hologram
        Position position = Position.of(location);
        // get the hologram based on the position
        for (Hologram hologram : api.getHolograms()) {
            if (hologram.getPosition().toLocation().equals(position.toLocation())) {
                hologram.delete();
            }
        }
    }
}
