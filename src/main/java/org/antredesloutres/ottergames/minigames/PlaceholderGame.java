package org.antredesloutres.ottergames.minigames;

import org.antredesloutres.ottergames.models.Minigame;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PlaceholderGame implements Minigame {

    @Override
    public String getName() { return "Survie Chronométrée"; }

    @Override
    public int getDurationSeconds() { return 30; }

    @Override
    public void onStart() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage("§aTenez bon pendant 30 secondes !");
            // We can put teleport here for exemple
        }
    }

    @Override
    public void onEnd() {
        Bukkit.broadcastMessage("§6Temps écoulé ! Bien joué.");
    }

}
