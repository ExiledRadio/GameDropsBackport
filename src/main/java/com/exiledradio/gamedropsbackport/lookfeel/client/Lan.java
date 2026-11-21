package com.exiledradio.gamedropsbackport.lookfeel.client;

import com.exiledradio.gamedropsbackport.GameDropsBackport;

import net.minecraft.client.multiplayer.ThreadLanServerPing;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 26.3's {@code publishServer(LAN, port)} on 1.12.2: {@code IntegratedServer.shareToLAN} with the port chosen on World
 * Options instead of the first free one. Game mode and command access are World Options' own (Force Game Mode,
 * Command Access), set by the caller. On the server thread.
 */
@SideOnly(Side.CLIENT)
final class Lan {

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
}
