package org.antredesloutres.ottergames.handlers;

import org.antredesloutres.ottergames.GameManager;
import org.antredesloutres.ottergames.commands.Otter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class CommandHandler {

    private final JavaPlugin plugin;

    public CommandHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        // Registering
        GameManager gameManager = new GameManager(this.plugin);
        Objects.requireNonNull(plugin.getCommand("otter")).setExecutor(new Otter(gameManager));
    }

}