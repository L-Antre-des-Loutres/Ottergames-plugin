package org.antredesloutres.ottergames.commands;

import org.antredesloutres.ottergames.Main;
import org.antredesloutres.ottergames.managers.GameManager;
import org.antredesloutres.ottergames.managers.GameManager.LeaveResult;
import org.antredesloutres.ottergames.models.minigames.Minigame;
import org.antredesloutres.ottergames.utils.Constants;
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

    private final Main plugin;
    private final GameManager gameManager;

    /**
     * Constructor for the Ottergames command handler.
     * @param plugin The Main plugin instance.
     * @param gameManager The GameManager instance used to manage game state and player participation.
     */
    public Ottergames(Main plugin, GameManager gameManager) {
        this.plugin = plugin;
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
                if (!gameManager.startGameLoop()) {
                    sender.sendMessage(OTTERGAMES_START_BLOCKED);
                    return true;
                }
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
            case "config" -> handleConfig(sender, args);
            default -> sender.sendMessage(OTTERGAMES_UNKNOWN_COMMAND);
        }

        return true;
    }

    private void handleConfig(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ottergames.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Constants.CONFIG_USAGE);
            return;
        }

        String category = args[1].toLowerCase(Locale.ROOT);
        GameManager gm = gameManager;

        switch (category) {
            case "games" -> handleGamesConfig(sender, args, gm);
            case "rules" -> handleRulesConfig(sender, args, gm);
            default -> sender.sendMessage(Constants.CONFIG_USAGE);
        }
    }

    private void handleGamesConfig(CommandSender sender, String[] args, GameManager gm) {
        if (args.length < 3) {
            sender.sendMessage(Constants.CONFIG_GAMES_USAGE);
            return;
        }

        String sub = args[2].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list" -> {
                sender.sendMessage(Constants.CONFIG_LIST_HEADER);
                for (Minigame game : gm.getGames()) {
                    String status = gm.getConfigManager().getGameConfig().isGameEnabled(game.getName())
                            ? Constants.CONFIG_GAME_ENABLED : Constants.CONFIG_GAME_DISABLED;
                    sender.sendMessage(String.format(Constants.CONFIG_GAME_ENTRY, game.getName(), status));
                }
            }
            case "enable", "disable" -> {
                if (args.length < 4) {
                    sender.sendMessage("§cUsage: /ottergames config games " + sub + " <game_name>");
                    return;
                }
                String gameName = args[3];
                Minigame target = null;
                for (Minigame g : gm.getGames()) {
                    if (g.getName().equalsIgnoreCase(gameName)) {
                        target = g;
                        break;
                    }
                }

                if (target == null) {
                    sender.sendMessage(String.format(Constants.CONFIG_GAME_NOT_FOUND, gameName));
                    return;
                }

                boolean enable = sub.equals("enable");
                gm.getConfigManager().getGameConfig().setGameEnabled(target.getName(), enable);
                gm.getConfigManager().save();
                String statusLabel = enable ? Constants.CONFIG_GAME_ENABLED : Constants.CONFIG_GAME_DISABLED;
                sender.sendMessage(String.format(Constants.CONFIG_GAME_SET, target.getName(), statusLabel));
            }
            default -> sender.sendMessage(Constants.CONFIG_GAMES_USAGE);
        }
    }

    private void handleRulesConfig(CommandSender sender, String[] args, GameManager gm) {
        if (args.length < 3) {
            sender.sendMessage(Constants.CONFIG_RULES_USAGE);
            return;
        }

        String sub = args[2].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list" -> {
                sender.sendMessage(Constants.CONFIG_RULES_HEADER);
                String status = gm.getConfigManager().getGameConfig().isPreventSameGameConsecutively()
                        ? Constants.CONFIG_GAME_ENABLED : Constants.CONFIG_GAME_DISABLED;
                sender.sendMessage(String.format(Constants.CONFIG_STATUS_LINE, status));
                sender.sendMessage(String.format(Constants.CONFIG_LIVES_STATUS, gm.getConfigManager().getGameConfig().getMaxLives()));
            }
            case "preventconsecutive" -> {
                if (args.length < 4) {
                    sender.sendMessage(Constants.CONFIG_USAGE_PREVENT_CONSECUTIVE);
                    return;
                }
                boolean prevent = Boolean.parseBoolean(args[3]);
                gm.getConfigManager().getGameConfig().setPreventSameGameConsecutively(prevent);
                gm.getConfigManager().save();
                String statusLabel = prevent ? Constants.CONFIG_GAME_ENABLED : Constants.CONFIG_GAME_DISABLED;
                sender.sendMessage(String.format(Constants.CONFIG_PREVENT_CONSECUTIVE_SET, statusLabel));
            }
            case "lives" -> {
                if (args.length < 4) {
                    sender.sendMessage(Constants.CONFIG_USAGE_LIVES);
                    return;
                }
                int lives;
                try {
                    lives = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(Constants.CONFIG_LIVES_INVALID);
                    return;
                }
                if (lives < 1) {
                    sender.sendMessage(Constants.CONFIG_LIVES_INVALID);
                    return;
                }
                gm.getConfigManager().getGameConfig().setMaxLives(lives);
                gm.getConfigManager().save();
                sender.sendMessage(String.format(Constants.CONFIG_LIVES_SET, lives));
            }
            case "minplayerstocontinue" -> {
                if (args.length < 4) {
                    sender.sendMessage(Constants.CONFIG_USAGE_MIN_PLAYERS);
                    return;
                }
                try {
                    int min = Integer.parseInt(args[3]);
                    if (min < 0) throw new NumberFormatException();
                    gm.getConfigManager().getGameConfig().setMinPlayersToContinue(min);
                    gm.getConfigManager().save();
                    sender.sendMessage(String.format(Constants.CONFIG_MIN_PLAYERS_SET, min));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Constants.CONFIG_USAGE_MIN_PLAYERS);
                }
            }
            default -> sender.sendMessage(Constants.CONFIG_RULES_USAGE);
        }
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
            StringUtil.copyPartialMatches(args[0].toLowerCase(Locale.ROOT), List.of(OTTERGAMES_ARGS_START, OTTERGAMES_ARGS_STOP, OTTERGAMES_ARGS_LEAVE, "config"), completions);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("config")) {
            StringUtil.copyPartialMatches(args[1].toLowerCase(Locale.ROOT), List.of("games", "rules"), completions);
        } else if (args.length == 3 && args[0].equalsIgnoreCase("config")) {
            if (args[1].equalsIgnoreCase("games")) {
                StringUtil.copyPartialMatches(args[2].toLowerCase(Locale.ROOT), List.of("list", "enable", "disable"), completions);
            } else if (args[1].equalsIgnoreCase("rules")) {
                StringUtil.copyPartialMatches(args[2].toLowerCase(Locale.ROOT), List.of("list", "preventConsecutive", "lives", "minplayerstocontinue"), completions);
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("config")) {
            if (args[1].equalsIgnoreCase("games") && (args[2].equalsIgnoreCase("enable") || args[2].equalsIgnoreCase("disable"))) {
                List<String> gameNames = new ArrayList<>();
                for (Minigame g : gameManager.getGames()) gameNames.add(g.getName());
                StringUtil.copyPartialMatches(args[3].toLowerCase(Locale.ROOT), gameNames, completions);
            } else if (args[1].equalsIgnoreCase("rules") && args[2].equalsIgnoreCase("preventConsecutive")) {
                StringUtil.copyPartialMatches(args[3].toLowerCase(Locale.ROOT), List.of("true", "false"), completions);
            } else if (args[1].equalsIgnoreCase("rules") && args[2].equalsIgnoreCase("lives")) {
                StringUtil.copyPartialMatches(args[3].toLowerCase(Locale.ROOT), List.of("1", "2", "3", "5"), completions);
            }
        }
        return completions;
    }
}
