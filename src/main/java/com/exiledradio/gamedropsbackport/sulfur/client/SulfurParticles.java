package com.exiledradio.gamedropsbackport.sulfur.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Potent sulfur's particles, called from the block and its block entity on the client. Only common types in these
 * signatures, so the server may link the callers; the particles themselves are in {@link Particles}.
 */
public final class SulfurParticles {

    private SulfurParticles() {
    }

    /** 26.3's {@code sulfur_bubbles}: a bubble rising through the water above the block. */
    public static void bubble(World world, double x, double y, double z) {
        add(new Particles.Bubble(world, x, y, z));
    }

    /** 26.3's {@code noxious_gas_cloud}: puffs gas over the pool's surface for a second. */
    public static void gasCloud(World world, BlockPos source) {
        add(new Particles.GasCloud(world, source));
    }

    /** 26.3's {@code geyser}: one second of plume, base and poof particles. */
    public static void geyser(World world, double x, double y, double z, int waterBlocks) {
        add(new Particles.Eruption(world, x, y, z, waterBlocks));
    }

    static void add(net.minecraft.client.particle.Particle particle) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.effectRenderer != null && mc.getRenderViewEntity() != null) {
            mc.effectRenderer.addEffect(particle);
        }
    }
}
