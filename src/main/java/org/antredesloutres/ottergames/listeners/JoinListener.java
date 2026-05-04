package org.antredesloutres.ottergames.listeners;

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
        boolean spectator = gameManager.handlePlayerJoin(event.getPlayer());
        boolean optedOut = gameManager.isPlayerOptedOut(event.getPlayer().getUniqueId());
        boolean disconnectedDuringGame = gameManager.isPlayerDisconnectedDuringGame(event.getPlayer().getUniqueId());
        Component statusMessage;

        if (spectator) {
            if (optedOut) {
                statusMessage = Component.text(Constants.JOIN_OPTED_OUT_PREFIX, NamedTextColor.RED)
                        .append(Component.text(Constants.JOIN_OPTED_OUT_SUFFIX, NamedTextColor.GRAY));
            } else if (disconnectedDuringGame) {
                statusMessage = Component.text(Constants.JOIN_DISCONNECTED_PREFIX, NamedTextColor.RED)
                        .append(Component.text(Constants.JOIN_DISCONNECTED_SUFFIX, NamedTextColor.GRAY));
            } else {
                statusMessage = Component.text(Constants.JOIN_GAME_IN_PROGRESS_PREFIX, NamedTextColor.RED)
                        .append(Component.text(Constants.JOIN_GAME_IN_PROGRESS_SUFFIX, NamedTextColor.GRAY));
            }
        } else {
            statusMessage = Component.text(Constants.JOIN_REGISTERED, NamedTextColor.GREEN);
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> event.getPlayer().sendMessage(statusMessage), 1L);
    }

}
