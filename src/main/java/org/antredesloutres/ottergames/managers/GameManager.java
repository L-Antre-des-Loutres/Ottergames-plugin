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
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
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
    private final ScoreboardManager scoreboardManager;
    private final Lobby lobbyGame;
    private final GameSelector gameSelector;
    private final LivesManager livesManager;
    private final TeleportManager teleportManager;

    private List<ArenaInstance> lobbyArenas = Collections.emptyList();
    private Minigame currentGame;
    private List<ArenaInstance> currentArenas = Collections.emptyList();
    private Minigame nextGame = null;
    private List<ArenaInstance> nextArenas = Collections.emptyList();

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
        this.scoreboardManager = new ScoreboardManager();
        this.lobbyGame = new Lobby(plugin);

        this.games.add(new Dropper());
        this.games.add(new Spleef(plugin));
        this.games.add(new Hikabrain(plugin));
        this.games.add(new Clutch(plugin));
        this.games.add(new AnvilGame(plugin));

        this.gameSelector = new GameSelector(games, configManager);
        this.livesManager = new LivesManager(configManager);
        this.teleportManager = new TeleportManager(participantManager, plugin, random);
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
        livesManager.initialize(participantManager.getParticipants());

        GameSelectionContext initialSelectionContext = gameSelector.buildSelectionContext(1, participantManager);
        if (gameSelector.getSelectableGames(initialSelectionContext).isEmpty()) {
            plugin.getLogger().warning(String.format(Constants.LOGGER_NO_GAME_ROUND_START,
                    initialSelectionContext.roundNumber(),
                    initialSelectionContext.activeParticipantCount(),
                    initialSelectionContext.spectatorCount(),
                    initialSelectionContext.totalParticipantCount()
            ));
            return false;
        }

        if (lobbyArenas.isEmpty()) {
            lobbyArenas = arenaSlotManager.allocate(lobbyGame.getStructureName(), 1);
        }

        this.running = true;
        this.isPaused = true;
        this.timer = 0;
        this.maxTimer = 0;
        this.startCountdownSecondsRemaining = INITIAL_COUNTDOWN_SECONDS;
        this.currentRound = 0;
        this.currentGame = lobbyGame;

        teleportManager.teleportToLobby(lobbyGame, lobbyArenas);
        prepareNextMinigame();

        scoreboardManager.init();
        for (GamePlayer gamePlayer : participantManager.getParticipants()) {
            Player player = Bukkit.getPlayer(gamePlayer.getUuid());
            if (player != null && player.isOnline()) {
                scoreboardManager.assignToPlayer(player);
            }
        }

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

        if (!nextArenas.isEmpty()) {
            arenaSlotManager.free(nextArenas);
            nextArenas = Collections.emptyList();
        }

        if (!lobbyArenas.isEmpty()) {
            arenaSlotManager.free(lobbyArenas);
            lobbyArenas = Collections.emptyList();
        }

        nextGame = null;
        gameSelector.reset();

        scoreboardManager.destroy();
        clearAllParticipantsInventories();
        clearAllParticipantsXp();

        for (GamePlayer gamePlayer : participantManager.getParticipants()) {
            Player player = Bukkit.getPlayer(gamePlayer.getUuid());
            if (player != null && player.isOnline()) {
                player.teleport(player.getWorld().getSpawnLocation());
                PlayerUtils.resetForLobby(player, GameMode.SURVIVAL, false);
            }
        }

        teleportManager.clear();

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
        livesManager.clear();
        participantManager.clearAll();
        participantManager.registerOnlinePlayersAsParticipants();
        plugin.getLogger().info(Constants.LOGGER_LOOP_STOPPED);
    }

    public void forceTimerEnd() {
        this.timer = 0;
    }

    public boolean isRunning() {
        return running;
    }

    public GameParticipantManager.JoinStatus handlePlayerJoin(Player player) {
        GameParticipantManager.JoinStatus status = participantManager.handlePlayerJoin(player, running);
        if (running) {
            scoreboardManager.assignToPlayer(player);
        }
        return status;
    }

    public LeaveResult handlePlayerLeave(Player player) {
        LeaveResult result = switch (participantManager.handlePlayerLeave(player, running)) {
            case ALREADY_LEFT -> LeaveResult.ALREADY_LEFT;
            case LEFT_AND_SPECTATING -> LeaveResult.LEFT_AND_SPECTATING;
            case LEFT -> LeaveResult.LEFT;
        };

        if (result == LeaveResult.LEFT) {
            player.setLevel(0);
            player.setExp(0f);
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
            updateScoreboard();
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
        updateScoreboard();
    }

    private void updateScoreboard() {
        int active = participantManager.getActiveParticipantCount();
        int total = active + participantManager.getSpectatorParticipantCount();
        int completedGames = isPaused ? currentRound : currentRound - 1;
        Set<UUID> spectators = new HashSet<>();
        for (GamePlayer gp : participantManager.getSpectatorParticipants()) {
            spectators.add(gp.getUuid());
        }
        scoreboardManager.updateAll(
            active, total, completedGames,
            !isPaused, currentRound, nextGame != null ? nextGame.getName() : null,
            currentGame.getName(), spectators, livesManager.getPlayerLivesMap(),
            configManager.getGameConfig().getMaxLives()
        );
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

        livesManager.snapshotRoundStart(participantManager.getActiveParticipants());

        teleportManager.teleportActivePlayers(currentGame, currentArenas);
        currentGame.onStart(currentArenas, this);

        for (UUID playerId : teleportManager.getPlayerArenaAssignments().keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline() && !participantManager.isPlayerSpectator(playerId)) {
                currentGame.onGamePlayerSpawn(player);
            }
        }

        teleportManager.teleportSpectators(currentGame, currentArenas);

        if (currentGame instanceof org.bukkit.event.Listener listener) {
            Bukkit.getPluginManager().registerEvents(listener, plugin);
        }

        plugin.getLogger().info(String.format(Constants.LOGGER_MINIGAME_STARTED, currentGame.getName(), timer));
    }

    private void prepareNextMinigame() {
        GameSelectionContext selectionContext = gameSelector.buildSelectionContext(currentRound + 1, participantManager);
        List<Minigame> selectableGames = gameSelector.getSelectableGames(selectionContext);

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
        gameSelector.setLastGame(currentGame);

        java.util.Set<java.util.UUID> losers = currentGame.getLosers(this);

        currentGame.onEnd(this);
        livesManager.applyRoundResult(participantManager, scoreboardManager, losers);
        arenaSlotManager.free(currentArenas);
        currentArenas = Collections.emptyList();
        teleportManager.clear();

        participantManager.clearDisconnectedDuringGamePlayers();
        clearAllParticipantsInventories();

        plugin.getLogger().info(String.format(Constants.LOGGER_MINIGAME_ENDED, gameName));
        isPaused = true;
        currentGame = lobbyGame;
        timer = BREAK_TIME_SECONDS;
        maxTimer = timer;
        Bukkit.broadcastMessage(String.format(Constants.GAME_MANAGER_BREAK_TIME, timer));

        teleportManager.teleportToLobby(lobbyGame, lobbyArenas);
        prepareNextMinigame();
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

    private void clearAllParticipantsInventories() {
        for (GamePlayer gamePlayer : participantManager.getParticipants()) {
            Player player = Bukkit.getPlayer(gamePlayer.getUuid());
            if (player != null && player.isOnline()) {
                PlayerUtils.clearInventory(player);
            }
        }
    }

    private void clearAllParticipantsXp() {
        for (GamePlayer gamePlayer : participantManager.getParticipants()) {
            Player player = Bukkit.getPlayer(gamePlayer.getUuid());
            if (player != null && player.isOnline()) {
                player.setLevel(0);
                player.setExp(0f);
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
        return teleportManager.getPlayerSpawn(playerId);
    }

    public Location rerandomizePlayerSpawn(UUID playerId) {
        return teleportManager.rerandomizePlayerSpawn(playerId, currentGame);
    }

    public ArenaInstance getPlayerArena(UUID playerId) {
        return teleportManager.getPlayerArena(playerId);
    }

    public Map<UUID, ArenaInstance> getPlayerArenaAssignments() {
        return teleportManager.getPlayerArenaAssignments();
    }

    public Map<UUID, Location> getPlayerSpawnLocations() {
        return teleportManager.getPlayerSpawnLocations();
    }

    public ArenaInstance getArenaAt(Location location) {
        return teleportManager.getArenaAt(location, currentArenas, lobbyArenas);
    }

    public boolean isInLobby(Location location) {
        return teleportManager.isInLobby(location, lobbyArenas);
    }

    public boolean eliminatePlayer(UUID playerId) {
        return participantManager.eliminatePlayer(playerId);
    }

    public void setupPlayerAsSpectator(Player player) {
        if (currentGame == null) return;
        List<ArenaInstance> arenas = isPaused ? lobbyArenas : currentArenas;
        teleportManager.teleportSingleSpectator(player, currentGame, arenas);
    }

    public boolean isPlayerSpectator(UUID playerId) {
        return participantManager.isPlayerSpectator(playerId);
    }

    public void clearPlayerInventory(Player player) {
        PlayerUtils.clearInventory(player);
    }

    public int getPlayerLives(UUID playerId) {
        return livesManager.getPlayerLives(playerId);
    }

    public enum LeaveResult {
        LEFT,
        LEFT_AND_SPECTATING,
        ALREADY_LEFT
    }
}
