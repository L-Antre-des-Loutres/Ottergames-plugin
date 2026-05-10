package org.antredesloutres.ottergames.managers;

import org.antredesloutres.ottergames.models.participant.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class GameParticipantManager {

    private final Map<UUID, GamePlayer> participants = new HashMap<>();
    private final Set<UUID> disconnectedDuringGamePlayers = new HashSet<>();
    private final Random random = new Random();

    /**
     * Called when a player connects. Every connected player is a participant.
     * - If the player was already tracked as a spectator (eliminated or opted out), they stay spectator.
     * - If the game is running and they were not tracked, they join as spectator.
     * - Otherwise they join as an active participant.
     */
    public boolean handlePlayerJoin(Player player, boolean gameRunning) {
        UUID playerId = player.getUniqueId();
        GamePlayer existing = participants.get(playerId);
        boolean isSpectator = (existing != null && existing.isSpectator()) || gameRunning;
        participants.put(playerId, new GamePlayer(playerId, player.getName(), isSpectator));
        return isSpectator;
    }

    /**
     * Called when a player uses /leave. Opted-out = spectator, same state as eliminated.
     * The player stays in participants.
     */
    public LeaveResult handlePlayerLeave(Player player, boolean gameRunning) {
        UUID playerId = player.getUniqueId();
        GamePlayer existing = participants.get(playerId);
        if (existing != null && existing.isSpectator()) {
            return LeaveResult.ALREADY_LEFT;
        }
        participants.put(playerId, new GamePlayer(playerId, player.getName(), true));
        disconnectedDuringGamePlayers.remove(playerId);
        return gameRunning ? LeaveResult.LEFT_AND_SPECTATING : LeaveResult.LEFT;
    }

    public void handlePlayerQuit(Player player, boolean gameRunning) {
        UUID playerId = player.getUniqueId();
        GamePlayer existingGamePlayer = participants.get(playerId);

        if (!gameRunning) {
            participants.remove(playerId);
            disconnectedDuringGamePlayers.remove(playerId);
            return;
        }

        if (existingGamePlayer == null) {
            participants.put(playerId, new GamePlayer(playerId, player.getName(), true));
            return;
        }

        if (!existingGamePlayer.isSpectator()) {
            disconnectedDuringGamePlayers.add(playerId);
        }

        participants.put(playerId, new GamePlayer(playerId, existingGamePlayer.username(), true));
    }

    /**
     * Registers all online players as active participants (no exclusions).
     * Called at the start of a new game — every connected player starts active.
     */
    public void registerOnlinePlayersAsParticipants() {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            participants.put(
                onlinePlayer.getUniqueId(),
                new GamePlayer(onlinePlayer.getUniqueId(), onlinePlayer.getName(), false)
            );
        }
    }

    public void clearDisconnectedDuringGamePlayers() {
        disconnectedDuringGamePlayers.clear();
    }

    public void clearAll() {
        participants.clear();
        disconnectedDuringGamePlayers.clear();
    }

    public List<GamePlayer> getParticipants() {
        return List.copyOf(participants.values());
    }

    public List<GamePlayer> getActiveParticipants() {
        List<GamePlayer> activeParticipants = new ArrayList<>();
        for (GamePlayer participant : participants.values()) {
            if (participant.isSpectator()) continue;
            if (isPlayerOnline(participant.getUuid())) {
                activeParticipants.add(participant);
            }
        }
        return activeParticipants;
    }

    public List<GamePlayer> getSpectatorParticipants() {
        List<GamePlayer> spectatorParticipants = new ArrayList<>();
        for (GamePlayer participant : participants.values()) {
            if (!participant.isSpectator()) continue;
            if (isPlayerOnline(participant.getUuid())) {
                spectatorParticipants.add(participant);
            }
        }
        return spectatorParticipants;
    }

    public int getActiveParticipantCount() {
        return getActiveParticipants().size();
    }

    public int getSpectatorParticipantCount() {
        return getSpectatorParticipants().size();
    }

    public boolean isPlayerDisconnectedDuringGame(UUID playerId) {
        return disconnectedDuringGamePlayers.contains(playerId);
    }

    public boolean isPlayerSpectator(UUID playerId) {
        GamePlayer gamePlayer = participants.get(playerId);
        return gamePlayer != null && gamePlayer.isSpectator();
    }

    /**
     * Eliminates a player by converting them to a spectator.
     * @return true if the player was active and is now eliminated, false otherwise.
     */
    public boolean eliminatePlayer(UUID playerId) {
        GamePlayer gamePlayer = participants.get(playerId);
        if (gamePlayer == null || gamePlayer.isSpectator()) {
            return false;
        }
        participants.put(playerId, new GamePlayer(playerId, gamePlayer.username(), true));
        return true;
    }

    public boolean restoreAsActive(UUID playerId) {
        GamePlayer gamePlayer = participants.get(playerId);
        if (gamePlayer == null || !gamePlayer.isSpectator()) {
            return false;
        }
        participants.put(playerId, new GamePlayer(playerId, gamePlayer.username(), false));
        disconnectedDuringGamePlayers.remove(playerId);
        return true;
    }

    public List<List<GamePlayer>> createActiveGroups(int groupSize) {
        return createActiveGroups(groupSize, true);
    }

    public List<List<GamePlayer>> createActiveGroups(int groupSize, boolean shuffle) {
        if (groupSize <= 0) {
            throw new IllegalArgumentException("groupSize must be > 0");
        }

        List<GamePlayer> playersToGroup = new ArrayList<>(getActiveParticipants());
        if (shuffle) {
            Collections.shuffle(playersToGroup, random);
        }

        List<List<GamePlayer>> groups = new ArrayList<>();
        for (int i = 0; i < playersToGroup.size(); i += groupSize) {
            int end = Math.min(i + groupSize, playersToGroup.size());
            groups.add(List.copyOf(playersToGroup.subList(i, end)));
        }
        return groups;
    }

    private boolean isPlayerOnline(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        return player != null && player.isOnline();
    }

    public enum LeaveResult {
        LEFT,
        LEFT_AND_SPECTATING,
        ALREADY_LEFT
    }
}
