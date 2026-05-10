package org.antredesloutres.ottergames.managers;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.antredesloutres.ottergames.utils.Constants;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ScoreboardManager {

    private static final int PLACEHOLDER_POINTS = 0;
    // Index 0 = top (highest score), index 9 = bottom
    private static final String[] LINE_KEYS = {
        "og_l10", "og_l9", "og_l8", "og_l7", "og_l6",
        "og_l5",  "og_l4", "og_l3", "og_l2", "og_l1"
    };

    private final Map<UUID, Scoreboard> playerBoards = new HashMap<>();
    private final Map<UUID, Integer> gamesSurvived = new HashMap<>();
    private int animFrame = 0;

    public void init() {
        playerBoards.clear();
        gamesSurvived.clear();
        animFrame = 0;
    }

    public void assignToPlayer(Player player) {
        UUID id = player.getUniqueId();
        Scoreboard board = createBoard();
        playerBoards.put(id, board);
        player.setScoreboard(board);
    }

    private Scoreboard createBoard() {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective(
            "ottergames",
            Criteria.DUMMY,
            Component.text("✦ " + Constants.PLUGIN_NAME + " ✦", NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD)
        );
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.numberFormat(NumberFormat.blank());
        for (int i = 0; i < LINE_KEYS.length; i++) {
            obj.getScore(LINE_KEYS[i]).setScore(LINE_KEYS.length - i);
        }
        return board;
    }

    public void recordSurvivedGame(UUID playerId) {
        gamesSurvived.merge(playerId, 1, Integer::sum);
    }

    public void updateAll(int activePlayers, int totalPlayers, int completedGames,
                          boolean gameRunning, int currentRound, String nextGameName,
                          String currentGameName, Set<UUID> spectators,
                          Map<UUID, Integer> playerLives, int maxLives) {
        animFrame++;

        Component gameNameLine = buildGameNameLine(currentGameName, !gameRunning);
        Component statusLine   = buildStatusLine(gameRunning, currentRound, nextGameName);

        for (UUID id : playerBoards.keySet()) {
            int lives = playerLives.getOrDefault(id, 0);
            updateBoard(id, activePlayers, totalPlayers, completedGames,
                        gameNameLine, statusLine, spectators.contains(id), lives, maxLives);
        }
    }

    private Component buildGameNameLine(String gameName, boolean isLobby) {
        NamedTextColor nameColor = isLobby ? NamedTextColor.GOLD : NamedTextColor.AQUA;
        return Component.text("▶ ", nameColor)
            .append(Component.text(gameName, nameColor).decorate(TextDecoration.BOLD));
    }

    private Component buildStatusLine(boolean gameRunning, int currentRound, String nextGameName) {
        if (gameRunning) {
            return Component.text(
                String.format(Constants.SCOREBOARD_GAME_IN_PROGRESS, currentRound),
                NamedTextColor.YELLOW
            );
        }
        if (nextGameName != null) {
            NamedTextColor color = (animFrame % 2 == 0) ? NamedTextColor.YELLOW : NamedTextColor.GOLD;
            return Component.text(
                String.format(Constants.SCOREBOARD_NEXT_GAME, currentRound + 1, nextGameName),
                color
            );
        }
        return Component.empty();
    }

    private void updateBoard(UUID playerId, int activePlayers, int totalPlayers, int completedGames,
                             Component gameNameLine, Component statusLine, boolean isSpectator, int lives, int maxLives) {
        Scoreboard board = playerBoards.get(playerId);
        if (board == null) return;
        Objective obj = board.getObjective("ottergames");
        if (obj == null) return;

        int survived = gamesSurvived.getOrDefault(playerId, 0);

        Component separator = Component.empty();
        Component roleComponent = isSpectator
            ? Component.text(Constants.SCOREBOARD_SPECTATOR_ICON + Constants.SCOREBOARD_ROLE_SPECTATOR, NamedTextColor.GRAY)
                .decorate(TextDecoration.ITALIC)
            : Component.text(Constants.SCOREBOARD_PLAYER_ICON + Constants.SCOREBOARD_ROLE_PLAYER, NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD);

        setLine(obj, 0, separator);
        setLine(obj, 1, gameNameLine);
        setLine(obj, 2, statusLine);
        setLine(obj, 3, separator);
        setLine(obj, 4, roleComponent);
        setLine(obj, 5, Component.text(Constants.SCOREBOARD_LIVES_LABEL, NamedTextColor.GRAY)
            .append(buildHeartsComponent(lives, maxLives)));
        setLine(obj, 6, line(Constants.SCOREBOARD_POINTS_LABEL,  String.valueOf(PLACEHOLDER_POINTS)));
        setLine(obj, 7, separator);
        setLine(obj, 8, line(Constants.SCOREBOARD_PLAYERS_LABEL, activePlayers + "/" + totalPlayers));
        setLine(obj, 9, line(Constants.SCOREBOARD_GAMES_LABEL,   survived + "/" + completedGames));
    }

    private Component buildHeartsComponent(int lives, int maxLives) {
        Component hearts = Component.empty();
        for (int i = 0; i < maxLives; i++) {
            hearts = hearts.append(Component.text("❤", i < lives ? NamedTextColor.RED : NamedTextColor.DARK_GRAY));
        }
        return hearts;
    }

    private Component line(String label, String value) {
        return Component.text(label, NamedTextColor.GRAY)
            .append(Component.text(value, NamedTextColor.WHITE));
    }

    private void setLine(Objective obj, int index, Component text) {
        obj.getScore(LINE_KEYS[index]).customName(text);
    }

    public void destroy() {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(main);
        }
        playerBoards.clear();
        gamesSurvived.clear();
    }
}
