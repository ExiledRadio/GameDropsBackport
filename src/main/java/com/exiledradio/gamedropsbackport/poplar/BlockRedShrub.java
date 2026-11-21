package com.exiledradio.gamedropsbackport.poplar;

import net.minecraft.block.BlockBush;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The red shrub: 26.3's {@code BushBlock}. A plant 13 pixels high on dirt, grass, podzol, farmland or any of the
 * dirt family; replaceable, no collision, breaks at once. Bone meal spreads it to one empty side (north, east,
 * south or west, in a random order) where it can stand, every time, as long as such a side exists.
 */
public class BlockRedShrub extends BlockBush implements IGrowable {

    private static final AxisAlignedBB AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 13.0D / 16.0D, 1.0D);
    private static final EnumFacing[] HORIZONTAL = {EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST};

    public BlockRedShrub() {
        setSoundType(PoplarContent.RED_SHRUB);
        setHardness(0.0F);
    }

    /** 26.3: {@code MapColor.CRIMSON_NYLIUM}, which 1.12.2 lacks; its nearest is red. */
    @Override
    @SuppressWarnings("deprecation")
    public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
        return MapColor.RED;
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return AABB;
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
        for (EnumFacing side : HORIZONTAL) {
            if (spreadable(world, pos.offset(side))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canUseBonemeal(World world, Random rand, BlockPos pos, IBlockState state) {
        return true;
    }

    /** {@code BonemealableBlock.findSpreadableNeighbourPos}: the four sides shuffled, the first that is free. */
    @Override
    public void grow(World world, Random rand, BlockPos pos, IBlockState state) {
        List<EnumFacing> sides = new ArrayList<EnumFacing>();
        Collections.addAll(sides, HORIZONTAL);
        Collections.shuffle(sides, rand);
        for (EnumFacing side : sides) {
            BlockPos at = pos.offset(side);
            if (spreadable(world, at)) {
                world.setBlockState(at, getDefaultState());
                return;
            }
        }
    }

    private boolean spreadable(World world, BlockPos at) {
        return world.isAirBlock(at) && canBlockStay(world, at, getDefaultState());
    }
}
