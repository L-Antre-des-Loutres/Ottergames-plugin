package org.antredesloutres.ottergames;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.antredesloutres.ottergames.minigames.PlaceholderGame;
import org.antredesloutres.ottergames.models.Minigame;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameManager {

    private static final int INITIAL_COUNTDOWN_SECONDS = 5;
    private static final int BREAK_TIME_SECONDS = 5;

    private final List<Minigame> games = new ArrayList<>();
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

        this.currentGame = null;
        this.isPaused = true;
        this.timer = 0;
        this.startCountdownSecondsRemaining = 0;
        this.running = false;
        plugin.getLogger().info("Boucle OtterGames arretee.");
    }

    public boolean isRunning() {
        return running;
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
}
