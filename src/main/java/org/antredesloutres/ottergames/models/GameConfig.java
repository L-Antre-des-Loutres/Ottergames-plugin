package org.antredesloutres.ottergames.models;

import java.util.HashSet;
import java.util.Set;

public class GameConfig {
    private Set<String> disabledGames = new HashSet<>();
    private boolean preventSameGameConsecutively = true;
    private int minPlayersToContinue = 1;

    public Set<String> getDisabledGames() {
        return disabledGames;
    }

    public void setDisabledGames(Set<String> disabledGames) {
        this.disabledGames = disabledGames;
    }

    public boolean isPreventSameGameConsecutively() {
        return preventSameGameConsecutively;
    }

    public void setPreventSameGameConsecutively(boolean preventSameGameConsecutively) {
        this.preventSameGameConsecutively = preventSameGameConsecutively;
    }

    public int getMinPlayersToContinue() {
        return minPlayersToContinue;
    }

    public void setMinPlayersToContinue(int minPlayersToContinue) {
        this.minPlayersToContinue = minPlayersToContinue;
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
