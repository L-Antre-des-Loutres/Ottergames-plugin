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
    //  Logger
    // ──────────────────────────────────────────────
    public static final String LOGGER_PLUGIN_READY    = "Ottergames ready!";
    public static final String LOGGER_PLUGIN_DISABLED = "Ottergames disabled!";
    public static final String LOGGER_LOOP_STARTED    = "Ottergame game loop started with lobby.";
    public static final String LOGGER_LOOP_STOPPED    = "Ottergame game loop stopped.";

    // ──────────────────────────────────────────────
    //  GameManager
    // ──────────────────────────────────────────────
    public static final String GAME_MANAGER_BREAK_TIME        = "Break time! Next game starts in %d seconds.";
    public static final String GAME_MANAGER_STARTING_GAME     = "Starting %s in...";
    public static final String GAME_MANAGER_STARTING_OTTER    = "OtterGames starts in...";
    public static final String GAME_MANAGER_NO_GAME_AVAILABLE = "§cNo minigame compatible with the current situation. Game stopped.";
    public static final String GAME_MANAGER_NEXT_GAME         = "§6Next game: §e%s";
    public static final String LOGGER_NO_GAME_PRESELECTED     = "No minigame was pre-selected. Stopping game loop.";
    public static final String LOGGER_NEXT_GAME_PRELOADING    = "Pre-loading: %s (%d arena(s)).";
    public static final String LOGGER_NO_GAME_ROUND_START     = "Cannot start game loop: no minigame available for round %d (active=%d, spectators=%d, total=%d).";
    public static final String LOGGER_MINIGAME_STOPPED        = "Minigame stopped: %s.";
    public static final String LOGGER_MINIGAME_STARTED        = "Minigame started: %s (%ds).";
    public static final String LOGGER_NO_GAME_FOR_ROUND       = "No minigame available for round %d (active=%d, spectators=%d, total=%d).";
    public static final String LOGGER_MINIGAME_ENDED          = "Minigame ended: %s.";
    public static final String LOGGER_TELEPORT_WORLD_NULL     = "Cannot teleport %s: spawn world is null.";

    // ──────────────────────────────────────────────
    //  Configuration
    // ──────────────────────────────────────────────
    public static final String CONFIG_USAGE           = "§b[OtterConfig] §7Use §f/ottergames config games §7or §f/ottergames config rules§7.";
    public static final String CONFIG_GAMES_USAGE     = "§b[OtterConfig] §7Use §f/ottergames config games list§7, §f... enable <name> §7or §f... disable <name>§7.";
    public static final String CONFIG_RULES_USAGE     = "§b[OtterConfig] §7Use §f/ottergames config rules list §7or §f... preventConsecutive <true|false>§7.";
    public static final String CONFIG_LIST_HEADER     = "§6=== Minigames Status ===";
    public static final String CONFIG_RULES_HEADER    = "§6=== Game Rules ===";
    public static final String CONFIG_GAME_ENTRY      = "§7- %s: %s";
    public static final String CONFIG_GAME_ENABLED    = "§aEnabled";
    public static final String CONFIG_GAME_DISABLED   = "§cDisabled";
    public static final String CONFIG_GAME_NOT_FOUND  = "§cGame '%s' not found.";
    public static final String CONFIG_GAME_SET        = "§aGame '%s' is now %s§a.";
    public static final String CONFIG_PREVENT_CONSECUTIVE_SET = "§aPrevent consecutive games is now %s§a.";
    public static final String CONFIG_USAGE_PREVENT_CONSECUTIVE = "§cUsage: /ottergames config rules preventConsecutive <true|false>";
    public static final String CONFIG_STATUS_LINE     = "§7- Prevent consecutive: %s";

    // ──────────────────────────────────────────────
    //  Structure / Arena loading
    // ──────────────────────────────────────────────
    public static final String LOGGER_STRUCTURE_NOT_FOUND  = "Structure not found in resources: %s";
    public static final String LOGGER_STRUCTURE_LOAD_ERROR = "Error loading structure: %s";
    public static final String LOGGER_STRUCTURE_LOADED     = "Loaded structure %s size=%dx%dx%d";
    public static final String LOGGER_ARENA_PLACED         = "Placed instance %d/%d @ x=%d y=%d z=%d";
    public static final String LOGGER_ARENA_SLOT_UNKNOWN   = "ArenaSlotManager: slot unknown for startX=%d, skipping free operation.";

    // ──────────────────────────────────────────────
    //  Arena listener
    // ──────────────────────────────────────────────
    public static final String ARENA_ELIMINATED_BOUNDS = "§cEliminated! You left the arena.";
    public static final String ARENA_ELIMINATED_DEATH  = "§cEliminated! You are now a spectator.";

    // ──────────────────────────────────────────────
    //  Join listener
    // ──────────────────────────────────────────────
    public static final String JOIN_DISCONNECTED_PREFIX     = "You became a spectator because ";
    public static final String JOIN_DISCONNECTED_SUFFIX     = "you disconnected during the game.";
    public static final String JOIN_GAME_IN_PROGRESS_PREFIX = "Game in progress: ";
    public static final String JOIN_GAME_IN_PROGRESS_SUFFIX = "spectator until the game ends.";
    public static final String JOIN_REGISTERED              = "You are registered for Ottergames.";

    // ──────────────────────────────────────────────
    //  Minigames - Hikabrain
    // ──────────────────────────────────────────────
    public static final String HIKABRAIN_TEAM_RED_NAME      = "Red Team";
    public static final String HIKABRAIN_TEAM_BLUE_NAME     = "Blue Team";
    public static final String HIKABRAIN_JOIN_TEAM_RED      = "§aYou are on the §cRed Team §a!";
    public static final String HIKABRAIN_JOIN_TEAM_BLUE     = "§aYou are on the §9Blue Team §a!";
    public static final String HIKABRAIN_PROTECTED_ZONE     = "§c✗ You cannot modify this zone!";
    public static final String HIKABRAIN_PROTECTED_OBSIDIAN = "§c✗ You cannot break obsidian!";
    public static final String HIKABRAIN_POINT_SCORED       = "§6%s (%s) scored a point! Score: Red §c%d §f- §9%d §6Blue";
    public static final String HIKABRAIN_GAME_END_SCORE     = "§eFinal Score: Red §c%d §f- §9%d §eBlue";
    public static final String HIKABRAIN_WINNER_RED         = "§c Red Team §a (%s) won!";
    public static final String HIKABRAIN_WINNER_BLUE        = "§9 Blue Team §a (%s) won!";
    public static final String HIKABRAIN_DRAW               = "§7 It's a draw!";

    // ──────────────────────────────────────────────
    //  Minigames - Dropper
    // ──────────────────────────────────────────────
    public static final String DROPPER_SURVIVE_MESSAGE  = "§aSurvive the fall!";
    public static final String DROPPER_GAME_END         = "§6=== Dropper Over ===";
    public static final String DROPPER_WINNERS_PREFIX   = "§e🏆 Winner(s): §a";
    public static final String DROPPER_LOSERS_PREFIX    = "§c✗ Eliminated: §7";
    public static final String DROPPER_NO_WINNER        = "§cNobody reached the bottom!";
    public static final String DROPPER_VICTORY_TITLE    = "Victory!";
    public static final String DROPPER_VICTORY_SUBTITLE = "You reached the bottom!";
    public static final String DROPPER_OTHER_REACHED    = "§a%s §6reached the bottom!";

    // ──────────────────────────────────────────────
    //  Minigames - Clutch
    // ──────────────────────────────────────────────
    public static final String CLUTCH_START_MESSAGE    = "§6[Clutch] §eSurvive the fall! §7Height: %d blocks.";
    public static final String CLUTCH_ITEM_MESSAGE     = "§aSurvival is victory! Clutch with: §b%s";
    public static final String CLUTCH_VICTORY_MESSAGE  = "§aVictory! You survived the clutch.";
    public static final String CLUTCH_VICTORY_TITLE    = "VICTORY!";
    public static final String CLUTCH_VICTORY_SUBTITLE = "You survived the fall";

    // ──────────────────────────────────────────────
    //  Minigames - Anvil
    // ──────────────────────────────────────────────
    public static final String ANVIL_START_MESSAGE    = "§6[Anvil] §eWatch out! Anvils are falling from the sky! §7Survive until the end.";
    public static final String ANVIL_VICTORY_MESSAGE  = "§aVictory! You survived the anvil rain.";
    public static final String ANVIL_VICTORY_TITLE    = "VICTORY!";
    public static final String ANVIL_VICTORY_SUBTITLE = "You dodged them all!";

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
    public static final String OTTERGAMES_ARGS_START      = "start";
    public static final String OTTERGAMES_ARGS_STOP       = "stop";
    public static final String OTTERGAMES_ARGS_LEAVE      = "leave";
    public static final String OTTERGAMES_USAGE           = "§b[OtterGames] §7Use §f/ottergames start§7, §f/ottergames stop §7or §f/ottergames leave§7.";
    public static final String OTTERGAMES_ALREADY_RUNNING = "§cThe game is already running!";
    public static final String OTTERGAMES_STARTED         = "§a§lStarting the minigame series!";
    public static final String OTTERGAMES_START_BLOCKED   = "§cUnable to start: no minigame compatible with the current conditions.";
    public static final String OTTERGAMES_NOT_RUNNING     = "§eNo game is currently running.";
    public static final String OTTERGAMES_STOPPED         = "§cGame stopped.";
    public static final String OTTERGAMES_ALREADY_LEFT    = "§eYou have already opted out of Ottergames.";
    public static final String OTTERGAMES_LEFT_SPECTATING = "§eYou have opted out of Ottergames and will spectate until the game ends.";
    public static final String OTTERGAMES_LEFT            = "§eYou have opted out of Ottergames.";
    public static final String OTTERGAMES_UNKNOWN_COMMAND = "§cUnknown command. Use §f/ottergames start§c, §f/ottergames stop §cor §f/ottergames leave§c.";
    /// Otterdev
    public static final String OTTERDEV_ARGS_TEST         = "test";
    public static final String OTTERDEV_USAGE             = "§b[OtterDev] §7Subcommands: §ftest <structure_name>";
    public static final String OTTERDEV_TEST_USAGE        = "§cUsage: /otterdev test <structure_name>";
    public static final String OTTERDEV_STRUCTURE_SPAWNED = "§a[OtterDev] §7Structure spawned: ";

    // ──────────────────────────────────────────────
    //  Structure names
    // ──────────────────────────────────────────────
    public static final String STRUCTURE_DROPPER   = "dropper/ottergames_dropper_warden";
    public static final String STRUCTURE_HIKABRAIN = "ottergames_hikabrain_map";
    public static final String STRUCTURE_SPLEEF    = "spleef/ottergames_spleef_single_floor";
    public static final String STRUCTURE_LOBBY      = "ottergames_lobby";
    public static final String STRUCTURE_CLUTCH     = "clutch";
    public static final String STRUCTURE_ANVIL_GAME = "anvil_game";
}
