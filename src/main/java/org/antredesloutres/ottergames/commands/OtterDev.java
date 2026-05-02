package org.antredesloutres.ottergames.commands;

import org.antredesloutres.ottergames.Main;
import org.antredesloutres.ottergames.utils.StructureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public class OtterDev implements CommandExecutor {

    private final Main plugin;

    public OtterDev(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, @NonNull String @NonNull [] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCette commande est réservée aux joueurs.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§b[OtterDev] §7Sous-commandes : §ftest <nom_structure>");
            return true;
        }

        if (args[0].equalsIgnoreCase("test")) {
            if (args.length < 2) {
                player.sendMessage("§cUsage : /otterdev test <nom_structure>");
                return true;
            }

            String structureName = args[1];
            StructureSpawner.spawn(plugin, structureName, player.getLocation());
            player.sendMessage("§a[OtterDev] §7Structure §f" + structureName + " §7spawned à ta position.");
            return true;
        }

        return false;
    }
}
