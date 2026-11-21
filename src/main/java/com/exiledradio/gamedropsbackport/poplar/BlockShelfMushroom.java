package com.exiledradio.gamedropsbackport.poplar;

import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.item.Item;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The shelf mushroom: 26.3's {@code ShelfMushroomBlock}, a bracket fungus on the side of a block, in two sizes.
 *
 * <ul>
 *   <li>{@code facing} points away from its support, which must have a full face toward it; it pops off (and drops)
 *       when that support goes. Placed on the clicked side when it can stand there, else on the first side in the
 *       order the player looks.</li>
 *   <li>{@code age} 0 or 1: bone meal grows a small one to the big one, every time; a big one drops two.</li>
 *   <li>Bounces what lands on it like a bed (restitution 0.75, four fifths of that for anything not alive) unless it
 *       sneaks, plays its bounce sound (not for items or lit TNT), and halves the fall damage.</li>
 *   <li>Breaks at once; pushed by a piston it pops off. Its outline and collision are 26.3's two boxes per size.</li>
 * </ul>
 */
public class BlockShelfMushroom extends Block implements IGrowable {

    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, 1);
    public static final float BOUNCE = 0.75F;
    public static final float FALL_REDUCTION = 0.5F;

    private static final EnumFacing[] HORIZONTAL = {EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST};
    /** [age][horizontal index][box]: 26.3's shapes, made for north and turned. */
    private static final AxisAlignedBB[][][] BOXES = new AxisAlignedBB[2][4][];
    private static final AxisAlignedBB[][] OUTLINE = new AxisAlignedBB[2][4];

    static {
        // Shapes.or(column(10 + 4i, 7 + 3i, 1 + i, 3 + 2i) moved (0, (8 - 2i) / 16, (4.5 - 1.5i) / 16),
        //           column(6 + 2i, 4 + 2i, 0, 1 + i) moved (0, (8 - 2i) / 16, (6 - i) / 16)), for north.
        for (int i = 0; i <= 1; i++) {
            AxisAlignedBB cap = column(10 + 4 * i, 7 + 3 * i, 1 + i, 3 + 2 * i).offset(0.0D, (8 - 2 * i) / 16.0D, (4.5D - 1.5D * i) / 16.0D);
            AxisAlignedBB stem = column(6 + 2 * i, 4 + 2 * i, 0, 1 + i).offset(0.0D, (8 - 2 * i) / 16.0D, (6 - i) / 16.0D);
            for (int f = 0; f < 4; f++) {
                AxisAlignedBB a = rotate(cap, HORIZONTAL[f]);
                AxisAlignedBB b = rotate(stem, HORIZONTAL[f]);
                BOXES[i][f] = new AxisAlignedBB[] {a, b};
                OUTLINE[i][f] = a.union(b);
            }
        }
    }

    /** {@code Block.column(sizeX, sizeZ, minY, maxY)}: centered in x and z, in pixels. */
    private static AxisAlignedBB column(double sizeX, double sizeZ, double minY, double maxY) {
        return new AxisAlignedBB((8 - sizeX / 2) / 16, minY / 16, (8 - sizeZ / 2) / 16, (8 + sizeX / 2) / 16, maxY / 16,
                (8 + sizeZ / 2) / 16);
    }

    /** A north box turned to face another horizontal side ({@code Shapes.rotateHorizontal}). */
    private static AxisAlignedBB rotate(AxisAlignedBB b, EnumFacing to) {
        switch (to) {
            case SOUTH:
                return new AxisAlignedBB(1 - b.maxX, b.minY, 1 - b.maxZ, 1 - b.minX, b.maxY, 1 - b.minZ);
            case EAST:
                return new AxisAlignedBB(1 - b.maxZ, b.minY, b.minX, 1 - b.minZ, b.maxY, b.maxX);
            case WEST:
                return new AxisAlignedBB(b.minZ, b.minY, 1 - b.maxX, b.maxZ, b.maxY, 1 - b.minX);
            default:
                return b;
        }
    }

    public BlockShelfMushroom() {
        super(Material.PLANTS, MapColor.YELLOW_STAINED_HARDENED_CLAY);
        setSoundType(PoplarContent.SHELF_MUSHROOM);
        setHardness(0.0F);
        setDefaultState(this.blockState.getBaseState().withProperty(BlockHorizontal.FACING, EnumFacing.NORTH).withProperty(AGE, 0));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, BlockHorizontal.FACING, AGE);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(BlockHorizontal.FACING, EnumFacing.byHorizontalIndex(meta & 3))
                .withProperty(AGE, (meta >> 2) & 1);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(BlockHorizontal.FACING).getHorizontalIndex() | state.getValue(AGE) << 2;
    }

    private static int index(IBlockState state) {
        EnumFacing f = state.getValue(BlockHorizontal.FACING);
        for (int i = 0; i < 4; i++) {
            if (HORIZONTAL[i] == f) {
                return i;
            }
        }
        return 0;
    }

    // ---------------------------------------------------------------------------------------------
    // Support

    /** {@code canSurvive}: the block behind it (opposite its facing) has a full face toward it. */
    public static boolean canStay(IBlockAccess world, BlockPos pos, EnumFacing facing) {
        BlockPos support = pos.offset(facing.getOpposite());
        return world.getBlockState(support).getBlockFaceShape(world, support, facing) == BlockFaceShape.SOLID;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        if (!super.canPlaceBlockAt(world, pos)) {
            return false;
        }
        for (EnumFacing f : HORIZONTAL) {
            if (canStay(world, pos, f)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side) {
        return canPlaceBlockAt(world, pos);
    }

    /**
     * {@code getStateForPlacement}: the sides in the order the player looks ({@code getNearestLookingDirections}),
     * horizontal ones only, the mushroom facing back toward the player, the first that holds. 1.12.2 gives no look
     * order for a block placed by something other than a player, so the clicked side comes first then.
     */
    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing clicked, float hitX, float hitY, float hitZ,
                                            int meta, EntityLivingBase placer, EnumHand hand) {
        for (EnumFacing look : lookOrder(placer, clicked)) {
            EnumFacing facing = look.getOpposite();
            if (canStay(world, pos, facing)) {
                return getDefaultState().withProperty(BlockHorizontal.FACING, facing);
            }
        }
        return getDefaultState();
    }

    /** {@code Direction.orderedByNearest}, horizontal sides only, in the order the placer looks. */
    static List<EnumFacing> lookOrder(@Nullable EntityLivingBase placer, EnumFacing clicked) {
        List<EnumFacing> out = new ArrayList<EnumFacing>();
        if (placer == null) {
            if (clicked.getAxis().isHorizontal()) {
                out.add(clicked.getOpposite());
            }
            for (EnumFacing f : HORIZONTAL) {
                if (!out.contains(f)) {
                    out.add(f);
                }
            }
            return out;
        }
        Vec3d look = placer.getLook(1.0F);
        final double x = look.x;
        final double z = look.z;
        // Nearest first: the larger of |x| and |z| gives the first side, the smaller the second, then their opposites
        // in reverse (as orderedByNearest's makeDirectionArray does for the horizontal plane).
        EnumFacing ex = x > 0 ? EnumFacing.EAST : EnumFacing.WEST;
        EnumFacing ez = z > 0 ? EnumFacing.SOUTH : EnumFacing.NORTH;
        boolean xFirst = Math.abs(x) > Math.abs(z);
        EnumFacing first = xFirst ? ex : ez;
        EnumFacing second = xFirst ? ez : ex;
        out.add(first);
        out.add(second);
        out.add(second.getOpposite());
        out.add(first.getOpposite());
        return out;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
        if (!canStay(world, pos, state.getValue(BlockHorizontal.FACING))) {
            dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    // ---------------------------------------------------------------------------------------------
    // Drops and growth

    @Override
    public int quantityDropped(IBlockState state, int fortune, Random random) {
        return state.getValue(AGE) == 1 ? 2 : 1;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(this);
    }

    @Override
    public int damageDropped(IBlockState state) {
        return 0;
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
        return state.getValue(AGE) < 1;
    }

    @Override
    public boolean canUseBonemeal(World world, Random rand, BlockPos pos, IBlockState state) {
        return true;
    }

    @Override
    public void grow(World world, Random rand, BlockPos pos, IBlockState state) {
        world.setBlockState(pos, state.withProperty(AGE, state.getValue(AGE) + 1), 2);
    }

    // ---------------------------------------------------------------------------------------------
    // Bounce

    /** {@code fallOn}: fall damage reduced by {@code fallDistanceReduction} 0.5. */
    @Override
    public void onFallenUpon(World world, BlockPos pos, Entity entity, float fallDistance) {
        entity.fall(fallDistance * (1.0F - FALL_REDUCTION), 1.0F);
    }

    /**
     * {@code Entity.restituteMovementAfterCollisions} with {@code bounceRestitution} 0.75: sent back up at 0.75 of the
     * speed it landed with (0.6 for anything not alive), unless it sneaks or came down slower than gravity pulls in
     * a tick; then {@code bounceOn}'s sound.
     */
    @Override
    public void onLanded(World world, Entity entity) {
        double vy = entity.motionY;
        if (entity.isSneaking() || -vy <= 0.08D) {
            super.onLanded(world, entity);
            return;
        }
        double restitution = entity instanceof EntityLivingBase ? BOUNCE : BOUNCE * 0.8D;
        entity.motionY = -vy * restitution;
        if (!(entity instanceof EntityItem) && !(entity instanceof EntityTNTPrimed)) {
            BlockPos pos = new BlockPos(entity.posX, entity.posY - 0.2D, entity.posZ);
            world.playSound(null, pos, PoplarContent.SHELF_MUSHROOM_BOUNCE, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Shape

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return OUTLINE[state.getValue(AGE)][index(state)];
    }

    @Override
    @SuppressWarnings("deprecation")
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox,
                                      List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity, boolean actualState) {
        for (AxisAlignedBB box : BOXES[state.getValue(AGE)][index(state)]) {
            addCollisionBoxToList(pos, entityBox, collidingBoxes, box);
        }
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
    public boolean isPassable(IBlockAccess world, BlockPos pos) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.withProperty(BlockHorizontal.FACING, rot.rotate(state.getValue(BlockHorizontal.FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(BlockHorizontal.FACING)));
    }
}
