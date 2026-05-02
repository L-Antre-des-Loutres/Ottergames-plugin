package org.antredesloutres.ottergames.listeners;

import org.bukkit.plugin.java.JavaPlugin;
import org.antredesloutres.ottergames.GameManager;
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
                statusMessage = Component.text("Tu es desinscrit des Ottergames: ", NamedTextColor.RED)
                        .append(Component.text("mode spectateur neutre.", NamedTextColor.GRAY));
            } else if (disconnectedDuringGame) {
                statusMessage = Component.text("Tu passes en spectateur car ", NamedTextColor.RED)
                        .append(Component.text("tu t'es deconnecte pendant la partie.", NamedTextColor.GRAY));
            } else {
                statusMessage = Component.text("Partie en cours: ", NamedTextColor.RED)
                        .append(Component.text("mode spectateur jusqu'a la fin.", NamedTextColor.GRAY));
            }
        } else {
            statusMessage = Component.text("Tu es inscrit pour les Ottergames.", NamedTextColor.GREEN);
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> event.getPlayer().sendMessage(statusMessage), 1L);
    }

}
