package org.antredesloutres.ottergames.commands;

import org.antredesloutres.ottergames.GameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.util.StringUtil;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class Ottergames implements TabExecutor {

    private final GameManager gameManager;

    public Ottergames(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, @NonNull String @NonNull [] args) {

        if (args.length == 0) {
            sender.sendMessage("§b[OtterGames] §7Utilisez §f/ottergames start §7ou §f/ottergames stop§7.");
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "start" -> {
                if (gameManager.isRunning()) {
                    sender.sendMessage("§cLa partie est deja en cours !");
                    return true;
                }

                gameManager.startGameLoop();
                sender.sendMessage("§a§lLancement de la serie de mini-jeux !");
                return true;
            }
            case "stop" -> {
                if (!gameManager.isRunning()) {
                    sender.sendMessage("§eAucune partie n'est en cours.");
                    return true;
                }

                gameManager.stopEverything();
                sender.sendMessage("§cPartie interrompue.");
                return true;
            }
            default -> {
                sender.sendMessage("§cCommande inconnue. Utilisez §f/ottergames start §cou §f/ottergames stop§c.");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(@NonNull CommandSender sender, @NonNull Command command, @NonNull String alias, @NonNull String @NonNull [] args) {
        List<String> completions = new ArrayList<>();

        if (args.length > 1) {
            return completions;
        }

        String typed = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        StringUtil.copyPartialMatches(typed, Arrays.asList("start", "stop"), completions);
        return completions;
    }

}
