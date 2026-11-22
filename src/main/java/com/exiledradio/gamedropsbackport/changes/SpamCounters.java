package com.exiledradio.gamedropsbackport.changes;

import com.exiledradio.gamedropsbackport.ModConfig;

import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.CPacketChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;

import com.mojang.authlib.GameProfile;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 26.2's separate spam counters (MC-302268): {@code chatSpamThrottler} and {@code commandSpamThrottler}, each a
 * {@code TickThrottler(20, 20 * seconds)}: a message adds 20, every tick takes 1 away, and the player is kicked
 * ("Kicked for spamming") when a message leaves the counter at the threshold or more. The thresholds are the
 * dedicated server's {@code chat-spam-threshold-seconds} and {@code command-spam-threshold-seconds} (10 each, written to
 * server.properties as 26.2 lists them; 0 never kicks); an integrated server (Open to LAN) has 0 for both, as 26.2's
 * {@code IntegratedServer}. Operators and the host are never kicked. 1.12.2's single counter (chat and commands together,
 * kicked over 200) stays in the class but never kicks while this is on: patch P7 hands its limit here.
 */
public final class SpamCounters {

    /** 26.2's defaults (seconds). */
    static final int DEFAULT_SECONDS = 10;

    static int chatThreshold = 20 * DEFAULT_SECONDS;
    static int commandThreshold = 20 * DEFAULT_SECONDS;

    /** Per connection: {chat count, command count, server tick of the last update, last packet was a command (1)}. */
    private static final Map<NetHandlerPlayServer, int[]> COUNTS = new WeakHashMap<NetHandlerPlayServer, int[]>();

    private SpamCounters() {
    }

    /** At server start: the two thresholds, as ticks. */
    static void serverStarting(MinecraftServer server) {
        if (server.isDedicatedServer()) {
            chatThreshold = 20 * ServerProperties.getInt(server, "chat-spam-threshold-seconds", DEFAULT_SECONDS);
            commandThreshold = 20 * ServerProperties.getInt(server, "command-spam-threshold-seconds", DEFAULT_SECONDS);
            if (Changes.on(ModConfig.changes.spamThresholds)) {
                // getIntProperty adds a missing key; write them both now, as 26.2's server.properties has them.
                ServerProperties.save(server);
            }
        } else {
            chatThreshold = 0;
            commandThreshold = 0;
        }
        synchronized (COUNTS) {
            COUNTS.clear();
        }
    }

    private static int[] counts(NetHandlerPlayServer handler, int now) {
        int[] c = COUNTS.get(handler);
        if (c == null) {
            c = new int[]{0, 0, now, 0};
            COUNTS.put(handler, c);
        }
        int elapsed = now - c[2];
        if (elapsed > 0) {
            // TickThrottler.tick: one off per tick, down to 0.
            c[0] = Math.max(0, c[0] - elapsed);
            c[1] = Math.max(0, c[1] - elapsed);
        }
        c[2] = now;
        return c;
    }

    /**
     * P7, at the head of {@code processChatMessage}: on the server thread, remembers whether this message is a command
     * (1.12.2 sends both through the one packet). Off the server thread (the first call, which queues the packet) it
     * does nothing. Never stops the method.
     */
    public static boolean chatPacket(NetHandlerPlayServer handler, CPacketChatMessage packet) {
        MinecraftServer server = handler.player.server;
        if (server == null || !server.isCallingFromMinecraftThread() || !Changes.on(ModConfig.changes.spamThresholds)) {
            return false;
        }
        String message = org.apache.commons.lang3.StringUtils.normalizeSpace(packet.getMessage());
        synchronized (COUNTS) {
            counts(handler, server.getTickCounter())[3] = message.startsWith("/") ? 1 : 0;
        }
        return false;
    }

    /**
     * P7, in place of {@code processChatMessage}'s 200: 1.12.2's own limit while off. While on, the message just handled
     * counts on its own counter, the player is kicked as 26.2 kicks, and the limit returned is one 1.12.2's counter
     * never passes.
     */
    public static int spamLimit(NetHandlerPlayServer handler) {
        if (!Changes.on(ModConfig.changes.spamThresholds)) {
            return 200;
        }
        MinecraftServer server = handler.player.server;
        boolean command;
        synchronized (COUNTS) {
            command = counts(handler, server.getTickCounter())[3] == 1;
        }
        if (count(handler, command, server.getTickCounter())) {
            handler.disconnect(new TextComponentTranslation("disconnect.spam"));
        }
        return Integer.MAX_VALUE;
    }

    /**
     * 26.2's {@code detectRateSpam}: one message on its counter at server tick {@code now}.
     *
     * @return whether the player is to be kicked
     */
    public static boolean count(NetHandlerPlayServer handler, boolean command, int now) {
        int count;
        synchronized (COUNTS) {
            int[] c = counts(handler, now);
            int i = command ? 1 : 0;
            c[i] += 20;
            count = c[i];
        }
        int threshold = command ? commandThreshold : chatThreshold;
        return threshold > 0 && count >= threshold && !exempt(handler.player.server, handler.player.getGameProfile());
    }

    /** The two thresholds, in ticks: {chat, command}. */
    public static int[] thresholds() {
        return new int[]{chatThreshold, commandThreshold};
    }

    /** 26.2's {@code isOp(...) || isSingleplayerOwner(...)}. */
    static boolean exempt(MinecraftServer server, GameProfile profile) {
        if (server.getPlayerList().getOppedPlayers().getEntry(profile) != null) {
            return true;
        }
        return server.isSinglePlayer() && server.getServerOwner() != null
                && server.getServerOwner().equalsIgnoreCase(profile.getName());
    }

    /** For the self-test: {chat, command} counts of a connection now. */
    public static int[] countsNow(NetHandlerPlayServer handler, int now) {
        synchronized (COUNTS) {
            int[] c = counts(handler, now);
            return new int[]{c[0], c[1]};
        }
    }

}
