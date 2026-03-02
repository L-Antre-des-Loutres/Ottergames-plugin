package org.antredesloutres.ottergames.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class JoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage(
                Component.text("placeholder ")
                        .append(Component.text(event.getPlayer().getName(), NamedTextColor.GOLD))
                        .append(Component.text(" placeholder", NamedTextColor.GRAY))
        );
    }

}