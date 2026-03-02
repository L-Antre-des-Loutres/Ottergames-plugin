package org.antredesloutres.ottergames;

import org.antredesloutres.ottergames.commands.Otter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public final class Ottergames extends JavaPlugin {

    @Override
    public void onEnable() {
        // Init commands
        CommandHandler commandHandler = new CommandHandler(this);
        commandHandler.init();

        // Plugin startup logic
        getLogger().info("Ottergames plugin enabled!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("Ottergames plugin disabled!");
    }

}
