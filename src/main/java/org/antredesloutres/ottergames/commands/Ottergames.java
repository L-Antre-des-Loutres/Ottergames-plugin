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
import java.util.List;
import java.util.Locale;

import static org.antredesloutres.ottergames.utils.Constants.*;

public class Ottergames implements TabExecutor {

    private final GameManager gameManager;

    public Ottergames(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, @NonNull String @NonNull [] args) {
        if (args.length == 0) {
            sender.sendMessage(OTTERGAMES_USAGE);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "start" -> {
                if (gameManager.isRunning()) {
                    sender.sendMessage(OTTERGAMES_ALREADY_RUNNING);
                    return true;
                }
                gameManager.startGameLoop();
                sender.sendMessage(OTTERGAMES_STARTED);
            }
            case "stop" -> {
                if (!gameManager.isRunning()) {
                    sender.sendMessage(OTTERGAMES_NOT_RUNNING);
                    return true;
                }
                gameManager.stopEverything();
                sender.sendMessage(OTTERGAMES_STOPPED);
            }
            case "leave" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(COMMAND_USER_MUST_BE_PLAYER);
                    return true;
                }
                LeaveResult result = gameManager.handlePlayerLeave(player);
                sender.sendMessage(switch (result) {
                    case ALREADY_LEFT      -> OTTERGAMES_ALREADY_LEFT;
                    case LEFT_AND_SPECTATING -> OTTERGAMES_LEFT_SPECTATING;
                    default                -> OTTERGAMES_LEFT;
                });
            }
            default -> sender.sendMessage(OTTERGAMES_UNKNOWN_COMMAND);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NonNull CommandSender sender, @NonNull Command command, @NonNull String alias, @NonNull String @NonNull [] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0].toLowerCase(Locale.ROOT), List.of("start", "stop", "leave"), completions);
        }
        return completions;
    }
}
