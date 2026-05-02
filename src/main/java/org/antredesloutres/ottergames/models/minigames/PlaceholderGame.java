package org.antredesloutres.ottergames.models.minigames;

import org.antredesloutres.ottergames.models.ArenaInstance;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;


public class PlaceholderGame implements Minigame {

    @Override
    public String getName() { return "Survie Chronométrée"; }

    @Override
    public int getDurationSeconds() { return 30; }

    @Override
    public String getStructureName() { return "house"; }

    @Override
    public void onStart(List<ArenaInstance> arenas) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage("§aTenez bon pendant 30 secondes !");
        }
    }

    @Override
    public void onEnd() {
        Bukkit.broadcastMessage("§6Temps écoulé ! Bien joué.");
    }

}
