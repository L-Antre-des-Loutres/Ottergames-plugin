package org.antredesloutres.ottergames.models.minigames;

import org.antredesloutres.ottergames.Main;
import org.antredesloutres.ottergames.managers.GameManager;
import org.antredesloutres.ottergames.models.ArenaInstance;
import org.antredesloutres.ottergames.models.minigames.selection.GameSelectionContext;
import org.antredesloutres.ottergames.models.minigames.selection.SelectionCondition;
import org.antredesloutres.ottergames.models.minigames.selection.SelectionConditions;
import org.antredesloutres.ottergames.models.structures.ArenaRegion;
import org.antredesloutres.ottergames.models.structures.ArenaSpawnZone;
import org.antredesloutres.ottergames.models.structures.MinigameStructure;
import org.antredesloutres.ottergames.utils.StructureSpawner;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class Hikabrain implements Minigame {

    private static final String TEAM_1 = "team_1";
    private static final String TEAM_2 = "team_2";

    // Zones for the beds (goals to reach)
    // Expanded bounding boxes to ensure the point triggers when players reach the general bed area
    private static final ArenaRegion GOAL_TEAM_1 = new ArenaRegion(1, 14, 9, 11, 16, 9);
    private static final ArenaRegion GOAL_TEAM_2 = new ArenaRegion(47, 14, 9, 47, 16, 9);

    private final Main plugin;
    private final MinigameStructure structure;

    private final Map<UUID, String> playerTeams = new HashMap<>();
    private final Map<ArenaInstance, Map<String, Integer>> arenaScores = new HashMap<>();

    public Hikabrain(Main plugin) {
        this.plugin = plugin;
        this.structure = new MinigameStructure(
                plugin,
                "ottergames_hikabrain_map",
                2,
                Map.of(
                        TEAM_1, new ArenaSpawnZone(2, 19, 9, 2, 19, 9, -90.0f, 19.3f),
                        TEAM_2, new ArenaSpawnZone(46, 19, 9, 46, 19, 9, 90.0f, 18.9f)
                )
        );
    }

    public MinigameStructure getStructure() {
        return structure;
    }

    @Override
    public String getName() {
        return "Hikabrain";
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
    public int getInstanceCount(GameSelectionContext selectionContext) {
        return Math.max(1, structure.computeRequiredStructureCount(selectionContext.activeParticipantCount()));
    }

    @Override
    public List<SelectionCondition> getSelectionConditions() {
        return List.of(SelectionConditions.minActiveParticipants(1));
    }

    @Override
    public ArenaSpawnZone getSpawnZone(ArenaInstance arena) {
        return structure.firstSpawnZone();
    }

    @Override
    public Location getSpawnLocation(ArenaInstance arena, Random random, int playerIndexInArena, int playersInArena) {
        // Alternate players between the two team zones
        String zoneName = (playerIndexInArena % 2 == 0) ? TEAM_1 : TEAM_2;
        ArenaSpawnZone spawnZone = structure.spawnZone(zoneName);
        return spawnZone.randomLocation(arena, random);
    }



    @Override
    public boolean keepPlayersInBounds() {
        return true;
    }

    @Override
    public boolean instantRespawn() {
        return true;
    }

    @Override
    public boolean healOnBoundsExit() {
        return true;
    }

    @Override
    public boolean eliminateOnDeath() {
        return false;
    }

    @Override
    public boolean disableFallDamage() {
        return true;
    }

    @Override
    public boolean restoreInventoryOnDeath() {
        return true;
    }

    @Override
    public boolean restoreInventoryOnBoundsExit() {
        return true;
    }

    @Override
    public void applyStartingInventory(Player player) {
        // Hikabrain gear
        player.getInventory().setItem(0, new ItemStack(Material.IRON_SWORD));
        player.getInventory().setItem(1, new ItemStack(Material.STONE_PICKAXE));
        player.getInventory().setItem(2, new ItemStack(Material.SANDSTONE, 64));
        player.getInventory().setItem(3, new ItemStack(Material.GOLDEN_APPLE, 8));

        // Offhand
        player.getInventory().setItemInOffHand(new ItemStack(Material.SANDSTONE, 64));

        // Basic armor
        player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));

        // Colored Helmet
        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta meta = (LeatherArmorMeta) helmet.getItemMeta();
        if (meta != null) {
            String team = playerTeams.get(player.getUniqueId());
            if (TEAM_1.equals(team)) {
                meta.setColor(Color.RED);
            } else if (TEAM_2.equals(team)) {
                meta.setColor(Color.BLUE);
            }
            helmet.setItemMeta(meta);
        }
        player.getInventory().setHelmet(helmet);

        player.updateInventory();
    }

    @Override
    public void onStart(List<ArenaInstance> arenas, GameManager gameManager) {
        playerTeams.clear();
        arenaScores.clear();

        for (ArenaInstance arena : arenas) {
            Map<String, Integer> scores = new HashMap<>();
            scores.put(TEAM_1, 0);
            scores.put(TEAM_2, 0);
            arenaScores.put(arena, scores);
        }

        // We can determine team by checking the spawn location relative to the arena
        for (UUID playerId : gameManager.getPlayerSpawnLocations().keySet()) {
            Location spawn = gameManager.getPlayerSpawnLocation(playerId);
            ArenaInstance arena = gameManager.getPlayerArena(playerId);
            if (spawn != null && arena != null) {
                double relX = spawn.getX() - arena.origin().getX();
                if (relX < 24) { // Team 1 is at X=2, Team 2 is at X=46
                    playerTeams.put(playerId, TEAM_1);
                    Player p = Bukkit.getPlayer(playerId);
                    if (p != null) p.sendMessage("§aTu es dans la §cTeam Rouge §a!");
                } else {
                    playerTeams.put(playerId, TEAM_2);
                    Player p = Bukkit.getPlayer(playerId);
                    if (p != null) p.sendMessage("§aTu es dans la §9Team Bleue §a!");
                }
            }
        }
    }

    @Override
    public void onEnd(GameManager gameManager) {
        // Announce winners
        for (Map.Entry<ArenaInstance, Map<String, Integer>> entry : arenaScores.entrySet()) {
            Map<String, Integer> scores = entry.getValue();
            int score1 = scores.get(TEAM_1);
            int score2 = scores.get(TEAM_2);
            String winnerTeam = score1 > score2 ? "§cTeam Rouge" : (score2 > score1 ? "§9Team Bleue" : "§7Personne (Égalité)");
            
            // Send to players in this arena
            for (Map.Entry<UUID, ArenaInstance> playerEntry : gameManager.getPlayerArenaAssignments().entrySet()) {
                if (playerEntry.getValue().equals(entry.getKey())) {
                    Player p = Bukkit.getPlayer(playerEntry.getKey());
                    if (p != null) {
                        p.sendMessage("§6=== Fin du Hikabrain ===");
                        p.sendMessage("§eScore final: Rouge §c" + score1 + " §f- §9" + score2 + " §eBleu");
                        p.sendMessage("§aGagnant: §b" + winnerTeam);
                        p.playSound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                    }
                }
            }
        }
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent event, GameManager gameManager) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        ArenaInstance arena = gameManager.getPlayerArena(playerId);
        String team = playerTeams.get(playerId);

        if (arena == null || team == null) return;

        boolean scored = false;

        // Team 1 scores by reaching Team 2's goal
        if (team.equals(TEAM_1) && GOAL_TEAM_2.contains(arena, event.getTo())) {
            Map<String, Integer> scores = arenaScores.get(arena);
            scores.put(TEAM_1, scores.get(TEAM_1) + 1);
            scored = true;
        } 
        // Team 2 scores by reaching Team 1's goal
        else if (team.equals(TEAM_2) && GOAL_TEAM_1.contains(arena, event.getTo())) {
            Map<String, Integer> scores = arenaScores.get(arena);
            scores.put(TEAM_2, scores.get(TEAM_2) + 1);
            scored = true;
        }

        if (scored) {
            Map<String, Integer> scores = arenaScores.get(arena);
            int score1 = scores.get(TEAM_1);
            int score2 = scores.get(TEAM_2);

            // Announce point and reset
            for (Map.Entry<UUID, ArenaInstance> playerEntry : gameManager.getPlayerArenaAssignments().entrySet()) {
                if (playerEntry.getValue().equals(arena)) {
                    Player p = Bukkit.getPlayer(playerEntry.getKey());
                    if (p != null) {
                        p.sendMessage("§6Un point a été marqué ! Score actuel: Rouge §c" + score1 + " §f- §9" + score2 + " §6Bleu");
                        
                        // Clear the player to avoid double pickup
                        p.getInventory().clear();
                        p.getInventory().setArmorContents(null);
                        p.getInventory().setItemInOffHand(null);
                        
                        // Teleport back to spawn
                        Location spawn = gameManager.getPlayerSpawnLocation(p.getUniqueId());
                        if (spawn != null) {
                            p.teleport(spawn);
                            applyStartingInventory(p);
                            p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                        }
                    }
                }
            }

            // Reset structure
            arena.clear();
            StructureSpawner.spawn(plugin, structure.structureName(), arena.origin());
        }
    }
}
