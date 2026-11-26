package com.exiledradio.gamedropsbackport.icecaves;

import com.exiledradio.backportcore.gameevent.GameEvent;
import com.exiledradio.backportcore.gameevent.GameEventContext;
import com.exiledradio.backportcore.gameevent.GameEvents;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBreakable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityStray;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * 26.4's brittle ice ({@code BrittleIceBlock}, a {@code CrackingIceBlock}).
 *
 * <h2>Numbers (26.4 Pre-Release 1)</h2>
 * <ul>
 *   <li>Ages 0-3. A living thing standing on it ({@code stepOn}) cracks it one age, then waits {@value #TICK_DELAY}
 *       ticks (a scheduled tick that does nothing): four steps' worth to break. At age 3 the next crack breaks it,
 *       with no drop, and cracks each brittle ice beside it once, scheduling theirs.</li>
 *   <li>Frostbites and strays never crack it ({@code #does_not_break_brittle_ice}); other mobs only with
 *       {@code mobGriefing}; players unless the spot is spawn-protected.</li>
 *   <li>Each crack plays {@code block.brittle_ice.crack} and sends {@code block_change}; the first shows break
 *       particles (level event 2014).</li>
 *   <li>Hardness 0.5, resistance 0.5, friction 0.98, the chime, never melts (no random ticks), drops only with Silk
 *       Touch, leaves air when broken, pistons break it. Its faces against ice, frosted
 *       ice and brittle ice are hidden ({@code #face_occluding_ice_blocks}).</li>
 * </ul>
 */
public class BlockBrittleIce extends BlockBreakable {

    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, 3);
    public static final int MAX_AGE = 3;
    public static final int TICK_DELAY = 24;

    private static final ResourceLocation FROSTBITE = new ResourceLocation("gamedropsbackport", "frostbite");

    private final SoundEvent crack;

    public BlockBrittleIce(SoundType sound, SoundEvent crack) {
        super(Material.ICE, false, MapColor.ICE);
        this.crack = crack;
        setSoundType(sound);
        setHardness(0.5F);
        setResistance(0.5F * 5.0F / 3.0F);
        setLightOpacity(3);
        this.slipperiness = 0.98F;
        setDefaultState(this.blockState.getBaseState().withProperty(AGE, 0));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AGE);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(AGE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(AGE, Math.min(MAX_AGE, meta & 3));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.TRANSLUCENT;
    }

    /** {@code IceBlock.skipRendering} against {@code #face_occluding_ice_blocks}. */
    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        Block other = world.getBlockState(pos.offset(side)).getBlock();
        if (other == Blocks.ICE || other == Blocks.FROSTED_ICE || other instanceof BlockBrittleIce) {
            return false;
        }
        return super.shouldSideBeRendered(state, world, pos, side);
    }

    @Override
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(this);
    }

    @Override
    protected boolean canSilkHarvest() {
        return true;
    }

    @Override
    public int damageDropped(IBlockState state) {
        return 0;
    }

    /** {@code stepOn}, from {@link IceCavesEvents} each tick a living thing stands on this block. */
    public void stepOn(World world, BlockPos pos, IBlockState state, Entity entity) {
        if (world.isRemote || !shouldCrack(entity, pos, world) || world.isUpdateScheduled(pos, this)) {
            return;
        }
        if (slightlyMelt(state, world, pos)) {
            propagateMelting(world, pos);
        }
        world.scheduleUpdate(pos, this, TICK_DELAY);
    }

    /** {@code shouldEntityIncreaseCracking}. */
    static boolean shouldCrack(Entity entity, BlockPos pos, World world) {
        if (!(entity instanceof EntityLivingBase) || doesNotBreak(entity)) {
            return false;
        }
        if (entity instanceof EntityPlayer) {
            return world.getMinecraftServer() == null || !world.getMinecraftServer().isBlockProtected(world, pos, (EntityPlayer) entity);
        }
        return world.getGameRules().getBoolean("mobGriefing");
    }

    /** {@code #does_not_break_brittle_ice}: frostbites and strays. */
    static boolean doesNotBreak(Entity entity) {
        return entity instanceof EntityStray || FROSTBITE.equals(EntityList.getKey(entity));
    }

    /** {@code CrackingIceBlock.slightlyMelt}: @return whether it broke */
    boolean slightlyMelt(IBlockState state, World world, BlockPos pos) {
        int age = state.getValue(AGE);
        if (age < MAX_AGE) {
            world.setBlockState(pos, state.withProperty(AGE, age + 1), 2);
            world.playSound(null, pos, this.crack, SoundCategory.BLOCKS, 1.0F, 1.0F);
            if (age == 0 && world instanceof WorldServer) {
                ((WorldServer) world).spawnParticle(EnumParticleTypes.BLOCK_CRACK, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                        64, 0.25D, 0.25D, 0.25D, 0.0D, Block.getStateId(state));
            }
            GameEvents.emit(world, GameEvent.BLOCK_CHANGE, pos, GameEventContext.of(state));
            return false;
        }
        // melt(): destroyBlock(pos, false).
        world.destroyBlock(pos, false);
        return true;
    }

    /** {@code CrackingIceBlock.propagateMelting}: cracks each brittle ice beside it once and schedules its wait. */
    void propagateMelting(World world, BlockPos pos) {
        for (EnumFacing direction : EnumFacing.values()) {
            BlockPos n = pos.offset(direction);
            IBlockState neighbor = world.getBlockState(n);
            if (neighbor.getBlock() == this && !slightlyMelt(neighbor, world, n)) {
                world.scheduleUpdate(n, this, TICK_DELAY);
            }
        }
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        // The cooldown only: 26.4's BrittleIceBlock has no tick of its own.
    }
}
