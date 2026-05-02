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
import java.util.List;
import java.util.Locale;

import static org.antredesloutres.ottergames.utils.Constants.*;

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
            player.sendMessage(OTTERDEV_USAGE);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "test" -> {
                if (args.length < 2) {
                    player.sendMessage(OTTERDEV_TEST_USAGE);
                    return true;
                }
                String structureName = args[1];
                StructureSpawner.spawn(plugin, structureName, player.getLocation());
                player.sendMessage(OTTERDEV_STRUCTURE_SPAWNED + structureName);
            }
            default -> player.sendMessage(OTTERDEV_USAGE);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NonNull CommandSender sender, @NonNull Command command, @NonNull String alias, @NonNull String @NonNull [] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0].toLowerCase(Locale.ROOT), List.of("test"), completions);
        }
        return completions;
    }
}
