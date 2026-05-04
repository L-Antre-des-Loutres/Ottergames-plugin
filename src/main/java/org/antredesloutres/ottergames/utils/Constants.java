package org.antredesloutres.ottergames.utils;

public final class Constants {

    private Constants() {}

    public static final String PLUGIN_NAME = "Ottergames";

    // ──────────────────────────────────────────────
    //  Arena
    // ──────────────────────────────────────────────
    public static final int ARENA_BASE_X = 10000;
    public static final int ARENA_BASE_Y = -62;
    public static final int ARENA_BASE_Z = 0;
    public static final int OUTER_PADDING = 20;

    // ──────────────────────────────────────────────
    //  GameManager
    // ──────────────────────────────────────────────
    public static final String GAME_MANAGER_ALREADY_RUNNING = "A game is already running.";
    public static final String GAME_MANAGER_NO_GAME_RUNNING  = "No game is currently running.";
    public static final String GAME_MANAGER_STARTED          = "Game is started";
    public static final String GAME_MANAGER_STOPPED          = "Game is stopped";

    // ──────────────────────────────────────────────
    //  Participant
    // ──────────────────────────────────────────────
    public static final String PARTICIPANT_NO_UUID     = "UUID cannot be null";
    public static final String PARTICIPANT_NO_USERNAME = "Username cannot be null";

    // ──────────────────────────────────────────────
    //  Commands
    // ──────────────────────────────────────────────
    public static final String COMMAND_USER_MUST_BE_PLAYER = "§cThis command is for players only.";
    /// Ottergames
    public static final String OTTERGAMES_ARGS_START       = "start";
    public static final String OTTERGAMES_ARGS_STOP        = "stop";
    public static final String OTTERGAMES_ARGS_LEAVE       = "leave";
    public static final String OTTERGAMES_USAGE            = "§b[OtterGames] §7Use §f/ottergames start§7, §f/ottergames stop §7or §f/ottergames leave§7.";
    public static final String OTTERGAMES_ALREADY_RUNNING  = "§cThe game is already running!";
    public static final String OTTERGAMES_STARTED          = "§a§lStarting the minigame series!";
    public static final String OTTERGAMES_START_BLOCKED    = "§cImpossible de démarrer: aucun mini-jeu compatible avec les conditions actuelles.";
    public static final String OTTERGAMES_NOT_RUNNING      = "§eNo game is currently running.";
    public static final String OTTERGAMES_STOPPED          = "§cGame stopped.";
    public static final String OTTERGAMES_ALREADY_LEFT     = "§eYou have already opted out of Ottergames.";
    public static final String OTTERGAMES_LEFT_SPECTATING  = "§eYou have opted out of Ottergames and will spectate until the game ends.";
    public static final String OTTERGAMES_LEFT             = "§eYou have opted out of Ottergames.";
    public static final String OTTERGAMES_UNKNOWN_COMMAND  = "§cUnknown command. Use §f/ottergames start§c, §f/ottergames stop §cor §f/ottergames leave§c.";
    /// Otterdev
    public static final String OTTERDEV_ARGS_TEST          = "test";
    public static final String OTTERDEV_USAGE              = "§b[OtterDev] §7Subcommands: §ftest <structure_name>";
    public static final String OTTERDEV_TEST_USAGE         = "§cUsage: /otterdev test <structure_name>";
    public static final String OTTERDEV_STRUCTURE_SPAWNED  = "§a[OtterDev] §7Structure spawned: ";
}
