package org.antredesloutres.ottergames.models.minigames;

import org.antredesloutres.ottergames.Main;
import org.antredesloutres.ottergames.managers.GameManager;
import org.antredesloutres.ottergames.models.arena.ArenaInstance;
import org.antredesloutres.ottergames.models.arena.ArenaSpawnZone;
import org.antredesloutres.ottergames.models.arena.MinigameArena;
import org.antredesloutres.ottergames.utils.Constants;
import org.antredesloutres.ottergames.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Lobby is a special "minigame" that acts as a waiting area between actual games.
 */
public class Lobby implements Minigame {

    private final MinigameArena structure;

    public Lobby(Main plugin) {
        // Lobby capacity is high, and it uses the ottergames_lobby structure.
        // We define a default spawn zone in the middle of the structure.
        this.structure = new MinigameArena(
                plugin,
                Constants.STRUCTURE_LOBBY,
                100,
                Map.of("spawn", new ArenaSpawnZone(8, 19, 8, 8, 19, 8, 0f, 0f))
        );
        }

    @Override
    public String getName() {
        return "Lobby";
    }

    @Override
    public void onStart(List<ArenaInstance> arenas, GameManager gameManager) {
        // No special start logic for the lobby.
    }

    @Override
    public void onEnd(GameManager gameManager) {
        // No special end logic for the lobby.
    }

    @Override
    public int getDurationSeconds() {
        return 0; // The lobby duration is managed by the break timer in GameManager.
    }

    @Override
    public String getStructureName() {
        return structure.structureName();
    }

    @Override
    public ArenaSpawnZone getSpawnZone(ArenaInstance arena) {
        return structure.firstSpawnZone();
    }

    @Override
    public Location getSpawnLocation(ArenaInstance arena, Random random, int playerIndexInArena, int playersInArena) {
        // Everyone spawns in the same zone in the lobby.
        return getSpawnZone(arena).randomLocation(arena, random);
    }

    @Override
    public void applyStartingInventory(Player player) {
        // Clear inventory in the lobby.
        PlayerUtils.clearInventory(player);
    }

    @Override
    public boolean canModifyBlock(Player player, Location blockLocation, GameManager gameManager) {
        // Players cannot modify the lobby.
        return false;
    }

    @Override
    public void onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent event, GameManager gameManager) {
        // Block all entity interactions in Lobby
        event.setCancelled(true);
    }

    @Override
    public boolean pvpEnabled() {
        return false;
    }
}
