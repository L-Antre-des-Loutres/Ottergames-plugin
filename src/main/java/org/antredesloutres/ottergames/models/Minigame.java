package org.antredesloutres.ottergames.models;

public interface Minigame {
    String getName();
    void onStart();
    void onEnd();
    int getDurationSeconds();
}
