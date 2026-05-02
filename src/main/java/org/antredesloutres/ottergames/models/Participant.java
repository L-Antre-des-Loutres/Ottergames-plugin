package org.antredesloutres.ottergames.models;

import org.antredesloutres.ottergames.models.interfaces.GameParticipant;

import java.util.Objects;
import java.util.UUID;

import static org.antredesloutres.ottergames.utils.Constants.PARTICIPANT_NO_USERNAME;
import static org.antredesloutres.ottergames.utils.Constants.PARTICIPANT_NO_UUID;

/**
 * Represents a game participant.
 * @author matheo-1712
 */
public record Participant(UUID uuid, String username, boolean spectator) implements GameParticipant {

    // Validation to prevent null values upon creation
    public Participant {
        Objects.requireNonNull(uuid, PARTICIPANT_NO_UUID);
        Objects.requireNonNull(username, PARTICIPANT_NO_USERNAME);
    }

    // Implementing interface methods to return the record's fields
    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isSpectator() {
        return spectator;
    }
}