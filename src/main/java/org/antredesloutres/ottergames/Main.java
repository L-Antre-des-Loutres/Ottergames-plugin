package org.antredesloutres.ottergames;

import org.antredesloutres.ottergames.handlers.CommandHandler;
import org.antredesloutres.ottergames.handlers.ListenersHandler;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    private GameManager gameManager;

    @Override
    public void onEnable() {
        this.gameManager = new GameManager(this);
        new ListenersHandler(this, gameManager).setup(); // Enables listeners
        new CommandHandler(this, gameManager).setup(); // Enables commands

        getLogger().info("Ottergames ready!");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.stopEverything();
        }

        getLogger().info("Ottergames disabled!");
    }

}
