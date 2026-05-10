package org.antredesloutres.ottergames.models.arena;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.BlockVector;

public record ArenaInstance(Location origin, BlockVector size) {

    /**
     * Creates a new ArenaInstance with the given origin and size.
     * @param origin The origin of the arena (the corner with the lowest x, y, z coordinates).
     * @param size The size of the arena in blocks (width, height, depth).
     */
    public ArenaInstance(Location origin, BlockVector size) {
        this.origin = origin.clone();
        this.size = size;
    }

    @Override
    public Location origin() {
        return origin.clone();
    }

    /**
     * Clears the arena by setting all blocks within the arena's bounds to AIR
     * and removing all entities except players.
     */
    public void clear() {
        var world = origin.getWorld();
        if (world == null) return;

        int ox = origin.getBlockX();
        int oy = origin.getBlockY();
        int oz = origin.getBlockZ();

        // Clear blocks
        for (int x = 0; x < size.getBlockX(); x++)
            for (int y = 0; y < size.getBlockY(); y++)
                for (int z = 0; z < size.getBlockZ(); z++)
                    world.getBlockAt(ox + x, oy + y, oz + z).setType(Material.AIR, false);

        // Clear entities
        double centerX = origin.getX() + size.getX() / 2.0;
        double centerY = origin.getY() + size.getY() / 2.0;
        double centerZ = origin.getZ() + size.getZ() / 2.0;

        world.getNearbyEntities(new Location(world, centerX, centerY, centerZ), size.getX() / 2.0, size.getY() / 2.0, size.getZ() / 2.0)
                .forEach(entity -> {
                    if (!(entity instanceof org.bukkit.entity.Player)) {
                        entity.remove();
                    }
                });
    }

    /**
     * Checks whether a world location is inside this arena's bounding box.
     */
    public boolean contains(Location loc) {
        if (loc.getWorld() == null || origin.getWorld() == null) return false;
        if (!loc.getWorld().equals(origin.getWorld())) return false;

        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();

        double ox = origin.getX();
        double oy = origin.getY();
        double oz = origin.getZ();

        return x >= ox && x <= ox + size.getBlockX()
            && y >= oy && y <= oy + size.getBlockY()
            && z >= oz && z <= oz + size.getBlockZ();
    }

}
