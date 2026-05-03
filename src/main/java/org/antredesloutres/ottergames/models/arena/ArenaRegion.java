package org.antredesloutres.ottergames.models.arena;

import org.bukkit.Location;

/**
 * Represents a generic 3D bounding box area relative to an arena's origin.
 */
public record ArenaRegion(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean allowBlockChanges) {

    /**
     * Creates a new ArenaRegion with block changes disabled by default.
     */
    public ArenaRegion(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this(minX, minY, minZ, maxX, maxY, maxZ, false);
    }

    public ArenaRegion {
        if (minX < 0 || minY < 0 || minZ < 0) {
            throw new IllegalArgumentException("Region minimum offsets must be >= 0");
        }
        if (maxX < minX || maxY < minY || maxZ < minZ) {
            throw new IllegalArgumentException("Region maximum offsets must be >= minimum offsets");
        }
    }

    /**
     * Checks if a given location is within this region, relative to the arena's origin.
     */
    public boolean contains(ArenaInstance arena, Location location) {
        if (location.getWorld() == null || !location.getWorld().equals(arena.origin().getWorld())) return false;

        int relX = location.getBlockX() - arena.origin().getBlockX();
        int relY = location.getBlockY() - arena.origin().getBlockY();
        int relZ = location.getBlockZ() - arena.origin().getBlockZ();

        return relX >= minX && relX <= maxX &&
               relY >= minY && relY <= maxY &&
               relZ >= minZ && relZ <= maxZ;
    }
}
