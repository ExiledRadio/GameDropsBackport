package com.exiledradio.gamedropsbackport.softblocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Biomes;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.Random;

/**
 * 26.3's straw bed ({@code StrawBedBlock}, {@code straw_bed_rule}): a two-block bed 4 pixels high (5 with the
 * pillow). In the Overworld it lets a player sleep when it is dark, never sets the spawn point and breaks when the
 * sleeper gets up; where a bed explodes (the Nether, the End) using it only breaks it. Either way nothing drops.
 *
 * <p>It uses vanilla's bed properties ({@link BlockBed#PART}, {@link BlockBed#OCCUPIED}) and answers
 * {@link #isBed}, so 1.12.2's own sleeping code (and Forge's {@code setBedOccupied}) works on it. It is not a
 * {@link BlockBed}, so villagers and cats (Village &amp; Pillage) never take it for one. Spawn and breaking on
 * waking are in {@link StrawBedSleep}.
 */
public class BlockStrawBed extends BlockHorizontal {

    private static final AxisAlignedBB FOOT = new AxisAlignedBB(0, 0, 0, 1, 0.25, 1);
    private static final AxisAlignedBB HEAD = new AxisAlignedBB(0, 0, 0, 1, 0.3125, 1);
    /** The pillow by the head part's facing: the half toward the head end. */
    private static final AxisAlignedBB[] PILLOW = {
            new AxisAlignedBB(0, 0, 0.5, 1, 0.3125, 1), // south
            new AxisAlignedBB(0, 0, 0, 0.5, 0.3125, 1), // west
            new AxisAlignedBB(0, 0, 0, 1, 0.3125, 0.5), // north
            new AxisAlignedBB(0.5, 0, 0, 1, 0.3125, 1), // east
    };

    Item item;

    BlockStrawBed() {
        super(Material.CLOTH, MapColor.YELLOW);
        setHardness(0.2F);
        setSoundType(SoftBlocks.STRAW_BED);
        setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
                .withProperty(BlockBed.PART, BlockBed.EnumPartType.FOOT).withProperty(BlockBed.OCCUPIED, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, BlockBed.PART, BlockBed.OCCUPIED);
    }

    /** The other half: from the foot toward the head, from the head back. */
    public static BlockPos other(IBlockState state, BlockPos pos) {
        EnumFacing f = state.getValue(FACING);
        return state.getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT ? pos.offset(f) : pos.offset(f.getOpposite());
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }
        if (state.getValue(BlockBed.PART) != BlockBed.EnumPartType.HEAD) {
            pos = pos.offset(state.getValue(FACING));
            state = world.getBlockState(pos);
            if (state.getBlock() != this) {
                return true;
            }
        }
        if (!world.provider.canRespawnHere() || world.getBiome(pos) == Biomes.HELL) {
            // Where 1.12.2's bed explodes (straw_bed_rule in the Nether and the End): destroy_on_use, no message.
            breakBed(world, pos);
            return true;
        }
        if (state.getValue(BlockBed.OCCUPIED)) {
            EntityPlayer sleeper = sleeperAt(world, pos);
            if (sleeper != null) {
                player.sendStatusMessage(new TextComponentTranslation("tile.bed.occupied"), true);
                return true;
            }
            setOccupied(world, pos, state, false);
        }
        EntityPlayer.SleepResult result = player.trySleep(pos);
        if (result == EntityPlayer.SleepResult.OK) {
            setOccupied(world, pos, world.getBlockState(pos), true);
            countSleep(player);
        } else if (result == EntityPlayer.SleepResult.NOT_POSSIBLE_NOW) {
            player.sendStatusMessage(new TextComponentTranslation("tile.bed.noSleep"), true);
        } else if (result == EntityPlayer.SleepResult.NOT_SAFE) {
            player.sendStatusMessage(new TextComponentTranslation("tile.bed.notSafe"), true);
        } else if (result == EntityPlayer.SleepResult.TOO_FAR_AWAY) {
            player.sendStatusMessage(new TextComponentTranslation("tile.bed.tooFarAway"), true);
        }
        return true;
    }

    /** 26.3's {@code getSleepStat}: a night in a straw bed counts as one, not as a night in a bed. */
    static void countSleep(EntityPlayer player) {
        if (player instanceof net.minecraft.entity.player.EntityPlayerMP && SoftBlocks.sleepInStrawBed != null) {
            player.addStat(net.minecraft.stats.StatList.SLEEP_IN_BED, -1);
            player.addStat(SoftBlocks.sleepInStrawBed);
        }
    }

    private static EntityPlayer sleeperAt(World world, BlockPos pos) {
        for (EntityPlayer p : world.playerEntities) {
            if (p.isPlayerSleeping() && pos.equals(p.bedLocation)) {
                return p;
            }
        }
        return null;
    }

    /**
     * 26.4's {@code destroyOnUse} and {@code destroyOnLeave}: {@code destroyBlock} without a drop, so the break particles
     * and the sound set's break sound (break_leave since 26.4 Pre-Release 2); the head takes the foot with it.
     */
    void breakBed(World world, BlockPos head) {
        world.destroyBlock(head, false);
    }

    private void setOccupied(World world, BlockPos pos, IBlockState state, boolean occupied) {
        if (state.getBlock() != this) {
            return;
        }
        world.setBlockState(pos, state.withProperty(BlockBed.OCCUPIED, occupied), 4);
        BlockPos o = other(state, pos);
        IBlockState os = world.getBlockState(o);
        if (os.getBlock() == this) {
            world.setBlockState(o, os.withProperty(BlockBed.OCCUPIED, occupied), 4);
        }
    }

    @Override
    public boolean isBed(IBlockState state, IBlockAccess world, BlockPos pos, Entity player) {
        return true;
    }

    @Override
    public void setBedOccupied(IBlockAccess world, BlockPos pos, EntityPlayer player, boolean occupied) {
        if (world instanceof World) {
            setOccupied((World) world, pos, world.getBlockState(pos), occupied);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
        if (world.getBlockState(other(state, pos)).getBlock() == this) {
            return;
        }
        // 1.12.2's bed: the head drops the bed when the foot is gone; the foot just goes.
        if (state.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD && !world.isRemote) {
            dropBlockAsItem(world, pos, state, 0);
        }
        world.setBlockToAir(pos);
    }

    @Override
    public void onBlockHarvested(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        if (player.capabilities.isCreativeMode && state.getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT) {
            BlockPos head = other(state, pos);
            if (world.getBlockState(head).getBlock() == this) {
                world.setBlockToAir(head);
            }
        }
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return state.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD ? this.item : Items.AIR;
    }

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return new ItemStack(this.item);
    }

    @Override
    @SuppressWarnings("deprecation")
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return state.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD ? HEAD : FOOT;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB box,
                                      List<AxisAlignedBB> out, Entity entity, boolean actual) {
        addCollisionBoxToList(pos, box, out, FOOT);
        if (state.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD) {
            addCollisionBoxToList(pos, box, out, PILLOW[state.getValue(FACING).getHorizontalIndex()]);
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
    @SuppressWarnings("deprecation")
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        if (state.getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT) {
            IBlockState head = world.getBlockState(other(state, pos));
            if (head.getBlock() == this) {
                state = state.withProperty(BlockBed.OCCUPIED, head.getValue(BlockBed.OCCUPIED));
            }
        }
        return state;
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing f = EnumFacing.byHorizontalIndex(meta);
        return (meta & 8) > 0
                ? getDefaultState().withProperty(BlockBed.PART, BlockBed.EnumPartType.HEAD).withProperty(FACING, f)
                        .withProperty(BlockBed.OCCUPIED, (meta & 4) > 0)
                : getDefaultState().withProperty(BlockBed.PART, BlockBed.EnumPartType.FOOT).withProperty(FACING, f);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = state.getValue(FACING).getHorizontalIndex();
        if (state.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD) {
            meta |= 8;
            if (state.getValue(BlockBed.OCCUPIED)) {
                meta |= 4;
            }
        }
        return meta;
    }

    /** As 1.12.2's bed: a rotated structure turns both halves, so each still points at the other. */
    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withRotation(IBlockState state, net.minecraft.util.Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withMirror(IBlockState state, net.minecraft.util.Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }
}
