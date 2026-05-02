package org.antredesloutres.ottergames.models.minigames.selection;

public final class SelectionConditions {

    private SelectionConditions() {
    }

    public static SelectionCondition activeParticipantCountIsOdd() {
        return SelectionCondition.of(context -> context.activeParticipantCount() > 0 && context.activeParticipantCount() % 2 != 0);
    }

    public static SelectionCondition activeParticipantCountIsEven() {
        return SelectionCondition.of(context -> context.activeParticipantCount() > 0 && context.activeParticipantCount() % 2 == 0);
    }

    public static SelectionCondition minActiveParticipants(int minActiveParticipants) {
        requireNonNegative(minActiveParticipants, "minActiveParticipants");
        return SelectionCondition.of(context -> context.activeParticipantCount() >= minActiveParticipants);
    }

    public static SelectionCondition maxActiveParticipants(int maxActiveParticipants) {
        requireNonNegative(maxActiveParticipants, "maxActiveParticipants");
        return SelectionCondition.of(context -> context.activeParticipantCount() <= maxActiveParticipants);
    }

    public static SelectionCondition minSpectators(int minSpectators) {
        requireNonNegative(minSpectators, "minSpectators");
        return SelectionCondition.of(context -> context.spectatorCount() >= minSpectators);
    }

    public static SelectionCondition maxSpectators(int maxSpectators) {
        requireNonNegative(maxSpectators, "maxSpectators");
        return SelectionCondition.of(context -> context.spectatorCount() <= maxSpectators);
    }

    public static SelectionCondition minTotalParticipants(int minTotalParticipants) {
        requireNonNegative(minTotalParticipants, "minTotalParticipants");
        return SelectionCondition.of(context -> context.totalParticipantCount() >= minTotalParticipants);
    }

    public static SelectionCondition maxTotalParticipants(int maxTotalParticipants) {
        requireNonNegative(maxTotalParticipants, "maxTotalParticipants");
        return SelectionCondition.of(context -> context.totalParticipantCount() <= maxTotalParticipants);
    }

    public static SelectionCondition minRound(int minRound) {
        if (minRound < 1) {
            throw new IllegalArgumentException("minRound must be >= 1");
        }
        return SelectionCondition.of(context -> context.roundNumber() >= minRound);
    }

    public static SelectionCondition maxRound(int maxRound) {
        if (maxRound < 1) {
            throw new IllegalArgumentException("maxRound must be >= 1");
        }
        return SelectionCondition.of(context -> context.roundNumber() <= maxRound);
    }

    public static SelectionCondition notFirstRound() {
        return minRound(2);
    }

    @Deprecated(forRemoval = false)
    public static SelectionCondition oddActiveParticipants() {
        return activeParticipantCountIsOdd();
    }

    @Deprecated(forRemoval = false)
    public static SelectionCondition evenActiveParticipants() {
        return activeParticipantCountIsEven();
    }

    @Deprecated(forRemoval = false)
    public static SelectionCondition oddParticipants() {
        return activeParticipantCountIsOdd();
    }

    @Deprecated(forRemoval = false)
    public static SelectionCondition evenParticipants() {
        return activeParticipantCountIsEven();
    }

    @Deprecated(forRemoval = false)
    public static SelectionCondition minParticipants(int minParticipants) {
        return minActiveParticipants(minParticipants);
    }

    @Deprecated(forRemoval = false)
    public static SelectionCondition maxParticipants(int maxParticipants) {
        return maxActiveParticipants(maxParticipants);
    }

    private static void requireNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must be >= 0");
        }
    }
}
