package com.exiledradio.gamedropsbackport.changes;

import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.ModConfig;

import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.passive.EntitySkeletonHorse;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;

/**
 * Three small 26.x fixes to things 1.12.2 already has:
 * <ul>
 * <li>{@code saplingBonemeal} (26.3, {@code TreeGrower.canGrow}): a sapling whose tree is only a 2x2 one (1.12.2's
 * dark oak) is no bone meal target without its 2x2; the bone meal stays in hand.</li>
 * <li>{@code spawnTrampling} (26.3, {@code FarmlandBlock.fallOn}): a player cannot trample farmland the server's spawn
 * protection covers for them (mobs follow mob griefing, as before).</li>
 * <li>{@code persistentTrapHorses} (26.1, {@code SkeletonHorse.aiStep}): a skeleton trap horse that must persist (a
 * named one) never reaches 1.12.2's 18000-tick end.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID)
public final class SmallRules {

    private static Field trapTime;

    private SmallRules() {
    }

    // --- saplings --------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onBonemeal(BonemealEvent event) {
        // Both sides, so the client does not use up the bone meal the server keeps.
        if (Changes.on(ModConfig.changes.saplingBonemeal)
                && !canGrow(event.getWorld(), event.getPos(), event.getBlock())) {
            event.setCanceled(true);
        }
    }

    /** 26.3's {@code canGrow}: true unless a dark oak sapling lacks a 2x2 of dark oak saplings it belongs to. */
    public static boolean canGrow(World world, BlockPos pos, IBlockState state) {
        if (state.getBlock() != Blocks.SAPLING || state.getValue(BlockSapling.TYPE) != BlockPlanks.EnumType.DARK_OAK) {
            return true;
        }
        // 1.12.2's BlockSapling.generateTree looks for the 2x2 with this sapling at any of its corners.
        for (int dx = 0; dx >= -1; dx--) {
            for (int dz = 0; dz >= -1; dz--) {
                if (darkOak(world, pos.add(dx, 0, dz)) && darkOak(world, pos.add(dx + 1, 0, dz))
                        && darkOak(world, pos.add(dx, 0, dz + 1)) && darkOak(world, pos.add(dx + 1, 0, dz + 1))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean darkOak(World world, BlockPos pos) {
        IBlockState s = world.getBlockState(pos);
        return s.getBlock() == Blocks.SAPLING && s.getValue(BlockSapling.TYPE) == BlockPlanks.EnumType.DARK_OAK;
    }

    // --- farmland --------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getWorld().isRemote || !(event.getEntity() instanceof EntityPlayer) || !Changes.on(ModConfig.changes.spawnTrampling)) {
            return;
        }
        if (underSpawnProtection(event.getWorld(), event.getPos(), (EntityPlayer) event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /** 26.3's {@code isUnderSpawnProtection}: 1.12.2's {@code isBlockProtected} (dedicated servers, ops excepted). */
    public static boolean underSpawnProtection(World world, BlockPos pos, EntityPlayer player) {
        MinecraftServer server = world.getMinecraftServer();
        return server != null && server.isBlockProtected(world, pos, player);
    }

    // --- skeleton trap horses --------------------------------------------------------------------

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntityLiving() instanceof EntitySkeletonHorse) || event.getEntityLiving().world.isRemote
                || !Changes.on(ModConfig.changes.persistentTrapHorses)) {
            return;
        }
        EntitySkeletonHorse horse = (EntitySkeletonHorse) event.getEntityLiving();
        if (horse.isTrap() && horse.isNoDespawnRequired()) {
            holdTrapTime(horse);
        }
    }

    /** 26.1 stops counting for a persistent trap horse; its count stays where it was (1.12.2's update adds the tick). */
    static void holdTrapTime(EntitySkeletonHorse horse) {
        try {
            if (trapTime == null) {
                trapTime = ReflectionHelper.findField(EntitySkeletonHorse.class, "skeletonTrapTime", "field_184794_bV");
            }
            int t = trapTime.getInt(horse);
            if (t > 0) {
                // onLivingUpdate runs after this event and adds one: undo it ahead.
                trapTime.setInt(horse, t - 1);
            }
        } catch (IllegalAccessException e) {
            GameDropsBackport.LOGGER.warn("persistentTrapHorses: cannot reach the skeleton trap time", e);
        }
    }

    /** For the self-test. */
    public static int trapTime(EntitySkeletonHorse horse) throws IllegalAccessException {
        if (trapTime == null) {
            trapTime = ReflectionHelper.findField(EntitySkeletonHorse.class, "skeletonTrapTime", "field_184794_bV");
        }
        return trapTime.getInt(horse);
    }
}
