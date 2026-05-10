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
    private final ConfigManager configManager;

    private final Lobby lobbyGame;
    private List<ArenaInstance> lobbyArenas = Collections.emptyList();
    private Minigame currentGame;
    private Minigame lastGame = null;
    private List<ArenaInstance> currentArenas = Collections.emptyList();
    private Minigame nextGame = null;
    private List<ArenaInstance> nextArenas = Collections.emptyList();
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
        this.configManager = new ConfigManager(plugin);
        this.participantManager = new GameParticipantManager();
        this.lobbyGame = new Lobby(plugin);

        // Add games
        this.games.add(new Dropper());
        this.games.add(new Spleef(plugin));
        this.games.add(new Hikabrain(plugin));
        this.games.add(new Clutch(plugin));
        this.games.add(new AnvilGame(plugin));
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public List<Minigame> getGames() {
        return Collections.unmodifiableList(games);
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
            plugin.getLogger().warning(String.format(Constants.LOGGER_NO_GAME_ROUND_START,
                    initialSelectionContext.roundNumber(),
                    initialSelectionContext.activeParticipantCount(),
                    initialSelectionContext.spectatorCount(),
                    initialSelectionContext.totalParticipantCount()
            ));
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
        prepareNextMinigame();

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
            plugin.getLogger().info(String.format(Constants.LOGGER_MINIGAME_STOPPED, currentGame.getName()));
        }

        // Free lobby
        if (!lobbyArenas.isEmpty()) {
            arenaSlotManager.free(lobbyArenas);
            lobbyArenas = Collections.emptyList();
        }

        // Free pre-loaded next game arenas (may exist if stopped during break time)
        if (!nextArenas.isEmpty()) {
            arenaSlotManager.free(nextArenas);
            nextArenas = Collections.emptyList();
        }
        nextGame = null;
        lastGame = null; // Reset last game played on full stop

        clearAllParticipantsInventories();
        clearAllParticipantsXp();

        // Teleport everyone to world spawn and reset health/state
        for (GamePlayer gamePlayer : participantManager.getParticipants()) {
            Player player = Bukkit.getPlayer(gamePlayer.getUuid());
            if (player != null && player.isOnline()) {
                player.teleport(player.getWorld().getSpawnLocation());
                resetForLobby(player, GameMode.SURVIVAL); // Reset health, effects, and mode
            }
        }

        playerSpawnLocations.clear();
        playerArenaAssignments.clear();

        if (this.loopTask != null) {
            this.loopTask.cancel();
            this.loopTask = null;
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

    public GameParticipantManager.JoinStatus handlePlayerJoin(Player player) {
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
        if (nextGame == null) {
            Bukkit.broadcastMessage(Constants.GAME_MANAGER_NO_GAME_AVAILABLE);
            plugin.getLogger().warning(Constants.LOGGER_NO_GAME_PRESELECTED);
            stopEverything();
            return;
        }

        currentGame = nextGame;
        currentArenas = nextArenas;
        nextGame = null;
        nextArenas = Collections.emptyList();
        currentRound++;
        isPaused = false;
        timer = currentGame.getDurationSeconds();
        maxTimer = timer;

        teleportActiveParticipantsToArenas();
        currentGame.onStart(currentArenas, this);

        // Apply starting inventories after onStart (allows minigames to initialize teams/data first)
        for (UUID playerId : playerArenaAssignments.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline() && !participantManager.isPlayerSpectator(playerId)) {
                currentGame.onGamePlayerSpawn(player);
            }
        }

        teleportSpectatorsToArena();

        // Register minigame events if it implements Listener
        if (currentGame instanceof org.bukkit.event.Listener listener) {
            Bukkit.getPluginManager().registerEvents(listener, plugin);
        }

        plugin.getLogger().info(String.format(Constants.LOGGER_MINIGAME_STARTED, currentGame.getName(), timer));
    }

    private void prepareNextMinigame() {
        GameSelectionContext selectionContext = buildSelectionContext(currentRound + 1);
        List<Minigame> selectableGames = getSelectableGames(selectionContext);

        if (selectableGames.isEmpty()) {
            nextGame = null;
            nextArenas = Collections.emptyList();
            plugin.getLogger().warning(String.format(Constants.LOGGER_NO_GAME_FOR_ROUND,
                    selectionContext.roundNumber(),
                    selectionContext.activeParticipantCount(),
                    selectionContext.spectatorCount(),
                    selectionContext.totalParticipantCount()
            ));
            return;
        }

        nextGame = selectableGames.get(random.nextInt(selectableGames.size()));
        nextArenas = arenaSlotManager.allocate(nextGame.getStructureName(), nextGame.getInstanceCount(selectionContext));
        Bukkit.broadcastMessage(String.format(Constants.GAME_MANAGER_NEXT_GAME, nextGame.getName()));
        plugin.getLogger().info(String.format(Constants.LOGGER_NEXT_GAME_PRELOADING, nextGame.getName(), nextArenas.size()));
    }

    private void stopCurrentMinigame() {
        String gameName = currentGame.getName();
        lastGame = currentGame;

        currentGame.onEnd(this);
        arenaSlotManager.free(currentArenas);
        currentArenas = Collections.emptyList();
        playerSpawnLocations.clear();
        playerArenaAssignments.clear();

        participantManager.clearDisconnectedDuringGamePlayers();
        clearAllParticipantsInventories();

        plugin.getLogger().info(String.format(Constants.LOGGER_MINIGAME_ENDED, gameName));
        isPaused = true;
        currentGame = lobbyGame;
        timer = BREAK_TIME_SECONDS;
        maxTimer = timer;
        Bukkit.broadcastMessage(String.format(Constants.GAME_MANAGER_BREAK_TIME, timer));

        teleportToLobby();
        prepareNextMinigame();
    }

    private void teleportToLobby() {
        if (lobbyArenas.isEmpty()) return;
        ArenaInstance lobby = lobbyArenas.getFirst();

        List<GamePlayer> players = participantManager.getParticipants();
        for (int i = 0; i < players.size(); i++) {
            GamePlayer gamePlayer = players.get(i);
            Player player = Bukkit.getPlayer(gamePlayer.getUuid());
            if (player == null || !player.isOnline()) continue;

            boolean isSpectator = participantManager.isPlayerSpectator(player.getUniqueId());
            Location spawn = isSpectator ? lobbyGame.getSpectatorSpawnLocation(lobby, random) : lobbyGame.getSpawnLocation(lobby, random, i, players.size());
            
            playerSpawnLocations.put(player.getUniqueId(), spawn.clone());
            playerArenaAssignments.put(player.getUniqueId(), lobby);
            player.teleport(spawn);
            resetForLobby(player, GameMode.ADVENTURE);
            if (isSpectator) {
                lobbyGame.onGameSpectatorSpawn(player);
            } else {
                lobbyGame.onGamePlayerSpawn(player);
            }
        }
    }

    private void resetForLobby(Player player, GameMode gameMode) {
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

        player.setGameMode(gameMode);
        player.setInvulnerable(false);
        player.setAbsorptionAmount(0);
        player.setAllowFlight(gameMode == GameMode.CREATIVE || gameMode == GameMode.SPECTATOR);
        player.setFlying(gameMode == GameMode.CREATIVE || gameMode == GameMode.SPECTATOR);
        player.setCollidable(true);
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
        String subtitleMessage = (nextGame != null)
                ? String.format(Constants.GAME_MANAGER_STARTING_GAME, nextGame.getName())
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
        // 1. Identify all enabled and selectable games (ignoring consecutive rule for now)
        List<Minigame> availableGames = new ArrayList<>();
        for (Minigame game : games) {
            if (configManager.getGameConfig().isGameEnabled(game.getName()) && game.canBeSelected(selectionContext)) {
                availableGames.add(game);
            }
        }

        if (availableGames.isEmpty()) {
            return Collections.emptyList();
        }

        boolean preventConsecutive = configManager.getGameConfig().isPreventSameGameConsecutively();

        // 2. If the consecutive rule is disabled, or no last game exists, or only 1 game is available, 
        // return all available games (no filtering needed).
        if (!preventConsecutive || lastGame == null || availableGames.size() <= 1) {
            return availableGames;
        }

        // 3. Apply the consecutive filter
        List<Minigame> filteredGames = new ArrayList<>();
        for (Minigame game : availableGames) {
            if (!game.getName().equals(lastGame.getName())) {
                filteredGames.add(game);
            }
        }

        // 4. Safety: if filtering removed everything (shouldn't happen if size > 1), return all available
        return filteredGames.isEmpty() ? availableGames : filteredGames;
    }

    private GameSelectionContext buildSelectionContext(int roundNumber) {
        int activeParticipantCount = participantManager.getActiveParticipantCount();
        int spectatorCount = participantManager.getSpectatorParticipantCount();
        int totalParticipantCount = activeParticipantCount + spectatorCount;
        return new GameSelectionContext(roundNumber, activeParticipantCount, spectatorCount, totalParticipantCount);
    }

    private void teleportSpectatorsToArena() {
        if (currentArenas.isEmpty()) return;
        ArenaInstance arena = currentArenas.getFirst();

        for (GamePlayer spec : participantManager.getSpectatorParticipants()) {
            Player player = Bukkit.getPlayer(spec.getUuid());
            if (player == null || !player.isOnline()) continue;

            Location spawn = currentGame.getSpectatorSpawnLocation(arena, random);
            if (spawn.getWorld() == null) continue;
            playerSpawnLocations.put(player.getUniqueId(), spawn.clone());
            playerArenaAssignments.put(player.getUniqueId(), arena);
            player.teleport(spawn);
            currentGame.onGameSpectatorSpawn(player);
        }
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
                    plugin.getLogger().warning(String.format(Constants.LOGGER_TELEPORT_WORLD_NULL, player.getName()));
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

    public boolean isPlayerSpectator(UUID playerId) {
        return participantManager.isPlayerSpectator(playerId);
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
