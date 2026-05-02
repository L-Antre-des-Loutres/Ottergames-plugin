package org.antredesloutres.ottergames.models.minigames;

import org.antredesloutres.ottergames.models.ArenaInstance;
import org.antredesloutres.ottergames.models.minigames.selection.GameSelectionContext;
import org.antredesloutres.ottergames.models.minigames.selection.SelectionCondition;
import org.antredesloutres.ottergames.models.minigames.selection.SelectionConditions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;


public class SoloGame implements Minigame {

    @Override
    public String getName() { return "Solo arena"; }

    @Override
    public int getDurationSeconds() { return 30; }

    @Override
    public String getStructureName() { return "pvp_arena"; }

    @Override
    public int getInstanceCount(GameSelectionContext selectionContext) { return Math.max(1, selectionContext.activeParticipantCount()); }

    @Override
    public List<SelectionCondition> getSelectionConditions() {
        return List.of(SelectionConditions.activeParticipantCountIsOdd(),
                SelectionConditions.minSpectators(2));

    }

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
