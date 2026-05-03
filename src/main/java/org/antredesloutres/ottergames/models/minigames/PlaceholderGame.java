package org.antredesloutres.ottergames.models.minigames;

import org.antredesloutres.ottergames.models.arena.ArenaInstance;
import org.antredesloutres.ottergames.models.minigames.selection.SelectionConditionMode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;


public class PlaceholderGame implements Minigame {

    @Override
    public String getName() { return "Survie Chronométrée"; }

    @Override
    public int getDurationSeconds() { return 10; }

    @Override
    public String getStructureName() { return "house"; }

    @Override
    public SelectionConditionMode getSelectionConditionMode() { return SelectionConditionMode.ANY; }

    @Override
    public void onStart(List<ArenaInstance> arenas, org.antredesloutres.ottergames.managers.GameManager gameManager) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage("§aTenez bon pendant 10 secondes !");
        }
    }

    @Override
    public void onEnd(org.antredesloutres.ottergames.managers.GameManager gameManager) {
        Bukkit.broadcastMessage("§e[OtterGames] Placeholder Game ended.");
    }

}
