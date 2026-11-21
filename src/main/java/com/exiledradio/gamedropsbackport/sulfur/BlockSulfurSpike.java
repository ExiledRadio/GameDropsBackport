package com.exiledradio.gamedropsbackport.sulfur;

import com.exiledradio.backportcore.waterlog.Waterloggable;
import com.exiledradio.backportcore.waterlog.Waterlogging;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * The sulfur spike (26.2): 26.3's {@code SulfurSpikeBlock} on its {@code SpeleothemBlock}, pointed dripstone's
 * rules without the dripping.
 *
 * <h2>States</h2>
 * {@code thickness} (tip_merge, tip, frustum, middle, base) and {@code vertical_direction} (up, down), metadata
 * {@code thickness | (down ? 8 : 0)}; 26.3's {@code waterlogged} is Backport Core's kept water. Every segment works
 * out its thickness from the segments before and after it, and two tips that meet merge unless the placer sneaked.
 *
 * <h2>Behavior</h2>
 * <ul>
 *   <li>A segment needs a solid face or a segment of its own direction behind it. A stalagmite that loses it
 *       breaks next tick; a stalactite falls two ticks later with every segment below it, as falling blocks. The
 *       tip hurts what it lands on: its length (at least 6) per block fallen, 40 at most, as a falling stalactite.
 *       A fallen segment breaks into its item with the spike's landing sound.</li>
 *   <li>Random ticks grow a stalactite hanging from a sulfur block, or the stalagmite under it, by one
 *       (0.011377778 per tick), up to two long.</li>
 *   <li>A thrown trident faster than 0.6 breaks it ({@link SulfurEvents}).</li>
 *   <li>Any tool gets it; it is pushed off by pistons and offset sideways as pointed dripstone.</li>
 * </ul>
 * <p>It extends {@link BlockFalling} only so 1.12.2's falling block tells it where it landed; it never falls as
 * sand does.
 */
public class BlockSulfurSpike extends BlockFalling implements Waterloggable {

    public static final PropertyEnum<Thickness> THICKNESS = PropertyEnum.create("thickness", Thickness.class);
    public static final PropertyDirection VERTICAL_DIRECTION = PropertyDirection.create("vertical_direction", EnumFacing.Plane.VERTICAL);

    private static final AxisAlignedBB TIP_MERGE = box(5, 0, 11, 16);
    private static final AxisAlignedBB TIP_UP = box(5, 0, 11, 11);
    private static final AxisAlignedBB TIP_DOWN = box(5, 5, 11, 16);
    private static final AxisAlignedBB FRUSTUM = box(4, 0, 12, 16);
    private static final AxisAlignedBB MIDDLE = box(3, 0, 13, 16);
    private static final AxisAlignedBB BASE = box(2, 0, 14, 16);

    static final float GROWTH_CHANCE = 0.011377778F;
    static final int MAX_GROWTH_LENGTH = 2;
    private static final int MAX_STALAGMITE_SEARCH = 10;
    static final int MAX_DAMAGE = 40;

    /** The block a stalactite grows from (our sulfur). */
    private final Block growsOn;

    public BlockSulfurSpike(SoundType sound, Block growsOn) {
        super(Material.ROCK);
        this.growsOn = growsOn;
        setSoundType(sound);
        setHardness(1.5F);
        setResistance(5.0F);
        setTickRandomly(true);
        setDefaultState(this.blockState.getBaseState().withProperty(THICKNESS, Thickness.TIP).withProperty(VERTICAL_DIRECTION, EnumFacing.UP));
    }

    private static AxisAlignedBB box(int min, int y0, int max, int y1) {
        return new AxisAlignedBB(min / 16.0, y0 / 16.0, min / 16.0, max / 16.0, y1 / 16.0, max / 16.0);
    }

    // ------------------------------------------------------------------------------- states and shape

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, THICKNESS, VERTICAL_DIRECTION);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(THICKNESS).ordinal() | (state.getValue(VERTICAL_DIRECTION) == EnumFacing.DOWN ? 8 : 0);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        Thickness[] all = Thickness.values();
        return state((meta & 8) != 0 ? EnumFacing.DOWN : EnumFacing.UP, all[Math.min(meta & 7, all.length - 1)]);
    }

    public IBlockState state(EnumFacing direction, Thickness thickness) {
        return getDefaultState().withProperty(VERTICAL_DIRECTION, direction).withProperty(THICKNESS, thickness);
    }

    @Override
    @SuppressWarnings("deprecation")
    public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
        return MapColor.YELLOW;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public EnumOffsetType getOffsetType() {
        return EnumOffsetType.XZ;
    }

    /** 26.3's offset: up to an eighth of a block each way (the base's inset), the same for a whole column. */
    @Override
    @SuppressWarnings("deprecation")
    public Vec3d getOffset(IBlockState state, IBlockAccess world, BlockPos pos) {
        long i = MathHelper.getCoordinateRandom(pos.getX(), 0, pos.getZ());
        double x = MathHelper.clamp(((i & 15L) / 15.0F - 0.5) * 0.5, -0.125, 0.125);
        double z = MathHelper.clamp(((i >> 8 & 15L) / 15.0F - 0.5) * 0.5, -0.125, 0.125);
        return new Vec3d(x, 0, z);
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        AxisAlignedBB shape;
        switch (state.getValue(THICKNESS)) {
            case TIP_MERGE:
                shape = TIP_MERGE;
                break;
            case TIP:
                shape = state.getValue(VERTICAL_DIRECTION) == EnumFacing.DOWN ? TIP_DOWN : TIP_UP;
                break;
            case FRUSTUM:
                shape = FRUSTUM;
                break;
            case MIDDLE:
                shape = MIDDLE;
                break;
            default:
                shape = BASE;
        }
        Vec3d off = getOffset(state, world, pos);
        return shape.offset(off.x, 0, off.z);
    }

    @Override
    @SuppressWarnings("deprecation")
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    /** No tool needed (26.3 has no {@code requiresCorrectToolForDrops} here); a pickaxe is only faster. */
    @Override
    public boolean canHarvestBlock(IBlockAccess world, BlockPos pos, EntityPlayer player) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        // No falling-dust particles: it never falls as sand does.
    }

    // ------------------------------------------------------------------------------- queries

    public static boolean isSpike(IBlockState state) {
        return state.getBlock() instanceof BlockSulfurSpike;
    }

    static boolean withDirection(IBlockState state, EnumFacing direction) {
        return isSpike(state) && state.getValue(VERTICAL_DIRECTION) == direction;
    }

    static boolean isStalactite(IBlockState state) {
        return withDirection(state, EnumFacing.DOWN);
    }

    static boolean isTip(IBlockState state, boolean merged) {
        if (!isSpike(state)) {
            return false;
        }
        Thickness t = state.getValue(THICKNESS);
        return t == Thickness.TIP || merged && t == Thickness.TIP_MERGE;
    }

    static boolean isUnmergedTip(IBlockState state, EnumFacing direction) {
        return isTip(state, false) && state.getValue(VERTICAL_DIRECTION) == direction;
    }

    /** @return whether a segment pointing {@code direction} at {@code pos} has something to hold on to */
    public static boolean isValidPlacement(IBlockAccess world, BlockPos pos, EnumFacing direction) {
        BlockPos behind = pos.offset(direction.getOpposite());
        IBlockState s = world.getBlockState(behind);
        return s.getBlockFaceShape(world, behind, direction) == BlockFaceShape.SOLID || withDirection(s, direction);
    }

    public boolean canSurvive(IBlockAccess world, BlockPos pos, IBlockState state) {
        return isValidPlacement(world, pos, state.getValue(VERTICAL_DIRECTION));
    }

    static Thickness calculateThickness(IBlockAccess world, BlockPos pos, EnumFacing direction, boolean mergeTips) {
        EnumFacing opposite = direction.getOpposite();
        IBlockState ahead = world.getBlockState(pos.offset(direction));
        if (withDirection(ahead, opposite)) {
            return !mergeTips && ahead.getValue(THICKNESS) != Thickness.TIP_MERGE ? Thickness.TIP : Thickness.TIP_MERGE;
        }
        if (!withDirection(ahead, direction)) {
            return Thickness.TIP;
        }
        Thickness aheadThickness = ahead.getValue(THICKNESS);
        if (aheadThickness == Thickness.TIP || aheadThickness == Thickness.TIP_MERGE) {
            return Thickness.FRUSTUM;
        }
        return withDirection(world.getBlockState(pos.offset(opposite)), direction) ? Thickness.MIDDLE : Thickness.BASE;
    }

    // ------------------------------------------------------------------------------- placing

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ,
                                            int meta, EntityLivingBase placer, EnumHand hand) {
        EnumFacing preferred = placer.rotationPitch > 0 ? EnumFacing.UP : EnumFacing.DOWN;
        return placementState(world, pos, preferred, !placer.isSneaking());
    }

    /** Pointing the preferred way if it can hold on that way, else the other way (if neither, it does not survive). */
    public IBlockState placementState(IBlockAccess world, BlockPos pos, EnumFacing preferred, boolean mergeTips) {
        EnumFacing direction = isValidPlacement(world, pos, preferred) || !isValidPlacement(world, pos, preferred.getOpposite())
                ? preferred : preferred.getOpposite();
        return state(direction, calculateThickness(world, pos, direction, mergeTips));
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        // BlockFalling's own fall check is not ours.
    }

    // ------------------------------------------------------------------------------- support and thickness

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
        if (world.isRemote || from.getX() != pos.getX() || from.getZ() != pos.getZ()) {
            return;
        }
        EnumFacing direction = state.getValue(VERTICAL_DIRECTION);
        if (direction == EnumFacing.DOWN && world.isUpdateScheduled(pos, this)) {
            return;
        }
        if (from.equals(pos.offset(direction.getOpposite())) && !canSurvive(world, pos, state)) {
            world.scheduleUpdate(pos, this, direction == EnumFacing.DOWN ? 2 : 1);
            return;
        }
        Thickness now = calculateThickness(world, pos, direction, state.getValue(THICKNESS) == Thickness.TIP_MERGE);
        if (now != state.getValue(THICKNESS)) {
            world.setBlockState(pos, state.withProperty(THICKNESS, now), 3);
        }
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (world.isRemote) {
            return;
        }
        if (state.getValue(VERTICAL_DIRECTION) == EnumFacing.UP) {
            if (!canSurvive(world, pos, state)) {
                world.destroyBlock(pos, true);
            }
        } else {
            spawnFallingStalactite(state, world, pos);
        }
    }

    /** Turns a stalactite, from {@code pos} down to its tip, into falling blocks; only the tip hurts. */
    static void spawnFallingStalactite(IBlockState state, World world, BlockPos pos) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(pos);
        IBlockState s = state;
        while (isStalactite(s)) {
            EntityFallingBlock falling = new EntityFallingBlock(world, m.getX() + 0.5, m.getY(), m.getZ() + 0.5, s);
            boolean tip = isTip(s, true);
            NBTTagCompound tag = new NBTTagCompound();
            falling.writeToNBT(tag);
            // The block goes now, as 26.3's FallingBlockEntity.fall; past its first tick the entity no longer looks for it.
            tag.setInteger("Time", 1);
            tag.setBoolean("DropItem", true);
            if (tip) {
                tag.setBoolean("HurtEntities", true);
                tag.setFloat("FallHurtAmount", Math.max(1 + pos.getY() - m.getY(), 6));
                tag.setInteger("FallHurtMax", MAX_DAMAGE);
            }
            falling.readFromNBT(tag);
            BlockPos at = m.toImmutable();
            // A waterlogged segment leaves its water behind.
            world.setBlockState(at, Waterlogging.hasWater(world, at) ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState(), 3);
            world.spawnEntity(falling);
            if (tip) {
                break;
            }
            m.move(EnumFacing.DOWN);
            s = world.getBlockState(m);
        }
    }

    /** Where 1.12.2's falling block set it down: a stalactite cannot hang there, so it breaks with its landing sound. */
    @Override
    public void onEndFalling(World world, BlockPos pos, IBlockState falling, IBlockState hit) {
        IBlockState now = world.getBlockState(pos);
        if (!isSpike(now) || canSurvive(world, pos, now)) {
            return;
        }
        world.setBlockToAir(pos);
        if (world.getGameRules().getBoolean("doEntityDrops")) {
            spawnAsEntity(world, pos, new ItemStack(this));
        }
        world.playSound(null, pos, Reg.sound("block.sulfur_spike.land"), SoundCategory.BLOCKS, 2.0F, world.rand.nextFloat() * 0.1F + 0.9F);
    }

    // ------------------------------------------------------------------------------- growth

    @Override
    public void randomTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (!world.isRemote && random.nextFloat() < GROWTH_CHANCE && isStalactite(state) && !isSpike(world.getBlockState(pos.up()))) {
            growIfPossible(state, world, pos, random);
        }
    }

    /** Grows the stalactite whose topmost segment is at {@code pos}, or the stalagmite under its tip, by one. */
    void growIfPossible(IBlockState state, World world, BlockPos pos, Random random) {
        if (world.getBlockState(pos.up()).getBlock() != this.growsOn) {
            return;
        }
        BlockPos tip = findTip(state, world, pos, MAX_GROWTH_LENGTH);
        if (tip == null) {
            return;
        }
        IBlockState tipState = world.getBlockState(tip);
        boolean freeHanging = isStalactite(tipState) && tipState.getValue(THICKNESS) == Thickness.TIP && !Waterlogging.hasWater(world, tip);
        if (freeHanging && canTipGrow(tipState, world, tip)) {
            if (random.nextBoolean()) {
                grow(world, tip, EnumFacing.DOWN);
            } else {
                growStalagmiteBelow(world, tip);
            }
        }
    }

    @Nullable
    static BlockPos findTip(IBlockState state, World world, BlockPos pos, int maxSteps) {
        if (isTip(state, false)) {
            return pos;
        }
        EnumFacing direction = state.getValue(VERTICAL_DIRECTION);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(pos);
        for (int i = 1; i < maxSteps; i++) {
            m.move(direction);
            IBlockState s = world.getBlockState(m);
            if (isTip(s, false)) {
                return m.toImmutable();
            }
            if (world.isOutsideBuildHeight(m) || !withDirection(s, direction)) {
                return null;
            }
        }
        return null;
    }

    private static boolean canTipGrow(IBlockState tipState, World world, BlockPos tip) {
        EnumFacing direction = tipState.getValue(VERTICAL_DIRECTION);
        BlockPos aheadPos = tip.offset(direction);
        IBlockState ahead = world.getBlockState(aheadPos);
        if (ahead.getMaterial().isLiquid() || Waterlogging.hasWater(world, aheadPos)) {
            return false;
        }
        return ahead.getBlock().isAir(ahead, world, aheadPos) || isUnmergedTip(ahead, direction.getOpposite());
    }

    private void growStalagmiteBelow(World world, BlockPos tip) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(tip);
        for (int i = 0; i < MAX_STALAGMITE_SEARCH; i++) {
            m.move(EnumFacing.DOWN);
            IBlockState s = world.getBlockState(m);
            if (s.getMaterial().isLiquid() || Waterlogging.hasWater(world, m)) {
                return;
            }
            if (isUnmergedTip(s, EnumFacing.UP) && canTipGrow(s, world, m)) {
                grow(world, m.toImmutable(), EnumFacing.UP);
                return;
            }
            if (isValidPlacement(world, m, EnumFacing.UP) && !Waterlogging.hasWater(world, m.down())) {
                grow(world, m.down(), EnumFacing.UP);
                return;
            }
        }
    }

    private void grow(World world, BlockPos from, EnumFacing direction) {
        BlockPos target = from.offset(direction);
        IBlockState there = world.getBlockState(target);
        if (isUnmergedTip(there, direction.getOpposite())) {
            BlockPos top = there.getValue(VERTICAL_DIRECTION) == EnumFacing.UP ? target.up() : target;
            world.setBlockState(top, state(EnumFacing.DOWN, Thickness.TIP_MERGE), 3);
            world.setBlockState(top.down(), state(EnumFacing.UP, Thickness.TIP_MERGE), 3);
        } else if (there.getBlock().isAir(there, world, target)) {
            world.setBlockState(target, state(direction, Thickness.TIP), 3);
        } else if (there.getBlock() == Blocks.WATER || there.getBlock() == Blocks.FLOWING_WATER) {
            // Into water it keeps the water (26.3: waterlogged when the fluid there is water).
            Waterlogging.placeKeepingWater(world, target, state(direction, Thickness.TIP), 3);
        }
    }
}
