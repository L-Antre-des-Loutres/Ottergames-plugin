package org.antredesloutres.ottergames.models.minigames;

import org.antredesloutres.ottergames.models.arena.ArenaInstance;
import org.antredesloutres.ottergames.models.minigames.selection.GameSelectionContext;
import org.antredesloutres.ottergames.models.minigames.selection.SelectionCondition;
import org.antredesloutres.ottergames.models.minigames.selection.SelectionConditionMode;
import org.antredesloutres.ottergames.models.arena.ArenaSpawnZone;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public interface Minigame {
    String getName();
    void onStart(List<ArenaInstance> arenas, org.antredesloutres.ottergames.managers.GameManager gameManager);
    void onEnd(org.antredesloutres.ottergames.managers.GameManager gameManager);
    int getDurationSeconds();
    String getStructureName();
    
    default void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event, org.antredesloutres.ottergames.managers.GameManager gameManager) {}

    default int getInstanceCount() { return 1; }
    default int getInstanceCount(GameSelectionContext selectionContext) { return getInstanceCount(); }
    default ArenaSpawnZone getSpawnZone(ArenaInstance arena) { return ArenaSpawnZone.full(arena.size()); }
    default Location getSpawnLocation(ArenaInstance arena, Random random, int playerIndexInArena, int playersInArena) {
        return getSpawnZone(arena).randomLocation(arena, random);
    }
    // Called on each respawn. Override when respawn logic differs from initial spawn (e.g. team-based games).
    default Location getRespawnLocation(UUID playerId, ArenaInstance arena, Random random) {
        return getSpawnLocation(arena, random, 0, 1);
    }
    default boolean canBeSelected(int activeParticipantCount) { return true; }
    default List<SelectionCondition> getSelectionConditions() { return List.of(); }
    default SelectionConditionMode getSelectionConditionMode() { return SelectionConditionMode.ALL; }

    /**
     * Checks if a player can modify (place/break) a block at the given location.
     * @param player The player attempting the modification.
     * @param blockLocation The location of the block.
     * @param gameManager The current game manager.
     * @return True if allowed, false otherwise.
     */
    default boolean canModifyBlock(Player player, Location blockLocation, org.antredesloutres.ottergames.managers.GameManager gameManager) {
        return true;
    }

    /**
     * Handles block interaction (right-click, physical).
     * @param event The interaction event.
     * @param gameManager The current game manager.
     */
    default void onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent event, org.antredesloutres.ottergames.managers.GameManager gameManager) {}

    /**
     * Handles entity interaction.
     * @param event The interaction event.
     * @param gameManager The current game manager.
     */
    default void onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent event, org.antredesloutres.ottergames.managers.GameManager gameManager) {}

    // ──────────────────────────────────────────────
    //  Arena rules
    // ──────────────────────────────────────────────
    /** If true, players who leave their arena bounds are teleported back to their spawn point. */
    default boolean keepPlayersInBounds() { return false; }
    /** If true, leaving the arena bounds eliminates the player (spectator). Overrides keepPlayersInBounds. */
    default boolean eliminateOnBoundsExit() { return false; }
    /** If true, players are fully healed when teleported back after leaving bounds. */
    default boolean healOnBoundsExit() { return false; }
    /** If true, the starting inventory is re-applied when a player is teleported back after leaving bounds. */
    default boolean restoreInventoryOnBoundsExit() { return false; }
    /** If true, fall damage is disabled during the minigame. */
    default boolean disableFallDamage() { return false; }

    /** If true, PvP (damage between players) is enabled. */
    default boolean pvpEnabled() { return true; }

    // ──────────────────────────────────────────────
    //  Death rules
    // ──────────────────────────────────────────────
    /** If true, players respawn instantly at their spawn point without the death screen. */
    default boolean instantRespawn() { return false; }
    /** If true, players keep their current inventory and XP on death (vanilla keepInventory). */
    default boolean keepInventoryOnDeath() { return false; }
    /** If true, the starting inventory is re-applied after respawning. */
    default boolean restoreInventoryOnDeath() { return false; }
    /** If true, dying eliminates the player (they become a spectator for the rest of the game). */
    default boolean eliminateOnDeath() { return false; }

    // ──────────────────────────────────────────────
    //  Player spawn and respawn events
    // ──────────────────────────────────────────────
    /**
     * Called when a player spawns for the first time at the start of the game or when they respawn after death.
     */
    default void onGamePlayerSpawn(Player player) {}

    /**
     * Called when a player spawns as a spectator (either by elimination or by joining mid-game).
     */
    default void onGameSpectatorSpawn(Player player) {}

    // ──────────────────────────────────────────────
    //  Conditions
    // ──────────────────────────────────────────────
    default boolean canBeSelected(GameSelectionContext selectionContext) {
        if (!canBeSelected(selectionContext.activeParticipantCount())) {
            return false;
        }

        List<SelectionCondition> selectionConditions = getSelectionConditions();
        if (selectionConditions.isEmpty()) {
            return true;
        }

        return switch (getSelectionConditionMode()) {
            case ANY -> selectionConditions.stream().anyMatch(condition -> condition.matches(selectionContext));
            case ALL -> selectionConditions.stream().allMatch(condition -> condition.matches(selectionContext));
        };
    }
}
