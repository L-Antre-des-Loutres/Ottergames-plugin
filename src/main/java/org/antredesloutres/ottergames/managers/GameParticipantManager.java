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
    private final Set<UUID> optedOutPlayers = new HashSet<>();
    private final Set<UUID> disconnectedDuringGamePlayers = new HashSet<>();
    private final Random random = new Random();

    public boolean handlePlayerJoin(Player player, boolean gameRunning) {
        UUID playerId = player.getUniqueId();
        if (optedOutPlayers.contains(playerId)) {
            participants.remove(playerId);
            return gameRunning;
        }

        if (disconnectedDuringGamePlayers.contains(playerId) && gameRunning) {
            participants.put(playerId, new GamePlayer(playerId, player.getName(), true));
            return true;
        }

        if (gameRunning) {
            participants.put(playerId, new GamePlayer(playerId, player.getName(), true));
            return true;
        }

        participants.put(playerId, new GamePlayer(playerId, player.getName(), false));
        return false;
    }

    public LeaveResult handlePlayerLeave(Player player, boolean gameRunning) {
        UUID playerId = player.getUniqueId();
        if (optedOutPlayers.contains(playerId)) {
            return LeaveResult.ALREADY_LEFT;
        }

        optedOutPlayers.add(playerId);
        disconnectedDuringGamePlayers.remove(playerId);
        if (gameRunning) {
            participants.put(playerId, new GamePlayer(playerId, player.getName(), true));
            return LeaveResult.LEFT_AND_SPECTATING;
        }

        participants.remove(playerId);
        return LeaveResult.LEFT;
    }

    public void handlePlayerQuit(Player player, boolean gameRunning) {
        UUID playerId = player.getUniqueId();
        GamePlayer existingGamePlayer = participants.get(playerId);
        if (optedOutPlayers.contains(playerId)) {
            participants.remove(playerId);
            return;
        }

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

    public void registerOnlinePlayersAsParticipants() {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            UUID playerId = onlinePlayer.getUniqueId();
            if (optedOutPlayers.contains(playerId)) {
                continue;
            }

            participants.put(playerId, new GamePlayer(playerId, onlinePlayer.getName(), false));
        }
    }

    public void clearDisconnectedDuringGamePlayers() {
        disconnectedDuringGamePlayers.clear();
    }

    public void clearAll() {
        participants.clear();
        optedOutPlayers.clear();
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

    public boolean isPlayerOptedOut(UUID playerId) {
        return optedOutPlayers.contains(playerId);
    }

    public boolean isPlayerDisconnectedDuringGame(UUID playerId) {
        return disconnectedDuringGamePlayers.contains(playerId);
    }

    public boolean isPlayerSpectator(UUID playerId) {
        GamePlayer gamePlayer = participants.get(playerId);
        return gamePlayer != null && gamePlayer.isSpectator();
    }

    public Set<UUID> getOptedOutPlayerIds() {
        return Set.copyOf(optedOutPlayers);
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
