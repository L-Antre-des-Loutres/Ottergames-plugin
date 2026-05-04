package org.antredesloutres.ottergames.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.antredesloutres.ottergames.Main;
import org.antredesloutres.ottergames.models.minigames.*;
import org.antredesloutres.ottergames.models.arena.ArenaInstance;
import org.antredesloutres.ottergames.models.minigames.selection.GameSelectionContext;
import org.antredesloutres.ottergames.models.participant.GamePlayer;
import org.antredesloutres.ottergames.utils.Constants;
import org.antredesloutres.ottergames.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class GameManager {

    private static final int INITIAL_COUNTDOWN_SECONDS = 5;
    private static final int BREAK_TIME_SECONDS = 5;

    private final List<Minigame> games = new ArrayList<>();
    private final GameParticipantManager participantManager;
    private final Random random = new Random();
    private final Main plugin;
    private final ArenaSlotManager arenaSlotManager;

    private final Lobby lobbyGame;
    private List<ArenaInstance> lobbyArenas = Collections.emptyList();
    private Minigame currentGame;
    private List<ArenaInstance> currentArenas = Collections.emptyList();
    private final Map<UUID, Location> playerSpawnLocations = new HashMap<>();
    private final Map<UUID, ArenaInstance> playerArenaAssignments = new HashMap<>();
    private int timer;
    private int maxTimer;
    private int startCountdownSecondsRemaining;
    private int currentRound;
    private boolean isPaused = true;
    private boolean running = false;
    private BukkitTask loopTask;

    public GameManager(Main plugin) {
        this.plugin = plugin;
        this.arenaSlotManager = new ArenaSlotManager(plugin);
        this.participantManager = new GameParticipantManager();
        this.lobbyGame = new Lobby(plugin);

        // Add games
        this.games.add(new Dropper());
        this.games.add(new Spleef(plugin));
        this.games.add(new Hikabrain(plugin));
    }

    public boolean startGameLoop() {
        if (running) return false;

        if (loopTask != null) {
            loopTask.cancel();
            loopTask = null;
        }

        participantManager.registerOnlinePlayersAsParticipants();
        GameSelectionContext initialSelectionContext = buildSelectionContext(1);
        if (getSelectableGames(initialSelectionContext).isEmpty()) {
            plugin.getLogger().warning(
                    "Cannot start game loop: no minigame available for round " + initialSelectionContext.roundNumber()
                            + " (activeParticipants=" + initialSelectionContext.activeParticipantCount()
                            + ", spectators=" + initialSelectionContext.spectatorCount()
                            + ", total=" + initialSelectionContext.totalParticipantCount() + ")."
            );
            return false;
        }

        // Allocate lobby
        lobbyArenas = arenaSlotManager.allocate(lobbyGame.getStructureName(), 1);

        this.running = true;
        this.isPaused = true;
        this.timer = 0;
        this.maxTimer = 0;
        this.startCountdownSecondsRemaining = INITIAL_COUNTDOWN_SECONDS;
        this.currentRound = 0;
        this.currentGame = lobbyGame;

        teleportToLobby();

        this.loopTask = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 0L, 20L);

        plugin.getLogger().info(Constants.LOGGER_LOOP_STARTED);
        return true;
    }

    public void stopEverything() {
        if (!isPaused && currentGame != null) {
            currentGame.onEnd(this);
            arenaSlotManager.free(currentArenas);
            currentArenas = Collections.emptyList();
            plugin.getLogger().info("Minigame stopped: " + currentGame.getName() + ".");
        }

        // Free lobby
        if (!lobbyArenas.isEmpty()) {
            arenaSlotManager.free(lobbyArenas);
            lobbyArenas = Collections.emptyList();
        }

        clearAllParticipantsInventories();
        clearAllParticipantsXp();

        // Teleport everyone to world spawn
        for (GamePlayer gamePlayer : participantManager.getParticipants()) {
            Player player = Bukkit.getPlayer(gamePlayer.getUuid());
            if (player != null && player.isOnline()) {
                player.teleport(player.getWorld().getSpawnLocation());
            }
        }

        playerSpawnLocations.clear();
        playerArenaAssignments.clear();

        if (this.loopTask != null) {
            this.loopTask.cancel();
            this.loopTask = null;
        }

        for (UUID playerId : participantManager.getOptedOutPlayerIds()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.sendMessage(Constants.GAME_MANAGER_REJOINED);
            }
        }

        this.currentGame = null;
        this.isPaused = true;
        this.timer = 0;
        this.maxTimer = 0;
        this.startCountdownSecondsRemaining = 0;
        this.currentRound = 0;
        this.running = false;
        participantManager.clearAll();
        participantManager.registerOnlinePlayersAsParticipants();
        plugin.getLogger().info(Constants.LOGGER_LOOP_STOPPED);
    }

    public boolean isRunning() {
        return running;
    }

    public boolean handlePlayerJoin(Player player) {
        return participantManager.handlePlayerJoin(player, running);
    }

    public LeaveResult handlePlayerLeave(Player player) {
        LeaveResult result = switch (participantManager.handlePlayerLeave(player, running)) {
            case ALREADY_LEFT -> LeaveResult.ALREADY_LEFT;
            case LEFT_AND_SPECTATING -> LeaveResult.LEFT_AND_SPECTATING;
            case LEFT -> LeaveResult.LEFT;
        };

        if (result == LeaveResult.LEFT) {
            clearPlayerXp(player);
        }

        return result;
    }

    public void handlePlayerQuit(Player player) {
        participantManager.handlePlayerQuit(player, running);
    }

    private void tick() {
        if (!running) return;

        if (startCountdownSecondsRemaining > 0) {
            showStartCountdown(startCountdownSecondsRemaining);
            updateXpBar(startCountdownSecondsRemaining, INITIAL_COUNTDOWN_SECONDS);
            startCountdownSecondsRemaining--;
            return;
        }

        if (timer > 0) {
            updateXpBar(timer, maxTimer);
            timer--;
        } else {
            if (isPaused) {
                startNextMinigame();
            } else {
                stopCurrentMinigame();
            }
            updateXpBar(timer, maxTimer);
        }
    }

    private void updateXpBar(int current, int max) {
        float progress = (max > 0) ? (float) current / max : 0f;
        for (GamePlayer gamePlayer : participantManager.getParticipants()) {
            Player player = Bukkit.getPlayer(gamePlayer.getUuid());
            if (player != null && player.isOnline()) {
                player.setLevel(current);
                player.setExp(Math.min(1.0f, Math.max(0.0f, progress)));
            }
        }
    }

    private void clearAllParticipantsXp() {
        for (GamePlayer gamePlayer : participantManager.getParticipants()) {
            Player player = Bukkit.getPlayer(gamePlayer.getUuid());
            if (player != null && player.isOnline()) {
                clearPlayerXp(player);
            }
        }
    }

    private void clearPlayerXp(Player player) {
        player.setLevel(0);
        player.setExp(0f);
    }

    private void startNextMinigame() {
        GameSelectionContext selectionContext = buildSelectionContext(currentRound + 1);

        List<Minigame> selectableGames = getSelectableGames(selectionContext);
        if (selectableGames.isEmpty()) {
            currentGame = null;
            Bukkit.broadcastMessage(Constants.GAME_MANAGER_NO_GAME_AVAILABLE);
            plugin.getLogger().warning(
                    "No minigame available for round " + selectionContext.roundNumber()
                            + " (activeParticipants=" + selectionContext.activeParticipantCount()
                            + ", spectators=" + selectionContext.spectatorCount()
                            + ", total=" + selectionContext.totalParticipantCount() + "). Stopping game loop."
            );
            stopEverything();
            return;
        }

        currentGame = selectableGames.get(random.nextInt(selectableGames.size()));
        currentArenas = arenaSlotManager.allocate(currentGame.getStructureName(), currentGame.getInstanceCount(selectionContext));
        currentRound = selectionContext.roundNumber();
        isPaused = false;
        timer = currentGame.getDurationSeconds();
        maxTimer = timer;

        teleportActiveParticipantsToArenas();
        currentGame.onStart(currentArenas, this);

        // Apply starting inventories after onStart (allows minigames to initialize teams/data first)
        for (UUID playerId : playerArenaAssignments.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                currentGame.applyStartingInventory(player);
            }
        }

        // Register minigame events if it implements Listener
        if (currentGame instanceof org.bukkit.event.Listener listener) {
            Bukkit.getPluginManager().registerEvents(listener, plugin);
        }

        plugin.getLogger().info("Minigame started: " + currentGame.getName() + " (" + timer + "s).");
    }

    private void stopCurrentMinigame() {
        String gameName = currentGame.getName();

        currentGame.onEnd(this);
        arenaSlotManager.free(currentArenas);
        currentArenas = Collections.emptyList();
        playerSpawnLocations.clear();
        playerArenaAssignments.clear();

        participantManager.clearDisconnectedDuringGamePlayers();
        clearAllParticipantsInventories();

        plugin.getLogger().info("Minigame ended: " + gameName + ".");
        isPaused = true;
        currentGame = lobbyGame;
        timer = BREAK_TIME_SECONDS;
        maxTimer = timer;
        Bukkit.broadcastMessage(String.format(Constants.GAME_MANAGER_BREAK_TIME, timer));

        teleportToLobby();
    }

    private void teleportToLobby() {
        if (lobbyArenas.isEmpty()) return;
        ArenaInstance lobby = lobbyArenas.getFirst();

        List<GamePlayer> players = participantManager.getParticipants();
        for (int i = 0; i < players.size(); i++) {
            GamePlayer gamePlayer = players.get(i);
            Player player = Bukkit.getPlayer(gamePlayer.getUuid());
            if (player == null || !player.isOnline()) continue;

            Location spawn = lobbyGame.getSpawnLocation(lobby, random, i, players.size());
            playerSpawnLocations.put(player.getUniqueId(), spawn.clone());
            playerArenaAssignments.put(player.getUniqueId(), lobby);
            player.teleport(spawn);
            resetPlayerForLobby(player);
            lobbyGame.applyStartingInventory(player);
        }
    }

    private void resetPlayerForLobby(Player player) {
        // Reset GENERIC_MAX_HEALTH to its vanilla default (e.g. Dropper sets it to 1)
        var maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(maxHealth.getDefaultValue());
        }

        // Remove all potion effects except Night Vision
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType() != PotionEffectType.NIGHT_VISION) {
                player.removePotionEffect(effect.getType());
            }
        }

        player.setGameMode(GameMode.ADVENTURE);
        player.setInvulnerable(false);
        player.setAbsorptionAmount(0);
        healPlayer(player);
    }

    private void healPlayer(Player player) {
        var maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) player.setHealth(maxHealth.getValue());
        player.setFoodLevel(20);
        player.setSaturation(5.0f);
        player.setFireTicks(0);
    }

    private void showStartCountdown(int secondsRemaining) {
        String subtitleMessage = (currentGame != null && currentGame != lobbyGame)
                ? String.format(Constants.GAME_MANAGER_STARTING_GAME, currentGame.getName())
                : Constants.GAME_MANAGER_STARTING_OTTER;

        var title = Title.title(
                Component.text(String.valueOf(secondsRemaining), NamedTextColor.GOLD),
                Component.text(subtitleMessage, NamedTextColor.YELLOW),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)
        );

        Bukkit.getOnlinePlayers().forEach(player -> {
            player.showTitle(title);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.8f);
        });
    }

    private List<Minigame> getSelectableGames(GameSelectionContext selectionContext) {
        List<Minigame> selectableGames = new ArrayList<>();
        for (Minigame game : games) {
            if (game.canBeSelected(selectionContext)) {
                selectableGames.add(game);
            }
        }
        return selectableGames;
    }

    private GameSelectionContext buildSelectionContext(int roundNumber) {
        int activeParticipantCount = participantManager.getActiveParticipantCount();
        int spectatorCount = participantManager.getSpectatorParticipantCount();
        int totalParticipantCount = activeParticipantCount + spectatorCount;
        return new GameSelectionContext(roundNumber, activeParticipantCount, spectatorCount, totalParticipantCount);
    }

    private void teleportActiveParticipantsToArenas() {
        if (currentGame == null || currentArenas.isEmpty()) {
            return;
        }

        // Shuffle and group players using the participant manager
        int playersPerArena = Math.max(1, participantManager.getActiveParticipantCount() / currentArenas.size());
        List<List<GamePlayer>> playersByArena = participantManager.createActiveGroups(playersPerArena);

        for (int arenaIndex = 0; arenaIndex < playersByArena.size() && arenaIndex < currentArenas.size(); arenaIndex++) {
            ArenaInstance arena = currentArenas.get(arenaIndex);
            List<GamePlayer> playersInArena = playersByArena.get(arenaIndex);
            for (int playerIndex = 0; playerIndex < playersInArena.size(); playerIndex++) {
                GamePlayer gamePlayer = playersInArena.get(playerIndex);
                Player player = Bukkit.getPlayer(gamePlayer.getUuid());
                if (player == null || !player.isOnline()) {
                    continue;
                }

                var spawnLocation = currentGame.getSpawnLocation(arena, random, playerIndex, playersInArena.size());
                if (spawnLocation.getWorld() == null) {
                    plugin.getLogger().warning("Cannot teleport " + player.getName() + ": spawn world is null.");
                    continue;
                }
                playerSpawnLocations.put(player.getUniqueId(), spawnLocation.clone());
                playerArenaAssignments.put(player.getUniqueId(), arena);
                player.teleport(spawnLocation);
                
                clearPlayerInventory(player);
                healPlayer(player);
            }
        }
    }

    public boolean isPlayerOptedOut(UUID playerId) {
        return participantManager.isPlayerOptedOut(playerId);
    }

    public boolean isPlayerDisconnectedDuringGame(UUID playerId) {
        return participantManager.isPlayerDisconnectedDuringGame(playerId);
    }

    public List<GamePlayer> getParticipants() {
        return participantManager.getParticipants();
    }

    public List<GamePlayer> getActiveParticipants() {
        return participantManager.getActiveParticipants();
    }

    public List<List<GamePlayer>> createParticipantGroups(int groupSize) {
        return participantManager.createActiveGroups(groupSize);
    }

    public List<List<GamePlayer>> createParticipantGroups(int groupSize, boolean shuffle) {
        return participantManager.createActiveGroups(groupSize, shuffle);
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public Minigame getCurrentGame() {
        return currentGame;
    }

    public Location getPlayerSpawnLocation(UUID playerId) {
        return playerSpawnLocations.get(playerId);
    }

    // Picks a new random spawn for the player (via the current minigame's respawn logic), stores and returns it.
    // Returns null if the player has no arena assignment or no game is running.
    public Location rerandomizePlayerSpawn(UUID playerId) {
        ArenaInstance arena = playerArenaAssignments.get(playerId);
        if (arena == null || currentGame == null) return null;
        Location newSpawn = currentGame.getRespawnLocation(playerId, arena, random);
        playerSpawnLocations.put(playerId, newSpawn.clone());
        return newSpawn;
    }

    public ArenaInstance getPlayerArena(UUID playerId) {
        return playerArenaAssignments.get(playerId);
    }

    public Map<UUID, ArenaInstance> getPlayerArenaAssignments() {
        return Collections.unmodifiableMap(playerArenaAssignments);
    }

    public Map<UUID, Location> getPlayerSpawnLocations() {
        return Collections.unmodifiableMap(playerSpawnLocations);
    }

    /**
     * Finds the arena at the given location, if any (checks both current minigame arenas and the lobby).
     * @param location The location to check.
     * @return The ArenaInstance at that location, or null if none.
     */
    public ArenaInstance getArenaAt(Location location) {
        // Check current minigame arenas
        for (ArenaInstance arena : currentArenas) {
            if (arena.contains(location)) return arena;
        }
        // Check lobby
        for (ArenaInstance arena : lobbyArenas) {
            if (arena.contains(location)) return arena;
        }
        return null;
    }

    /**
     * Checks if the given location is within a lobby arena.
     * @param location The location to check.
     * @return True if inside a lobby arena, false otherwise.
     */
    public boolean isInLobby(Location location) {
        for (ArenaInstance arena : lobbyArenas) {
            if (arena.contains(location)) return true;
        }
        return false;
    }

    public boolean eliminatePlayer(UUID playerId) {
        return participantManager.eliminatePlayer(playerId);
    }

    private void clearAllParticipantsInventories() {
        for (GamePlayer gamePlayer : participantManager.getParticipants()) {
            Player player = Bukkit.getPlayer(gamePlayer.getUuid());
            if (player != null && player.isOnline()) {
                PlayerUtils.clearInventory(player);
            }
        }
    }

    public void clearPlayerInventory(Player player) {
        PlayerUtils.clearInventory(player);
    }

    public enum LeaveResult {
        LEFT,
        LEFT_AND_SPECTATING,
        ALREADY_LEFT
    }
}
