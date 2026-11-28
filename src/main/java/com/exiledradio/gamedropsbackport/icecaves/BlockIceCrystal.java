package com.exiledradio.gamedropsbackport.icecaves;

import com.exiledradio.backportcore.waterlog.Waterloggable;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * 26.4's ice crystal ({@code IceCrystalBlock}, an {@code AbstractCrystalClusterBlock} of height 7 and width 10, as the
 * amethyst cluster): light 4, hardness and resistance 1.5, the chime, faces the side it was placed on and needs a
 * sturdy face behind it; without one it turns to air (no drop). Drops itself with any tool or none. Holds water
 * (Backport Core's {@link Waterloggable}). Over packed ice it chimes ({@code block.ice_crystal.ambient}) with chance
 * 1/250 an animate tick ({@code AmbientIceCrystalBlockSoundsPlayer}).
 */
public class BlockIceCrystal extends Block implements Waterloggable {

    public static final PropertyDirection FACING = PropertyDirection.create("facing");
    public static final int AMBIENT_CHANCE = 250;

    private static final AxisAlignedBB UP = box(3, 0, 3, 13, 7, 13);
    private static final AxisAlignedBB DOWN = box(3, 9, 3, 13, 16, 13);
    private static final AxisAlignedBB NORTH = box(3, 3, 9, 13, 13, 16);
    private static final AxisAlignedBB SOUTH = box(3, 3, 0, 13, 13, 7);
    private static final AxisAlignedBB EAST = box(0, 3, 3, 7, 13, 13);
    private static final AxisAlignedBB WEST = box(9, 3, 3, 16, 13, 13);

    private final SoundEvent ambient;

    public BlockIceCrystal(SoundType sound, SoundEvent ambient) {
        super(Material.PACKED_ICE, MapColor.ICE);
        this.ambient = ambient;
        setSoundType(sound);
        setHardness(1.5F);
        setResistance(1.5F * 5.0F / 3.0F);
        setLightLevel(4.0F / 15.0F);
        setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.UP));
    }

    private static AxisAlignedBB box(int x0, int y0, int z0, int x1, int y1, int z1) {
        return new AxisAlignedBB(x0 / 16.0D, y0 / 16.0D, z0 / 16.0D, x1 / 16.0D, y1 / 16.0D, z1 / 16.0D);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.byIndex(meta));
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        switch (state.getValue(FACING)) {
            case DOWN:
                return DOWN;
            case NORTH:
                return NORTH;
            case SOUTH:
                return SOUTH;
            case EAST:
                return EAST;
            case WEST:
                return WEST;
            default:
                return UP;
        }
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    @Override
    public boolean canHarvestBlock(IBlockAccess world, BlockPos pos, EntityPlayer player) {
        return true;
    }

    /** {@code canSurvive}: the block behind has a sturdy face toward this one. */
    public static boolean canStay(IBlockAccess world, BlockPos pos, EnumFacing facing) {
        BlockPos behind = pos.offset(facing.getOpposite());
        return world.getBlockState(behind).getBlockFaceShape(world, behind, facing) == BlockFaceShape.SOLID;
    }

    @Override
    public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side) {
        return super.canPlaceBlockOnSide(world, pos, side) && canStay(world, pos, side);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ,
                                            int meta, EntityLivingBase placer, EnumHand hand) {
        return getDefaultState().withProperty(FACING, facing);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
        if (!world.isRemote && !canStay(world, pos, state.getValue(FACING))) {
            // updateShape gives air: no drop.
            world.setBlockToAir(pos);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        if (rand.nextInt(AMBIENT_CHANCE) == 0 && world.getBlockState(pos.down()).getBlock() == Blocks.PACKED_ICE) {
            world.playSound(pos.getX(), pos.getY(), pos.getZ(), this.ambient, SoundCategory.AMBIENT, 1.0F, 1.0F, false);
        }
    }
}
