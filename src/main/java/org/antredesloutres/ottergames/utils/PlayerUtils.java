package org.antredesloutres.ottergames.utils;

import org.bukkit.entity.Player;

public class PlayerUtils {

    /**
     * Completely clears a player's inventory, including armor and off-hand.
     * @param player The player to clear.
     */
    public static void clearInventory(Player player) {
        if (player == null) return;
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);
        player.updateInventory();
    }
}
