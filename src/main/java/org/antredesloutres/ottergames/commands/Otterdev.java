package org.antredesloutres.ottergames.commands;

import org.antredesloutres.ottergames.Main;
import org.antredesloutres.ottergames.utils.StructureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.antredesloutres.ottergames.utils.Constants.COMMAND_USER_MUST_BE_PLAYER;
import static org.antredesloutres.ottergames.utils.Constants.SPAWNED_STRUCTURE;

public class Otterdev implements TabExecutor {

    private final Main plugin;

    public Otterdev(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, @NonNull String @NonNull [] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(COMMAND_USER_MUST_BE_PLAYER);
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§b[OtterDev] §7Sous-commandes : §ftest <nom_structure>");
            return true;
        }

        if (args[0].equalsIgnoreCase("test")) {
            if (args.length < 2) {
                player.sendMessage("§cUsage : /otterdev test <nom_structure>");
                return true;
            }

            String structureName = args[1];
            StructureSpawner.spawn(plugin, structureName, player.getLocation());
            player.sendMessage(SPAWNED_STRUCTURE + structureName);
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(@NonNull CommandSender sender, @NonNull Command command, @NonNull String alias, @NonNull String @NonNull [] args) {
        List<String> completions = new ArrayList<>();

        if (args.length > 1) {
            return completions;
        }

        String typed = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        StringUtil.copyPartialMatches(typed, List.of("test"), completions);
        return completions;
    }

}
