package org.antredesloutres.ottergames.models.arena;

import org.bukkit.Location;
import org.bukkit.util.BlockVector;

import java.util.Random;

/**
 * A spawn zone defined as relative offsets from a structure's origin.
 *
 * @param minX  Minimum X offset (blocks)
 * @param minY  Minimum Y offset (blocks)
 * @param minZ  Minimum Z offset (blocks)
 * @param maxX  Maximum X offset (blocks, must be &gt;= minX)
 * @param maxY  Maximum Y offset (blocks, must be &gt;= minY)
 * @param maxZ  Maximum Z offset (blocks, must be &gt;= minZ)
 * @param yaw   Horizontal rotation in degrees (0 = south, 90 = west, -90/270 = east, 180 = north)
 * @param pitch Vertical rotation in degrees (negative = looking up, positive = looking down)
 */
public record ArenaSpawnZone(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, float yaw, float pitch) {

    public ArenaSpawnZone {
        if (minX < 0 || minY < 0 || minZ < 0) {
            throw new IllegalArgumentException("Spawn zone minimum offsets must be >= 0");
        }
        if (maxX < minX || maxY < minY || maxZ < minZ) {
            throw new IllegalArgumentException("Spawn zone maximum offsets must be >= minimum offsets");
        }
    }

    /**
     * Creates a zone covering the full structure size, facing south (yaw 0, pitch 0).
     */
    public static ArenaSpawnZone full(BlockVector size) {
        int maxX = Math.max(0, size.getBlockX() - 1);
        int maxY = Math.max(0, size.getBlockY() - 1);
        int maxZ = Math.max(0, size.getBlockZ() - 1);
        return new ArenaSpawnZone(0, 0, 0, maxX, maxY, maxZ, 0.0f, 0.0f);
    }

    public Location randomLocation(ArenaInstance arena, Random random) {
        Location origin = arena.origin();
        if (origin.getWorld() == null) {
            return origin;
        }

        int arenaMaxX = Math.max(0, arena.size().getBlockX() - 1);
        int arenaMaxY = Math.max(0, arena.size().getBlockY() - 1);
        int arenaMaxZ = Math.max(0, arena.size().getBlockZ() - 1);

        int minXClamped = clamp(minX, 0, arenaMaxX);
        int minYClamped = clamp(minY, 0, arenaMaxY);
        int minZClamped = clamp(minZ, 0, arenaMaxZ);

        int maxXClamped = clamp(maxX, 0, arenaMaxX);
        int maxYClamped = clamp(maxY, 0, arenaMaxY);
        int maxZClamped = clamp(maxZ, 0, arenaMaxZ);

        double x = origin.getX() + minXClamped + (maxXClamped > minXClamped ? random.nextDouble() * (maxXClamped - minXClamped) : 0.5);
        double y = origin.getY() + minYClamped + (maxYClamped > minYClamped ? random.nextDouble() * (maxYClamped - minYClamped) : 0.0);
        double z = origin.getZ() + minZClamped + (maxZClamped > minZClamped ? random.nextDouble() * (maxZClamped - minZClamped) : 0.5);

        return new Location(origin.getWorld(), x, y, z, yaw, pitch);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
