package org.antredesloutres.ottergames.listeners;

import org.antredesloutres.ottergames.managers.GameManager;
import org.antredesloutres.ottergames.models.arena.ArenaInstance;
import org.antredesloutres.ottergames.models.minigames.Minigame;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

/**
 * Handles in-game arena rules:
 * - Keeps players within their arena bounds (teleport back + optional heal / inventory restore).
 * - Instant respawn without death screen (optional).
 * - Elimination on death (optional).
 * - Starting inventory restore on death (optional).
 * - Block modification protection via minigame rules.
 */
public class ArenaGameListener implements Listener {

    private final GameManager gameManager;
    private final JavaPlugin plugin;

    public ArenaGameListener(GameManager gameManager, JavaPlugin plugin) {
        this.gameManager = gameManager;
        this.plugin = plugin;
    }

    // ──────────────────────────────────────────────
    //  Bounds check
    // ──────────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only check if block position actually changed (ignore head rotation)
        if (!event.hasChangedBlock()) return;

        Minigame currentGame = gameManager.getCurrentGame();
        if (currentGame == null) return;

        boolean keepInBounds = currentGame.keepPlayersInBounds();
        boolean eliminateOnExit = currentGame.eliminateOnBoundsExit();

        if (!keepInBounds && !eliminateOnExit) return;

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        ArenaInstance arena = gameManager.getPlayerArena(playerId);
        Location spawnLocation = gameManager.getPlayerSpawnLocation(playerId);
        if (arena == null || spawnLocation == null) return;

        if (!arena.contains(event.getTo())) {
            if (eliminateOnExit) {
                // Eliminate the player
                gameManager.eliminatePlayer(playerId);
                player.setGameMode(org.bukkit.GameMode.SPECTATOR);
                player.sendMessage("§cÉliminé ! Tu es sorti de l'arène.");
            } else {
                // Just teleport them back and handle optional heal/restore
                player.teleport(spawnLocation);

                if (currentGame.healOnBoundsExit()) {
                    healPlayer(player);
                }

                if (currentGame.restoreInventoryOnBoundsExit()) {
                    currentGame.applyStartingInventory(player);
                }
            }
        }

        // Pass movement to minigame for custom logic (e.g. goals)
        currentGame.onPlayerMove(event, gameManager);
    }

    // ──────────────────────────────────────────────
    //  Damage handling
    // ──────────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Minigame currentGame = gameManager.getCurrentGame();
        if (currentGame == null) return;

        // Check if player is an active participant in an arena
        if (gameManager.getPlayerArena(player.getUniqueId()) == null) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && currentGame.disableFallDamage()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Minigame currentGame = gameManager.getCurrentGame();
        if (currentGame == null) return;

        // If PvP is disabled, cancel any damage from another player
        if (!currentGame.pvpEnabled()) {
            Player damager = null;
            if (event.getDamager() instanceof Player p) {
                damager = p;
            } else if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile && projectile.getShooter() instanceof Player p) {
                damager = p;
            }

            if (damager != null) {
                event.setCancelled(true);
            }
        }
    }

    // ──────────────────────────────────────────────
    //  Death handling
    // ──────────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Minigame currentGame = gameManager.getCurrentGame();
        if (currentGame == null) return;

        boolean eliminate = currentGame.eliminateOnDeath();
        boolean instantRespawn = currentGame.instantRespawn();
        boolean restoreInventory = currentGame.restoreInventoryOnDeath();

        // Nothing to handle if no death feature is active
        if (!eliminate && !instantRespawn && !restoreInventory) return;

        Player player = event.getEntity();
        UUID playerId = player.getUniqueId();

        Location spawnLocation = gameManager.getPlayerSpawnLocation(playerId);
        if (spawnLocation == null) return;

        // Keep inventory only if the minigame enables it
        if (currentGame.keepInventoryOnDeath()) {
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);
        } else if (restoreInventory) {
            // If we are restoring the starting inventory, don't drop the current one on the ground
            event.getDrops().clear();
            event.setDroppedExp(0);
        }

        // Schedule instant respawn on the next tick (can't respawn in the same tick as death)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !player.isDead()) return;
                player.spigot().respawn();

                // Handle post-respawn on the tick after
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!player.isOnline()) return;

                        if (eliminate) {
                            // Eliminate: set as spectator
                            gameManager.eliminatePlayer(playerId);
                            player.setGameMode(GameMode.SPECTATOR);
                            player.sendMessage("§cÉliminé ! Tu es maintenant spectateur.");
                        } else {
                            // Re-randomize spawn on each death and teleport there
                            Location newSpawn = gameManager.rerandomizePlayerSpawn(playerId);
                            if (newSpawn != null) {
                                player.teleport(newSpawn);
                            }

                            // Restore starting inventory if enabled
                            if (restoreInventory) {
                                currentGame.applyStartingInventory(player);
                            }
                        }
                    }
                }.runTaskLater(plugin, 1L);
            }
        }.runTaskLater(plugin, 1L);
    }

    // ──────────────────────────────────────────────
    //  Block modification handling
    // ──────────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        checkBlockModification(event.getPlayer(), event.getBlock().getLocation(), event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        checkBlockModification(event.getPlayer(), event.getBlock().getLocation(), event);
    }

    private void checkBlockModification(Player player, Location blockLocation, org.bukkit.event.Cancellable event) {
        // High priority: Lobby is ALWAYS unbreakable if it exists
        if (gameManager.isInLobby(blockLocation)) {
            event.setCancelled(true);
            return;
        }

        Minigame currentGame = gameManager.getCurrentGame();
        if (currentGame == null) return;

        // Find the arena at this location (for non-lobby arenas)
        ArenaInstance arenaAtLocation = gameManager.getArenaAt(blockLocation);
        if (arenaAtLocation == null) return;

        // If the location is within an active arena, check if the current minigame allows modification.
        if (!currentGame.canModifyBlock(player, blockLocation, gameManager)) {
            event.setCancelled(true);
        }
    }

    // ──────────────────────────────────────────────
    //  Utilities
    // ──────────────────────────────────────────────
    private void healPlayer(Player player) {
        var maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            player.setHealth(maxHealth.getValue());
        }
        player.setFoodLevel(20);
        player.setSaturation(5.0f);
        player.setFireTicks(0);
    }
}
