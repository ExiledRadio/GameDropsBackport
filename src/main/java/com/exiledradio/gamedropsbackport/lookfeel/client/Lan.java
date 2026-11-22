package com.exiledradio.gamedropsbackport.lookfeel.client;

import com.exiledradio.gamedropsbackport.GameDropsBackport;

import io.netty.channel.ChannelFuture;
import io.netty.channel.local.LocalServerChannel;
import net.minecraft.client.multiplayer.ThreadLanServerPing;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetworkSystem;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 26.3's {@code publishServer(LAN, port)} and {@code unpublishServer()} on 1.12.2. Publishing is
 * {@code IntegratedServer.shareToLAN} with the port chosen on World Options instead of the first free one; game mode and
 * command access are World Options' own (Force Game Mode, Command Access), set by the caller. Unpublishing (1.12.2 has
 * none) closes the TCP listener (the integrated server's local channel stays), stops the LAN announcer, sends every
 * player but the host off with "Server closed", and clears the published state. On the server thread.
 */
@SideOnly(Side.CLIENT)
public final class Lan {

    private Lan() {
    }

    static boolean publish(IntegratedServer server, int port) {
        try {
            server.getNetworkSystem().addEndpoint(null, port);
            ReflectionHelper.setPrivateValue(IntegratedServer.class, server, true, "isPublic", "field_71346_p");
            ThreadLanServerPing ping = new ThreadLanServerPing(server.getMOTD(), String.valueOf(port));
            ReflectionHelper.setPrivateValue(IntegratedServer.class, server, ping, "lanServerPing", "field_71345_q");
            ping.start();
            WorldOptionsScreen.publishedPort = port;
            GameDropsBackport.LOGGER.info("Started on {}", port);
            return true;
        } catch (Exception e) {
            GameDropsBackport.LOGGER.warn("World Options: could not open to LAN on port {}", port, e);
            return false;
        }
    }

    /** 26.3's {@code IntegratedServer.unpublishServer}. @return false when the world was not open to LAN */
    public static boolean unpublish(IntegratedServer server) {
        if (!server.getPublic()) {
            return false;
        }
        GameDropsBackport.LOGGER.info("Unpublishing integrated server (was on port {})", port(server));
        // stopTcpServerListener: every endpoint but the local one the host plays through.
        NetworkSystem network = server.getNetworkSystem();
        List<ChannelFuture> endpoints = ReflectionHelper.getPrivateValue(NetworkSystem.class, network, "endpoints", "field_151274_e");
        synchronized (endpoints) {
            for (Iterator<ChannelFuture> it = endpoints.iterator(); it.hasNext(); ) {
                ChannelFuture future = it.next();
                if (!(future.channel() instanceof LocalServerChannel)) {
                    try {
                        future.channel().close().syncUninterruptibly();
                    } catch (RuntimeException e) {
                        GameDropsBackport.LOGGER.warn("World Options: closing the LAN listener", e);
                    }
                    it.remove();
                }
            }
        }
        PlayerList players = server.getPlayerList();
        String host = server.getServerOwner();
        for (EntityPlayerMP player : new ArrayList<EntityPlayerMP>(players.getPlayers())) {
            if (host == null || !host.equalsIgnoreCase(player.getName())) {
                player.connection.disconnect(new TextComponentTranslation("multiplayer.disconnect.server_shutdown"));
            }
        }
        // teardownPublishedState: the announcer, the port, the scope.
        ThreadLanServerPing ping = ReflectionHelper.getPrivateValue(IntegratedServer.class, server, "lanServerPing", "field_71345_q");
        if (ping != null) {
            ping.interrupt();
        }
        ReflectionHelper.setPrivateValue(IntegratedServer.class, server, null, "lanServerPing", "field_71345_q");
        ReflectionHelper.setPrivateValue(IntegratedServer.class, server, false, "isPublic", "field_71346_p");
        WorldOptionsScreen.publishedPort = -1;
        // 1.12.2's guests' settings (commands for everyone, a forced mode) only meant something while open.
        players.setCommandsAllowedForAll(false);
        players.setGameType(null);
        for (EntityPlayerMP player : players.getPlayers()) {
            players.updatePermissionLevel(player);
        }
        return true;
    }

    /** For the self-test: how many TCP listeners the server has open. */
    public static int tcpListeners(IntegratedServer server) {
        List<ChannelFuture> endpoints = ReflectionHelper.getPrivateValue(NetworkSystem.class, server.getNetworkSystem(),
                "endpoints", "field_151274_e");
        int n = 0;
        synchronized (endpoints) {
            for (ChannelFuture future : endpoints) {
                if (!(future.channel() instanceof LocalServerChannel) && future.channel().isOpen()) {
                    n++;
                }
            }
        }
        return n;
    }

    /** For the self-test: whether the LAN announcer is running. */
    public static boolean announcing(IntegratedServer server) {
        ThreadLanServerPing ping = ReflectionHelper.getPrivateValue(IntegratedServer.class, server, "lanServerPing", "field_71345_q");
        return ping != null && ping.isAlive() && !ping.isInterrupted();
    }

    /**
     * The port the world is open on: the one World Options opened it on, else the LAN announcer's (1.12.2's
     * {@code /publish} picks one and keeps it only there). -1 when not open.
     */
    static int port(IntegratedServer server) {
        if (!server.getPublic()) {
            return -1;
        }
        if (WorldOptionsScreen.publishedPort > 0) {
            return WorldOptionsScreen.publishedPort;
        }
        ThreadLanServerPing ping = ReflectionHelper.getPrivateValue(IntegratedServer.class, server, "lanServerPing", "field_71345_q");
        if (ping != null) {
            for (Field f : ThreadLanServerPing.class.getDeclaredFields()) {
                if (f.getType() == String.class && !java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    try {
                        f.setAccessible(true);
                        Object value = f.get(ping);
                        if (value instanceof String && ((String) value).matches("\\d{1,5}")) {
                            return Integer.parseInt((String) value);
                        }
                    } catch (ReflectiveOperationException | RuntimeException ignored) {
                        // not this field
                    }
                }
            }
        }
        return -1;
    }
}
