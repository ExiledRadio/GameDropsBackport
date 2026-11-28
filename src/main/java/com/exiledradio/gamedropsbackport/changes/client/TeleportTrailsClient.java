package com.exiledradio.gamedropsbackport.changes.client;

import com.exiledradio.gamedropsbackport.changes.TeleportTrails;

import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

/**
 * The client's side of {@link TeleportTrails}: 26.3's {@code LevelEventHandler} cases 2018 (an enderman's box,
 * 0.6 by 2.9) and 2016/2017 ({@code singleBlockTeleportationParticles}), 128 portal particles at random points
 * between the two positions.
 */
public final class TeleportTrailsClient {

    /** 26.3's {@code EntityTypes.ENDERMAN} size. */
    private static final float ENDERMAN_WIDTH = 0.6F;
    private static final float ENDERMAN_HEIGHT = 2.9F;

    private TeleportTrailsClient() {
    }

    public static void show(final byte kind, final BlockPos from, final BlockPos to) {
        final Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                if (mc.world != null) {
                    spawn(mc.world, kind, from, to);
                }
            }
        });
    }

    /** @return how many particles were asked for (always 128) */
    public static int spawn(World world, byte kind, BlockPos from, BlockPos to) {
        Random random = world.rand;
        for (int i = 0; i < 128; i++) {
            double d = random.nextDouble();
            float vx = (random.nextFloat() - 0.5F) * 0.2F;
            float vy = (random.nextFloat() - 0.5F) * 0.2F;
            float vz = (random.nextFloat() - 0.5F) * 0.2F;
            double x;
            double y;
            double z;
            if (kind == TeleportTrails.ENDERMAN) {
                x = lerp(d, to.getX(), from.getX()) + (random.nextDouble() - 0.5D) * ENDERMAN_WIDTH * 2.0D;
                y = lerp(d, to.getY(), from.getY()) + random.nextDouble() * ENDERMAN_HEIGHT;
                z = lerp(d, to.getZ(), from.getZ()) + (random.nextDouble() - 0.5D) * ENDERMAN_WIDTH * 2.0D;
            } else {
                x = lerp(d, to.getX(), from.getX()) + (random.nextDouble() - 0.5D) + 0.5D;
                y = lerp(d, to.getY(), from.getY()) + random.nextDouble() - 0.5D;
                z = lerp(d, to.getZ(), from.getZ()) + (random.nextDouble() - 0.5D) + 0.5D;
            }
            world.spawnParticle(EnumParticleTypes.PORTAL, x, y, z, vx, vy, vz);
        }
        return 128;
    }

    private static double lerp(double d, double a, double b) {
        return a + d * (b - a);
    }
}
