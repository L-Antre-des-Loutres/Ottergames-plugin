package org.antredesloutres.ottergames.utils;

import org.antredesloutres.ottergames.Main;
import org.antredesloutres.ottergames.models.ArenaInstance;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.structure.Structure;

import java.util.*;

import static org.antredesloutres.ottergames.utils.Constants.*;

public class ArenaSlotManager {

    private final Main plugin;
    private World world;
    private int frontier = ARENA_BASE_X;

    /**
     * Sorted map of currently available X ranges along the arena axis.
     * Each entry maps a zone's start X (inclusive) to its end X (exclusive).
     * Zones are kept sorted by start position so first-fit allocation always
     * scans from the lowest available address. Adjacent zones are merged
     * immediately when a batch is freed, keeping this map as compact as possible.
     * Example: {10000=10200, 10500=10700} means two disjoint free zones exist.
     */
    private final TreeMap<Integer, Integer> freeZones = new TreeMap<>();

    /**
     * Tracks every active allocation so it can be fully released later.
     * Maps the batch's start X to its reserved end X, which includes the trailing
     * OUTER_PADDING that separates this batch from the next one. When free() is
     * called, this entry is removed and the recovered range is handed back to
     * freeZones. A missing key for a given start X means the batch was never
     * registered here, which indicates a programming error and is logged as a warning.
     */
    private final Map<Integer, Integer> allocatedBatches = new HashMap<>();

    public ArenaSlotManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Returns the world where arenas should be allocated.
     * @return world returned by Bukkit.getWorlds().
     */
    private World getWorld() {
        if (world == null) world = Bukkit.getWorlds().get(0);
        return world;
    }

    /**
     * Allocates a contiguous batch of arena slots for the given structure and instance count.
     * @param structureName The name of the structure to load and place for each instance. Must be present in the plugin's resources/structures folder.
     * @param instanceCount The number of instances to allocate. Must be positive.
     * @return A list of ArenaInstance objects representing the allocated arenas, or an empty list if allocation failed (e.g. invalid structure name or non-positive instance count).
     */
    public List<ArenaInstance> allocate(String structureName, int instanceCount) {
        if (instanceCount <= 0) return Collections.emptyList();

        Structure structure = StructureSpawner.load(plugin, structureName);
        if (structure == null) return Collections.emptyList();

        plugin.getLogger().info("Loaded structure " + structureName
                + " size=" + structure.getSize().getBlockX() + "x" + structure.getSize().getBlockY() + "x" + structure.getSize().getBlockZ());

        int instanceWidth = structure.getSize().getBlockX();
        int batchWidth = instanceWidth * instanceCount + OUTER_PADDING * (instanceCount - 1);
        int totalReserved = batchWidth + OUTER_PADDING;

        int batchStartX = findOrExtend(totalReserved);
        allocatedBatches.put(batchStartX, batchStartX + totalReserved);

        List<ArenaInstance> result = new ArrayList<>();
        int cursorX = batchStartX;

        for (int i = 0; i < instanceCount; i++) {
            Location origin = new Location(getWorld(), cursorX, ARENA_BASE_Y, ARENA_BASE_Z);
            result.add(StructureSpawner.place(structure, origin));
            plugin.getLogger().info("Placed instance " + (i + 1) + "/" + instanceCount
                    + " @ x=" + cursorX + " y=" + ARENA_BASE_Y + " z=" + ARENA_BASE_Z);
            cursorX += instanceWidth;
            if (i < instanceCount - 1) cursorX += OUTER_PADDING;
        }

        return result;
    }

    /**
     * Frees the batch of arena slots occupied by the given instances, making them available for future allocations.
     * The batch is identified by the minimum X coordinate among the instances' origins.
     * If no batch is found for that start X, a warning is logged and the method returns without modifying any state.
     * @param instances The list of ArenaInstance objects to free. Must not be null, but can be empty (in which case this method does nothing).
     */
    public void free(List<ArenaInstance> instances) {
        if (instances.isEmpty()) return;

        int startX = instances.stream()
                .mapToInt(a -> a.origin().getBlockX())
                .min().orElseThrow();

        Integer endX = allocatedBatches.remove(startX);
        if (endX == null) {
            plugin.getLogger().warning("ArenaSlotManager: slot unknown for startX=" + startX + ", skipping free operation.");
            return;
        }

        instances.forEach(ArenaInstance::clear);
        addFreeZone(startX, endX);
    }

    /**
     * Finds a free zone of at least the given width and returns its start X coordinate, or extends the frontier if no such zone exists.
     * @param width The required width of the zone to find or create. Must be positive.
     * @return The start X coordinate of the allocated zone, which is guaranteed to be a valid key in allocatedBatches after this method returns.
     */
    private int findOrExtend(int width) {
        for (var entry : freeZones.entrySet()) {
            int start = entry.getKey();
            int end = entry.getValue();
            if (end - start >= width) {
                freeZones.remove(start);
                if (end - start > width) freeZones.put(start + width, end);
                return start;
            }
        }
        int start = frontier;
        frontier += width;
        return start;
    }

    /**
     * Adds a new free zone defined by the given start and end X coordinates, merging it with adjacent zones if necessary.
     * @param startX The start X coordinate of the free zone to add. Must be less than endX.
     * @param endX The end X coordinate of the free zone to add. Must be greater than startX.
     */
    private void addFreeZone(int startX, int endX) {
        Integer prevKey = freeZones.floorKey(startX);
        if (prevKey != null && freeZones.get(prevKey) >= startX) {
            endX = Math.max(endX, freeZones.remove(prevKey));
            startX = prevKey;
        }

        Integer nextKey = freeZones.ceilingKey(startX);
        if (nextKey != null && nextKey <= endX) {
            endX = Math.max(endX, freeZones.remove(nextKey));
        }

        if (endX >= frontier) {
            frontier = startX;
        } else {
            freeZones.put(startX, endX);
        }
    }
}
