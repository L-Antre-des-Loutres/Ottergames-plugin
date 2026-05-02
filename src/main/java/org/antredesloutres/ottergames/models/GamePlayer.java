package org.antredesloutres.ottergames.models;
import java.util.UUID;

/**
 * Represents a player in the game, whether they are a participant or a spectator.
 */
public interface GamePlayer {
    UUID getId();
    String getUsername();
    boolean isSpectator();
}