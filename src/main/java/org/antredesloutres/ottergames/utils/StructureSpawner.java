package org.antredesloutres.ottergames.utils;

import org.antredesloutres.ottergames.Main;
import org.bukkit.Location;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public final class StructureSpawner {

    private StructureSpawner() {}

    public static void spawn(Main plugin, String name, Location location) {
        String path = "structures/" + name + ".nbt";

        try (InputStream stream = plugin.getResource(path)) {
            if (stream == null) {
                plugin.getLogger().warning("Structure introuvable : " + path);
                return;
            }

            StructureManager manager = plugin.getServer().getStructureManager();
            Structure structure = manager.loadStructure(stream);
            structure.place(location, true, StructureRotation.NONE, Mirror.NONE, 0, 1.0f, new Random());

        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors du chargement de la structure : " + path);
            e.printStackTrace();
        }
    }
}
