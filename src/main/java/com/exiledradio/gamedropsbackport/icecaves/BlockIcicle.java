package com.exiledradio.gamedropsbackport.icecaves;

import com.exiledradio.backportcore.speleothem.BlockSpeleothem;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 26.4's icicle ({@code IcicleBlock}): a speleothem of packed ice, on Backport Core's {@link BlockSpeleothem}.
 *
 * <h2>Numbers (26.4 Pre-Release 1)</h2>
 * <ul>
 *   <li>Grows on packed ice, down only, never merging tips, to the length {@code MAX_GROWING_LENGTH = 4} lets
 *       {@code findTip} reach (5 segments), with the shared chance 0.011377778 a random tick.</li>
 *   <li>{@code wouldMelt}: water evaporates here (the Nether), block light (not sky light) above 4, or magma one or two
 *       blocks below ({@code #melts_icicle_above}). A melting icicle neither grows nor stays: a random tick on any
 *       segment but the one at its root drops it as a falling stalactite from there down, or, for a stalagmite or a
 *       tip over something, breaks it with drops.</li>
 *   <li>Hardness 0.5, resistance 0.5, light blue on maps, the chime, no sideways offset, any tool; its landing sound
 *       is {@code block.icicle.land} (level event 1055: volume 2, pitch 0.9-1.0).</li>
 *   <li>A free-hanging tip shows falling snow dust (snow's) with chance 0.02 an animate tick.</li>
 * </ul>
 * {@code attached} picks the base models. 26.4 sets it when a segment is placed or generated against something that
 * is not an icicle and never changes it; 1.12.2 has no metadata left for it, so it is read from the neighbor
 * ({@link #getActualState}), which gives the same answer except for a column whose support was replaced later.
 */
public class BlockIcicle extends BlockSpeleothem {

    public static final PropertyBool ATTACHED = PropertyBool.create("attached");
    public static final int MAX_GROWING_LENGTH = 4;
    public static final int BREAKS_IF_ABOVE_LIGHT_LEVEL = 4;
    public static final float DRIP_CHANCE = 0.02F;

    private final SoundEvent land;

    public BlockIcicle(SoundType sound, SoundEvent land) {
        super(Material.PACKED_ICE, MapColor.ICE, Blocks.PACKED_ICE.getDefaultState());
        this.land = land;
        setSoundType(sound);
        setHardness(0.5F);
        setResistance(0.5F * 5.0F / 3.0F);
        setDefaultState(getDefaultState().withProperty(ATTACHED, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, THICKNESS, VERTICAL_DIRECTION, ATTACHED);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.withProperty(ATTACHED, isStalactiteStart(state, world, pos) || isStalagmiteStart(state, world, pos));
    }

    @Override
    public EnumOffsetType getOffsetType() {
        return EnumOffsetType.NONE;
    }

    @Override
    protected SoundEvent landingSound() {
        return this.land;
    }

    @Override
    protected int maxGrowthLength() {
        return MAX_GROWING_LENGTH;
    }

    @Override
    protected List<EnumFacing> naturalGrowthDirections() {
        return Collections.singletonList(EnumFacing.DOWN);
    }

    @Override
    protected boolean shouldMergeTips() {
        return false;
    }

    @Override
    protected boolean canGrow(World world, BlockPos pos) {
        return super.canGrow(world, pos) && !wouldMelt(world, pos) && isStalactite(world.getBlockState(pos));
    }

    /** {@code wouldMelt}. */
    public static boolean wouldMelt(World world, BlockPos pos) {
        return world.provider.doesWaterVaporize() || world.getLightFor(EnumSkyBlock.BLOCK, pos) > BREAKS_IF_ABOVE_LIGHT_LEVEL
                || meltsAbove(world.getBlockState(pos.down())) || meltsAbove(world.getBlockState(pos.down(2)));
    }

    /** {@code #melts_icicle_above}: magma. */
    static boolean meltsAbove(IBlockState state) {
        return state.getBlock() == Blocks.MAGMA;
    }

    @Override
    protected void onRandomTick(World world, BlockPos pos, IBlockState state, Random random) {
        boolean start = isStalactiteStart(state, world, pos) || isStalagmiteStart(state, world, pos);
        if (!start && wouldMelt(world, pos)) {
            if (!isStalagmite(state) && (!isTip(state, true) || world.isAirBlock(pos.down()))) {
                spawnFallingStalactite(state, world, pos);
            } else {
                world.destroyBlock(pos, true);
            }
        } else {
            super.onRandomTick(world, pos, state, random);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        if (isFreeHangingStalactite(world, pos, state) && !(rand.nextFloat() > DRIP_CHANCE)) {
            // The shared drip position: the tip's centre, 5/16 up, a pixel lower.
            world.spawnParticle(EnumParticleTypes.FALLING_DUST, pos.getX() + 0.5D, pos.getY() + 5.0D / 16.0D - 1.0D / 16.0D,
                    pos.getZ() + 0.5D, 0.0D, 0.0D, 0.0D, Block.getStateId(Blocks.SNOW.getDefaultState()));
        }
    }
}
