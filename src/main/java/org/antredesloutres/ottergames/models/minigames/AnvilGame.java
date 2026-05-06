package org.antredesloutres.ottergames.models.minigames;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.antredesloutres.ottergames.Main;
import org.antredesloutres.ottergames.managers.GameManager;
import org.antredesloutres.ottergames.models.arena.ArenaInstance;
import org.antredesloutres.ottergames.models.arena.ArenaSpawnZone;
import org.antredesloutres.ottergames.models.arena.MinigameArena;
import org.antredesloutres.ottergames.models.minigames.selection.SelectionCondition;
import org.antredesloutres.ottergames.models.minigames.selection.SelectionConditions;
import org.antredesloutres.ottergames.models.participant.GamePlayer;
import org.antredesloutres.ottergames.utils.Constants;
import org.antredesloutres.ottergames.utils.PlayerUtils;
import org.bukkit.*;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;

/**
 * Anvil Game Minigame
 * Players must avoid falling anvils.
 * If they are alive at the end of the timer, they win.
 */
public class AnvilGame implements Minigame, Listener {

    private final Main plugin;
    private final MinigameArena structure;
    private final Random random = new Random();
    private BukkitTask spawnTask;
    private final Map<UUID, Long> spectatorCooldowns = new HashMap<>();

    private static final int DURATION_SECONDS = 30;
    private static final int FALL_HEIGHT = 25;
    private static final int INITIAL_SPAWN_INTERVAL_TICKS = 10;
    private static final int MIN_SPAWN_INTERVAL_TICKS = 2;
    private static final long SPECTATOR_COOLDOWN_MS = 2000;

    public AnvilGame(Main plugin) {
        this.plugin = plugin;
        this.structure = new MinigameArena(
                plugin,
                Constants.STRUCTURE_CLUTCH,
                1, // 1 player per structure
                Map.of(
                        "center", new ArenaSpawnZone(0, 0, 0, 0, 0, 0, 0f, 0f)
                )
        );
    }

    @EventHandler
    public void onAnvilLand(EntityChangeBlockEvent event) {
        // Prevent anvils from breaking when landing on non-solid blocks or other anvils
        if (event.getEntity() instanceof FallingBlock fallingBlock && fallingBlock.getBlockData().getMaterial() == Material.ANVIL) {
            // If it's turning into AIR, it means it's breaking. We want it to stay.
            if (event.getTo() == Material.AIR) {
                event.setCancelled(true);
                event.getBlock().setType(Material.ANVIL);
            }
        }
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event, GameManager gameManager) {
        Player player = event.getPlayer();
        if (!gameManager.isPlayerSpectator(player.getUniqueId())) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.ANVIL) return;

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        // Cancel the event to prevent placing the anvil block
        event.setCancelled(true);

        long now = System.currentTimeMillis();
        long lastUse = spectatorCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - lastUse < SPECTATOR_COOLDOWN_MS) {
            player.sendMessage("§cWait before spawning another anvil!");
            return;
        }

        // Spawn anvil below spectator
        Location spawnLoc = player.getLocation().clone();
        FallingBlock fallingAnvil = spawnLoc.getWorld().spawnFallingBlock(spawnLoc, Material.ANVIL.createBlockData());
        configureAnvil(fallingAnvil);

        spectatorCooldowns.put(player.getUniqueId(), now);
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 0.5f);
    }

    private void configureAnvil(FallingBlock anvil) {
        anvil.setHurtEntities(true);
        anvil.setDamagePerBlock(2.0f);
        anvil.setMaxDamage(40);
        anvil.setDropItem(false);
    }

    @Override
    public void onGameSpectatorSpawn(Player player) {
        // Teleport spectators to the top of the arena
        ArenaInstance arena = plugin.getGameManager().getPlayerArena(player.getUniqueId());
        if (arena != null) {
            var size = structure.structure().getSize();
            Location topSpawn = arena.origin().clone().add(
                    size.getX() / 2.0,
                    FALL_HEIGHT + 2,
                    size.getZ() / 2.0
            );
            player.teleport(topSpawn);
        }

        // Configure spectator state
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setCollidable(false);
        player.setInvulnerable(true);
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));

        // Give spawner item
        PlayerUtils.clearInventory(player);
        ItemStack spawner = new ItemStack(Material.ANVIL);
        ItemMeta meta = spawner.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§6Anvil Spawner §7(Right Click)", NamedTextColor.GOLD));
            spawner.setItemMeta(meta);
        }
        player.getInventory().setItem(0, spawner);
    }

    @Override
    public String getName() {
        return "Anvil Game";
    }

    @Override
    public int getDurationSeconds() {
        return DURATION_SECONDS;
    }

    @Override
    public String getStructureName() {
        return structure.structureName();
    }

    @Override
    public int getInstanceCount(org.antredesloutres.ottergames.models.minigames.selection.GameSelectionContext selectionContext) {
        return structure.computeRequiredStructureCount(selectionContext.activeParticipantCount());
    }

    @Override
    public Location getSpawnLocation(ArenaInstance arena, Random random, int playerIndexInArena, int playersInArena) {
        var size = structure.structure().getSize();
        // Spawn in the middle of the platform, slightly above the floor
        return arena.origin().clone().add(
                size.getX() / 2.0,
                1.0,
                size.getZ() / 2.0
        );
    }

    @Override
    public void onStart(List<ArenaInstance> arenas, GameManager gameManager) {
        Bukkit.broadcastMessage(Constants.ANVIL_START_MESSAGE);

        spawnTask = new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                ticksElapsed++;
                
                // Increase difficulty over time
                int interval = Math.max(MIN_SPAWN_INTERVAL_TICKS, INITIAL_SPAWN_INTERVAL_TICKS - (ticksElapsed / 100));
                
                if (ticksElapsed % interval == 0) {
                    for (ArenaInstance arena : arenas) {
                        spawnAnvil(arena);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void spawnAnvil(ArenaInstance arena) {
        var size = structure.structure().getSize();
        double x = arena.origin().getX() + random.nextDouble() * size.getX();
        double z = arena.origin().getZ() + random.nextDouble() * size.getZ();
        double y = arena.origin().getY() + FALL_HEIGHT;

        Location anvilLoc = new Location(arena.origin().getWorld(), x, y, z);
        FallingBlock fallingAnvil = anvilLoc.getWorld().spawnFallingBlock(anvilLoc, Material.ANVIL.createBlockData());
        
        fallingAnvil.setHurtEntities(true);
        fallingAnvil.setDamagePerBlock(2.0f); // Damage multiplier
        fallingAnvil.setMaxDamage(40); // 20 hearts max
        fallingAnvil.setDropItem(false); // Don't drop as item if it fails to land
    }

    @Override
    public void onEnd(GameManager gameManager) {
        if (spawnTask != null) {
            spawnTask.cancel();
            spawnTask = null;
        }

        for (GamePlayer gp : gameManager.getActiveParticipants()) {
            Player player = Bukkit.getPlayer(gp.getUuid());
            if (player != null && player.isOnline() && player.getGameMode() != GameMode.SPECTATOR) {
                player.sendMessage(Constants.ANVIL_VICTORY_MESSAGE);
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                player.showTitle(Title.title(
                        Component.text(Constants.ANVIL_VICTORY_TITLE, NamedTextColor.GOLD),
                        Component.text(Constants.ANVIL_VICTORY_SUBTITLE, NamedTextColor.YELLOW),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(2), Duration.ofMillis(500))
                ));
            } else {
                gameManager.eliminatePlayer(gp.getUuid());
            }
        }
    }

    @Override
    public void onGamePlayerSpawn(Player player) {
        PlayerUtils.clearInventory(player);
        player.setGameMode(GameMode.SURVIVAL);
        player.setInvulnerable(false);
        player.setCollidable(true);
        // Remove invisibility if they had it from being a spectator
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
    }

    @Override
    public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event, GameManager gameManager) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (gameManager.isPlayerSpectator(playerId)) return;

        ArenaInstance arena = gameManager.getPlayerArena(playerId);
        if (arena == null) return;

        if (player.getLocation().getY() < arena.origin().getY() - 5) {
            gameManager.eliminatePlayer(playerId);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_DEATH, 1f, 1f);
            
            // Apply custom spectator setup immediately
            this.onGameSpectatorSpawn(player);
        }
    }

    @Override
    public List<SelectionCondition> getSelectionConditions() {
        return List.of(SelectionConditions.minActiveParticipants(1));
    }

    @Override
    public boolean pvpEnabled() {
        return false;
    }

    @Override
    public boolean eliminateOnDeath() {
        return true;
    }

    @Override
    public boolean instantRespawn() {
        return true;
    }
}
