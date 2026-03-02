package org.antredesloutres.ottergames.commands;

import org.antredesloutres.ottergames.GameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;

public class Otter implements CommandExecutor {

    private final GameManager gameManager;

    public Otter(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, @NonNull String @NonNull [] args) {

        if (args.length == 0) {
            sender.sendMessage("§b[OtterGames] §7Utilisez §f/otter start §7pour lancer l'enchaînement.");
            return true;
        }

        if (args[0].equalsIgnoreCase("start")) {
            if (gameManager.isRunning()) {
                sender.sendMessage("§cLa partie est déjà en cours !");
                return true;
            }

            gameManager.startGameLoop();
            sender.sendMessage("§a§lLancement de la série de mini-jeux !");
            return true;
        }

        if (args[0].equalsIgnoreCase("stop")) {
            gameManager.stopEverything();
            sender.sendMessage("§cPartie interrompue.");
            return true;
        }

        return false;
    }

}