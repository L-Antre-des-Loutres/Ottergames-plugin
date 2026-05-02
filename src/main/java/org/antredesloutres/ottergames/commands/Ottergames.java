package org.antredesloutres.ottergames.commands;

import org.antredesloutres.ottergames.GameManager;
import org.antredesloutres.ottergames.GameManager.LeaveResult;
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

public class Ottergames implements TabExecutor {

    private final GameManager gameManager;

    public Ottergames(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, @NonNull String @NonNull [] args) {

        if (args.length == 0) {
            sender.sendMessage("§b[OtterGames] §7Utilisez §f/ottergames start§7, §f/ottergames stop §7ou §f/ottergames leave§7.");
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
            case "leave" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cCette commande est reservee aux joueurs.");
                    return true;
                }

                LeaveResult leaveResult = gameManager.handlePlayerLeave(player);
                if (leaveResult == LeaveResult.ALREADY_LEFT) {
                    sender.sendMessage("§eTu es deja desinscrit des Ottergames.");
                    return true;
                }

                if (leaveResult == LeaveResult.LEFT_AND_SPECTATING) {
                    sender.sendMessage("§eTu es desinscrit des Ottergames et passe en spectateur jusqu'a la fin de la partie.");
                    return true;
                }

                sender.sendMessage("§eTu es desinscrit des Ottergames.");
                return true;
            }
            default -> {
                sender.sendMessage("§cCommande inconnue. Utilisez §f/ottergames start§c, §f/ottergames stop §cou §f/ottergames leave§c.");
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
        StringUtil.copyPartialMatches(typed, Arrays.asList("start", "stop", "leave"), completions);
        return completions;
    }

}
