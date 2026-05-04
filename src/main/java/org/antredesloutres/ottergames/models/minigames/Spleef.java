package org.antredesloutres.ottergames.models.minigames;
import org.antredesloutres.ottergames.Main;
import org.antredesloutres.ottergames.managers.GameManager;
import org.antredesloutres.ottergames.models.arena.ArenaInstance;
import org.antredesloutres.ottergames.models.arena.ArenaSpawnZone;
import org.antredesloutres.ottergames.models.arena.MinigameArena;
import org.antredesloutres.ottergames.utils.Constants;
import org.antredesloutres.ottergames.models.minigames.selection.SelectionCondition;
import org.antredesloutres.ottergames.models.minigames.selection.SelectionConditions;
import org.antredesloutres.ottergames.utils.PlayerUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Spleef is a classic minigame where players try to make their opponents fall
 * by breaking the blocks (usually snow) under their feet.
 * Goal:
 * Be the last player standing on the platform.
 * Gameplay:
 * Players are equipped with an efficient shovel and Wind Charges in off-hand.
 * Fall damage is disabled. PvP is disabled.
 * Elimination:
 * Players are eliminated if they fall below the platform (bounds exit).
 */
public class Spleef implements Minigame {

    private static final int SNOW_BLOCKS_FOR_WIND_CHARGE = 4;
    private static final int WIND_CHARGE_AMOUNT = 1;

    private final MinigameArena structure;
    private final Map<UUID, Integer> playerBlocksBroken = new HashMap<>();

    public Spleef(Main plugin) {
        this.structure = new MinigameArena(
                plugin,
                Constants.STRUCTURE_SPLEEF,
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
        return 40;
    }

    @Override
    public String getStructureName() {
        return structure.structureName();
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
        return structure.spawnZone("middle").randomLocation(arena, random);
    }

    @Override
    public void onGamePlayerSpawn(Player player) {
        player.setGameMode(GameMode.SURVIVAL);

        // Clear inventory to ensure clean start
        PlayerUtils.clearInventory(player);

        ItemStack shovel = new ItemStack(Material.DIAMOND_SHOVEL);
        ItemMeta meta = shovel.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
            shovel.setItemMeta(meta);
        }
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

            // Reward system: Wind Charges
            UUID uuid = player.getUniqueId();
            int count = playerBlocksBroken.getOrDefault(uuid, 0) + 1;

            if (count >= SNOW_BLOCKS_FOR_WIND_CHARGE) {
                ItemStack offHand = player.getInventory().getItemInOffHand();
                if (offHand.getType() == Material.WIND_CHARGE) {
                    offHand.setAmount(offHand.getAmount() + WIND_CHARGE_AMOUNT);
                } else {
                    player.getInventory().setItemInOffHand(new ItemStack(Material.WIND_CHARGE, WIND_CHARGE_AMOUNT));
                }
                count = 0;
            }

            playerBlocksBroken.put(uuid, count);
            return false; // Return false to cancel the actual event, since we manually set it to AIR
        }
        return false;
    }

    @Override
    public void onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent event, GameManager gameManager) {
        Block block = event.getClickedBlock();
        if (block == null) return;

        // Standard Spleef rule: only doors allowed
        if (!Tag.DOORS.isTagged(block.getType())) {
            if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK || event.getAction() == org.bukkit.event.block.Action.PHYSICAL) {
                event.setCancelled(true);
            }
        }
    }

    @Override
    public void onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent event, GameManager gameManager) {
        // Block all entity interactions in Spleef (Armor Stands, etc.)
        event.setCancelled(true);
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
        playerBlocksBroken.clear();
    }

    @Override
    public void onEnd(GameManager gameManager) {
        playerBlocksBroken.clear();
    }
}
