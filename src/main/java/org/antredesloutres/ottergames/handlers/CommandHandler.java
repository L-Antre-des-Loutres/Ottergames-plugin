package org.antredesloutres.ottergames.handlers;

import org.antredesloutres.ottergames.GameManager;
import org.antredesloutres.ottergames.commands.Ottergames;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class CommandHandler {

    private final JavaPlugin plugin;
    private final GameManager gameManager;

    public CommandHandler(JavaPlugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    public void setup() {
        // Registering
        Ottergames otterCommand = new Ottergames(gameManager);
        var otterPluginCommand = Objects.requireNonNull(plugin.getCommand("ottergames"));
        otterPluginCommand.setExecutor(otterCommand);
        otterPluginCommand.setTabCompleter(otterCommand);
    }

}
