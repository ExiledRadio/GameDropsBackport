package com.exiledradio.gamedropsbackport.changes.client;

import com.exiledradio.gamedropsbackport.ModConfig;
import com.exiledradio.gamedropsbackport.changes.Changes;
import com.exiledradio.gamedropsbackport.changes.DeepFreezeChanges;

import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Random;

/**
 * 26.4's {@code AmbientIceBlockSoundsPlayer}: ice "resonates" ({@code block.ice.ambient}, ambient, at the block's
 * corner) with chance 1/10000 an animate tick, when water is under it, air above, and ice in all four directions out to
 * 8 blocks: the middle of a frozen lake. 1.12.2's ice has no animate tick, so the animate ticks are drawn here as
 * {@code WorldClient.doVoidFogParticles} draws them: 667 random blocks within 16 and 667 within 32 of the player, each
 * client tick.
 */
public final class IceAmbience {

    public static final int CHANCE = 10000;
    private final Random random = new Random();

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new IceAmbience());
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END || mc.world == null || mc.player == null || mc.isGamePaused()
                || !Changes.on(ModConfig.changes.iceSounds)) {
            return;
        }
        int x = MathHelper.floor(mc.player.posX), y = MathHelper.floor(mc.player.posY), z = MathHelper.floor(mc.player.posZ);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 667; i++) {
            sample(mc.world, x, y, z, 16, m);
            sample(mc.world, x, y, z, 32, m);
        }
    }

    private void sample(World world, int x, int y, int z, int range, BlockPos.MutableBlockPos m) {
        m.setPos(x + this.random.nextInt(range) - this.random.nextInt(range), y + this.random.nextInt(range) - this.random.nextInt(range),
                z + this.random.nextInt(range) - this.random.nextInt(range));
        if (world.getBlockState(m).getBlock() != Blocks.ICE || this.random.nextInt(CHANCE) != 0) {
            return;
        }
        if (resonates(world, m.toImmutable())) {
            world.playSound(m.getX(), m.getY(), m.getZ(), DeepFreezeChanges.ICE_AMBIENT, SoundCategory.AMBIENT, 1.0F, 1.0F, false);
        }
    }

    /** {@code playAmbientIceSounds}' conditions. */
    public static boolean resonates(World world, BlockPos pos) {
        if (world.getBlockState(pos.down()).getMaterial() != net.minecraft.block.material.Material.WATER || !world.isAirBlock(pos.up())) {
            return false;
        }
        for (EnumFacing side : EnumFacing.Plane.HORIZONTAL) {
            for (int d = 1; d <= 8; d++) {
                if (world.getBlockState(pos.offset(side, d)).getBlock() != Blocks.ICE) {
                    return false;
                }
            }
        }
        return true;
    }
}
