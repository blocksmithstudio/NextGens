package com.muhammaddaffa.nextgens.hologram.imp;

import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.nextgens.hologram.HologramProvider;
import com.muhammaddaffa.nextgens.utils.Settings;
import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

public class FancyHologramProvider implements HologramProvider {


    @Override
    public String id() {
        return "FancyHologram";
    }

    @Override
    public String pluginName() {
        return "FancyHolograms";
    }

    @Override
    public void spawn(String name, Location location, List<String> lines) {
        HologramManager manager = FancyHologramsPlugin.get().getHologramManager();

        TextHologramData hologram = new TextHologramData(name, location);
        hologram.setPersistent(false);
        hologram.setText(lines);

        // apply the configurable settings
        hologram.setBillboard(parseBillboard(Settings.FANCY_HOLOGRAM_BILLBOARD));
        hologram.setScale(parseScale(Settings.FANCY_HOLOGRAM_SCALE));
        hologram.setTextAlignment(parseAlignment(Settings.FANCY_HOLOGRAM_TEXT_ALIGNMENT));
        hologram.setTextShadow(Settings.FANCY_HOLOGRAM_TEXT_SHADOW);
        hologram.setSeeThrough(Settings.FANCY_HOLOGRAM_SEE_THROUGH);

        Color background = parseBackground(Settings.FANCY_HOLOGRAM_BACKGROUND);
        if (background != null) {
            hologram.setBackground(background);
        }

        Hologram created = manager.create(hologram);
        manager.addHologram(created);
    }

    @Override
    public void destroy(String name, Location location) {
        HologramManager manager = FancyHologramsPlugin.get().getHologramManager();
        Optional<Hologram> hologram = manager.getHologram(name);
        // Remove hologram if present
        hologram.ifPresent(manager::removeHologram);
    }

    private Display.Billboard parseBillboard(String value) {
        if (value == null || value.isBlank()) {
            return Display.Billboard.CENTER;
        }
        try {
            return Display.Billboard.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            Logger.warning("Invalid FancyHolograms billboard '" + value + "', defaulting to CENTER.");
            return Display.Billboard.CENTER;
        }
    }

    private TextDisplay.TextAlignment parseAlignment(String value) {
        if (value == null || value.isBlank()) {
            return TextDisplay.TextAlignment.CENTER;
        }
        try {
            return TextDisplay.TextAlignment.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            Logger.warning("Invalid FancyHolograms text-alignment '" + value + "', defaulting to CENTER.");
            return TextDisplay.TextAlignment.CENTER;
        }
    }

    private Vector3f parseScale(double value) {
        float scale = value > 0 ? (float) value : 1.0f;
        return new Vector3f(scale, scale, scale);
    }

    private Color parseBackground(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("default")) {
            // null = keep FancyHolograms' own default background
            return null;
        }
        if (value.equalsIgnoreCase("transparent")) {
            return Hologram.TRANSPARENT;
        }
        try {
            String hex = value.startsWith("#") ? value.substring(1) : value;
            return Color.fromRGB(Integer.parseInt(hex, 16));
        } catch (IllegalArgumentException ex) {
            Logger.warning("Invalid FancyHolograms background '" + value + "', using the default background.");
            return null;
        }
    }
}