package org.antredesloutres.ottergames.models.minigames;

import org.antredesloutres.ottergames.models.ArenaInstance;
import org.antredesloutres.ottergames.models.minigames.selection.GameSelectionContext;
import org.antredesloutres.ottergames.models.minigames.selection.SelectionCondition;
import org.antredesloutres.ottergames.models.minigames.selection.SelectionConditionMode;

import java.util.List;

public interface Minigame {
    String getName();
    void onStart(List<ArenaInstance> arenas);
    void onEnd();
    int getDurationSeconds();
    String getStructureName();

    default int getInstanceCount() { return 1; }
    default int getInstanceCount(GameSelectionContext selectionContext) { return getInstanceCount(); }
    default boolean canBeSelected(int activeParticipantCount) { return true; }
    default List<SelectionCondition> getSelectionConditions() { return List.of(); }
    default SelectionConditionMode getSelectionConditionMode() { return SelectionConditionMode.ALL; }

    default boolean canBeSelected(GameSelectionContext selectionContext) {
        if (!canBeSelected(selectionContext.activeParticipantCount())) {
            return false;
        }

        List<SelectionCondition> selectionConditions = getSelectionConditions();
        if (selectionConditions.isEmpty()) {
            return true;
        }

        return switch (getSelectionConditionMode()) {
            case ANY -> selectionConditions.stream().anyMatch(condition -> condition.matches(selectionContext));
            case ALL -> selectionConditions.stream().allMatch(condition -> condition.matches(selectionContext));
        };
    }
}
