package org.antredesloutres.ottergames.listeners;

import org.antredesloutres.ottergames.managers.GameManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class QuitListener implements Listener {

    private final GameManager gameManager;

    public QuitListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        gameManager.handlePlayerQuit(event.getPlayer());
    }
}
