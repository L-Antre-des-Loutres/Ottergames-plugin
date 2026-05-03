package org.antredesloutres.ottergames.models.minigames;

import org.antredesloutres.ottergames.models.ArenaInstance;
import org.antredesloutres.ottergames.models.minigames.selection.GameSelectionContext;
import org.antredesloutres.ottergames.models.minigames.selection.SelectionCondition;
import org.antredesloutres.ottergames.models.minigames.selection.SelectionConditionMode;
import org.antredesloutres.ottergames.models.structures.ArenaSpawnZone;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Random;

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
    default boolean canBeSelected(int activeParticipantCount) { return true; }
    default List<SelectionCondition> getSelectionConditions() { return List.of(); }
    default SelectionConditionMode getSelectionConditionMode() { return SelectionConditionMode.ALL; }

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
    //  Starting inventory
    // ──────────────────────────────────────────────

    /**
     * Applies the starting inventory and equipment to a player.
     * Override this to give items, armor, effects, etc. at the start of the game.
     * Called once when the game starts and optionally on respawn / bounds exit.
     * <p>
     * The default implementation does nothing (players keep whatever they had).
     *
     * @param player The player to equip.
     */
    default void applyStartingInventory(Player player) {}

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
