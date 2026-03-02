package org.antredesloutres.ottergames;

import org.antredesloutres.ottergames.minigames.PlaceholderGame;
import org.antredesloutres.ottergames.models.Minigame;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameManager extends BukkitRunnable {

    private List<Minigame> games = new ArrayList<>();
    private Minigame currentGame;
    private int timer;
    private boolean isPaused = true;
    private boolean running = false;
    private final JavaPlugin plugin;

    public GameManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.games.add(new PlaceholderGame());
    }

    public void startGameLoop() {
        this.running = true;

        // Launch the runnable every seconds (20 ticks)
        this.runTaskTimer(plugin, 0L, 20L);
    }

    public void stopEverything() {
        this.running = false; // Stops the BukkitRunnable
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void run() {
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
        currentGame = games.get(new Random().nextInt(games.size()));
        isPaused = false;
        timer = currentGame.getDurationSeconds();

        currentGame.onStart();
        Bukkit.broadcastMessage("§eJeu : §b" + currentGame.getName());
    }

    private void stopCurrentGame() {
        currentGame.onEnd();
        isPaused = true;
        timer = 5; // break time between games, in seconds
        Bukkit.broadcastMessage("§7Pause...");
    }

}
