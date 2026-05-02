package org.antredesloutres.ottergames;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.antredesloutres.ottergames.minigames.PlaceholderGame;
import org.antredesloutres.ottergames.models.Minigame;
import org.antredesloutres.ottergames.models.Participant;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GameManager {

    private static final int INITIAL_COUNTDOWN_SECONDS = 5;
    private static final int BREAK_TIME_SECONDS = 5;

    private final List<Minigame> games = new ArrayList<>();
    private final Map<UUID, Participant> participants = new HashMap<>();
    private final Set<UUID> optedOutPlayers = new HashSet<>();
    private final Set<UUID> disconnectedDuringGamePlayers = new HashSet<>();
    private final Random random = new Random();
    private Minigame currentGame;
    private int timer;
    // Countdown shown only once, before the first mini-game starts.
    private int startCountdownSecondsRemaining;
    private boolean isPaused = true;
    private boolean running = false;
    private final JavaPlugin plugin;
    private BukkitTask loopTask;

    public GameManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.games.add(new PlaceholderGame());
    }

    public void startGameLoop() {
        if (running) {
            return;
        }

        if (loopTask != null) {
            loopTask.cancel();
            loopTask = null;
        }

        this.running = true;
        this.isPaused = true;
        this.timer = 0;
        this.startCountdownSecondsRemaining = INITIAL_COUNTDOWN_SECONDS;
        this.currentGame = null;
        registerOnlinePlayersAsParticipants();

        this.loopTask = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 0L, 20L);

        plugin.getLogger().info("Boucle OtterGames demarree.");
    }

    public void stopEverything() {
        if (!isPaused && currentGame != null) {
            String gameName = currentGame.getName();
            currentGame.onEnd();
            plugin.getLogger().info("Mini-jeu termine: " + gameName + " (arret manuel).");
        }

        // Ensure every scheduled task from this plugin is stopped.
        Bukkit.getScheduler().cancelTasks(plugin);
        this.loopTask = null;

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            Participant participant = participants.get(onlinePlayer.getUniqueId());
            if ((participant != null && participant.isSpectator()) || optedOutPlayers.contains(onlinePlayer.getUniqueId())) {
                onlinePlayer.setGameMode(GameMode.SURVIVAL);
            }
        }

        for (UUID playerId : optedOutPlayers) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.sendMessage("§aTu es reinscrit pour la prochaine partie Ottergames.");
            }
        }

        this.currentGame = null;
        this.isPaused = true;
        this.timer = 0;
        this.startCountdownSecondsRemaining = 0;
        this.running = false;
        this.participants.clear();
        this.optedOutPlayers.clear();
        this.disconnectedDuringGamePlayers.clear();
        registerOnlinePlayersAsParticipants();
        plugin.getLogger().info("Boucle OtterGames arretee.");
    }

    public boolean isRunning() {
        return running;
    }

    public boolean handlePlayerJoin(Player player) {
        UUID playerId = player.getUniqueId();
        if (optedOutPlayers.contains(playerId)) {
            participants.remove(playerId);
            if (running) {
                player.setGameMode(GameMode.SPECTATOR);
                return true;
            }

            player.setGameMode(GameMode.SURVIVAL);
            return false;
        }

        if (disconnectedDuringGamePlayers.contains(playerId) && running) {
            Participant participant = new Participant(playerId, player.getName(), true);
            player.setGameMode(GameMode.SPECTATOR);
            participants.put(playerId, participant);
            return true;
        }

        if (running) {
            Participant participant = new Participant(playerId, player.getName(), true);
            player.setGameMode(GameMode.SPECTATOR);
            participants.put(playerId, participant);
            return true;
        } else {
            Participant participant = new Participant(playerId, player.getName(), false);
            player.setGameMode(GameMode.SURVIVAL);
            participants.put(playerId, participant);
            return false;
        }
    }

    public LeaveResult handlePlayerLeave(Player player) {
        UUID playerId = player.getUniqueId();
        if (optedOutPlayers.contains(playerId)) {
            return LeaveResult.ALREADY_LEFT;
        }

        optedOutPlayers.add(playerId);
        disconnectedDuringGamePlayers.remove(playerId);
        if (running) {
            participants.put(playerId, new Participant(playerId, player.getName(), true));
            player.setGameMode(GameMode.SPECTATOR);
            return LeaveResult.LEFT_AND_SPECTATING;
        }

        participants.remove(playerId);
        return LeaveResult.LEFT;
    }

    public void handlePlayerQuit(Player player) {
        UUID playerId = player.getUniqueId();
        Participant existingParticipant = participants.get(playerId);
        if (optedOutPlayers.contains(playerId)) {
            participants.remove(playerId);
            return;
        }

        if (!running) {
            participants.remove(playerId);
            disconnectedDuringGamePlayers.remove(playerId);
            return;
        }

        if (existingParticipant == null) {
            participants.put(playerId, new Participant(playerId, player.getName(), true));
            return;
        }

        if (!existingParticipant.isSpectator()) {
            disconnectedDuringGamePlayers.add(playerId);
        }

        participants.put(playerId, new Participant(playerId, existingParticipant.username(), true));
    }

    private void tick() {
        if (!running) {
            return;
        }

        // Initial countdown before launching the very first mini-game.
        if (startCountdownSecondsRemaining > 0) {
            showStartCountdown(startCountdownSecondsRemaining);
            startCountdownSecondsRemaining--;
            return;
        }

        // Standard loop timer used by game phases and pauses.
        if (timer > 0) {
            timer--;
            return;
        }

        if (isPaused) {
            startNextGame();
        } else {
            stopCurrentGame();
        }
    }

    private void startNextGame() {
        currentGame = games.get(random.nextInt(games.size()));
        isPaused = false;
        timer = currentGame.getDurationSeconds();

        currentGame.onStart();
        plugin.getLogger().info("Mini-jeu demarre: " + currentGame.getName() + " (" + timer + "s).");
    }

    private void stopCurrentGame() {
        String gameName = currentGame.getName();
        currentGame.onEnd();
        plugin.getLogger().info("Mini-jeu termine: " + gameName + ".");
        isPaused = true;
        timer = BREAK_TIME_SECONDS;
        Bukkit.broadcastMessage("§7Pause... (" + BREAK_TIME_SECONDS + "s)");
    }

    private void showStartCountdown(int secondsRemaining) {
        Title title = Title.title(
                Component.text(String.valueOf(secondsRemaining), NamedTextColor.GOLD),
                Component.text("Debut du mini-jeu...", NamedTextColor.YELLOW),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)
        );

        Bukkit.getOnlinePlayers().forEach(player -> {
            player.showTitle(title);
            // "Ding" each second of the start countdown.
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.8f);
        });
    }

    private void registerOnlinePlayersAsParticipants() {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (optedOutPlayers.contains(onlinePlayer.getUniqueId())) {
                onlinePlayer.setGameMode(GameMode.SPECTATOR);
                continue;
            }

            participants.put(onlinePlayer.getUniqueId(), new Participant(onlinePlayer.getUniqueId(), onlinePlayer.getName(), false));
            onlinePlayer.setGameMode(GameMode.SURVIVAL);
        }
    }

    public boolean isPlayerOptedOut(UUID playerId) {
        return optedOutPlayers.contains(playerId);
    }

    public boolean isPlayerDisconnectedDuringGame(UUID playerId) {
        return disconnectedDuringGamePlayers.contains(playerId);
    }

    public enum LeaveResult {
        LEFT,
        LEFT_AND_SPECTATING,
        ALREADY_LEFT
    }
}
