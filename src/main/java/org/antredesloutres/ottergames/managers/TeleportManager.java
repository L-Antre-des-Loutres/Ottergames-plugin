package org.antredesloutres.ottergames.managers;

import org.antredesloutres.ottergames.Main;
import org.antredesloutres.ottergames.models.arena.ArenaInstance;
import org.antredesloutres.ottergames.models.minigames.Lobby;
import org.antredesloutres.ottergames.models.minigames.Minigame;
import org.antredesloutres.ottergames.models.participant.GamePlayer;
import org.antredesloutres.ottergames.utils.Constants;
import org.antredesloutres.ottergames.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class TeleportManager {

    private final GameParticipantManager participantManager;
    private final Main plugin;
    private final Random random;
    private final Map<UUID, Location> playerSpawnLocations = new HashMap<>();
    private final Map<UUID, ArenaInstance> playerArenaAssignments = new HashMap<>();

    public TeleportManager(GameParticipantManager participantManager, Main plugin, Random random) {
        this.participantManager = participantManager;
        this.plugin = plugin;
        this.random = random;
    }

    public void teleportToLobby(Lobby lobbyGame, List<ArenaInstance> lobbyArenas) {
        if (lobbyArenas.isEmpty()) return;

        ArenaInstance lobby = lobbyArenas.getFirst();
        List<GamePlayer> players = participantManager.getParticipants();

        for (int i = 0; i < players.size(); i++) {
            GamePlayer gamePlayer = players.get(i);
            Player player = Bukkit.getPlayer(gamePlayer.getUuid());
            if (player == null || !player.isOnline()) continue;

            boolean isSpectator = participantManager.isPlayerSpectator(player.getUniqueId());
            Location spawn = isSpectator
                    ? lobbyGame.getSpectatorSpawnLocation(lobby, random)
                    : lobbyGame.getSpawnLocation(lobby, random, i, players.size());

            playerSpawnLocations.put(player.getUniqueId(), spawn.clone());
            playerArenaAssignments.put(player.getUniqueId(), lobby);
            player.teleport(spawn);
            PlayerUtils.resetForLobby(player, GameMode.ADVENTURE);
            if (isSpectator) {
                lobbyGame.onGameSpectatorSpawn(player);
            } else {
                lobbyGame.onGamePlayerSpawn(player);
            }
        }
    }

    public void teleportActivePlayers(Minigame currentGame, List<ArenaInstance> currentArenas) {
        if (currentGame == null || currentArenas.isEmpty()) return;

        int playersPerArena = Math.max(1, participantManager.getActiveParticipantCount() / currentArenas.size());
        List<List<GamePlayer>> playersByArena = participantManager.createActiveGroups(playersPerArena);

        for (int arenaIndex = 0; arenaIndex < playersByArena.size() && arenaIndex < currentArenas.size(); arenaIndex++) {
            ArenaInstance arena = currentArenas.get(arenaIndex);
            List<GamePlayer> playersInArena = playersByArena.get(arenaIndex);

            for (int playerIndex = 0; playerIndex < playersInArena.size(); playerIndex++) {
                GamePlayer gamePlayer = playersInArena.get(playerIndex);
                Player player = Bukkit.getPlayer(gamePlayer.getUuid());
                if (player == null || !player.isOnline()) continue;

                Location spawnLocation = currentGame.getSpawnLocation(arena, random, playerIndex, playersInArena.size());
                if (spawnLocation.getWorld() == null) {
                    plugin.getLogger().warning(String.format(Constants.LOGGER_TELEPORT_WORLD_NULL, player.getName()));
                    continue;
                }

                playerSpawnLocations.put(player.getUniqueId(), spawnLocation.clone());
                playerArenaAssignments.put(player.getUniqueId(), arena);
                player.teleport(spawnLocation);
                player.setInvulnerable(false);
                PlayerUtils.clearInventory(player);
                PlayerUtils.healPlayer(player);
            }
        }
    }

    public void teleportSpectators(Minigame currentGame, List<ArenaInstance> currentArenas) {
        if (currentArenas.isEmpty()) return;
        ArenaInstance arena = currentArenas.getFirst();

        for (GamePlayer spec : participantManager.getSpectatorParticipants()) {
            Player player = Bukkit.getPlayer(spec.getUuid());
            if (player == null || !player.isOnline()) continue;

            applySpectatorState(player, currentGame, arena);
        }
    }

    public void teleportSingleSpectator(Player player, Minigame game, List<ArenaInstance> arenas) {
        if (arenas.isEmpty()) return;
        ArenaInstance arena = playerArenaAssignments.getOrDefault(player.getUniqueId(), arenas.getFirst());
        applySpectatorState(player, game, arena);
    }

    private void applySpectatorState(Player player, Minigame game, ArenaInstance arena) {
        Location spawn = game.getSpectatorSpawnLocation(arena, random);
        if (spawn.getWorld() == null) return;

        playerSpawnLocations.put(player.getUniqueId(), spawn.clone());
        playerArenaAssignments.put(player.getUniqueId(), arena);
        player.teleport(spawn);
        PlayerUtils.clearInventory(player);
        player.setInvulnerable(true);
        game.onGameSpectatorSpawn(player);
    }

    public Location getPlayerSpawn(UUID playerId) {
        return playerSpawnLocations.get(playerId);
    }

    public ArenaInstance getPlayerArena(UUID playerId) {
        return playerArenaAssignments.get(playerId);
    }

    public Map<UUID, Location> getPlayerSpawnLocations() {
        return Collections.unmodifiableMap(playerSpawnLocations);
    }

    public Map<UUID, ArenaInstance> getPlayerArenaAssignments() {
        return Collections.unmodifiableMap(playerArenaAssignments);
    }

    public Location rerandomizePlayerSpawn(UUID playerId, Minigame currentGame) {
        ArenaInstance arena = playerArenaAssignments.get(playerId);
        if (arena == null || currentGame == null) return null;
        Location newSpawn = currentGame.getRespawnLocation(playerId, arena, random);
        playerSpawnLocations.put(playerId, newSpawn.clone());
        return newSpawn;
    }

    public ArenaInstance getArenaAt(Location location, List<ArenaInstance> currentArenas, List<ArenaInstance> lobbyArenas) {
        for (ArenaInstance arena : currentArenas) {
            if (arena.contains(location)) return arena;
        }
        for (ArenaInstance arena : lobbyArenas) {
            if (arena.contains(location)) return arena;
        }
        return null;
    }

    public boolean isInLobby(Location location, List<ArenaInstance> lobbyArenas) {
        for (ArenaInstance arena : lobbyArenas) {
            if (arena.contains(location)) return true;
        }
        return false;
    }

    public void clear() {
        playerSpawnLocations.clear();
        playerArenaAssignments.clear();
    }
}
