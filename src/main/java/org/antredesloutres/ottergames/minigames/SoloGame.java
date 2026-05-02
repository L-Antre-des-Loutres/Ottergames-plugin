package org.antredesloutres.ottergames.minigames;

import org.antredesloutres.ottergames.models.ArenaInstance;
import org.antredesloutres.ottergames.models.Minigame;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;


public class SoloGame implements Minigame {

    @Override
    public String getName() { return "Solo arena"; }

    @Override
    public int getDurationSeconds() { return 30; }

    @Override
    public String getStructureName() { return "house"; }

    @Override
    public int getInstanceCount() { return Math.max(1, Bukkit.getOnlinePlayers().size()); }

    @Override
    public void onStart(List<ArenaInstance> arenas) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage("§aSolo Challenge ! Une arène par joueur.");
        }
    }

    @Override
    public void onEnd() {
        Bukkit.broadcastMessage("§6Temps écoulé ! Bien joué.");
    }

}
