package org.antredesloutres.ottergames.handlers;

import org.antredesloutres.ottergames.managers.GameManager;
import org.antredesloutres.ottergames.listeners.JoinListener;
import org.antredesloutres.ottergames.listeners.QuitListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ListenersHandler {

    private final JavaPlugin plugin;
    private final GameManager gameManager;

    public ListenersHandler(JavaPlugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    public void setup() {
        PluginManager pm = Bukkit.getPluginManager();

        // Registering
        pm.registerEvents(new JoinListener(gameManager, plugin), plugin);
        pm.registerEvents(new QuitListener(gameManager), plugin);
    }

}
