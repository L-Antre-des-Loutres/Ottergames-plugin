package org.antredesloutres.ottergames.models.structures;

import org.antredesloutres.ottergames.models.ArenaInstance;
import org.bukkit.Location;

/**
 * Represents a generic 3D bounding box area relative to an arena's origin.
 */
public record ArenaRegion(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

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

        double relX = location.getX() - arena.origin().getX();
        double relY = location.getY() - arena.origin().getY();
        double relZ = location.getZ() - arena.origin().getZ();

        // Check if within bounds (adding 1 to max to account for block boundaries)
        return relX >= minX && relX <= (maxX + 1.0) &&
               relY >= minY && relY <= (maxY + 2.0) && // Player is 2 blocks tall
               relZ >= minZ && relZ <= (maxZ + 1.0);
    }
}
