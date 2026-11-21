package com.exiledradio.gamedropsbackport.softblocks;

import com.exiledradio.gamedropsbackport.GameDropsBackport;

import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The straw bed's rules that live outside the block ({@code straw_bed_rule}: can_set_spawn never, destroy_on_leave),
 * and shears on wool shapes ({@code #shears_major_breaking_speed}).
 */
@Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID)
public final class StrawBedSleep {

    /** 26.3 lays a sleeper on the foot's top (0.25); 1.12.2 lays one 0.125 above a 0.5625 bed. */
    static final double SLEEP_HEIGHT = 0.25D + 0.125D;

    private static final List<Object[]> TO_BREAK = new ArrayList<Object[]>();

    private StrawBedSleep() {
    }

    static boolean isStrawBed(World world, BlockPos pos) {
        return pos != null && SoftBlocks.strawBed != null && world.isBlockLoaded(pos)
                && world.getBlockState(pos).getBlock() == SoftBlocks.strawBed;
    }

    /** The spawn point is never set by a straw bed (the bed is still there while the sleeper wakes). */
    @SubscribeEvent
    public static void onSetSpawn(PlayerSetSpawnEvent event) {
        if (!event.isForced() && isStrawBed(event.getEntityPlayer().world, event.getNewSpawn())) {
            event.setCanceled(true);
        }
    }

    /** Getting up breaks the bed, once 1.12.2 has finished waking the sleeper (it still reads the bed). */
    @SubscribeEvent
    public static void onWakeUp(PlayerWakeUpEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (!player.world.isRemote && isStrawBed(player.world, player.bedLocation)) {
            synchronized (TO_BREAK) {
                TO_BREAK.add(new Object[]{player.world, player.bedLocation});
            }
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) {
            return;
        }
        synchronized (TO_BREAK) {
            for (Iterator<Object[]> it = TO_BREAK.iterator(); it.hasNext(); ) {
                Object[] entry = it.next();
                if (entry[0] != event.world) {
                    continue;
                }
                it.remove();
                BlockPos pos = (BlockPos) entry[1];
                if (isStrawBed(event.world, pos)) {
                    SoftBlocks.strawBed.breakBed(event.world, pos);
                }
            }
        }
    }

    /** A sleeper lies on the straw, not 0.3 above it. Both sides: the client moves its own player. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        if (event.phase != TickEvent.Phase.END || !player.isPlayerSleeping() || !isStrawBed(player.world, player.bedLocation)) {
            return;
        }
        double y = player.bedLocation.getY() + SLEEP_HEIGHT;
        if (Math.abs(player.posY - y) > 1.0E-4D) {
            player.setPosition(player.posX, y, player.posZ);
        }
    }

    /** Shears cut wool stairs and slabs as fast as wool (5). */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        IBlockState state = event.getState();
        if (!(state.getBlock() instanceof BlockSoftStairs || state.getBlock() instanceof BlockSoftSlab)
                || state.getMaterial() != net.minecraft.block.material.Material.CLOTH) {
            return;
        }
        ItemStack held = event.getEntityPlayer().getHeldItemMainhand();
        if (!(held.getItem() instanceof ItemShears)) {
            return;
        }
        // The original speed is 1.0 times the player's multipliers (haste, water, air); efficiency only adds to a
        // speed above 1, as EntityPlayer.getDigSpeed.
        float speed = 5.0F;
        int efficiency = EnchantmentHelper.getEnchantmentLevel(Enchantments.EFFICIENCY, held);
        if (efficiency > 0) {
            speed += efficiency * efficiency + 1;
        }
        event.setNewSpeed(event.getOriginalSpeed() * speed);
    }
}
