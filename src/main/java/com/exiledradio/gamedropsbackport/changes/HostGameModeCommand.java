package com.exiledradio.gamedropsbackport.changes;

import com.exiledradio.gamedropsbackport.ModConfig;

import net.minecraft.command.CommandException;
import net.minecraft.command.CommandGameMode;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameType;

/**
 * 1.12.2's {@code /gamemode}, and 26.2's {@code GameModeCommand}: when the player whose mode changes is the host of the
 * world ({@code isSingleplayerOwner}), the world's default game mode becomes it too ({@code setDefaultGameType}), so
 * F3 + N (which runs {@code /gamemode}) does the same. 26.3 dropped this when World Options split the host's own game
 * mode from the default, so the setting ({@code hostGameModeDefault}) is off by default.
 */
public final class HostGameModeCommand extends CommandGameMode {

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        super.execute(server, sender, args);
        if (!Changes.on(ModConfig.changes.hostGameModeDefault)) {
            return;
        }
        GameType mode = getGameModeFromCommand(sender, args[0]);
        EntityPlayer target = args.length >= 2 ? getPlayer(server, sender, args[1]) : getCommandSenderAsPlayer(sender);
        if (isHost(server, target)) {
            // 26.2's MinecraftServer.setDefaultGameType: the world's own game mode (1.12.2's /defaultgamemode call).
            server.setGameType(mode);
        }
    }

    /** 26.x's {@code IntegratedServer.isSingleplayerOwner}: the owner's name, ignoring case. */
    public static boolean isHost(MinecraftServer server, EntityPlayer player) {
        return server.isSinglePlayer() && server.getServerOwner() != null
                && server.getServerOwner().equalsIgnoreCase(player.getName());
    }
}
