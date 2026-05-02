package org.antredesloutres.ottergames.models.minigames.selection;

import java.util.Objects;
import java.util.function.Predicate;

@FunctionalInterface
public interface SelectionCondition {

    boolean matches(GameSelectionContext context);

    static SelectionCondition of(Predicate<GameSelectionContext> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return predicate::test;
    }
}
