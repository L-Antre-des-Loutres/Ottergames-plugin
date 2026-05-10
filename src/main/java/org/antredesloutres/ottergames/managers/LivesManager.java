package org.antredesloutres.ottergames.managers;

import org.antredesloutres.ottergames.models.participant.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class LivesManager {

    private final ConfigManager configManager;
    private final Map<UUID, Integer> playerLives = new HashMap<>();
    private Set<UUID> roundStartActivePlayers = new HashSet<>();

    public LivesManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void initialize(List<GamePlayer> participants) {
        int maxLives = configManager.getGameConfig().getMaxLives();
        playerLives.clear();
        for (GamePlayer gp : participants) {
            playerLives.put(gp.getUuid(), maxLives);
        }
    }

    public void snapshotRoundStart(List<GamePlayer> activeParticipants) {
        roundStartActivePlayers = new HashSet<>();
        for (GamePlayer gp : activeParticipants) {
            roundStartActivePlayers.add(gp.getUuid());
        }
    }

    public void applyRoundResult(GameParticipantManager participantManager, ScoreboardManager scoreboardManager) {
        for (UUID playerId : roundStartActivePlayers) {
            if (!participantManager.isPlayerSpectator(playerId)) {
                scoreboardManager.recordSurvivedGame(playerId);
            } else {
                int remaining = playerLives.merge(playerId, -1, Integer::sum);
                if (remaining > 0) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null && player.isOnline()) {
                        participantManager.restoreAsActive(playerId);
                    }
                }
            }
        }
        roundStartActivePlayers.clear();
    }

    public int getPlayerLives(UUID playerId) {
        return playerLives.getOrDefault(playerId, 0);
    }

    public Map<UUID, Integer> getPlayerLivesMap() {
        return Collections.unmodifiableMap(playerLives);
    }

    public void clear() {
        playerLives.clear();
        roundStartActivePlayers.clear();
    }
}
