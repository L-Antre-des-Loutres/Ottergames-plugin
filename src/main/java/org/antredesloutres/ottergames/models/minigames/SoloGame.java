package org.antredesloutres.ottergames.models.minigames;

import org.antredesloutres.ottergames.models.arena.ArenaInstance;
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
    public int getDurationSeconds() { return 15; }

    @Override
    public String getStructureName() { return "pvp_arena"; }

    @Override
    public int getInstanceCount(GameSelectionContext selectionContext) { return Math.max(1, selectionContext.activeParticipantCount()); }

    @Override
    public List<SelectionCondition> getSelectionConditions() {
        return List.of(SelectionConditions.minActiveParticipants(1));

    }

    @Override
    public void onStart(List<ArenaInstance> arenas, org.antredesloutres.ottergames.managers.GameManager gameManager) {
        // Nothing special to do, GameManager already teleports players.
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage("§aSolo Challenge ! Une arène par joueur.");
        }
    }

    @Override
    public void onEnd(org.antredesloutres.ottergames.managers.GameManager gameManager) {
        Bukkit.broadcastMessage("§e[OtterGames] Solo Game ended.");
        Bukkit.broadcastMessage("§6Temps écoulé ! Bien joué.");
    }

}
