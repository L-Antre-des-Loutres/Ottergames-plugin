package org.antredesloutres.ottergames.models.minigames;

import org.antredesloutres.ottergames.Main;
import org.antredesloutres.ottergames.managers.GameManager;
import org.antredesloutres.ottergames.models.arena.ArenaInstance;
import org.antredesloutres.ottergames.models.arena.ArenaRegion;
import org.antredesloutres.ottergames.models.arena.ArenaSpawnZone;
import org.antredesloutres.ottergames.models.arena.MinigameArena;
import org.antredesloutres.ottergames.models.minigames.selection.GameSelectionContext;
import org.antredesloutres.ottergames.models.minigames.selection.SelectionCondition;
import org.antredesloutres.ottergames.models.minigames.selection.SelectionConditionMode;
import org.antredesloutres.ottergames.models.minigames.selection.SelectionConditions;
import org.antredesloutres.ottergames.models.participant.GamePlayer;
import org.antredesloutres.ottergames.utils.StructureSpawner;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Dropper
 */
public class Dropper implements Minigame {

    private final List<ArenaSpawnZone> spawnZones;

    public Dropper() {
        this.spawnZones = List.of(
                // Zone 1 (East)
                new ArenaSpawnZone(109, 161, 25, 112, 161, 35, 90.0f, 0.0f),
                // Zone 2 (North)
                new ArenaSpawnZone(78, 161, 1, 88, 161, 4, 0.0f, 0.0f),
                // Zone 3 (West)
                new ArenaSpawnZone(54, 161, 25, 57, 161, 35, -90.0f, 0.0f),
                // Zone 4 (South)
                new ArenaSpawnZone(78, 161, 56, 88, 161, 59, 180.0f, 0.0f)
        );
    }

    @Override
    public String getName() { return "Dropper"; }

    @Override
    public int getDurationSeconds() { return 999999; }

    @Override
    public String getStructureName() { return "dropper/ottergames_dropper_warden"; }

    @Override
    public int getInstanceCount(GameSelectionContext selectionContext) { return Math.max(1, selectionContext.activeParticipantCount()); }

    @Override
    public SelectionConditionMode getSelectionConditionMode() { return SelectionConditionMode.ANY; }

    @Override
    public void onStart(List<ArenaInstance> arenas, GameManager gameManager) {
        for (GamePlayer gp : gameManager.getParticipants()) {
            Player bukkitPlayer = Bukkit.getPlayer(gp.getUuid());

            if (bukkitPlayer != null) {
                bukkitPlayer.setGameMode(GameMode.ADVENTURE);
                bukkitPlayer.sendMessage("§aSurvivez à la chute !");
                bukkitPlayer.setHealth(1);
                Objects.requireNonNull(bukkitPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(1);
                bukkitPlayer.setFoodLevel(20);
                bukkitPlayer.setSaturation(20);
                bukkitPlayer.setExhaustion(0);
                bukkitPlayer.getInventory().clear();
                bukkitPlayer.getInventory().setArmorContents(new ItemStack[4]);
            }
        }
    }

    @Override
    public void onEnd(GameManager gameManager) {
        Bukkit.broadcastMessage("§e[OtterGames] Dropper ended.");
    }

    @Override
    public Location getSpawnLocation(ArenaInstance arena, Random random, int playerIndexInArena, int playersInArena) {
        ArenaSpawnZone selectedZone = spawnZones.get(random.nextInt(spawnZones.size()));
        return selectedZone.randomLocation(arena, random);
    }

    @Override
    public boolean keepPlayersInBounds() {
        return true;
    }

    @Override
    public boolean instantRespawn() {
        return true;
    }

}