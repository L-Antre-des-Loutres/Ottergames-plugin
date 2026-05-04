package org.antredesloutres.ottergames.handlers;

import org.antredesloutres.ottergames.managers.GameManager;
import org.antredesloutres.ottergames.Main;
import org.antredesloutres.ottergames.commands.Otterdev;
import org.antredesloutres.ottergames.commands.Ottergames;

import java.util.Objects;

public class CommandHandler {

    private final Main plugin;
    private final GameManager gameManager;

    public CommandHandler(Main plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    public void setup() {
        Ottergames ottergamesCommand = new Ottergames(plugin, gameManager);
        var ottergamesPluginCommand = Objects.requireNonNull(plugin.getCommand("ottergames"));
        ottergamesPluginCommand.setExecutor(ottergamesCommand);
        ottergamesPluginCommand.setTabCompleter(ottergamesCommand);

        Otterdev otterdevCommand = new Otterdev(plugin);
        var otterdevPluginCommand = Objects.requireNonNull(plugin.getCommand("otterdev"));
        otterdevPluginCommand.setExecutor(otterdevCommand);
        otterdevPluginCommand.setTabCompleter(otterdevCommand);
    }
}
