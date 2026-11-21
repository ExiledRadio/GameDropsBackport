package com.exiledradio.gamedropsbackport.sulfurcube.client;

import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.sulfurcube.EntitySulfurCube;
import com.exiledradio.gamedropsbackport.sulfurcube.SulfurCubes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleBreaking;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.init.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/** The cube's renderer, the bucket's model and the goo a landing cube splashes (26.3's {@code sulfur_cube_goo}). */
@Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID, value = Side.CLIENT)
public final class SulfurCubeClient {

    private static TextureAtlasSprite goo;

    private SulfurCubeClient() {
    }

    @SubscribeEvent
    public static void onModels(ModelRegistryEvent event) {
        if (!SulfurCubes.enabled()) {
            return;
        }
        ModelLoader.setCustomModelResourceLocation(SulfurCubes.bucket, 0,
                new ModelResourceLocation(GameDropsBackport.MOD_ID + ":sulfur_cube_bucket", "inventory"));
        RenderingRegistry.registerEntityRenderingHandler(EntitySulfurCube.class, RenderSulfurCube::new);
        SulfurCubes.landingParticles = SulfurCubeClient::landing;
    }

    @SubscribeEvent
    public static void onStitch(TextureStitchEvent.Pre event) {
        if (SulfurCubes.enabled() && event.getMap() == Minecraft.getMinecraft().getTextureMapBlocks()) {
            goo = event.getMap().registerSprite(new ResourceLocation(GameDropsBackport.MOD_ID, "particle/sulfur_cube_goo"));
        }
    }

    /** 26.3's {@code AbstractCubeMob.tick}: 16 goo particles per block of width, in a ring around the cube. */
    private static void landing(EntitySulfurCube cube) {
        if (goo == null) {
            return;
        }
        float size = cube.width * 2.0F;
        float radius = size / 2.0F;
        for (int i = 0; i < size * 16.0F; i++) {
            float dir = cube.getRNG().nextFloat() * (float) (Math.PI * 2);
            float d = cube.getRNG().nextFloat() * 0.5F + 0.5F;
            float xd = MathHelper.sin(dir) * radius * d;
            float zd = MathHelper.cos(dir) * radius * d;
            Minecraft.getMinecraft().effectRenderer.addEffect(new Goo(cube.world, cube.posX + xd, cube.posY, cube.posZ + zd));
        }
    }

    /** 26.3's {@code BreakingItemParticle} with the goo sprite: 1.12.2's item-breaking particle. */
    private static final class Goo extends ParticleBreaking {
        Goo(World world, double x, double y, double z) {
            super(world, x, y, z, Items.SLIME_BALL);
            this.setParticleTexture(goo);
        }
    }
}
