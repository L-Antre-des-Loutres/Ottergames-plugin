package org.antredesloutres.ottergames.models.minigames;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.antredesloutres.ottergames.Main;
import org.antredesloutres.ottergames.managers.GameManager;
import org.antredesloutres.ottergames.models.arena.ArenaInstance;
import org.antredesloutres.ottergames.models.arena.ArenaRegion;
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

    // Game constants (Parameters stay here)
    private static final int GAME_DURATION_SECONDS = 30;
    private static final int SPAWN_ZONE_SIZE = 24;
    private static final int INITIAL_SPAWN_TICKS = 10;
    private static final int MIN_SPAWN_TICKS = 2;
    private static final int DIFFICULTY_INCREASE_INTERVAL_TICKS = 100;
    private static final long SPAWN_TASK_DELAY_TICKS = 20L;
    private static final long SPAWN_TASK_PERIOD_TICKS = 1L;

    // Anvil properties
    private static final float DAMAGE_PER_BLOCK = 2.0f;
    private static final int MAX_DAMAGE = 40;
    private static final int MIN_ANVILS_PER_SPAWN = 1;
    private static final int EXTRA_ANVILS_PER_SPAWN = 3; // 1 to 3 anvils total

    private final Main plugin;
    private final MinigameArena structure;
    private final Random random = new Random();
    private BukkitTask spawnTask;
    private final ArenaRegion anvilSpawnRegion;

    public AnvilGame(Main plugin) {
        this.plugin = plugin;
        
        // Load structure once to get dimensions and calculate center
        var struct = org.antredesloutres.ottergames.utils.StructureSpawner.load(plugin, Constants.STRUCTURE_ANVIL_GAME);
        if (struct == null) {
            throw new IllegalStateException("Could not load anvil_game structure!");
        }
        
        var size = struct.getSize();
        int centerX = size.getBlockX() / 2;
        int centerZ = size.getBlockZ() / 2;
        int halfAnvilZone = SPAWN_ZONE_SIZE / 2;

        // Define the anvil spawn area (Region)
        this.anvilSpawnRegion = new ArenaRegion(
                centerX - halfAnvilZone, 0, centerZ - halfAnvilZone,
                centerX + halfAnvilZone, size.getBlockY(), centerZ + halfAnvilZone
        );

        // Define the player spawn zone (SpawnZone) - Y=24 is 3 blocks above floor at Y=21
        this.structure = new MinigameArena(
                plugin,
                Constants.STRUCTURE_ANVIL_GAME,
                20,
                Map.of(
                        "players", new ArenaSpawnZone(
                                centerX - 5, 4, centerZ - 5,
                                centerX + 5, 4, centerZ + 5,
                                0f, 0f
                        )
                )
        );
    }


    @Override
    public List<SelectionCondition> getSelectionConditions() {
        return List.of(SelectionConditions.minActiveParticipants(1));
    }

    @Override
    public String getName() {
        return "Anvil Game";
    }

    @Override
    public int getDurationSeconds() {
        return GAME_DURATION_SECONDS;
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
        return structure.spawnZone("players").randomLocation(arena, random);
    }

    @Override
    public void onStart(List<ArenaInstance> arenas, GameManager gameManager) {
        Bukkit.broadcastMessage(Constants.ANVIL_START_MESSAGE);

        spawnTask = new BukkitRunnable() {
            int ticks = 0;
            int spawnInterval = INITIAL_SPAWN_TICKS;

            @Override
            public void run() {
                if (ticks % spawnInterval == 0) {
                    for (ArenaInstance arena : arenas) {
                        spawnAnvils(arena);
                    }
                }

                // Increase difficulty: decrease spawn interval every few seconds
                if (ticks > 0 && ticks % DIFFICULTY_INCREASE_INTERVAL_TICKS == 0 && spawnInterval > MIN_SPAWN_TICKS) {
                    spawnInterval--;
                }

                ticks++;
            }
        }.runTaskTimer(plugin, SPAWN_TASK_DELAY_TICKS, SPAWN_TASK_PERIOD_TICKS);
    }

    private void spawnAnvils(ArenaInstance arena) {
        // Spawn a random number of anvils per arena per interval
        int count = MIN_ANVILS_PER_SPAWN + random.nextInt(EXTRA_ANVILS_PER_SPAWN);

        for (int i = 0; i < count; i++) {
            int relX = anvilSpawnRegion.minX() + random.nextInt(anvilSpawnRegion.maxX() - anvilSpawnRegion.minX() + 1);
            int relZ = anvilSpawnRegion.minZ() + random.nextInt(anvilSpawnRegion.maxZ() - anvilSpawnRegion.minZ() + 1);
            int relY = anvilSpawnRegion.maxY() - 9; // Dynamically use the top of the structure

            Location spawnLoc = arena.origin().clone().add(relX, relY, relZ);

            FallingBlock anvil = spawnLoc.getWorld().spawnFallingBlock(spawnLoc, Bukkit.createBlockData(Material.ANVIL));
            anvil.setHurtEntities(true);
            anvil.setDamagePerBlock(DAMAGE_PER_BLOCK);
            anvil.setMaxDamage(MAX_DAMAGE);
            anvil.setDropItem(false);
        }
    }

    @Override
    public void onEnd(GameManager gameManager) {
        if (spawnTask != null) {
            spawnTask.cancel();
            spawnTask = null;
        }

        // Unregister this listener to avoid duplicate registrations in next rounds
        org.bukkit.event.HandlerList.unregisterAll(this);

        // Success message for survivors
        var title = Title.title(
                Component.text(Constants.ANVIL_VICTORY_TITLE, NamedTextColor.GOLD),
                Component.text(Constants.ANVIL_VICTORY_SUBTITLE, NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
        );

        for (GamePlayer gamePlayer : gameManager.getActiveParticipants()) {
            Player player = Bukkit.getPlayer(gamePlayer.getUuid());
            if (player != null && player.isOnline()) {
                player.showTitle(title);
                player.sendMessage(Constants.ANVIL_VICTORY_MESSAGE);
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
            }
        }
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock fallingBlock)) return;
        if (fallingBlock.getBlockData().getMaterial() != Material.ANVIL) return;

        // Prevent anvil from becoming a block
        event.setCancelled(true);
        fallingBlock.remove();

        // Play landing sound
        event.getBlock().getWorld().playSound(event.getBlock().getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.0f);
    }

    @Override
    public void onGamePlayerSpawn(Player player) {
        PlayerUtils.clearInventory(player);
        player.setGameMode(GameMode.ADVENTURE);
        player.setInvulnerable(false);
        player.setCollidable(true);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
    }


    @Override
    public boolean keepPlayersInBounds() {
        return true;
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
