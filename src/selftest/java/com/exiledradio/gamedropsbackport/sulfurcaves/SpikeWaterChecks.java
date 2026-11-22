package com.exiledradio.gamedropsbackport.sulfurcaves;

import com.exiledradio.backportcore.waterlog.Waterlogging;
import com.exiledradio.gamedropsbackport.selftest.Check;
import com.exiledradio.gamedropsbackport.sulfur.BlockSulfurSpike;
import com.exiledradio.gamedropsbackport.sulfur.Thickness;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

/**
 * For the waterlog self-test: the sulfur caves' spike placement ({@link Features#placeSpike}, package-private), as 26.3's
 * {@code SpeleothemUtils.buildBaseToTipColumn}: {@code waterlogged = level.isWaterAt(pos)}, any water, source or flowing.
 */
public final class SpikeWaterChecks {

    private SpikeWaterChecks() {
    }

    /** @param corner a fresh area's corner (air above a stone floor); {@code wet}: Fluidlogged API is installed */
    public static void run(Check c, World world, BlockPos corner, boolean wet) {
        Features f = new Features(world, corner.getX(), corner.getZ(), new Random(3L));
        BlockSulfurSpike spike = (BlockSulfurSpike) f.spike;
        IBlockState tip = spike.state(EnumFacing.UP, Thickness.TIP);
        BlockPos source = corner.add(4, 0, 4);
        BlockPos flowing = corner.add(8, 0, 4);
        BlockPos air = corner.add(12, 0, 4);
        world.setBlockState(source, Blocks.WATER.getDefaultState(), 2 | 16);
        world.setBlockState(flowing, Blocks.FLOWING_WATER.getDefaultState().withProperty(BlockLiquid.LEVEL, 4), 2 | 16);
        f.placeSpike(source, tip);
        f.placeSpike(flowing, tip);
        f.placeSpike(air, tip);
        c.ok(world.getBlockState(source).getBlock() == spike && Waterlogging.hasWaterSource(world, source) == wet,
                wet ? "a generated spike in a water source is waterlogged" : "a generated spike in water replaces it");
        c.ok(world.getBlockState(flowing).getBlock() == spike && Waterlogging.hasWaterSource(world, flowing) == wet,
                wet ? "a generated spike in flowing water is waterlogged with a source (26.3's isWaterAt)" : "a generated spike in flowing water replaces it");
        c.ok(world.getBlockState(air).getBlock() == spike && !Waterlogging.hasWater(world, air), "a generated spike in air is dry");
    }
}
