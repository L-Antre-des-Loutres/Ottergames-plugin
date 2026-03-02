package org.antredesloutres.ottergames;

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
        Objects.requireNonNull(plugin.getCommand("otter")).setExecutor(new Otter());
    }

}