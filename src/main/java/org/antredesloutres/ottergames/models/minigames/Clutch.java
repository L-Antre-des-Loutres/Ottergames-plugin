package org.antredesloutres.ottergames.models.minigames;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.antredesloutres.ottergames.Main;
import org.antredesloutres.ottergames.managers.GameManager;
import org.antredesloutres.ottergames.models.arena.ArenaInstance;
import org.antredesloutres.ottergames.models.arena.ArenaSpawnZone;
import org.antredesloutres.ottergames.models.arena.MinigameArena;
import org.antredesloutres.ottergames.models.participant.GamePlayer;
import org.antredesloutres.ottergames.utils.Constants;
import org.antredesloutres.ottergames.utils.PlayerUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.*;

/**
 * Clutch Minigame
 * Players have ONE attempt to clutch.
 * If they are alive at the end of the timer, they win.
 */
public class Clutch implements Minigame {

    private final Main plugin;
    private final MinigameArena structure;
    private final Random random = new Random();

    // Configuration constants
    private static final int INITIAL_FALL_HEIGHT = 20;
    private static final int HEIGHT_INCREASE_PER_ROUND = 10;
    private static final int MEDIUM_LEVEL_ROUND = 6;
    private static final int HARD_LEVEL_ROUND = 12;

    private static final List<Material> EASY_ITEMS = List.of(Material.WATER_BUCKET);
    private static final List<Material> MEDIUM_ITEMS = List.of(Material.WIND_CHARGE, Material.COBWEB);
    private static final List<Material> HARD_ITEMS = List.of(Material.SLIME_BLOCK, Material.ENDER_PEARL, Material.WHITE_BED, Material.SWEET_BERRIES);

    public Clutch(Main plugin) {
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

    @Override
    public String getName() {
        return "Clutch";
    }

    @Override
    public int getDurationSeconds() {
        // Calculate height first to determine duration
        int round = plugin.getGameManager().getCurrentRound();
        int fallHeight = INITIAL_FALL_HEIGHT + (round * HEIGHT_INCREASE_PER_ROUND);

        return (fallHeight / 20);
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
        int round = plugin.getGameManager().getCurrentRound();
        int fallHeight = INITIAL_FALL_HEIGHT + (round * HEIGHT_INCREASE_PER_ROUND);
        
        var size = structure.structure().getSize();
        Location spawn = arena.origin().clone().add(
                size.getX() / 2.0,
                fallHeight,
                size.getZ() / 2.0
        );

        // Set pitch to 90 degrees to look straight down
        spawn.setPitch(90.0f);
        return spawn;
    }

    @Override
    public void onStart(List<ArenaInstance> arenas, GameManager gameManager) {
        int round = gameManager.getCurrentRound();
        int fallHeight = INITIAL_FALL_HEIGHT + (round * HEIGHT_INCREASE_PER_ROUND);
        Bukkit.broadcastMessage(String.format("§6[Clutch] §eSurvive the fall! §7Height: %d blocks.", fallHeight));
    }

    @Override
    public void onGamePlayerSpawn(Player player) {
        int round = plugin.getGameManager().getCurrentRound();
        Material clutchItem = selectItemForRound(round);

        PlayerUtils.clearInventory(player);
        player.getInventory().setItem(0, new ItemStack(clutchItem, 1));
        player.sendMessage("§aSurvival is victory! Clutch with: §b" + clutchItem.name().replace("_", " "));
        
        player.setGameMode(GameMode.SURVIVAL);
    }

    @Override
    public void onEnd(GameManager gameManager) {
        for (GamePlayer gp : gameManager.getActiveParticipants()) {
            Player player = Bukkit.getPlayer(gp.getUuid());
            if (player != null && player.isOnline() && player.getGameMode() != GameMode.SPECTATOR) {
                // If the player is still alive and not a spectator, they won!
                player.sendMessage("§aVictory! You survived the clutch.");
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                player.showTitle(Title.title(
                        Component.text("VICTORY!", NamedTextColor.GOLD),
                        Component.text("You survived the fall", NamedTextColor.YELLOW),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(2), Duration.ofMillis(500))
                ));
            } else {
                gameManager.eliminatePlayer(gp.getUuid());
            }
        }
    }

    private Material selectItemForRound(int round) {
        if (round <= MEDIUM_LEVEL_ROUND) return EASY_ITEMS.get(random.nextInt(EASY_ITEMS.size()));
        if (round <= HARD_LEVEL_ROUND) return MEDIUM_ITEMS.get(random.nextInt(MEDIUM_ITEMS.size()));
        return HARD_ITEMS.get(random.nextInt(HARD_ITEMS.size()));
    }

    @Override
    public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event, GameManager gameManager) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (gameManager.isPlayerSpectator(playerId)) return;

        ArenaInstance arena = gameManager.getPlayerArena(playerId);
        if (arena == null) return;

        // Still eliminate if they fall into the void (below the arena)
        if (player.getLocation().getY() < arena.origin().getY() - 5) {
            gameManager.eliminatePlayer(playerId);
            player.setGameMode(GameMode.SPECTATOR);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_DEATH, 1f, 1f);
        }
    }

    @Override
    public boolean pvpEnabled() {
        return false;
    }

    @Override
    public boolean disableFallDamage() {
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
