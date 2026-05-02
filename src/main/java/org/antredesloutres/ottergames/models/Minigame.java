package org.antredesloutres.ottergames.models;

import java.util.List;

public interface Minigame {
    String getName();
    void onStart(List<ArenaInstance> arenas);
    void onEnd();
    int getDurationSeconds();
    String getStructureName();

    default int getInstanceCount() { return 1; }
}
