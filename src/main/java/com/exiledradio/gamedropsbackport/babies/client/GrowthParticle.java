package com.exiledradio.gamedropsbackport.babies.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.world.World;

/**
 * 26.3's {@code pause_mob_growth} and {@code reset_mob_growth} ({@code SimpleVerticalParticle}): the glint sprite,
 * no gravity, drifting 0.03 a tick down (paused) or up (reset), living 8 ticks, sized 0.5-1.1 of normal.
 */
final class GrowthParticle extends Particle {

    GrowthParticle(World world, double x, double y, double z, boolean upwards) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        this.motionX = 0.0D;
        this.motionZ = 0.0D;
        this.motionY = upwards ? 0.03D : -0.03D;
        this.particleGravity = 0.0F;
        this.particleScale *= this.rand.nextFloat() * 0.6F + 0.5F;
        this.particleMaxAge = 8;
        setParticleTexture(BabiesClient.glint);
    }

    @Override
    public int getFXLayer() {
        return 1;
    }
}
