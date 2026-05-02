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

    /**
     * Constructor for the Ottergames command handler.
     * @param gameManager The GameManager instance used to manage game state and player participation.
     */
    public Ottergames(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * Handles the execution of the /ottergames command and its subcommands.
     * @param sender Source of the command
     * @param command Command which was executed
     * @param label Alias of the command which was used
     * @param args Passed command arguments
     * @return true if the command was handled successfully, false otherwise
     */
    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, @NonNull String @NonNull [] args) {
        if (args.length == 0) {
            sender.sendMessage(OTTERGAMES_USAGE);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case OTTERGAMES_ARGS_START -> {
                if (gameManager.isRunning()) {
                    sender.sendMessage(OTTERGAMES_ALREADY_RUNNING);
                    return true;
                }
                gameManager.startGameLoop();
                sender.sendMessage(OTTERGAMES_STARTED);
            }
            case OTTERGAMES_ARGS_STOP -> {
                if (!gameManager.isRunning()) {
                    sender.sendMessage(OTTERGAMES_NOT_RUNNING);
                    return true;
                }
                gameManager.stopEverything();
                sender.sendMessage(OTTERGAMES_STOPPED);
            }
            case OTTERGAMES_ARGS_LEAVE -> {
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

    /**
     * Provides tab completion for the /ottergames command and its subcommands.
     * @param sender Source of the command (For tab-completing inside a command block, this will be the player not the command block).
     * @param command Command which was executed
     * @param alias Alias of the command which was used
     * @param args The arguments passed to the command, including final partial argument to be completed
     * @return A list of possible completions for the final argument, or an empty list if there are no completions
     */
    @Override
    public List<String> onTabComplete(@NonNull CommandSender sender, @NonNull Command command, @NonNull String alias, @NonNull String @NonNull [] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0].toLowerCase(Locale.ROOT), List.of(OTTERGAMES_ARGS_START, OTTERGAMES_ARGS_STOP, OTTERGAMES_ARGS_LEAVE), completions);
        }
        return completions;
    }
}
