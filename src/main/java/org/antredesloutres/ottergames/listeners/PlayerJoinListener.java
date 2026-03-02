package org.antredesloutres.ottergames.listeners;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // On récupère le joueur
        var player = event.getPlayer();

        // Message de bienvenue stylé avec MiniMessage (Gradients, Couleurs hex)
        var message = MiniMessage.miniMessage().deserialize(
                "<gold>Bienvenue sur le serveur, <gradient:aqua:blue>" + player.getName() + "</gradient> !</gold>"
        );

        // Envoyer le message uniquement au joueur qui rejoint
        player.sendMessage(message);

        // Modifier le message global de connexion (optionnel)
        event.joinMessage(MiniMessage.miniMessage().deserialize("<gray>[<green>+</green>] " + player.getName()));
    }
}