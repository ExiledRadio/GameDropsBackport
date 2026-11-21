package com.exiledradio.gamedropsbackport.poplar;

import net.minecraft.block.BlockBush;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.terraingen.TerrainGen;

import java.util.Random;

/**
 * The poplar sapling: 26.3's {@code SaplingBlock} with {@code TreeGrower.POPLAR}: a red, orange or yellow poplar,
 * one in three each, with no mega tree and no flower trees. It grows like any sapling (a random tick with light 9
 * or more, one time in seven; bone meal 45%), first to stage 1, then into a tree. Bone meal works only where the
 * shortest poplar (base height 7) stays inside the world.
 */
public class BlockPoplarSapling extends BlockBush implements IGrowable {

    public static final PropertyInteger STAGE = PropertyInteger.create("stage", 0, 1);
    private static final AxisAlignedBB AABB = new AxisAlignedBB(0.1D, 0.0D, 0.1D, 0.9D, 0.8D, 0.9D);

    public BlockPoplarSapling() {
        setDefaultState(this.blockState.getBaseState().withProperty(STAGE, 0));
        setSoundType(SoundType.PLANT);
        setHardness(0.0F);
        setTickRandomly(true);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, STAGE);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(STAGE, meta & 1);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(STAGE);
    }

    @Override
    @SuppressWarnings("deprecation")
    public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
        return MapColor.IRON;
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return AABB;
    }

    @Override
    public int damageDropped(IBlockState state) {
        return 0;
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (world.isRemote) {
            return;
        }
        super.updateTick(world, pos, state, rand);
        if (!world.isAreaLoaded(pos, 1)) {
            return;
        }
        if (world.getLightFromNeighbors(pos.up()) >= 9 && rand.nextInt(7) == 0) {
            grow(world, rand, pos, state);
        }
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
        return pos.getY() + PoplarFeatures.BASE_HEIGHT < world.getHeight();
    }

    @Override
    public boolean canUseBonemeal(World world, Random rand, BlockPos pos, IBlockState state) {
        return world.rand.nextFloat() < 0.45D;
    }

    @Override
    public void grow(World world, Random rand, BlockPos pos, IBlockState state) {
        if (state.getValue(STAGE) == 0) {
            world.setBlockState(pos, state.cycleProperty(STAGE), 4);
            return;
        }
        if (!TerrainGen.saplingGrowTree(world, rand, pos)) {
            return;
        }
        // TreeGrower.growTree: one of the three trees (weights 1, 1, 1), the sapling removed first and put back if
        // the tree does not fit.
        BlockPoplarLeaves.Color color = BlockPoplarLeaves.Color.values()[rand.nextInt(3)];
        world.setBlockState(pos, Blocks.AIR.getDefaultState(), 4);
        if (!PoplarFeatures.tree(world, rand, pos, color, false)) {
            world.setBlockState(pos, state, 4);
        }
    }
}
