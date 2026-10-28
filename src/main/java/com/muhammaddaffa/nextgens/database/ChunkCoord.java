package com.muhammaddaffa.nextgens.database;

import org.bukkit.Location;

public record ChunkCoord(
        String world,
        int x,
        int z
) {

    public static ChunkCoord fromLocation(Location loc) {
        return new ChunkCoord(
                loc.getWorld().getName(),
                loc.getBlockX() >> 4,
                loc.getBlockZ() >> 4
        );
    }

    public static ChunkCoord fromSerializedLocation(String serialized) {
        if (serialized == null) throw new IllegalArgumentException("Missing generator location");
        String[] parts = serialized.split(";", -1);
        if (parts.length != 6 || parts[5].isBlank()) {
            throw new IllegalArgumentException("Invalid generator location");
        }
        double[] coordinates = new double[5];
        for (int i = 0; i < coordinates.length; i++) {
            coordinates[i] = Double.parseDouble(parts[i]);
            if (!Double.isFinite(coordinates[i])) {
                throw new IllegalArgumentException("Non-finite generator location");
            }
        }
        for (int i = 0; i < 3; i++) {
            if (coordinates[i] < Integer.MIN_VALUE || coordinates[i] >= (double) Integer.MAX_VALUE + 1) {
                throw new IllegalArgumentException("Generator location outside block coordinates");
            }
        }
        if (Math.abs(coordinates[3]) > Float.MAX_VALUE || Math.abs(coordinates[4]) > Float.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid generator rotation");
        }
        return new ChunkCoord(parts[5], (int) Math.floor(coordinates[0]) >> 4,
                (int) Math.floor(coordinates[2]) >> 4);
    }

}
