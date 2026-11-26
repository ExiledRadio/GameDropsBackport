package com.exiledradio.gamedropsbackport.icecaves;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * What the ice cave blocks do outside their own methods:
 * <ul>
 *   <li>Brittle ice's {@code stepOn}: 26.4 calls it each tick something stands on the block; 1.12.2's
 *       {@code onEntityWalk} only on a step, so a living thing on the ground is checked here every tick, at
 *       {@code getOnPos} (0.2 below the feet).</li>
 *   <li>26.4's {@code LiquidBlock.shouldSpreadLiquid}: water (any level) with blue ice beside it, on one of the four
 *       horizontal sides, becomes brittle ice (age 0). 1.12.2 runs it from the water's own block update; here, from
 *       Forge's neighbor notification of the water or of the blue ice. Any mod's {@code blue_ice}.</li>
 * </ul>
 */
public final class IceCavesEvents {

    @SubscribeEvent
    public void livingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase e = event.getEntityLiving();
        if (e.world.isRemote || !e.onGround || IceCavesContent.brittleIce == null) {
            return;
        }
        BlockPos on = new BlockPos(MathHelper.floor(e.posX), MathHelper.floor(e.posY - 0.2D), MathHelper.floor(e.posZ));
        IBlockState state = e.world.getBlockState(on);
        if (state.getBlock() == IceCavesContent.brittleIce) {
            IceCavesContent.brittleIce.stepOn(e.world, on, state, e);
        }
    }

    /** Under ground in the ice caves, 26.4's spawn lists (monsters, bats) instead of the column biome's. */
    @SubscribeEvent
    public void potentialSpawns(net.minecraftforge.event.world.WorldEvent.PotentialSpawns event) {
        net.minecraft.entity.EnumCreatureType type = event.getType();
        if ((type != net.minecraft.entity.EnumCreatureType.MONSTER && type != net.minecraft.entity.EnumCreatureType.AMBIENT)
                || IceCavesContent.biome == null || !event.getWorld().isBlockLoaded(event.getPos())
                || !IceCaves.at(event.getWorld(), event.getPos())) {
            return;
        }
        event.getList().clear();
        event.getList().addAll(IceCavesContent.biome.getSpawnableList(type));
    }

    @SubscribeEvent
    public void notify(BlockEvent.NeighborNotifyEvent event) {
        if (IceCavesContent.brittleIce == null || event.getWorld().isRemote) {
            return;
        }
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        IBlockState state = event.getState();
        if (isWater(state)) {
            freezeNextToBlueIce(world, pos);
        } else if (isBlueIce(state.getBlock())) {
            for (EnumFacing side : EnumFacing.Plane.HORIZONTAL) {
                BlockPos n = pos.offset(side);
                if (isWater(world.getBlockState(n))) {
                    freezeNextToBlueIce(world, n);
                }
            }
        }
    }

    /** @return whether the water at {@code pos} turned into brittle ice */
    public static boolean freezeNextToBlueIce(World world, BlockPos pos) {
        if (!isWater(world.getBlockState(pos))) {
            return false;
        }
        for (EnumFacing side : EnumFacing.Plane.HORIZONTAL) {
            if (isBlueIce(world.getBlockState(pos.offset(side)).getBlock())) {
                world.setBlockState(pos, IceCavesContent.brittleIce.getDefaultState());
                return true;
            }
        }
        return false;
    }

    static boolean isWater(IBlockState state) {
        return state.getMaterial() == Material.WATER && state.getBlock() instanceof net.minecraft.block.BlockLiquid;
    }

    /** Any installed mod's blue ice (Update Aquatic's in the family). */
    static boolean isBlueIce(Block block) {
        return block.getRegistryName() != null && "blue_ice".equals(block.getRegistryName().getPath());
    }
}
