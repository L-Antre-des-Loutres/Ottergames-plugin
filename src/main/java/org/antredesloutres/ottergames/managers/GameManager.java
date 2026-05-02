package org.antredesloutres.ottergames.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.antredesloutres.ottergames.Main;
import org.antredesloutres.ottergames.models.minigames.Hikabrain;
import org.antredesloutres.ottergames.models.minigames.PlaceholderGame;
import org.antredesloutres.ottergames.models.minigames.SoloGame;
import org.antredesloutres.ottergames.models.ArenaInstance;
import org.antredesloutres.ottergames.models.minigames.Minigame;
import org.antredesloutres.ottergames.models.minigames.selection.GameSelectionContext;
import org.antredesloutres.ottergames.models.participant.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    private Minigame currentGame;
    private List<ArenaInstance> currentArenas = Collections.emptyList();
    private int timer;
    private int startCountdownSecondsRemaining;
    private int currentRound;
    private boolean isPaused = true;
    private boolean running = false;
    private BukkitTask loopTask;

    public GameManager(Main plugin) {
        this.plugin = plugin;
        this.arenaSlotManager = new ArenaSlotManager(plugin);
        this.participantManager = new GameParticipantManager();

        // Add games
//        this.games.add(new PlaceholderGame());
//        this.games.add(new SoloGame());
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

        this.running = true;
        this.isPaused = true;
        this.timer = 0;
        this.startCountdownSecondsRemaining = INITIAL_COUNTDOWN_SECONDS;
        this.currentRound = 0;
        this.currentGame = null;

        this.loopTask = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 0L, 20L);

        plugin.getLogger().info("Ottergame game loop started.");
        return true;
    }

    public void stopEverything() {
        if (!isPaused && currentGame != null) {
            currentGame.onEnd();
            arenaSlotManager.free(currentArenas);
            currentArenas = Collections.emptyList();
            plugin.getLogger().info("Minigame stopped: " + currentGame.getName() + ".");
        }

        if (this.loopTask != null) {
            this.loopTask.cancel();
            this.loopTask = null;
        }

        for (UUID playerId : participantManager.getOptedOutPlayerIds()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.sendMessage("§aTu es reinscrit pour la prochaine partie Ottergames.");
            }
        }

        this.currentGame = null;
        this.isPaused = true;
        this.timer = 0;
        this.startCountdownSecondsRemaining = 0;
        this.currentRound = 0;
        this.running = false;
        participantManager.clearAll();
        participantManager.registerOnlinePlayersAsParticipants();
        plugin.getLogger().info("Boucle OtterGames arretee.");
        plugin.getLogger().info("Ottergame game loop stopped.");
    }

    public boolean isRunning() {
        return running;
    }

    public boolean handlePlayerJoin(Player player) {
        return participantManager.handlePlayerJoin(player, running);
    }

    public LeaveResult handlePlayerLeave(Player player) {
        return switch (participantManager.handlePlayerLeave(player, running)) {
            case ALREADY_LEFT -> LeaveResult.ALREADY_LEFT;
            case LEFT_AND_SPECTATING -> LeaveResult.LEFT_AND_SPECTATING;
            case LEFT -> LeaveResult.LEFT;
        };
    }

    public void handlePlayerQuit(Player player) {
        participantManager.handlePlayerQuit(player, running);
    }

    private void tick() {
        if (!running) return;

        if (startCountdownSecondsRemaining > 0) {
            showStartCountdown(startCountdownSecondsRemaining);
            startCountdownSecondsRemaining--;
            return;
        }

        if (timer > 0) {
            timer--;
            return;
        }

        if (isPaused) {
            startNextMinigame();
        } else {
            stopCurrentMinigame();
        }
    }

    private void startNextMinigame() {
        GameSelectionContext selectionContext = buildSelectionContext(currentRound + 1);

        List<Minigame> selectableGames = getSelectableGames(selectionContext);
        if (selectableGames.isEmpty()) {
            currentGame = null;
            Bukkit.broadcastMessage("§cAucun mini-jeu compatible avec la situation actuelle. Partie arrêtée.");
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

        teleportActiveParticipantsToArenas();
        currentGame.onStart(currentArenas);
        plugin.getLogger().info("Minigame started: " + currentGame.getName() + " (" + timer + "s).");
    }

    private void stopCurrentMinigame() {
        String gameName = currentGame.getName();
        currentGame.onEnd();
        arenaSlotManager.free(currentArenas);
        currentArenas = Collections.emptyList();

        participantManager.clearDisconnectedDuringGamePlayers();

        plugin.getLogger().info("Minigame ended: " + gameName + ".");
        isPaused = true;
        timer = BREAK_TIME_SECONDS;
        Bukkit.broadcastMessage("Break time! Next game starts in " + timer + " seconds.");
    }

    private void showStartCountdown(int secondsRemaining) {
        String subtitleMessage = (currentGame != null)
                ? "Starting " + currentGame.getName() + " in..."
                : "OtterGames starts in...";

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

        List<GamePlayer> activeParticipants = participantManager.getActiveParticipants();
        if (activeParticipants.isEmpty()) {
            return;
        }

        List<List<GamePlayer>> playersByArena = new ArrayList<>();
        for (int i = 0; i < currentArenas.size(); i++) {
            playersByArena.add(new ArrayList<>());
        }

        for (int i = 0; i < activeParticipants.size(); i++) {
            int arenaIndex = i % currentArenas.size();
            playersByArena.get(arenaIndex).add(activeParticipants.get(i));
        }

        for (int arenaIndex = 0; arenaIndex < playersByArena.size(); arenaIndex++) {
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
                player.teleport(spawnLocation);
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

    public enum LeaveResult {
        LEFT,
        LEFT_AND_SPECTATING,
        ALREADY_LEFT
    }
}
