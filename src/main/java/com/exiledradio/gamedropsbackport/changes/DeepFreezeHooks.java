package com.exiledradio.gamedropsbackport.changes;

import com.exiledradio.backportcore.gameevent.GameEvent;
import com.exiledradio.backportcore.gameevent.GameEventContext;
import com.exiledradio.backportcore.gameevent.GameEvents;
import com.exiledradio.gamedropsbackport.ModConfig;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockFrostedIce;
import net.minecraft.block.BlockMushroom;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * What 26.4's patches P8-P11 call ({@code asm/DeepFreezeTransformer}); each answers as 1.12.2 while its setting is off.
 */
public final class DeepFreezeHooks {

    private static final ResourceLocation BRITTLE_ICE = new ResourceLocation("gamedropsbackport", "brittle_ice");
    private static final ResourceLocation CRIMSON_NYLIUM = new ResourceLocation("netherupdatebackport", "crimson_nylium");
    private static final ResourceLocation WARPED_NYLIUM = new ResourceLocation("netherupdatebackport", "warped_nylium");

    /** How many populations run now (server thread): world generation keeps 1.12.2's mushroom rule. */
    private static int populating;

    private DeepFreezeHooks() {
    }

    // P8 ------------------------------------------------------------------------------------------------------------

    /**
     * {@code BlockSnow.canPlaceBlockAt}'s packed ice: 26.4's {@code #cannot_support_snow_layer} is ice and barriers
     * only, so the comparison is made with ice a second time.
     */
    public static Block snowRefuses() {
        return Changes.on(ModConfig.changes.snowOnPackedIce) ? Blocks.ICE : Blocks.PACKED_ICE;
    }

    // P9 ------------------------------------------------------------------------------------------------------------

    /** Whether 26.4's mushroom rule answers {@code canBlockStay}: on, and not inside world generation. */
    public static boolean mushroomRule() {
        return Changes.on(ModConfig.changes.mushroomsAnyLight) && populating == 0;
    }

    /** 26.4's {@code MushroomBlock.canSurvive}: the block below supports its center, whatever the light. */
    public static boolean mushroomStays(World world, BlockPos pos) {
        if (pos.getY() < 0 || pos.getY() >= world.getHeight()) {
            return false;
        }
        return supportsCenter(world, pos.down());
    }

    /** 26.4's {@code canSpreadTo} (the caller checked it is empty): mycelium, podzol or nylium, or light under 13 and support. */
    public static boolean mushroomSpreads(BlockMushroom mushroom, World world, BlockPos pos, IBlockState state) {
        if (!mushroomRule()) {
            return mushroom.canBlockStay(world, pos, state);
        }
        IBlockState below = world.getBlockState(pos.down());
        Block b = below.getBlock();
        if (b == Blocks.MYCELIUM || b == Blocks.DIRT && below.getValue(BlockDirt.VARIANT) == BlockDirt.DirtType.PODZOL
                || CRIMSON_NYLIUM.equals(b.getRegistryName()) || WARPED_NYLIUM.equals(b.getRegistryName())) {
            return true;
        }
        return world.getLight(pos) < 13 && supportsCenter(world, pos.down());
    }

    /** {@code canSupportCenter(UP)}: a top face that covers the middle of the block. */
    static boolean supportsCenter(IBlockAccess world, BlockPos pos) {
        BlockFaceShape shape = world.getBlockState(pos).getBlockFaceShape(world, pos, EnumFacing.UP);
        return shape != BlockFaceShape.UNDEFINED && shape != BlockFaceShape.BOWL;
    }

    // P10 -----------------------------------------------------------------------------------------------------------

    /** {@code IceBlock.skipRendering}: ice and frosted ice hide their faces against {@code #face_occluding_ice_blocks}. */
    public static boolean iceFaceHidden(Block self, IBlockAccess world, BlockPos pos, EnumFacing side) {
        if (self != Blocks.ICE && self != Blocks.FROSTED_ICE || !Changes.on(ModConfig.changes.iceFaces)) {
            return false;
        }
        Block other = world.getBlockState(pos.offset(side)).getBlock();
        return other == Blocks.ICE || other == Blocks.FROSTED_ICE || BRITTLE_ICE.equals(other.getRegistryName());
    }

    // P11 -----------------------------------------------------------------------------------------------------------

    /** {@code CrackingIceBlock.slightlyMelt}: each crack (an age below 3 growing) sends {@code block_change}. */
    public static void frostedIceCracks(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote && Changes.on(ModConfig.changes.frostedIceVibrations) && state.getBlock() == Blocks.FROSTED_ICE
                && state.getValue(BlockFrostedIce.AGE) < 3) {
            GameEvents.emit(world, GameEvent.BLOCK_CHANGE, pos, GameEventContext.of(state));
        }
    }

    /** Counts populations, so generated mushrooms keep 1.12.2's rule (26.4 filters its own by the heightmap). */
    public static final class Population {
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void pre(PopulateChunkEvent.Pre event) {
            populating++;
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void post(PopulateChunkEvent.Post event) {
            populating = Math.max(0, populating - 1);
        }
    }
}
