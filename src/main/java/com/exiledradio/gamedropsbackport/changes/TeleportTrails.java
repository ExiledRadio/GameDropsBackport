package com.exiledradio.gamedropsbackport.changes;

import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.ModConfig;
import com.exiledradio.gamedropsbackport.network.NetworkHandler;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityShulker;
import net.minecraft.init.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 26.3's teleport trails ({@code teleportTrails}): 128 portal particles strewn between where an enderman, a shulker or
 * a chorus fruit eater was and where it went (level events 2018, 2016 and 2017). 1.12.2's {@code attemptTeleport}
 * spawns its trail on the server, where no one sees it. A teleport is caught by its event (endermen and shulkers; the
 * chorus fruit by its use finishing), known to have worked when the mob stands at the target, and sent to the players
 * within 64 blocks of where it began, as a level event is.
 */
@Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID)
public final class TeleportTrails {

    /** 26.3's two trail shapes: across the enderman's box, or around single blocks. */
    public static final byte ENDERMAN = 0;
    public static final byte BLOCK = 1;

    private static final List<Pending> PENDING = new ArrayList<Pending>();
    private static final Map<EntityLivingBase, double[]> EATING = new WeakHashMap<EntityLivingBase, double[]>();
    /** For the self-test: trails sent. */
    public static int sent;

    private TeleportTrails() {
    }

    private static final class Pending {
        final Entity entity;
        final double fromX, fromY, fromZ, toX, toY, toZ;

        Pending(Entity entity, double toX, double toY, double toZ) {
            this(entity, entity.posX, entity.posY, entity.posZ, toX, toY, toZ);
        }

        Pending(Entity entity, double fromX, double fromY, double fromZ, double toX, double toY, double toZ) {
            this.entity = entity;
            this.fromX = fromX;
            this.fromY = fromY;
            this.fromZ = fromZ;
            this.toX = toX;
            this.toY = toY;
            this.toZ = toZ;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEnderTeleport(EnderTeleportEvent event) {
        Entity e = event.getEntity();
        if (e.world.isRemote || !Changes.on(ModConfig.changes.teleportTrails) || e.isSilent()
                || !(e instanceof EntityEnderman || e instanceof EntityShulker)) {
            return;
        }
        synchronized (PENDING) {
            PENDING.add(new Pending(e, event.getTargetX(), event.getTargetY(), event.getTargetZ()));
        }
    }

    /** Where the eater stood while eating; the teleport comes as the use finishes. */
    @SubscribeEvent
    public static void onUseTick(LivingEntityUseItemEvent.Tick event) {
        if (!event.getEntityLiving().world.isRemote && event.getItem().getItem() == Items.CHORUS_FRUIT) {
            EntityLivingBase e = event.getEntityLiving();
            EATING.put(e, new double[]{e.posX, e.posY, e.posZ});
        }
    }

    @SubscribeEvent
    public static void onUseFinish(LivingEntityUseItemEvent.Finish event) {
        EntityLivingBase e = event.getEntityLiving();
        double[] from = EATING.remove(e);
        if (e.world.isRemote || from == null || event.getItem().getItem() != Items.CHORUS_FRUIT
                || !Changes.on(ModConfig.changes.teleportTrails)) {
            return;
        }
        double dx = e.posX - from[0];
        double dy = e.posY - from[1];
        double dz = e.posZ - from[2];
        if (dx * dx + dy * dy + dz * dz > 1.0D) {
            send(e.world, BLOCK, new BlockPos(from[0], from[1], from[2]), new BlockPos(e));
        }
    }

    /** At the end of each world tick: the teleports of this tick that worked. */
    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) {
            return;
        }
        List<Pending> done = new ArrayList<Pending>();
        synchronized (PENDING) {
            for (int i = PENDING.size() - 1; i >= 0; i--) {
                if (PENDING.get(i).entity.world == event.world) {
                    done.add(PENDING.remove(i));
                }
            }
        }
        for (Pending p : done) {
            if (landed(p)) {
                send(event.world, p.entity instanceof EntityShulker ? BLOCK : ENDERMAN,
                        new BlockPos(p.fromX, p.fromY, p.fromZ), new BlockPos(p.entity));
            }
        }
    }

    /** It stands at the target: 1.12.2 keeps x and z and only lowers y (a shulker is centered on its block). */
    static boolean landed(Pending p) {
        Entity e = p.entity;
        if (!e.isEntityAlive()) {
            return false;
        }
        if (e instanceof EntityShulker) {
            BlockPos at = new BlockPos(e);
            return at.getX() == (int) Math.floor(p.toX) && at.getZ() == (int) Math.floor(p.toZ)
                    && !at.equals(new BlockPos(p.fromX, p.fromY, p.fromZ));
        }
        return Math.abs(e.posX - p.toX) < 1.0E-4D && Math.abs(e.posZ - p.toZ) < 1.0E-4D && e.posY <= p.toY + 1.0E-4D
                && (e.posX != p.fromX || e.posZ != p.fromZ);
    }

    /** For the self-test: what {@link #onWorldTick} decides for one teleport. */
    public static boolean landedFor(Entity entity, double fromX, double fromY, double fromZ, double toX, double toY, double toZ) {
        return landed(new Pending(entity, fromX, fromY, fromZ, toX, toY, toZ));
    }

    public static void send(World world, byte kind, BlockPos from, BlockPos to) {
        if (NetworkHandler.channel == null) {
            return;
        }
        sent++;
        NetworkHandler.channel.sendToAllAround(new Message(kind, from, to),
                new NetworkRegistry.TargetPoint(world.provider.getDimension(), from.getX() + 0.5D, from.getY() + 0.5D, from.getZ() + 0.5D, 64.0D));
    }

    // --- the packet ------------------------------------------------------------------------------

    public static final class Message implements IMessage {
        byte kind;
        BlockPos from;
        BlockPos to;

        public Message() {
        }

        Message(byte kind, BlockPos from, BlockPos to) {
            this.kind = kind;
            this.from = from;
            this.to = to;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            this.kind = buf.readByte();
            this.from = BlockPos.fromLong(buf.readLong());
            this.to = BlockPos.fromLong(buf.readLong());
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeByte(this.kind);
            buf.writeLong(this.from.toLong());
            buf.writeLong(this.to.toLong());
        }
    }

    public static final class Handler implements IMessageHandler<Message, IMessage> {
        @Override
        public IMessage onMessage(final Message message, MessageContext ctx) {
            com.exiledradio.gamedropsbackport.changes.client.TeleportTrailsClient.show(message.kind, message.from, message.to);
            return null;
        }
    }
}
