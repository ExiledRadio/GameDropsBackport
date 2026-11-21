package com.exiledradio.gamedropsbackport.lookfeel;

import java.util.function.IntPredicate;

import javax.annotation.Nullable;

/**
 * The decisions of 26.3's {@code WorldOptionsScreen}, apart from its widgets, so the headless self-test checks the
 * same code the screen runs. Tooltips are lang keys.
 */
public final class WorldOptionsRules {

    public static final int PORT_MIN = 1024;
    public static final int PORT_MAX = 65535;

    private WorldOptionsRules() {
    }

    /** {@code updateButton}: Default and Personal Game Mode work only with commands allowed, never in hardcore. */
    public static boolean gameModeActive(boolean hardcore, boolean allowCommands) {
        return !hardcore && allowCommands;
    }

    public static String gameModeTooltip(boolean hardcore, boolean allowCommands, String tooltip) {
        return hardcore ? "options.worldOptions.game_mode.disabled.tooltip"
                : allowCommands ? tooltip : "options.worldOptions.game_mode.disabled.operator.tooltip";
    }

    /** {@code updateGuestCommandAccessButton}: on only with LAN and commands allowed; else off. */
    public static boolean commandAccessActive(boolean lan, boolean allowCommands) {
        return lan && allowCommands;
    }

    public static boolean commandAccessValue(boolean lan, boolean allowCommands, boolean current) {
        return lan && allowCommands && current;
    }

    public static String commandAccessTooltip(boolean lan, boolean allowCommands) {
        return !lan ? "options.worldOptions.guest.command_access.disabled.scope.tooltip"
                : !allowCommands ? "options.worldOptions.guest.command_access.disabled.commands.tooltip"
                : "options.worldOptions.guest.command_access.tooltip";
    }

    /** {@code updateForceGameModeButton}: forced in hardcore and without LAN, free with command access. */
    public static boolean forceActive(boolean hardcore, boolean lan, boolean commandAccess) {
        return lan && !commandAccess && !hardcore;
    }

    public static boolean forceValue(boolean hardcore, boolean lan, boolean commandAccess, boolean current) {
        if (hardcore || !lan) {
            return true;
        }
        return !commandAccess && current;
    }

    public static String forceTooltip(boolean hardcore, boolean lan, boolean commandAccess, boolean value) {
        return hardcore ? "options.worldOptions.guest.force_game_mode.off.hardcore.tooltip"
                : !lan ? "options.worldOptions.guest.force_game_mode.off.scope.tooltip"
                : commandAccess ? "options.worldOptions.guest.force_game_mode.off.commands.tooltip"
                : value ? "options.worldOptions.guest.force_game_mode.on.tooltip"
                : "options.worldOptions.guest.force_game_mode.off.tooltip";
    }

    /** {@code DifficultyButtons.updateDifficultyButtonsState}: the tooltip of a disabled difficulty, or null. */
    @Nullable
    public static String difficultyBlocked(boolean allowed, boolean locked, boolean hardcore) {
        return !allowed ? "options.worldOptions.difficulty.disabled.operator.tooltip"
                : locked ? "options.worldOptions.difficulty.disabled.locked.tooltip"
                : hardcore ? "options.worldOptions.difficulty.disabled.hardcore.tooltip" : null;
    }

    /**
     * {@code tryParsePort}: an empty box takes a free port; a number must be 1024 to 65535 and free (or the port the
     * world is already open on).
     *
     * @return the error's lang key, or null when {@code text} is a usable port
     */
    @Nullable
    public static String portError(String text, int initialPort, IntPredicate available) {
        String t = text.trim();
        if (t.isEmpty()) {
            return null;
        }
        int port;
        try {
            port = Integer.parseInt(t);
        } catch (NumberFormatException e) {
            return "lanServer.port.invalid";
        }
        if (port < PORT_MIN || port > PORT_MAX) {
            return "lanServer.port.invalid";
        }
        return port != initialPort && !available.test(port) ? "lanServer.port.unavailable" : null;
    }

    /** The pause and options screens change only while this is on and the newest menu look is 26.3. */
    public static boolean active() {
        return com.exiledradio.gamedropsbackport.ModConfig.lookAndFeel.enabled
                && com.exiledradio.gamedropsbackport.ModConfig.lookAndFeel.worldOptions
                && com.exiledradio.backportcore.presentation.Presentation.isAtLeast(
                        com.exiledradio.backportcore.presentation.LookArea.MENUS, "26.3");
    }
}
