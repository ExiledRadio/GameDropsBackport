package com.exiledradio.gamedropsbackport.sulfur.client;

import com.exiledradio.gamedropsbackport.sulfur.TilePotentSulfur;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * 26.3's potent sulfur particles on 1.12.2's particle class. 26.3's {@code quadSize} is 1.12.2's {@code 0.1 *
 * particleScale}; {@link Modern} runs 26.3's {@code Particle.tick} (gravity, friction, the sideways speed-up when
 * blocked), which 1.12.2's own update lacks. Frames are stitched into the block atlas ({@link #stitch}).
 */
final class Particles {

    static TextureAtlasSprite bubble;
    static final TextureAtlasSprite[] GAS = new TextureAtlasSprite[8];
    static final TextureAtlasSprite[] BASE = new TextureAtlasSprite[8];
    static final TextureAtlasSprite[] PLUME = new TextureAtlasSprite[8];
    static final TextureAtlasSprite[] POOF = new TextureAtlasSprite[8];

    private Particles() {
    }

    static void stitch(TextureMap map) {
        bubble = map.registerSprite(new net.minecraft.util.ResourceLocation("gamedropsbackport", "particle/bubble_white"));
        for (int i = 0; i < 8; i++) {
            GAS[i] = sprite(map, "noxious_gas", i);
            BASE[i] = sprite(map, "geyser_base", i);
            PLUME[i] = sprite(map, "geyser_plume", i);
            POOF[i] = sprite(map, "geyser_poof", i);
        }
    }

    private static TextureAtlasSprite sprite(TextureMap map, String kind, int i) {
        return map.registerSprite(new net.minecraft.util.ResourceLocation("gamedropsbackport",
                String.format("particle/%s_%02d", kind, i + 1)));
    }

    /** 26.3's particle tick. */
    abstract static class Modern extends Particle {
        float friction = 0.98F;
        float gravity;
        boolean speedUpWhenBlocked;
        TextureAtlasSprite[] frames;

        Modern(World world, double x, double y, double z) {
            super(world, x, y, z);
        }

        Modern(World world, double x, double y, double z, double xa, double ya, double za) {
            super(world, x, y, z, xa, ya, za);
        }

        float quadSize() {
            return 0.1F * this.particleScale;
        }

        void quadSize(float size) {
            this.particleScale = size * 10.0F;
        }

        void spriteFromAge() {
            if (this.frames != null) {
                setParticleTexture(this.frames[Math.min(this.frames.length - 1, this.particleAge * (this.frames.length - 1)
                        / Math.max(1, this.particleMaxAge))]);
            }
        }

        @Override
        public int getFXLayer() {
            return 1;
        }

        @Override
        public void onUpdate() {
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
            if (this.particleAge++ >= this.particleMaxAge) {
                setExpired();
                return;
            }
            this.motionY -= 0.04 * this.gravity;
            move(this.motionX, this.motionY, this.motionZ);
            if (this.speedUpWhenBlocked && this.posY == this.prevPosY) {
                this.motionX *= 1.1;
                this.motionZ *= 1.1;
            }
            this.motionX *= this.friction;
            this.motionY *= this.friction;
            this.motionZ *= this.friction;
            if (this.onGround && this.friction != 1.0F) {
                this.motionX *= 0.7;
                this.motionZ *= 0.7;
            }
        }
    }

    /** {@code SulfurBubbleParticle}: rises until it leaves the water, three blocks up or stops rising, growing. */
    static final class Bubble extends Modern {
        private final double yStart;
        private final double yEnd;
        private final float sizeStart;
        private double yPrev;

        Bubble(World world, double x, double y, double z) {
            super(world, x, y, z);
            this.gravity = -0.04F;
            this.friction = 0.85F;
            setSize(0.02F, 0.02F);
            this.motionX = (this.rand.nextFloat() * 2.0F - 1.0F) * 0.02F;
            this.motionY = 0.0;
            this.motionZ = (this.rand.nextFloat() * 2.0F - 1.0F) * 0.02F;
            this.sizeStart = 0.02F + 0.02F * this.rand.nextFloat();
            quadSize(this.sizeStart);
            this.particleMaxAge = Integer.MAX_VALUE;
            this.yStart = y;
            this.yEnd = y + 3.0;
            this.yPrev = y;
            setParticleTexture(bubble);
        }

        @Override
        public void onUpdate() {
            super.onUpdate();
            if (!isAlive()) {
                return;
            }
            if (!TilePotentSulfurAccess.water(this.world, new BlockPos(this.posX, this.posY, this.posZ)) || this.posY >= this.yEnd
                    || this.posY <= this.yPrev) {
                setExpired();
                return;
            }
            this.motionX += wiggle();
            this.motionZ += wiggle();
            move(this.motionX, 0.0, this.motionZ);
            float progress = (float) ((this.posY - this.yStart) / (this.yEnd - this.yStart));
            quadSize(this.sizeStart + progress * (0.15F - this.sizeStart));
            this.yPrev = this.posY;
        }

        private double wiggle() {
            return this.rand.nextFloat() * 0.003F * (this.rand.nextBoolean() ? 1 : -1) * 0.5;
        }
    }

    /** Water at a position, as the block sees it (Backport Core's kept water included). */
    static final class TilePotentSulfurAccess {
        static boolean water(World world, BlockPos pos) {
            return com.exiledradio.backportcore.waterlog.Waterlogging.hasWaterSource(world, pos);
        }
    }

    /** An emitter that draws nothing. */
    abstract static class Emitter extends Particle {
        Emitter(World world, double x, double y, double z) {
            super(world, x, y, z);
        }

        @Override
        public void renderParticle(BufferBuilder buffer, Entity entity, float partialTicks, float rx, float rz, float ryz,
                                   float rxy, float rxz) {
        }

        @Override
        public void onUpdate() {
            if (this.particleAge++ >= this.particleMaxAge) {
                setExpired();
                return;
            }
            tick();
        }

        abstract void tick();
    }

    /** {@code NoxiousGasCloudParticle}: for 20 ticks, every second tick, gas at a random reachable point. */
    static final class GasCloud extends Emitter {
        private final BlockPos source;

        GasCloud(World world, BlockPos source) {
            super(world, source.getX() + 0.5, source.getY() + 0.5, source.getZ() + 0.5);
            this.source = source;
            this.particleMaxAge = 20;
        }

        @Override
        void tick() {
            if (this.particleAge % 2 != 0) {
                return;
            }
            Vec3d dir = new Vec3d(this.rand.nextFloat() - 0.5F, 0.0, this.rand.nextFloat() - 0.5F).normalize();
            float distance = this.rand.nextFloat() * 3.0F;
            Vec3d at = new Vec3d(this.source.getX() + 0.5, this.source.getY() + 0.5, this.source.getZ() + 0.5)
                    .add(dir.scale(distance)).add(0, -0.25, 0);
            if (TilePotentSulfur.canBeReachedByGas(this.world, this.source, at)) {
                SulfurParticles.add(new Gas(this.world, at.x, at.y, at.z));
            }
        }
    }

    /** 26.3's {@code BaseAshSmokeParticle} setup. */
    abstract static class AshSmoke extends Modern {
        AshSmoke(World world, double x, double y, double z, float mulX, float mulY, float mulZ, float scale, TextureAtlasSprite[] frames,
                 float colorRandom, int maxLifetime, float gravity) {
            super(world, x, y, z, 0.0, 0.0, 0.0);
            this.friction = 0.96F;
            this.gravity = gravity;
            this.speedUpWhenBlocked = true;
            this.frames = frames;
            this.motionX *= mulX;
            this.motionY *= mulY;
            this.motionZ *= mulZ;
            float c = this.rand.nextFloat() * colorRandom;
            setRBGColorF(c, c, c);
            quadSize(quadSize() * 0.75F * scale);
            this.particleMaxAge = Math.max(1, (int) (maxLifetime / (this.rand.nextFloat() * 0.8 + 0.2) * scale));
            spriteFromAge();
        }

        @Override
        public void onUpdate() {
            super.onUpdate();
            spriteFromAge();
        }
    }

    /** {@code NoxiousGasParticle}: drifts up, fading out over its second half. */
    static final class Gas extends AshSmoke {
        private final float fadeStart;

        Gas(World world, double x, double y, double z) {
            super(world, x, y, z, 0.1F, 0.1F, 0.1F, 3.0F, GAS, 0.3F, 5, -0.02F);
            setRBGColorF(1.0F, 1.0F, 1.0F);
            this.particleMaxAge = (int) (6.0 / (this.rand.nextFloat() * 0.5 + 0.5) * 3.0F);
            this.fadeStart = this.particleMaxAge / 2.0F;
        }

        @Override
        public void onUpdate() {
            super.onUpdate();
            if (this.particleAge > this.fadeStart) {
                float since = this.particleAge - this.fadeStart;
                setAlphaF((this.particleMaxAge - since) / this.particleMaxAge);
            }
        }
    }

    /** {@code GeyserEruptionParticle}: one second of base (every 2 ticks), plume (every tick) and poof (every 10). */
    static final class Eruption extends Emitter {
        private final int water;

        Eruption(World world, double x, double y, double z, int water) {
            super(world, x, y, z);
            this.water = water;
            this.particleMaxAge = 20;
        }

        @Override
        void tick() {
            if (this.particleAge % 2 == 0) {
                for (int i = 0; i < 2; i++) {
                    SulfurParticles.add(base(BASE, 1.5F));
                }
            }
            for (int i = 0; i < this.water + 2; i++) {
                SulfurParticles.add(new Plume(this.world, this.posX + (this.rand.nextFloat() - 0.5F) * 0.2F,
                        this.posY + this.rand.nextFloat(), this.posZ + (this.rand.nextFloat() - 0.5F) * 0.2F, this.water));
            }
            if (this.particleAge % 10 == 0) {
                for (int i = 0; i < 20; i++) {
                    SulfurParticles.add(base(POOF, 2.0F));
                }
            }
        }

        private Base base(TextureAtlasSprite[] frames, float impulse) {
            return new Base(this.world, this.posX + (this.rand.nextFloat() - 0.5F) * 0.5F,
                    this.posY + (this.rand.nextFloat() - 0.5F) * 0.5F + 0.2F, this.posZ + (this.rand.nextFloat() - 0.5F) * 0.5F,
                    this.water, impulse, frames);
        }
    }

    /** {@code GeyserBaseParticle} (and the poof, with its own frames and impulse): a burst at the surface. */
    static final class Base extends AshSmoke {
        Base(World world, double x, double y, double z, int water, float impulseBase, TextureAtlasSprite[] frames) {
            super(world, x, y, z, impulseBase + 0.25F * water, impulseBase + 0.25F * water, impulseBase + 0.25F * water,
                    3.0F + 0.125F * water, frames, 0.0F, 0, 0.0F);
            this.friction = 0.725F;
            setRBGColorF(1.0F, 1.0F, 1.0F);
            this.motionY = Math.abs(this.motionY);
            this.particleMaxAge = (int) (25.0F * (0.8F + 0.2F * this.rand.nextFloat()));
            spriteFromAge();
        }
    }

    /** {@code GeyserPlumeParticle}: shoots up five blocks per water block, spraying outward and growing as it goes. */
    static final class Plume extends Modern {
        private final double startY;
        private final double maxY;
        private final float propulsion;
        private final float sprayX;
        private final float sprayZ;
        private final float minSize;
        private final float maxSize;
        private boolean done;

        Plume(World world, double x, double y, double z, int water) {
            super(world, x, y, z, 0.0, 0.0, 0.0);
            int height = 5 * Math.max(1, water);
            this.canCollide = true;
            this.speedUpWhenBlocked = true;
            this.particleMaxAge = height * 5;
            this.motionY = 0.0;
            this.startY = y;
            this.maxY = y + height - 1.0;
            this.sprayX = (this.rand.nextFloat() - 0.5F) * 0.2F;
            this.sprayZ = (this.rand.nextFloat() - 0.5F) * 0.2F;
            this.friction = 1.0F;
            this.propulsion = (water == 1 ? 1.5F : 1.0F) * height * 1.45F;
            this.gravity = -this.propulsion;
            float randomized = quadSize() * 0.75F;
            this.minSize = randomized * (2.0F + height / 8.0F);
            this.maxSize = randomized * (3.0F + height / 8.0F);
            quadSize(this.minSize);
            this.frames = PLUME;
            spriteFromAge();
        }

        @Override
        public void onUpdate() {
            super.onUpdate();
            if (!isAlive()) {
                return;
            }
            if (!this.done && (this.motionY < 0.0 || this.posY > this.maxY || this.posY == this.prevPosY)) {
                this.particleMaxAge = Math.min(this.particleMaxAge, this.particleAge + 5);
                this.friction = 0.0F;
                this.done = true;
            }
            double linear = MathHelper.clamp((this.posY - this.startY) / (this.maxY - this.startY), 0.0, 1.0);
            this.gravity = this.propulsion * (float) Math.pow(linear, 3.0) * 0.12F;
            this.motionX = linear * this.sprayX;
            this.motionZ = linear * this.sprayZ;
            spriteFromAge();
            quadSize(this.minSize + (float) (linear * (this.maxSize - this.minSize)));
        }
    }
}
