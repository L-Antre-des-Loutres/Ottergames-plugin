package org.antredesloutres.ottergames.managers;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

    private static final int PLACEHOLDER_LIVES = 3;
    private static final int PLACEHOLDER_POINTS = 0;
    // Index 0 = top (highest score), index 7 = bottom
    private static final String[] LINE_KEYS = {"og_l8", "og_l7", "og_l6", "og_l5", "og_l4", "og_l3", "og_l2", "og_l1"};

    private final Map<UUID, Scoreboard> playerBoards = new HashMap<>();
    private final Map<UUID, Integer> gamesSurvived = new HashMap<>();

    public void init() {
        playerBoards.clear();
        gamesSurvived.clear();
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
            Component.text(Constants.PLUGIN_NAME, NamedTextColor.GOLD)
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

    public void updateAll(int activePlayers, int totalPlayers, int completedGames, Component statusLine, String currentGameName, Set<UUID> spectators) {
        for (UUID id : playerBoards.keySet()) {
            updateBoard(id, activePlayers, totalPlayers, completedGames, statusLine, currentGameName, spectators.contains(id));
        }
    }

    private void updateBoard(UUID playerId, int activePlayers, int totalPlayers, int completedGames, Component statusLine, String currentGameName, boolean isSpectator) {
        Scoreboard board = playerBoards.get(playerId);
        if (board == null) return;
        Objective obj = board.getObjective("ottergames");
        if (obj == null) return;

        int survived = gamesSurvived.getOrDefault(playerId, 0);

        Component roleComponent = isSpectator
            ? Component.text(Constants.SCOREBOARD_ROLE_SPECTATOR, NamedTextColor.GRAY)
            : Component.text(Constants.SCOREBOARD_ROLE_PLAYER, NamedTextColor.GREEN);

        setLine(obj, 0, Component.text(currentGameName, NamedTextColor.AQUA));
        setLine(obj, 1, statusLine);
        setLine(obj, 2, Component.empty());
        setLine(obj, 3, roleComponent);
        setLine(obj, 4, line(Constants.SCOREBOARD_LIVES_LABEL, String.valueOf(PLACEHOLDER_LIVES)));
        setLine(obj, 5, line(Constants.SCOREBOARD_POINTS_LABEL, String.valueOf(PLACEHOLDER_POINTS)));
        setLine(obj, 6, line(Constants.SCOREBOARD_PLAYERS_LABEL, activePlayers + "/" + totalPlayers));
        setLine(obj, 7, line(Constants.SCOREBOARD_GAMES_LABEL, survived + "/" + completedGames));
    }

    private Component line(String label, String value) {
        return Component.text(label, NamedTextColor.GRAY).append(Component.text(value, NamedTextColor.WHITE));
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
