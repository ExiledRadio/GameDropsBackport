package com.exiledradio.gamedropsbackport.changes;

import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.ModConfig;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketTimeUpdate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 26.1's world clocks ({@code ServerClockManager}) over 1.12.2's world time.
 *
 * <ul>
 *   <li>{@code minecraft:overworld}: its total ticks are 1.12.2's overworld world time, so everything that reads the
 *       time of day (sky, sleep, mobs, other mods) follows it. Rate and pause are kept here: after each overworld tick
 *       the one tick 1.12.2 added is replaced by the clock's own advance ({@code partialTick + rate}).</li>
 *   <li>{@code minecraft:the_end}: 26.1's second clock, which nothing in 1.12.2 reads; counted here.</li>
 * </ul>
 * Both advance only while {@code doDaylightCycle} is true (26.x's {@code advance_time}). While a clock is paused or
 * not at rate 1, players get the time every tick with the client's own advance off, so the sky matches.
 */
@Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID)
public final class WorldClocks extends WorldSavedData {

    public static final String OVERWORLD = "minecraft:overworld";
    public static final String THE_END = "minecraft:the_end";
    static final String DATA_NAME = GameDropsBackport.MOD_ID + "_world_clocks";

    /** One clock's state (26.1's {@code ClockState}: total ticks, partial tick, rate, paused). */
    public static final class Clock {
        public long totalTicks;
        public float partialTick;
        public float rate = 1.0F;
        public boolean paused;

        boolean normal() {
            return !this.paused && this.rate == 1.0F;
        }

        /** {@code ServerClockInstance.tick}: the whole ticks this tick advances. */
        int advance() {
            if (this.paused) {
                return 0;
            }
            this.partialTick += this.rate;
            int full = (int) Math.floor(this.partialTick);
            this.partialTick -= full;
            return full;
        }
    }

    /** 26.1's time markers of {@code minecraft:day}: id, ticks, shown in commands. Period 24000. */
    static final Object[][] DAY_MARKERS = {
            {"minecraft:day", 1000, true}, {"minecraft:noon", 6000, true}, {"minecraft:night", 13000, true},
            {"minecraft:midnight", 18000, true}, {"minecraft:wake_up_from_sleep", 0, false},
            {"minecraft:roll_village_siege", 18000, false},
    };
    static final int DAY_PERIOD = 24000;

    /** 26.3's timelines, all on the overworld clock: id to period ticks (0 = none). */
    static final Map<String, Integer> TIMELINES = new LinkedHashMap<String, Integer>();

    static {
        TIMELINES.put("minecraft:day", 24000);
        TIMELINES.put("minecraft:early_game", 0);
        TIMELINES.put("minecraft:moon", 192000);
        TIMELINES.put("minecraft:villager_schedule", 24000);
    }

    final Clock overworld = new Clock();
    final Clock end = new Clock();

    public WorldClocks(String name) {
        super(name);
    }

    public WorldClocks() {
        this(DATA_NAME);
    }

    /** @return this save's clocks (kept with the overworld's data) */
    public static WorldClocks get(MinecraftServer server) {
        MapStorage storage = server.getWorld(0).getMapStorage();
        WorldClocks clocks = (WorldClocks) storage.getOrLoadData(WorldClocks.class, DATA_NAME);
        if (clocks == null) {
            clocks = new WorldClocks();
            storage.setData(DATA_NAME, clocks);
        }
        return clocks;
    }

    /** @return the clock by id, or null; {@code minecraft:} may be left out */
    @Nullable
    public Clock clock(String id) {
        String full = id.indexOf(':') < 0 ? "minecraft:" + id : id;
        if (OVERWORLD.equals(full)) {
            return this.overworld;
        }
        return THE_END.equals(full) ? this.end : null;
    }

    /** The overworld clock's total ticks are 1.12.2's world time. */
    public long totalTicks(MinecraftServer server, String clock) {
        return OVERWORLD.equals(clock) ? server.getWorld(0).getWorldTime() : this.end.totalTicks;
    }

    /** {@code setTotalTicks}: 1.12.2's {@code setAllWorldTimes} for the overworld clock (every world, as /time did). */
    public void setTotalTicks(MinecraftServer server, String clock, long ticks) {
        Clock c = clock(clock);
        c.partialTick = 0.0F;
        if (OVERWORLD.equals(clock)) {
            for (WorldServer w : server.worlds) {
                w.setWorldTime(ticks);
            }
        } else {
            c.totalTicks = ticks;
        }
        markDirty();
        sync(server);
    }

    // ------------------------------------------------------------------------------------------
    // Ticking
    // ------------------------------------------------------------------------------------------

    private static long before;
    private static boolean wasNormal = true;

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        World world = event.world;
        if (world.isRemote || world.provider.getDimension() != 0 || !Changes.on(ModConfig.changes.timeCommand)) {
            return;
        }
        MinecraftServer server = world.getMinecraftServer();
        if (server == null) {
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            before = world.getWorldTime();
            return;
        }
        if (!world.getGameRules().getBoolean("doDaylightCycle")) {
            return;
        }
        WorldClocks clocks = get(server);
        // The End's clock.
        clocks.end.totalTicks += clocks.end.advance();
        if (clocks.overworld.normal()) {
            clocks.overworld.partialTick = 0.0F;
        } else {
            // 1.12.2 added one tick (after any sleep skip); the clock's own advance takes its place.
            int full = clocks.overworld.advance();
            if (world.getWorldTime() != before) {
                world.setWorldTime(Math.max(world.getWorldTime() - 1 + full, 0L));
            }
        }
        clocks.markDirty();
        boolean normal = clocks.overworld.normal();
        if (!normal || !wasNormal) {
            sync(server);
        }
        wasNormal = normal;
    }

    /** Sends every player their world's time now; the client advances it itself only while the clock runs normally. */
    static void sync(MinecraftServer server) {
        WorldClocks clocks = get(server);
        boolean daylight = server.getWorld(0).getGameRules().getBoolean("doDaylightCycle");
        boolean advance = daylight && (!Changes.on(ModConfig.changes.timeCommand) || clocks.overworld.normal());
        for (EntityPlayerMP p : server.getPlayerList().getPlayers()) {
            p.connection.sendPacket(new SPacketTimeUpdate(p.world.getTotalWorldTime(), p.world.getWorldTime(), advance));
        }
    }

    static MinecraftServer server() {
        return FMLCommonHandler.instance().getMinecraftServerInstance();
    }

    // ------------------------------------------------------------------------------------------
    // Saving
    // ------------------------------------------------------------------------------------------

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        read(this.overworld, nbt.getCompoundTag("overworld"));
        read(this.end, nbt.getCompoundTag("the_end"));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setTag("overworld", write(this.overworld));
        nbt.setTag("the_end", write(this.end));
        return nbt;
    }

    private static void read(Clock c, NBTTagCompound tag) {
        c.totalTicks = tag.getLong("total_ticks");
        c.partialTick = tag.getFloat("partial_tick");
        c.rate = tag.hasKey("rate") ? tag.getFloat("rate") : 1.0F;
        c.paused = tag.getBoolean("paused");
    }

    private static NBTTagCompound write(Clock c) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("total_ticks", c.totalTicks);
        tag.setFloat("partial_tick", c.partialTick);
        tag.setFloat("rate", c.rate);
        tag.setBoolean("paused", c.paused);
        return tag;
    }
}
