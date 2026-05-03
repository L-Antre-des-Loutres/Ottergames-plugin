package org.antredesloutres.ottergames.models.minigames;

import org.antredesloutres.ottergames.Main;
import org.antredesloutres.ottergames.managers.GameManager;
import org.antredesloutres.ottergames.models.arena.ArenaInstance;
import org.antredesloutres.ottergames.models.minigames.selection.GameSelectionContext;
import org.antredesloutres.ottergames.models.minigames.selection.SelectionCondition;
import org.antredesloutres.ottergames.models.minigames.selection.SelectionConditions;
import org.antredesloutres.ottergames.models.arena.ArenaRegion;
import org.antredesloutres.ottergames.models.arena.ArenaSpawnZone;
import org.antredesloutres.ottergames.models.arena.MinigameArena;
import org.antredesloutres.ottergames.utils.StructureSpawner;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * Hikabrain is a fast-paced, team-based minigame where players are divided into two teams (Red and Blue).
 * <GOAL>
 * Goal:
 * Compete to reach the opposing team's goal area while defending your own.
 * Each time a player enters the enemy goal, their team scores a point, the arena is reset, 
 * and players are teleported back to spawn.
 * <GAMEPLAY>
 * Gameplay:
 * Players can place blocks (Sandstone) to build bridges or defenses. Fall damage is disabled, 
 * and players receive a full set of team-colored leather armor and basic tools.
 * <PROTECTION>
 * Map Protection:
 * Specific zones are protected from modification:
 * - Spawn areas (SPAWN_TEAM_1, SPAWN_TEAM_2)
 * - Goal areas (GOAL_TEAM_1, GOAL_TEAM_2)
 * - Obsidian blocks cannot be broken.
 * <SELECT CONDITION>
 * Selection & Launch Conditions:
 * - Minimum active participants: 2 (Even number required).
 * - Duration: 120 seconds.
 * - Structure: "ottergames_hikabrain_map".
 * <VICTORY CONDITION>
 * Victory:
 * The team with the most points when the timer reaches zero wins the game.
 */
public class Hikabrain implements Minigame {

    private static final String TEAM_1 = "team_1";
    private static final String TEAM_2 = "team_2";

    // Zones for the beds (goals to reach)
    private static final ArenaRegion GOAL_TEAM_1 = new ArenaRegion(1, 13, 9, 1, 16, 9, false);
    private static final ArenaRegion GOAL_TEAM_2 = new ArenaRegion(47, 13, 9, 47, 16, 9, false);

    // Spawn zones (protected areas where blocks cannot be placed)
    private static final ArenaRegion SPAWN_TEAM_1 = new ArenaRegion(1, 17, 8, 3, 20, 10, false);
    private static final ArenaRegion SPAWN_TEAM_2 = new ArenaRegion(45, 17, 8, 47, 20, 10, false);

    private static final List<ArenaRegion> PROTECTED_REGIONS = List.of(GOAL_TEAM_1, GOAL_TEAM_2, SPAWN_TEAM_1, SPAWN_TEAM_2);

    private final Main plugin;
    private final MinigameArena structure;

    private final Map<UUID, String> playerTeams = new HashMap<>();
    private final Map<ArenaInstance, Map<String, Integer>> arenaScores = new HashMap<>();

    public Hikabrain(Main plugin) {
        this.plugin = plugin;
        this.structure = new MinigameArena(
                plugin,
                "ottergames_hikabrain_map",
                2,
                Map.of(
                        TEAM_1, new ArenaSpawnZone(2, 19, 9, 2, 19, 9, -90.0f, 19.3f),
                        TEAM_2, new ArenaSpawnZone(46, 19, 9, 46, 19, 9, 90.0f, 18.9f)
                )
        );
    }

    @Override
    public String getName() {
        return "Hikabrain";
    }

    @Override
    public int getDurationSeconds() {
        return 60;
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
        return List.of(SelectionConditions.minActiveParticipants(2),
                SelectionConditions.activeParticipantCountIsEven()
        );
    }

    @Override
    public ArenaSpawnZone getSpawnZone(ArenaInstance arena) {
        return structure.firstSpawnZone();
    }

    @Override
    public Location getSpawnLocation(ArenaInstance arena, Random random, int playerIndexInArena, int playersInArena) {
        String zoneName = (playerIndexInArena % 2 == 0) ? TEAM_1 : TEAM_2;
        ArenaSpawnZone spawnZone = structure.spawnZone(zoneName);
        return spawnZone.randomLocation(arena, random);
    }

    @Override
    public boolean canModifyBlock(Player player, Location blockLocation, GameManager gameManager) {
        ArenaInstance arena = gameManager.getPlayerArena(player.getUniqueId());
        if (arena == null) return true;

        for (ArenaRegion region : PROTECTED_REGIONS) {
            if (region.contains(arena, blockLocation)) {
                if (!region.allowBlockChanges()) {
                    player.sendMessage("§c✗ Tu ne peux pas modifier cette zone!");
                    return false;
                }
            }
        }

        if (blockLocation.getBlock().getType() == Material.OBSIDIAN) {
            player.sendMessage("§c✗ Tu ne peux pas casser d'obsidienne!");
            return false;
        }

        return true;
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
        player.getInventory().setItem(0, new ItemStack(Material.IRON_SWORD));
        player.getInventory().setItem(1, new ItemStack(Material.IRON_PICKAXE));
        player.getInventory().setItem(2, new ItemStack(Material.SANDSTONE, 64));
        player.getInventory().setItem(3, new ItemStack(Material.GOLDEN_APPLE, 8));
        player.getInventory().setItemInOffHand(new ItemStack(Material.SANDSTONE, 64));
        player.getInventory().setItem(4, new ItemStack(Material.SANDSTONE, 64));
        player.getInventory().setItem(5, new ItemStack(Material.SANDSTONE, 64));
        player.getInventory().setItem(6, new ItemStack(Material.SANDSTONE, 64));
        player.getInventory().setItem(7, new ItemStack(Material.SANDSTONE, 64));
        player.getInventory().setItem(8, new ItemStack(Material.SANDSTONE, 64));
        player.getInventory().setItem(9, new ItemStack(Material.SANDSTONE, 64));

        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);

        String team = playerTeams.get(player.getUniqueId());
        Color teamColor = null;
        if (TEAM_1.equals(team)) {
            teamColor = Color.RED;
        } else if (TEAM_2.equals(team)) {
            teamColor = Color.BLUE;
        }

        if (teamColor != null) {
            applyColor(helmet, teamColor);
            applyColor(chestplate, teamColor);
            applyColor(leggings, teamColor);
            applyColor(boots, teamColor);
        }

        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);

        player.updateInventory();
    }

    private void applyColor(ItemStack item, Color color) {
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        if (meta != null) {
            meta.setColor(color);
            item.setItemMeta(meta);
        }
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

        for (UUID playerId : gameManager.getPlayerSpawnLocations().keySet()) {
            Location spawn = gameManager.getPlayerSpawnLocation(playerId);
            ArenaInstance arena = gameManager.getPlayerArena(playerId);
            if (spawn != null && arena != null) {
                double relX = spawn.getX() - arena.origin().getX();
                if (relX < 24) {
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
        for (Map.Entry<ArenaInstance, Map<String, Integer>> entry : arenaScores.entrySet()) {
            Map<String, Integer> scores = entry.getValue();
            int score1 = scores.get(TEAM_1);
            int score2 = scores.get(TEAM_2);

            List<Player> team1Players = new java.util.ArrayList<>();
            List<Player> team2Players = new java.util.ArrayList<>();

            for (Map.Entry<UUID, ArenaInstance> playerEntry : gameManager.getPlayerArenaAssignments().entrySet()) {
                if (playerEntry.getValue().equals(entry.getKey())) {
                    Player p = Bukkit.getPlayer(playerEntry.getKey());
                    if (p != null) {
                        String team = playerTeams.get(playerEntry.getKey());
                        if (TEAM_1.equals(team)) team1Players.add(p);
                        else if (TEAM_2.equals(team)) team2Players.add(p);
                    }
                }
            }

            String winnerMessage;
            if (score1 > score2) {
                winnerMessage = "§c✨ Team Rouge ✨§a (" + team1Players.stream().map(Player::getName).collect(Collectors.joining(", ")) + ") a gagné!";
            } else if (score2 > score1) {
                winnerMessage = "§9✨ Team Bleue ✨§a (" + team2Players.stream().map(Player::getName).collect(Collectors.joining(", ")) + ") a gagné!";
            } else {
                winnerMessage = "§7✨ Égalité! ✨";
            }

            for (Map.Entry<UUID, ArenaInstance> playerEntry : gameManager.getPlayerArenaAssignments().entrySet()) {
                if (playerEntry.getValue().equals(entry.getKey())) {
                    Player p = Bukkit.getPlayer(playerEntry.getKey());
                    if (p != null) {
                        p.sendMessage("§6=== Fin du Hikabrain ===");
                        p.sendMessage("§eScore final: Rouge §c" + score1 + " §f- §9" + score2 + " §eBleu");
                        p.sendMessage(winnerMessage);
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
        if (team.equals(TEAM_1) && GOAL_TEAM_2.contains(arena, event.getTo())) {
            Map<String, Integer> scores = arenaScores.get(arena);
            scores.put(TEAM_1, scores.get(TEAM_1) + 1);
            scored = true;
        } else if (team.equals(TEAM_2) && GOAL_TEAM_1.contains(arena, event.getTo())) {
            Map<String, Integer> scores = arenaScores.get(arena);
            scores.put(TEAM_2, scores.get(TEAM_2) + 1);
            scored = true;
        }

        if (scored) {
            Map<String, Integer> scores = arenaScores.get(arena);
            int score1 = scores.get(TEAM_1);
            int score2 = scores.get(TEAM_2);

            for (Map.Entry<UUID, ArenaInstance> playerEntry : gameManager.getPlayerArenaAssignments().entrySet()) {
                if (playerEntry.getValue().equals(arena)) {
                    Player p = Bukkit.getPlayer(playerEntry.getKey());
                    if (p != null) {
                        p.sendMessage("§6Un point a été marqué ! Score actuel: Rouge §c" + score1 + " §f- §9" + score2 + " §6Bleu");
                        healPlayer(p);
                        p.getInventory().clear();
                        p.getInventory().setArmorContents(null);
                        p.getInventory().setItemInOffHand(null);
                        Location spawn = gameManager.getPlayerSpawnLocation(p.getUniqueId());
                        if (spawn != null) {
                            p.teleport(spawn);
                            applyStartingInventory(p);
                            p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                        }
                    }
                }
            }
            arena.clear();
            StructureSpawner.spawn(plugin, structure.structureName(), arena.origin());
        }
    }

    private void healPlayer(Player player) {
        var maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) player.setHealth(maxHealth.getValue());
        player.setFoodLevel(20);
        player.setSaturation(5.0f);
        player.setFireTicks(0);
    }
}
