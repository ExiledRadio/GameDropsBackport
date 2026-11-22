package com.exiledradio.gamedropsbackport.changes;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;

/**
 * server.properties of a dedicated server. {@code DedicatedServer} is not in the client jar, so this class is only
 * loaded behind {@code MinecraftServer.isDedicatedServer()}.
 */
public final class ServerProperties {

    private ServerProperties() {
    }

    /** 1.12.2's {@code getIntProperty}: the value, or the default, which is then added to the file. */
    public static int getInt(MinecraftServer server, String key, int defaultValue) {
        return ((DedicatedServer) server).getIntProperty(key, defaultValue);
    }

    public static void save(MinecraftServer server) {
        ((DedicatedServer) server).saveProperties();
    }
}
