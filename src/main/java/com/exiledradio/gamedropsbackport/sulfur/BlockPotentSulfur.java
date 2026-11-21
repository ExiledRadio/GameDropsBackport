package com.exiledradio.gamedropsbackport.sulfur;

import com.exiledradio.backportcore.gameevent.GameEvent;
import com.exiledradio.backportcore.gameevent.GameEventContext;
import com.exiledradio.backportcore.gameevent.GameEvents;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * Potent sulfur (26.2): sulfur's numbers, gold on maps, and a block entity that does the work.
 *
 * <h2>States (26.3's {@code PotentSulfurBlock})</h2>
 * Worked out on placement and whenever a neighbor changes: {@code dry} without a water source right above;
 * with one, {@code continuous} over a lava source, {@code dormant} (or {@code erupting}, kept) over a magma
 * block, else {@code wet}. A block that newly becomes a geyser restarts its countdown.
 *
 * <h2>Water</h2>
 * A water source above means a still water block or water kept in a block (Backport Core, with Fluidlogged
 * API), as 26.3 reads the fluid state there.
 */
public class BlockPotentSulfur extends Block implements ITileEntityProvider {

    public static final PropertyEnum<PotentState> STATE = PropertyEnum.create("potent_sulfur_state", PotentState.class);

    /** Up to this many water blocks above make a pool (26.3's {@code ALLOWED_WATER_BLOCKS_ABOVE}). */
    public static final int WATER_BLOCKS = 4;

    public BlockPotentSulfur(SoundType sound) {
        super(Material.ROCK, MapColor.GOLD);
        setHardness(1.5F);
        setResistance(10.0F);
        setSoundType(sound);
        setHarvestLevel("pickaxe", 0);
        setDefaultState(this.blockState.getBaseState().withProperty(STATE, PotentState.DRY));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, STATE);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(STATE).ordinal();
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        PotentState[] all = PotentState.values();
        return getDefaultState().withProperty(STATE, all[meta < all.length ? meta : 0]);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TilePotentSulfur();
    }

    // ------------------------------------------------------------------------------- state

    /** @return whether the position holds a still water source (a water block, or water kept in a block) */
    static boolean isWaterSource(IBlockAccess world, BlockPos pos) {
        return com.exiledradio.backportcore.waterlog.Waterlogging.hasWaterSource(world, pos);
    }

    private static boolean isLavaSource(IBlockState state) {
        return (state.getBlock() == Blocks.LAVA || state.getBlock() == Blocks.FLOWING_LAVA) && state.getValue(BlockLiquid.LEVEL) == 0;
    }

    /** 26.3's {@code validBlockState}. */
    public IBlockState validState(IBlockState state, IBlockAccess world, BlockPos pos) {
        if (!isWaterSource(world, pos.up())) {
            return state.withProperty(STATE, PotentState.DRY);
        }
        IBlockState below = world.getBlockState(pos.down());
        if (isLavaSource(below)) {
            return state.withProperty(STATE, PotentState.CONTINUOUS);
        }
        if (below.getBlock() == Blocks.MAGMA) {
            PotentState now = state.getValue(STATE);
            if (now != PotentState.ERUPTING && now != PotentState.DORMANT) {
                TileEntity te = world.getTileEntity(pos);
                if (te instanceof TilePotentSulfur) {
                    ((TilePotentSulfur) te).resetCountdown();
                }
            }
            return now == PotentState.ERUPTING ? state : state.withProperty(STATE, PotentState.DORMANT);
        }
        return state.withProperty(STATE, PotentState.WET);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ,
                                            int meta, EntityLivingBase placer, EnumHand hand) {
        return validState(getDefaultState(), world, pos);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
        refresh(world, pos, state);
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        if (!refresh(world, pos, state)) {
            started(world, pos, state);
        }
    }

    /** Sets the valid state; @return whether it changed (the change calls {@link #onBlockAdded} itself) */
    private boolean refresh(World world, BlockPos pos, IBlockState state) {
        if (world.isRemote) {
            return false;
        }
        IBlockState valid = validState(state, world, pos);
        if (valid != state) {
            world.setBlockState(pos, valid, 3);
            started(world, pos, valid);
            return true;
        }
        return false;
    }

    /** 26.3's {@code onPlace}: an eruption starts with a block event (the clients' plume clock), its sound and a game event. */
    void started(World world, BlockPos pos, IBlockState state) {
        PotentState s = state.getValue(STATE);
        if (world.isRemote || !s.erupts()) {
            return;
        }
        world.addBlockEvent(pos, this, 0, 0);
        world.playSound(null, pos, Reg.sound(s == PotentState.CONTINUOUS
                ? "block.potent_sulfur.geyser_continuous_eruption" : "block.potent_sulfur.geyser_eruption"),
                SoundCategory.BLOCKS, 1.0F, 1.0F);
        GameEvents.emit(world, GameEvent.BLOCK_ACTIVATE, pos, GameEventContext.of(state));
    }

    @Override
    public boolean eventReceived(IBlockState state, World world, BlockPos pos, int id, int param) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TilePotentSulfur) {
            ((TilePotentSulfur) te).eruptionTick = world.getTotalWorldTime();
        }
        return true;
    }

    // ------------------------------------------------------------------------------- client

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random random) {
        if (state.getValue(STATE) == PotentState.DRY || !isWaterSource(world, pos.up())) {
            return;
        }
        for (int i = 0; i < 2; i++) {
            com.exiledradio.gamedropsbackport.sulfur.client.SulfurParticles.bubble(world,
                    pos.getX() + random.nextFloat(), pos.getY() + 1 + random.nextFloat(), pos.getZ() + random.nextFloat());
        }
        if (random.nextInt(10) == 0) {
            world.playSound(pos.getX(), pos.getY(), pos.getZ(), Reg.sound("block.potent_sulfur.noxious_gas"), SoundCategory.AMBIENT,
                    1.0F, 1.0F, false);
        }
    }
}
