package com.exiledradio.gamedropsbackport.sulfur;

import com.exiledradio.backportcore.gameevent.GameEvent;
import com.exiledradio.backportcore.gameevent.GameEventContext;
import com.exiledradio.backportcore.gameevent.GameEvents;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * Potent sulfur's block entity: 26.3's {@code PotentSulfurBlockEntity} and its five tickers, picked by the block's state.
 *
 * <ul>
 *   <li><b>Gas</b> (wet, dormant): every 10 ticks, living things whose eyes are within 3 blocks of the pool's surface,
 *       with water below the eyes and a clear line to the pool, get Nausea for 4 s. Clients puff gas every 20 ticks.</li>
 *   <li><b>Countdown</b> (dormant, erupting): counted in seconds; dormant waits 15-30 s plus 10 s per water block
 *       past the first, an eruption lasts 1-2 s plus 1 s per water block past the first. Each geyser's numbers are
 *       fixed by the world seed and its position, as 26.3's positional random.</li>
 *   <li><b>Launch</b> (erupting, continuous): every tick, anything in the plume (6 blocks per water block, up to the
 *       first solid block) that rises slower than {@code 0.3 + 0.1 * water} gets 0.2 more upward speed. Runs on clients
 *       too, as 26.3's: a player's own movement is the client's.</li>
 * </ul>
 */
public class TilePotentSulfur extends TileEntity implements ITickable {

    private static final long GEYSER_SALT = -904011478L;

    public int waitingCountdown = -1;
    public long eruptionTick = -1L;

    public void resetCountdown() {
        this.waitingCountdown = -1;
    }

    /** A modded block entity is replaced on every state change unless it says otherwise; this one lives with its block. */
    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);
        if (this.eruptionTick == -1L) {
            this.eruptionTick = world.getTotalWorldTime();
        }
    }

    @Override
    public void update() {
        World w = this.world;
        IBlockState state = w.getBlockState(this.pos);
        if (!(state.getBlock() instanceof BlockPotentSulfur)) {
            return;
        }
        PotentState s = state.getValue(BlockPotentSulfur.STATE);
        boolean client = w.isRemote;
        switch (s) {
            case WET:
                if (client) {
                    clientGas();
                } else {
                    nausea();
                }
                break;
            case DORMANT:
                if (client) {
                    clientGas();
                } else {
                    countdown(state, s);
                    nausea();
                }
                break;
            case ERUPTING:
                if (client) {
                    clientPlume(Reg.sound("block.potent_sulfur.geyser_eruption_active"));
                    launch();
                } else {
                    launch();
                    countdown(state, s);
                }
                break;
            case CONTINUOUS:
                if (client) {
                    clientPlume(Reg.sound("block.potent_sulfur.geyser_continuous_eruption_active"));
                }
                launch();
                break;
            default:
                break;
        }
    }

    // ------------------------------------------------------------------------------- server

    private void nausea() {
        if (this.world.getTotalWorldTime() % 10L != 0L) {
            return;
        }
        BlockPos source = findGasSource(this.world, this.pos);
        if (source == null) {
            return;
        }
        AxisAlignedBB box = new AxisAlignedBB(source).grow(2.5, 0.0, 2.5);
        for (EntityLivingBase e : this.world.getEntitiesWithinAABB(EntityLivingBase.class, box, EntitySelectors.NOT_SPECTATING)) {
            if (e.isEntityAlive() && canBeReachedByGas(this.world, source, new Vec3d(e.posX, e.posY + e.getEyeHeight(), e.posZ))) {
                e.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 80, 0, true, true));
            }
        }
    }

    /** A fresh random from the seed and the position, as 26.3's {@code geyserPositional}. */
    static Random positional(World world, BlockPos pos) {
        return new Random(world.getSeed() ^ GEYSER_SALT ^ MathHelper.getCoordinateRandom(pos.getX(), pos.getY(), pos.getZ()));
    }

    private void countdown(IBlockState state, PotentState s) {
        if (this.world.getTotalWorldTime() % 20L != 0L) {
            return;
        }
        BlockPos source = findGasSource(this.world, this.pos);
        if (source == null) {
            return;
        }
        if (this.waitingCountdown <= 0) {
            int water = source.getY() - this.pos.getY() - 1;
            Random r = positional(this.world, this.pos);
            if (s == PotentState.DORMANT) {
                this.waitingCountdown = 10 * (water - 1) + 15 + r.nextInt(16);
            } else {
                r.nextInt();
                this.waitingCountdown = water - 1 + 1 + r.nextInt(2);
            }
            markDirty();
        }
        if (this.waitingCountdown > 0) {
            this.waitingCountdown--;
        }
        if (this.waitingCountdown == 0) {
            PotentState next = s == PotentState.DORMANT ? PotentState.ERUPTING : PotentState.DORMANT;
            IBlockState now = state.withProperty(BlockPotentSulfur.STATE, next);
            this.world.setBlockState(this.pos, now, 3);
            if (next == PotentState.DORMANT) {
                GameEvents.emit(this.world, GameEvent.BLOCK_DEACTIVATE, this.pos, GameEventContext.of(state));
            } else {
                ((BlockPotentSulfur) state.getBlock()).started(this.world, this.pos, now);
            }
        }
    }

    /** 26.2's {@code #not_affected_by_geysers}: the ender dragon (and its parts). */
    public static boolean notAffectedByGeysers(Entity e) {
        return e instanceof net.minecraft.entity.boss.EntityDragon
                || e instanceof net.minecraft.entity.MultiPartEntityPart
                && ((net.minecraft.entity.MultiPartEntityPart) e).parent instanceof net.minecraft.entity.boss.EntityDragon;
    }

    private void launch() {
        BlockPos source = findGasSource(this.world, this.pos);
        if (source == null) {
            return;
        }
        int water = source.getY() - this.pos.getY() - 1;
        int height = unobstructed(this.world, this.pos.up(), water);
        AxisAlignedBB box = new AxisAlignedBB(this.pos.up()).expand(0.0, height - 1, 0.0);
        for (Entity e : this.world.getEntitiesWithinAABB(Entity.class, box, EntitySelectors.NOT_SPECTATING)) {
            if (!e.isEntityAlive()) {
                continue;
            }
            // Only where the movement is simulated: the client's own player there, everything else on the server.
            boolean player = e instanceof EntityPlayer;
            if (this.world.isRemote ? !player || !((EntityPlayer) e).isUser() : player) {
                continue;
            }
            if (e instanceof EntityPlayer && ((EntityPlayer) e).capabilities.isFlying || notAffectedByGeysers(e)) {
                continue;
            }
            if (!e.isRiding() && e.motionY < 0.3F + water * 0.1) {
                e.motionY += 0.2F;
                e.fallDistance = 0.0F;
                e.velocityChanged = !this.world.isRemote && !(e instanceof EntityPlayer);
            }
        }
    }

    // ------------------------------------------------------------------------------- client

    private void clientGas() {
        if (this.world.getTotalWorldTime() % 20L != 0L) {
            return;
        }
        BlockPos source = findGasSource(this.world, this.pos);
        if (source != null) {
            com.exiledradio.gamedropsbackport.sulfur.client.SulfurParticles.gasCloud(this.world, source);
        }
    }

    private void clientPlume(net.minecraft.util.SoundEvent sound) {
        BlockPos source = findGasSource(this.world, this.pos);
        if (source == null) {
            return;
        }
        long t = this.world.getTotalWorldTime() - this.eruptionTick;
        if (t % 20L == 0L) {
            com.exiledradio.gamedropsbackport.sulfur.client.SulfurParticles.geyser(this.world, source.getX() + 0.5, source.getY(),
                    source.getZ() + 0.5, source.getY() - this.pos.getY() - 1);
        }
        if (t % 40L == 0L) {
            this.world.playSound(source.getX() + 0.5, source.getY() + 0.5, source.getZ() + 0.5, sound, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
        }
    }

    // ------------------------------------------------------------------------------- the pool

    /** 26.3's {@code isGeyserPassableBlock}: air, water, or anything without a collision box. */
    static boolean passable(IBlockAccess world, BlockPos pos, IBlockState state) {
        if (state.getBlock().isAir(state, world, pos) || state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.FLOWING_WATER) {
            return true;
        }
        return state.getCollisionBoundingBox(world, pos) == null;
    }

    /** @return the first open block above up to four water blocks over the sulfur (the pool's surface), or null */
    @Nullable
    public static BlockPos findGasSource(IBlockAccess world, BlockPos origin) {
        int maxY = origin.getY() + BlockPotentSulfur.WATER_BLOCKS + 1;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(origin.up());
        while (m.getY() <= maxY) {
            IBlockState state = world.getBlockState(m);
            boolean water = BlockPotentSulfur.isWaterSource(world, m);
            boolean isWaterBlock = state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.FLOWING_WATER;
            if (!water || !isWaterBlock && !passable(world, m, state)) {
                if (passable(world, m, state)) {
                    return m.toImmutable();
                }
                break;
            }
            m.setY(m.getY() + 1);
        }
        return null;
    }

    private static int unobstructed(World world, BlockPos from, int water) {
        int max = 6 * water;
        for (int i = 0; i < max; i++) {
            BlockPos p = from.up(i);
            if (!passable(world, p, world.getBlockState(p))) {
                return i;
            }
        }
        return max;
    }

    /** 26.3's {@code canBeReachedByNoxiousGas}. */
    public static boolean canBeReachedByGas(World world, BlockPos source, Vec3d eye) {
        BlockPos at = new BlockPos(eye);
        if (!passable(world, at, world.getBlockState(at))) {
            return false;
        }
        Vec3d center = new Vec3d(source.getX() + 0.5, source.getY() + 0.5, source.getZ() + 0.5);
        if (eye.squareDistanceTo(center) > 9.0) {
            return false;
        }
        Vec3d belowSource = center.add(0, -1, 0);
        Vec3d belowEye = eye.add(0, -1, 0);
        if (!BlockPotentSulfur.isWaterSource(world, new BlockPos(belowEye))) {
            return false;
        }
        RayTraceResult hit = world.rayTraceBlocks(belowSource, belowEye, false, true, false);
        return hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("countdown", this.waitingCountdown);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasKey("countdown")) {
            this.waitingCountdown = tag.getInteger("countdown");
        }
    }

    /** For the self-test: the entities a launch would push. */
    List<Entity> plumeEntities() {
        BlockPos source = findGasSource(this.world, this.pos);
        if (source == null) {
            return java.util.Collections.emptyList();
        }
        int water = source.getY() - this.pos.getY() - 1;
        int height = unobstructed(this.world, this.pos.up(), water);
        return this.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(this.pos.up()).expand(0.0, height - 1, 0.0));
    }
}
