package org.antredesloutres.ottergames.models.arena;

import org.antredesloutres.ottergames.Main;
import org.antredesloutres.ottergames.utils.StructureSpawner;
import org.bukkit.structure.Structure;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a structure used by a minigame, along with named spawn zones.
 * Spawn zones are defined as relative offsets from the structure's origin
 * (the lowest X/Y/Z corner placed by {@link StructureSpawner#place}).
 * Because they are relative, the same {@code MinigameArena} definition works
 * no matter where the structure is placed in the world.
 * Each zone has a unique name (e.g. "team_red", "team_blue") so
 * that the minigame can pick the right zone for each player at start time.
 */
public class MinigameArena {
    private final String structureName;
    private final Structure structure;
    private final int playersPerStructure;
    private final Map<String, ArenaSpawnZone> spawnZones;
    /**
     * Creates a new MinigameArena.
     * @param plugin               Plugin instance used to load the structure NBT.
     * @param structureName        Name of the structure file (without .nbt extension).
     * @param playersPerStructure  Maximum number of players that fit in one instance of this structure. Must be > 0.
     * @param spawnZones           Named spawn zones, defined as relative offsets from the structure origin.
     *                             Must contain at least one entry.
     * @throws IllegalArgumentException if the structure NBT cannot be found, or if parameters are invalid.
     */
    public MinigameArena(
            Main plugin,
            String structureName,
            int playersPerStructure,
            Map<String, ArenaSpawnZone> spawnZones
    ) {
        Objects.requireNonNull(plugin, "plugin");
        this.structureName = Objects.requireNonNull(structureName, "structureName");

        if (playersPerStructure <= 0) {
            throw new IllegalArgumentException("playersPerStructure must be > 0");
        }
        this.playersPerStructure = playersPerStructure;

        Objects.requireNonNull(spawnZones, "spawnZones");
        if (spawnZones.isEmpty()) {
            throw new IllegalArgumentException("At least one spawn zone must be provided");
        }
        // Defensive copy – insertion order is preserved so the first zone stays first.
        this.spawnZones = Collections.unmodifiableMap(new LinkedHashMap<>(spawnZones));

        Structure loadedStructure = StructureSpawner.load(plugin, structureName);
        if (loadedStructure == null) {
            throw new IllegalArgumentException("Structure NBT not found: " + structureName);
        }
        this.structure = loadedStructure;
    }

    // ──────────────────────────────────────────────
    //  Accessors
    // ──────────────────────────────────────────────
    /** @return The structure file name (without extension). */
    public String structureName() {
        return structureName;
    }

    /** @return The loaded Bukkit {@link Structure}. */
    public Structure structure() {
        return structure;
    }

    /** @return Maximum players per structure instance. */
    public int playersPerStructure() {
        return playersPerStructure;
    }

    /**
     * @return An unmodifiable, insertion-ordered map of all named spawn zones.
     */
    public Map<String, ArenaSpawnZone> spawnZones() {
        return spawnZones;
    }

    /**
     * Retrieves a spawn zone by name.
     *
     * @param name The zone name (e.g. {@code "team_red"}).
     * @return The corresponding {@link ArenaSpawnZone}.
     * @throws IllegalArgumentException if no zone with that name exists.
     */
    public ArenaSpawnZone spawnZone(String name) {
        ArenaSpawnZone zone = spawnZones.get(name);
        if (zone == null) {
            throw new IllegalArgumentException("Unknown spawn zone: " + name
                    + " – available: " + spawnZones.keySet());
        }
        return zone;
    }

    /**
     * Convenience accessor: returns the first spawn zone that was inserted.
     * Useful for minigames that only define a single zone.
     */
    public ArenaSpawnZone firstSpawnZone() {
        return spawnZones.values().iterator().next();
    }

    // ──────────────────────────────────────────────
    //  Utilities
    // ──────────────────────────────────────────────
    /**
     * Computes how many instances of this structure are required to host
     * the given number of players.
     *
     * @param playerCount Total number of players that need to be placed.
     * @return Number of structure instances required (≥ 0).
     */
    public int computeRequiredStructureCount(int playerCount) {
        if (playerCount <= 0) {
            return 0;
        }
        return (int) Math.ceil(playerCount / (double) playersPerStructure);
    }
}
