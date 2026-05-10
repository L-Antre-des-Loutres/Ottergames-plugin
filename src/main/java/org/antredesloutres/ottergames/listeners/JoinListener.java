package org.antredesloutres.ottergames.listeners;

import org.antredesloutres.ottergames.managers.GameParticipantManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.antredesloutres.ottergames.managers.GameManager;
import org.antredesloutres.ottergames.utils.Constants;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class JoinListener implements Listener {

    private final GameManager gameManager;
    private final JavaPlugin plugin;

    public JoinListener(GameManager gameManager, JavaPlugin plugin) {
        this.gameManager = gameManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        GameParticipantManager.JoinStatus status = gameManager.handlePlayerJoin(event.getPlayer());
        boolean disconnectedDuringGame = gameManager.isPlayerDisconnectedDuringGame(event.getPlayer().getUniqueId());
        
        Component statusMessage = null;

        switch (status) {
            case ADDED_ACTIVE -> statusMessage = Component.text(Constants.JOIN_REGISTERED, NamedTextColor.GREEN);
            case RECONNECTED_SPECTATOR -> {
                if (disconnectedDuringGame) {
                    statusMessage = Component.text(Constants.JOIN_DISCONNECTED_PREFIX, NamedTextColor.RED)
                            .append(Component.text(Constants.JOIN_DISCONNECTED_SUFFIX, NamedTextColor.GRAY));
                } else {
                    statusMessage = Component.text(Constants.JOIN_GAME_IN_PROGRESS_PREFIX, NamedTextColor.RED)
                            .append(Component.text(Constants.JOIN_GAME_IN_PROGRESS_SUFFIX, NamedTextColor.GRAY));
                }
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (event.getPlayer().isOnline()) {
                        gameManager.setupPlayerAsSpectator(event.getPlayer());
                    }
                }, 2L);
            }
            case IGNORED_MID_GAME -> {
                // Do nothing, player is ignored by Ottergames
            }
        }

        if (statusMessage != null) {
            Component finalMessage = statusMessage;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> event.getPlayer().sendMessage(finalMessage), 1L);
        }
    }

}
