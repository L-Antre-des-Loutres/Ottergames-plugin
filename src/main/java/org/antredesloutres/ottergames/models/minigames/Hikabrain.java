package org.antredesloutres.ottergames.models.minigames;

import org.antredesloutres.ottergames.Main;
import org.antredesloutres.ottergames.models.ArenaInstance;
import org.antredesloutres.ottergames.models.minigames.selection.GameSelectionContext;
import org.antredesloutres.ottergames.models.minigames.selection.SelectionCondition;
import org.antredesloutres.ottergames.models.minigames.selection.SelectionConditions;
import org.antredesloutres.ottergames.models.structures.ArenaSpawnZone;
import org.antredesloutres.ottergames.models.structures.MinigameStructure;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class Hikabrain implements Minigame {

    private static final String TEAM_1 = "team_1";
    private static final String TEAM_2 = "team_2";

    private final MinigameStructure structure;

    public Hikabrain(Main plugin) {
        this.structure = new MinigameStructure(
                plugin,
                "ottergames_hikabrain_map",
                8,
                Map.of(
                        TEAM_1, new ArenaSpawnZone(2, 19, 9, 2, 19, 9, -90.0f, 19.3f),
                        TEAM_2, new ArenaSpawnZone(46, 19, 9, 46, 19, 9, 90.0f, 18.9f)
                )
        );
    }

    public MinigameStructure getStructure() {
        return structure;
    }

    @Override
    public String getName() {
        return "Hikabrain";
    }

    @Override
    public int getDurationSeconds() {
        return 120;
    }

    @Override
    public String getStructureName() {
        return structure.structureName();
    }

    @Override
    public int getInstanceCount(GameSelectionContext selectionContext) {
        return Math.max(1, structure.computeRequiredStructureCount(selectionContext.activeParticipantCount()));
    }

    @Override
    public List<SelectionCondition> getSelectionConditions() {
        return List.of(SelectionConditions.minActiveParticipants(2));
    }

    @Override
    public ArenaSpawnZone getSpawnZone(ArenaInstance arena) {
        return structure.firstSpawnZone();
    }

    @Override
    public Location getSpawnLocation(ArenaInstance arena, Random random, int playerIndexInArena, int playersInArena) {
        // Alternate players between the two team zones
        String zoneName = (playerIndexInArena % 2 == 0) ? TEAM_1 : TEAM_2;
        ArenaSpawnZone spawnZone = structure.spawnZone(zoneName);
        return spawnZone.randomLocation(arena, random);
    }

    @Override
    public void onStart(List<ArenaInstance> arenas) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage("§dHikabrain §7(base): préparation de la manche...");
        }
    }

    @Override
    public void onEnd() {
        Bukkit.broadcastMessage("§dHikabrain §7(base): manche terminée.");
    }
}
