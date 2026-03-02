package org.antredesloutres.ottergames;

import org.antredesloutres.ottergames.listeners.JoinListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ListenersHandler {

    private final JavaPlugin plugin;

    public ListenersHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        PluginManager pm = Bukkit.getPluginManager();

        // Registering
        pm.registerEvents(new JoinListener(), plugin);
    }

}