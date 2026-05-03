package org.antredesloutres.ottergames.models.minigames;

import org.antredesloutres.ottergames.Main;
import org.antredesloutres.ottergames.managers.GameManager;
import org.antredesloutres.ottergames.models.arena.ArenaInstance;
import org.antredesloutres.ottergames.models.arena.ArenaSpawnZone;
import org.antredesloutres.ottergames.models.arena.MinigameArena;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Spleef is a classic minigame where players try to make their opponents fall
 * by breaking the blocks (usually snow) under their feet.
 * Goal:
 * Be the last player standing on the platform.
 * Gameplay:
 * Players are equipped with an efficient shovel. Fall damage is disabled.
 * PvP is disabled.
 * Elimination:
 * Players are eliminated if they fall below the platform (bounds exit).
 */
public class Spleef implements Minigame {

    private final MinigameArena structure;

    public Spleef(Main plugin) {
        this.structure = new MinigameArena(
                plugin,
                "ottergames_spleef_single_floor",
                8, // Up to 8 players
                Map.of(
                    "middle", new ArenaSpawnZone(26, 28, 26, 29, 28, 29, 0f, 0f)
                )
        );
    }

    @Override
    public String getName() {
        return "Spleef";
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
    public ArenaSpawnZone getSpawnZone(ArenaInstance arena) {
        return structure.firstSpawnZone();
    }

    @Override
    public Location getSpawnLocation(ArenaInstance arena, Random random, int playerIndexInArena, int playersInArena) {
        return structure.spawnZone("middle").randomLocation(arena, random);
    }

    @Override
    public void applyStartingInventory(Player player) {
        ItemStack shovel = new ItemStack(Material.DIAMOND_SHOVEL);
        player.getInventory().setItem(0, shovel);

        // Apply Slow Falling effect for the "snow fall" feel (100 ticks = 5 seconds)
        // This allows players to drift and choose where they land.
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0, false, false));
        
        // Spawn snowflake particles around the player at the start
        player.spawnParticle(Particle.SNOWFLAKE, player.getLocation(), 50, 1.0, 1.0, 1.0, 0.05);
        player.updateInventory();
    }

    @Override
    public boolean canModifyBlock(Player player, Location blockLocation, GameManager gameManager) {
        // Only allow breaking snow blocks (or similar spleefable blocks)
        Material type = blockLocation.getBlock().getType();
        if (type == Material.SNOW_BLOCK) {
            // In Spleef, we usually want blocks to break instantly and not drop anything
            blockLocation.getBlock().setType(Material.AIR);
            player.playSound(blockLocation, Sound.BLOCK_SNOW_BREAK, 1f, 1f);
            return false; // Return false to cancel the actual event, since we manually set it to AIR
        }
        return false;
    }

    @Override
    public boolean pvpEnabled() {
        return false;
    }

    @Override
    public boolean disableFallDamage() {
        return true;
    }

    @Override
    public boolean eliminateOnBoundsExit() {
        return true;
    }

    @Override
    public boolean keepPlayersInBounds() {
        // We use bounds exit for elimination, so we don't teleport them back.
        return false;
    }

    @Override
    public void onStart(List<ArenaInstance> arenas, GameManager gameManager) {
        // No special start logic
    }

    @Override
    public void onEnd(GameManager gameManager) {
        // Broadcast the winner(s) could be added here if we tracked them
    }
}
