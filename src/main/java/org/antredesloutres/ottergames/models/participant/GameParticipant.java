package org.antredesloutres.ottergames.models.participant;
import java.util.UUID;

/**
 * Represents a player in the game, whether they are a participant or a spectator.
 */
public interface GameParticipant {
    UUID getUuid();
    String getUsername();
    boolean isSpectator();
}