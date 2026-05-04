package org.antredesloutres.ottergames.models.minigames;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.antredesloutres.ottergames.managers.GameManager;
import org.antredesloutres.ottergames.utils.Constants;
import org.antredesloutres.ottergames.models.arena.ArenaInstance;
import org.antredesloutres.ottergames.models.arena.ArenaRegion;
import org.antredesloutres.ottergames.models.arena.ArenaSpawnZone;
import org.antredesloutres.ottergames.models.minigames.selection.GameSelectionContext;
import org.antredesloutres.ottergames.models.minigames.selection.SelectionConditionMode;
import org.antredesloutres.ottergames.models.participant.GamePlayer;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.*;

/**
 * Dropper
 */
public class Dropper implements Minigame {

    // Zone de victoire : piscine d'eau en bas de la structure (coordonnées relatives à l'origine de l'arène)
    private static final ArenaRegion VICTORY_ZONE = new ArenaRegion(0, 0, 0, 59, 1, 59);

    private final List<ArenaSpawnZone> spawnZones;
    private final Set<UUID> finishedPlayers = new HashSet<>();

    public Dropper() {
        this.spawnZones = List.of(
                new ArenaSpawnZone(30, 162, 28, 30, 162, 28, 0.0f, 0.0f)
        );
    }

    @Override
    public String getName() { return "Dropper"; }

    @Override
    public int getDurationSeconds() { return 30; }

    @Override
    public String getStructureName() { return Constants.STRUCTURE_DROPPER; }

    @Override
    public int getInstanceCount(GameSelectionContext selectionContext) { return 1; }

    @Override
    public SelectionConditionMode getSelectionConditionMode() { return SelectionConditionMode.ANY; }

    @Override
    public Location getSpawnLocation(ArenaInstance arena, Random random, int playerIndexInArena, int playersInArena) {
        ArenaSpawnZone selectedZone = spawnZones.get(random.nextInt(spawnZones.size()));
        return selectedZone.randomLocation(arena, random);
    }

    @Override
    public void onGamePlayerSpawn(Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, -1, 0, false, false, false));
        player.sendMessage(Constants.DROPPER_SURVIVE_MESSAGE);
        player.setHealth(1);
        Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(1);
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
    }

    @Override
    public void onStart(List<ArenaInstance> arenas, GameManager gameManager) {
        finishedPlayers.clear();
    }

    @Override
    public void onEnd(GameManager gameManager) {
        List<String> winnerNames = new ArrayList<>();
        List<String> loserNames = new ArrayList<>();

        for (GamePlayer gp : gameManager.getParticipants()) {
            Player bukkitPlayer = Bukkit.getPlayer(gp.getUuid());
            String name = bukkitPlayer != null ? bukkitPlayer.getName() : gp.getUuid().toString();

            if (!gp.isSpectator()) {
                if (finishedPlayers.contains(gp.getUuid())) {
                    winnerNames.add(name);
                } else {
                    loserNames.add(name);
                    gameManager.eliminatePlayer(gp.getUuid());
                }
            }

            if (bukkitPlayer != null) {
                bukkitPlayer.setInvulnerable(false);
                bukkitPlayer.getInventory().clear();
                bukkitPlayer.getInventory().setArmorContents(new ItemStack[4]);
            }
        }

        Bukkit.broadcastMessage(Constants.DROPPER_GAME_END);
        if (!winnerNames.isEmpty()) {
            Bukkit.broadcastMessage(Constants.DROPPER_WINNERS_PREFIX + String.join(", ", winnerNames));
        }
        if (!loserNames.isEmpty()) {
            Bukkit.broadcastMessage(Constants.DROPPER_LOSERS_PREFIX + String.join(", ", loserNames));
        }
        if (winnerNames.isEmpty()) {
            Bukkit.broadcastMessage(Constants.DROPPER_NO_WINNER);
        }

        finishedPlayers.clear();
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent event, GameManager gameManager) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (finishedPlayers.contains(playerId)) return;
        if (gameManager.isPlayerSpectator(playerId)) return;

        ArenaInstance arena = gameManager.getPlayerArena(playerId);
        if (arena == null) return;

        if (!VICTORY_ZONE.contains(arena, event.getTo())) return;

        finishedPlayers.add(playerId);
        player.setInvulnerable(true);

        player.showTitle(Title.title(
                Component.text(Constants.DROPPER_VICTORY_TITLE, NamedTextColor.GOLD),
                Component.text(Constants.DROPPER_VICTORY_SUBTITLE, NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
        ));
        player.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

        for (Map.Entry<UUID, ArenaInstance> entry : gameManager.getPlayerArenaAssignments().entrySet()) {
            if (!entry.getValue().equals(arena) || entry.getKey().equals(playerId)) continue;
            Player other = Bukkit.getPlayer(entry.getKey());
            if (other != null) {
                other.sendMessage(String.format(Constants.DROPPER_OTHER_REACHED, player.getName()));
            }
        }

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
    public boolean pvpEnabled() {
        return false;
    }

    @Override
    public boolean restoreInventoryOnDeath() {
        return true;
    }

}
