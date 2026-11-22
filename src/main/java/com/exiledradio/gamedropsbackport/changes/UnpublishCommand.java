package com.exiledradio.gamedropsbackport.changes;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

import java.util.function.Predicate;

/**
 * 26.2's {@code /unpublish} (permission level 4): a world opened to LAN is closed again: the LAN listener stops, the
 * world is no longer announced on the network and every player but the host leaves ("Server closed"). 26.3's messages;
 * a dedicated server, or a world not open to LAN, answers "No multiplayer game published".
 */
public final class UnpublishCommand extends CommandBase {

    /** Set by the client: closes the integrated server's LAN (on the server thread); false when it was not open. */
    public static volatile Predicate<MinecraftServer> unpublisher;

    @Override
    public String getName() {
        return "unpublish";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 4;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.gamedropsbackport.unpublish.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        Predicate<MinecraftServer> closer = unpublisher;
        if (server.isDedicatedServer() || closer == null || !closer.test(server)) {
            throw new CommandException("commands.unpublish.notPublished");
        }
        notifyCommandListener(sender, this, "commands.unpublish.success");
    }
}
