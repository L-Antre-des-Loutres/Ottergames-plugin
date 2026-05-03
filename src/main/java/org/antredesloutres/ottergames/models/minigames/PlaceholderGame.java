package org.antredesloutres.ottergames.models.minigames;

import org.antredesloutres.ottergames.models.ArenaInstance;
import org.antredesloutres.ottergames.models.minigames.selection.SelectionCondition;
import org.antredesloutres.ottergames.models.minigames.selection.SelectionConditionMode;
import org.antredesloutres.ottergames.models.minigames.selection.SelectionConditions;
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
    public SelectionConditionMode getSelectionConditionMode() { return SelectionConditionMode.ANY; }

    @Override
    public List<SelectionCondition> getSelectionConditions() {
        return List.of(
                SelectionConditions.notFirstRound(),
                SelectionConditions.minSpectators(2)
        );
    }

    @Override
    public void onStart(List<ArenaInstance> arenas, org.antredesloutres.ottergames.managers.GameManager gameManager) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage("§aTenez bon pendant 30 secondes !");
        }
    }

    @Override
    public void onEnd(org.antredesloutres.ottergames.managers.GameManager gameManager) {
        Bukkit.broadcastMessage("§e[OtterGames] Placeholder Game ended.");
    }

}
