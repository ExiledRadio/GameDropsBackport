package com.exiledradio.gamedropsbackport.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * The mod's one network channel.
 *
 * <h2>Packet ids</h2>
 * Each stream owns a block of five ids (docs/PLAN.md) and registers its packets here, inside its own block, so
 * two streams never pick the same number:
 *
 * <pre>
 *   5-9   babies        25-29 poplar
 *   10-14 sulfur        30-34 softBlocks
 *   15-19 sulfurCube    35-39 camps
 *   20-24 sulfurCaves   40-44 changes
 * </pre>
 */
public final class NetworkHandler {

    /** Forge caps channel names at 20 characters. */
    public static final String CHANNEL = "gdbackport";

    public static SimpleNetworkWrapper channel;

    private NetworkHandler() {
    }

    public static void init() {
        channel = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL);
        // 5-9 babies
        channel.registerMessage(com.exiledradio.gamedropsbackport.babies.AgeLockMessage.Handler.class,
                com.exiledradio.gamedropsbackport.babies.AgeLockMessage.class, 5, Side.CLIENT);
        // 20-24 sulfurCaves
        channel.registerMessage(com.exiledradio.gamedropsbackport.sulfurcaves.SulfurNet.Handler.class,
                com.exiledradio.gamedropsbackport.sulfurcaves.SulfurNet.InsideMessage.class, 20, Side.CLIENT);
        // 40-44 changes
        channel.registerMessage(com.exiledradio.gamedropsbackport.changes.TeleportTrails.Handler.class,
                com.exiledradio.gamedropsbackport.changes.TeleportTrails.Message.class, 40, Side.CLIENT);
    }
}
