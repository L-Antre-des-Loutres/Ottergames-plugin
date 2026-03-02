package org.antredesloutres.ottergames;

import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        new ListenersHandler(this).setup(); // Enables listeners
        new CommandHandler(this).setup(); // Enables commands

        getLogger().info("Ottergames ready!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Ottergames disabled!");
    }


}
