package org.antredesloutres.ottergames.models;

import java.util.HashSet;
import java.util.Set;

public class GameConfig {
    private Set<String> disabledGames = new HashSet<>();

    public Set<String> getDisabledGames() {
        return disabledGames;
    }

    public void setDisabledGames(Set<String> disabledGames) {
        this.disabledGames = disabledGames;
    }

    public boolean isGameEnabled(String gameName) {
        return !disabledGames.contains(gameName.toLowerCase());
    }

    public void setGameEnabled(String gameName, boolean enabled) {
        if (enabled) {
            disabledGames.remove(gameName.toLowerCase());
        } else {
            disabledGames.add(gameName.toLowerCase());
        }
    }
}
