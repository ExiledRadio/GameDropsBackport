package com.exiledradio.gamedropsbackport.sulfurcaves;

import com.exiledradio.backportcore.worldgen.DeferredPlacement;
import com.exiledradio.gamedropsbackport.GameDropsBackport;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.Random;

/**
 * Sulfur cave work past 1.12's population window, through Core's {@link DeferredPlacement}: run once every chunk it
 * touches is generated and populated (so it reads what 26.3's 3 by 3 feature region would show it), saved with the
 * world until then.
 * <ul>
 *   <li>{@code sulfur_spike}: a spike try whose spot is within 3 columns of the window's edge or past it (26.3 throws
 *       them up to 10 out from a spot in the chunk). The whole placement after the count and offset: the biome check,
 *       up or down (its own random, seeded from the world seed and the spot), the scan, the speleothem with its base
 *       patch (3 columns at most each way).</li>
 *   <li>{@code sulfur_spring_tuff}: one of a large spring's tuff tries past the window: scan down at most 4 to a solid
 *       block, tuff there.</li>
 * </ul>
 */
final class Deferred {

    static final ResourceLocation SPIKE = new ResourceLocation(GameDropsBackport.MOD_ID, "sulfur_spike");
    static final ResourceLocation TUFF = new ResourceLocation(GameDropsBackport.MOD_ID, "sulfur_spring_tuff");

    /** How far a spike's work reaches sideways from its spot: the base patch, one step and two random ones. */
    static final int SPIKE_REACH = 3;
    /** And up and down: the scan (12) and a two-block spike with its root. */
    static final int SPIKE_HEIGHT = 15;

    private Deferred() {
    }

    static void register() {
        DeferredPlacement.register(SPIKE, Deferred::placeSpike);
        DeferredPlacement.register(TUFF, Deferred::placeTuff);
    }

    static void spike(World world, BlockPos p, long seed) {
        NBTTagCompound data = pos(p);
        data.setLong("Seed", seed);
        DeferredPlacement.queue(world, SPIKE, p.add(-SPIKE_REACH, -SPIKE_HEIGHT, -SPIKE_REACH),
                p.add(SPIKE_REACH, SPIKE_HEIGHT, SPIKE_REACH), data);
    }

    static void tuff(World world, BlockPos p) {
        DeferredPlacement.queue(world, TUFF, p.down(4), p, pos(p));
    }

    private static NBTTagCompound pos(BlockPos p) {
        NBTTagCompound data = new NBTTagCompound();
        data.setInteger("X", p.getX());
        data.setInteger("Y", p.getY());
        data.setInteger("Z", p.getZ());
        return data;
    }

    private static BlockPos pos(NBTTagCompound data) {
        return new BlockPos(data.getInteger("X"), data.getInteger("Y"), data.getInteger("Z"));
    }

    private static boolean live(World world) {
        return SulfurCaves.generates(world) && SulfurCavesContent.blocksPresent();
    }

    static void placeSpike(WorldServer world, BlockPos min, BlockPos max, NBTTagCompound data) {
        if (!live(world)) {
            return;
        }
        Features f = new Features(world, min.getX(), min.getZ(), max.getX() + 1, max.getZ() + 1, new Random(data.getLong("Seed")));
        BlockPos p = pos(data);
        if (!f.inSulfurCaves(p)) {
            return;
        }
        f.single(p, f.rand.nextInt(2) == 0);
    }

    static void placeTuff(WorldServer world, BlockPos min, BlockPos max, NBTTagCompound data) {
        if (!live(world)) {
            return;
        }
        Features f = new Features(world, min.getX(), min.getZ(), max.getX() + 1, max.getZ() + 1, new Random(0L));
        BlockPos p = f.scan(pos(data), EnumFacing.DOWN, 4, Features.Scan.ANY, Features.Scan.SOLID);
        if (p != null && f.solid(p)) {
            f.set(p, SulfurCavesContent.tuff());
        }
    }
}
