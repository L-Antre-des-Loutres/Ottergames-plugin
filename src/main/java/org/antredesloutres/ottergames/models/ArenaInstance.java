package org.antredesloutres.ottergames.models;

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
     * Clears the arena by setting all blocks within the arena's bounds to AIR.
     */
    public void clear() {
        var world = origin.getWorld();
        if (world == null) return;

        int ox = origin.getBlockX();
        int oy = origin.getBlockY();
        int oz = origin.getBlockZ();

        for (int x = 0; x < size.getBlockX(); x++)
            for (int y = 0; y < size.getBlockY(); y++)
                for (int z = 0; z < size.getBlockZ(); z++)
                    world.getBlockAt(ox + x, oy + y, oz + z).setType(Material.AIR, false);
    }

}
