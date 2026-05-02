package org.antredesloutres.ottergames.models.minigames.selection;

public record GameSelectionContext(int roundNumber, int activeParticipantCount, int spectatorCount, int totalParticipantCount) {

    public GameSelectionContext {
        if (roundNumber < 1) {
            throw new IllegalArgumentException("roundNumber must be >= 1");
        }
        if (activeParticipantCount < 0) {
            throw new IllegalArgumentException("activeParticipantCount must be >= 0");
        }
        if (spectatorCount < 0) {
            throw new IllegalArgumentException("spectatorCount must be >= 0");
        }
        if (totalParticipantCount < 0) {
            throw new IllegalArgumentException("totalParticipantCount must be >= 0");
        }
    }
}
