package org.antredesloutres.ottergames.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;

public class Otter implements CommandExecutor {
    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, @NonNull String @NonNull [] args) {
        if (command.getName().equalsIgnoreCase("otter")) {
            sender.sendMessage("§bHello from Ottergames!");
            return true;
        }
        return false;
    }
}