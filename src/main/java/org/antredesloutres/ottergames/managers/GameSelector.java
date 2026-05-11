package org.antredesloutres.ottergames.managers;

import org.antredesloutres.ottergames.models.minigames.Minigame;
import org.antredesloutres.ottergames.models.minigames.selection.GameSelectionContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameSelector {

    private final List<Minigame> games;
    private final ConfigManager configManager;
    private Minigame lastGame = null;

    public GameSelector(List<Minigame> games, ConfigManager configManager) {
        this.games = games;
        this.configManager = configManager;
    }

    public GameSelectionContext buildSelectionContext(int roundNumber, GameParticipantManager participantManager) {
        int active = participantManager.getActiveParticipantCount();
        int spectators = participantManager.getSpectatorParticipantCount();
        return new GameSelectionContext(roundNumber, active, spectators, active + spectators);
    }

    public List<Minigame> getSelectableGames(GameSelectionContext selectionContext) {
        int minRequired = configManager.getGameConfig().getMinPlayersToContinue();

        List<Minigame> available = new ArrayList<>();
        for (Minigame game : games) {
            if (configManager.getGameConfig().isGameEnabled(game.getName())
                    && game.canBeSelected(selectionContext)
                    && selectionContext.activeParticipantCount() >= minRequired) {
                available.add(game);
            }
        }

        if (available.isEmpty()) return Collections.emptyList();

        boolean preventConsecutive = configManager.getGameConfig().isPreventSameGameConsecutively();

        if (!preventConsecutive || lastGame == null || available.size() <= 1) {
            return available;
        }

        List<Minigame> filtered = new ArrayList<>();
        for (Minigame game : available) {
            if (!game.getName().equals(lastGame.getName())) {
                filtered.add(game);
            }
        }

        return filtered.isEmpty() ? available : filtered;
    }

    public void setLastGame(Minigame game) {
        this.lastGame = game;
    }

    public Minigame getLastGame() {
        return lastGame;
    }

    public void reset() {
        lastGame = null;
    }
}
