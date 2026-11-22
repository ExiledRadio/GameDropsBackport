package com.exiledradio.gamedropsbackport.poplar.client;

import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.poplar.BiomeDappledForest;
import com.exiledradio.gamedropsbackport.poplar.BlockPoplarLeaves;
import com.exiledradio.gamedropsbackport.poplar.Content;
import com.exiledradio.gamedropsbackport.poplar.PoplarContent;
import com.exiledradio.gamedropsbackport.poplar.wood.WoodSign;
import com.exiledradio.gamedropsbackport.poplar.wood.client.WoodSignRenderer;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Map;
import java.util.Random;

/**
 * Client parts of the poplar stream: item and block models, the falling poplar leaves, the sign renderer (the boats are Backport Core's),
 * and the dappled forest's fog.
 */
public final class PoplarClient {

    /** [color][1-4]: {@code particle/red_poplar_1} to {@code yellow_poplar_4}. */
    private static final TextureAtlasSprite[][] SPRITES = new TextureAtlasSprite[3][4];

    private PoplarClient() {
    }

    public static void init() {
        if (!PoplarContent.enabled()) {
            return;
        }
        MinecraftForge.EVENT_BUS.register(new Events());
        ClientRegistry.bindTileEntitySpecialRenderer(WoodSign.Tile.class, new WoodSignRenderer());
    }

    /** Called from the leaves' random display tick: one leaf just under the block ({@code spawnParticleBelow}). */
    public static void leaf(World world, BlockPos pos, Random rand, BlockPoplarLeaves.Color color) {
        TextureAtlasSprite[] sprites = SPRITES[color.ordinal()];
        if (sprites[0] == null) {
            return;
        }
        Minecraft.getMinecraft().effectRenderer.addEffect(new Leaf(world,
                pos.getX() + rand.nextDouble(), pos.getY() - 0.05D, pos.getZ() + rand.nextDouble(), rand, sprites));
    }

    public static final class Events {
        /** A model for every item model request, and the state mappers for pinned and multipart blocks. */
        @SubscribeEvent
        public void registerModels(ModelRegistryEvent event) {
            for (Map.Entry<Item, Map<Integer, String>> entry : Content.itemModels().entrySet()) {
                for (Map.Entry<Integer, String> meta : entry.getValue().entrySet()) {
                    ModelLoader.setCustomModelResourceLocation(entry.getKey(), meta.getKey(),
                            new ModelResourceLocation(GameDropsBackport.MOD_ID + ":" + meta.getValue(), "inventory"));
                }
            }
            for (Map.Entry<Block, IProperty<?>[]> entry : Content.ignoredProperties().entrySet()) {
                ModelLoader.setCustomStateMapper(entry.getKey(), new StateMap.Builder().ignore(entry.getValue()).build());
            }
            for (final Block block : Content.multipartBlocks()) {
                final ModelResourceLocation one = new ModelResourceLocation(block.getRegistryName(), "multipart");
                ModelLoader.setCustomStateMapper(block, new StateMapperBase() {
                    @Override
                    protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
                        return one;
                    }
                });
            }
        }

        @SubscribeEvent
        public void stitch(TextureStitchEvent.Pre event) {
            for (BlockPoplarLeaves.Color c : BlockPoplarLeaves.Color.values()) {
                for (int i = 0; i < 4; i++) {
                    SPRITES[c.ordinal()][i] = event.getMap().registerSprite(
                            new ResourceLocation(GameDropsBackport.MOD_ID, "particle/" + c.name + "_poplar_" + (i + 1)));
                }
            }
        }

        /**
         * The dappled forest's fog color, {@code #CCD8E2}, blended in by how much of the area around the camera
         * (a 5x5 of 4-block cells, as modern fog samples biomes) is dappled forest, with the overworld's
         * day-night darkening applied as 1.12.2 applies it to its own fog color.
         */
        @SubscribeEvent
        public void fog(EntityViewRenderEvent.FogColors event) {
            BiomeDappledForest dappled = PoplarContent.biome;
            Entity view = event.getEntity();
            if (dappled == null || view == null || view.world == null || !view.world.provider.isSurfaceWorld()) {
                return;
            }
            World world = view.world;
            int px = MathHelper.floor(view.posX);
            int pz = MathHelper.floor(view.posZ);
            int count = 0;
            BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    at.setPos(px + dx * 4, 64, pz + dz * 4);
                    Biome b = world.getBiome(at);
                    if (b == dappled) {
                        count++;
                    }
                }
            }
            if (count == 0) {
                return;
            }
            float share = count / 25.0F;
            float angle = world.getCelestialAngle((float) event.getRenderPartialTicks());
            float light = MathHelper.clamp(MathHelper.cos(angle * ((float) Math.PI * 2F)) * 2.0F + 0.5F, 0.0F, 1.0F);
            float r = (BiomeDappledForest.FOG >> 16 & 255) / 255.0F * (light * 0.94F + 0.06F);
            float g = (BiomeDappledForest.FOG >> 8 & 255) / 255.0F * (light * 0.94F + 0.06F);
            float b = (BiomeDappledForest.FOG & 255) / 255.0F * (light * 0.91F + 0.09F);
            event.setRed(event.getRed() + (r - event.getRed()) * share);
            event.setGreen(event.getGreen() + (g - event.getGreen()) * share);
            event.setBlue(event.getBlue() + (b - event.getBlue()) * share);
        }
    }

    /**
     * 26.3's {@code FallingLeavesParticle.PoplarProvider}: gravity 0.07 (x 1.2 x 0.0025), wind 10, swirl on,
     * flow-away off, size 2 x (0.05 or 0.075), falling at 0.021 from the start; lives up to 300 ticks and is
     * gone when it lands or stops against something.
     */
    static final class Leaf extends Particle {
        private final double swirlPeriod;
        private final float windBig = 10.0F;
        private float rotSpeed;
        private final float spinAcceleration;
        private int life = 300;

        Leaf(World world, double x, double y, double z, Random rand, TextureAtlasSprite[] sprites) {
            super(world, x, y, z);
            setParticleTexture(sprites[rand.nextInt(sprites.length)]);
            this.rotSpeed = (float) Math.toRadians(rand.nextBoolean() ? -30.0D : 30.0D);
            this.spinAcceleration = (float) Math.toRadians(rand.nextBoolean() ? -5.0D : 5.0D);
            this.particleMaxAge = 300;
            this.particleGravity = 0.07F * 1.2F * 0.0025F;
            float size = 2.0F * (rand.nextBoolean() ? 0.05F : 0.075F);
            // 1.12.2 draws a quad 0.1 * particleScale across; modern particles give the size directly.
            this.particleScale = size * 10.0F;
            setSize(size, size);
            this.motionX = 0.0D;
            this.motionY = -0.021D;
            this.motionZ = 0.0D;
            float f = rand.nextFloat();
            this.swirlPeriod = Math.toRadians(1000.0F + f * 3000.0F);
            this.particleRed = this.particleGreen = this.particleBlue = 1.0F;
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
            if (this.life-- <= 0) {
                setExpired();
                return;
            }
            float t = Math.min((300 - this.life) / 300.0F, 1.0F);
            double ax = t * Math.cos(t * this.swirlPeriod) * this.windBig;
            double az = t * Math.sin(t * this.swirlPeriod) * this.windBig;
            this.motionX += ax * 0.0025F;
            this.motionZ += az * 0.0025F;
            this.motionY -= this.particleGravity;
            this.rotSpeed += this.spinAcceleration / 20.0F;
            this.prevParticleAngle = this.particleAngle;
            this.particleAngle += this.rotSpeed / 20.0F;
            move(this.motionX, this.motionY, this.motionZ);
            if (this.onGround || this.life < 299 && (this.motionX == 0.0D || this.motionZ == 0.0D)) {
                setExpired();
            }
        }
    }
}
