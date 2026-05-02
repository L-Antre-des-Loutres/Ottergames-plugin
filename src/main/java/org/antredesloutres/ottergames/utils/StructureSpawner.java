package org.antredesloutres.ottergames.utils;

import org.antredesloutres.ottergames.Main;
import org.antredesloutres.ottergames.models.ArenaInstance;
import org.bukkit.Location;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.structure.Structure;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public final class StructureSpawner {

    private StructureSpawner() {}

    /**
     * Loads a structure from the plugin's resources/structures folder.
     * @param plugin Plugin instance used to access resources and the structure manager.
     * @param name The name of the structure to load, without the .nbt extension.
     * @return The loaded Structure object, or null if the structure could not be found or loaded.
     */
    public static Structure load(Main plugin, String name) {
        String path = "structures/" + name + ".nbt";
        try (InputStream stream = plugin.getResource(path)) {
            if (stream == null) {
                plugin.getLogger().warning("Structure not found in resources: " + path);
                return null;
            }
            return plugin.getServer().getStructureManager().loadStructure(stream);
        } catch (IOException e) {
            plugin.getLogger().severe("Error loading structure: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Places a structure in the world at the specified origin location.
     * @param structure The Structure object to place. Must not be null.
     * @param origin The Location representing the origin point (lowest X, Y, Z corner) where the structure should be placed. Must not be null.
     * @return An ArenaInstance representing the placed structure, with its origin and size. The structure is placed with no rotation or mirroring, and entities are included.
     */
    public static ArenaInstance place(Structure structure, Location origin) {
        structure.place(origin, true, StructureRotation.NONE, Mirror.NONE, 0, 1.0f, new Random());
        return new ArenaInstance(origin, structure.getSize());
    }

    /**
     * Loads a structure by name and places it at the specified location. This is a convenience method that combines loading and placing in one step.
     * @param plugin Plugin instance used to access resources and the structure manager.
     * @param name The name of the structure to load, without the .nbt extension.
     * @param location The Location representing the origin point (lowest X, Y, Z corner) where the structure should be placed. Must not be null.
     */
    public static void spawn(Main plugin, String name, Location location) {
        Structure structure = load(plugin, name);
        if (structure != null) place(structure, location);
    }

}
