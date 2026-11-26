package com.exiledradio.gamedropsbackport.frostbite.client;

import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.frostbite.EntityFrostbite;
import com.exiledradio.gamedropsbackport.frostbite.EntityIceBall;
import com.exiledradio.gamedropsbackport.frostbite.FrostbiteContent;
import com.exiledradio.gamedropsbackport.frostbite.PotionFreezing;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderSnowball;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Random;

/**
 * The frostbite stream's client: the renderers, the ice ball's model, Freezing's icon, and 26.4's {@code freezing}
 * particle on entities whose only effect is Freezing (1.12.2's client knows an entity's effect color, not its
 * effects: color 0x9EEEEE is Freezing alone), one in 4 ticks as {@code LivingEntity.tickEffects}, beside 1.12.2's swirl.
 */
@Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID, value = Side.CLIENT)
public final class FrostbiteClient {

    private static final ResourceLocation ICON = new ResourceLocation(GameDropsBackport.MOD_ID, "textures/mob_effect/freezing.png");
    static final TextureAtlasSprite[] FRAMES = new TextureAtlasSprite[6];
    /** {@code EntityLivingBase.POTION_EFFECTS}: the synced swirl color. */
    @SuppressWarnings("unchecked")
    private static final net.minecraft.network.datasync.DataParameter<Integer> POTION_COLOR = (net.minecraft.network.datasync.DataParameter<Integer>)
            net.minecraftforge.fml.relauncher.ReflectionHelper.getPrivateValue(EntityLivingBase.class, null, "POTION_EFFECTS", "field_184633_f");

    private FrostbiteClient() {
    }

    /** From pre-init on a client. */
    public static void preInit() {
        RenderingRegistry.registerEntityRenderingHandler(EntityFrostbite.class, new IRenderFactory<EntityFrostbite>() {
            @Override
            public Render<? super EntityFrostbite> createRenderFor(RenderManager manager) {
                return new RenderFrostbite(manager);
            }
        });
        RenderingRegistry.registerEntityRenderingHandler(EntityIceBall.class, new IRenderFactory<EntityIceBall>() {
            @Override
            public Render<? super EntityIceBall> createRenderFor(RenderManager manager) {
                return new RenderSnowball<EntityIceBall>(manager, FrostbiteContent.iceBall, Minecraft.getMinecraft().getRenderItem());
            }
        });
        MinecraftForge.EVENT_BUS.register(new Ticker());
    }

    @SubscribeEvent
    public static void models(ModelRegistryEvent event) {
        if (FrostbiteContent.enabled() && FrostbiteContent.iceBall != null) {
            ModelLoader.setCustomModelResourceLocation(FrostbiteContent.iceBall, 0,
                    new ModelResourceLocation(GameDropsBackport.MOD_ID + ":ice_ball", "inventory"));
        }
    }

    @SubscribeEvent
    public static void stitch(TextureStitchEvent.Pre event) {
        if (!FrostbiteContent.enabled()) {
            return;
        }
        for (int i = 0; i < FRAMES.length; i++) {
            FRAMES[i] = event.getMap().registerSprite(new ResourceLocation(GameDropsBackport.MOD_ID, "particle/freezing_" + i));
        }
    }

    /** Freezing's 18x18 icon at a screen point, then the inventory sheet bound again for vanilla's next cells. */
    public static void drawIcon(int x, int y, float alpha) {
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        mc.getTextureManager().bindTexture(ICON);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, 0.0F, 18, 18, 18.0F, 18.0F);
        mc.getTextureManager().bindTexture(GuiContainer.INVENTORY_BACKGROUND);
    }

    static final class Ticker {
        private final Random random = new Random();

        @SubscribeEvent
        public void tick(TickEvent.ClientTickEvent event) {
            Minecraft mc = Minecraft.getMinecraft();
            if (event.phase != TickEvent.Phase.END || mc.world == null || mc.isGamePaused() || FRAMES[0] == null) {
                return;
            }
            for (Entity e : mc.world.loadedEntityList) {
                if (!(e instanceof EntityLivingBase) || e.isInvisible()) {
                    continue;
                }
                int color = ((EntityLivingBase) e).getDataManager().get(POTION_COLOR);
                if (color == PotionFreezing.COLOR && this.random.nextInt(4) == 0) {
                    double x = e.posX + (this.random.nextDouble() - 0.5D) * e.width;
                    double y = e.posY + this.random.nextDouble() * e.height;
                    double z = e.posZ + (this.random.nextDouble() - 0.5D) * e.width;
                    mc.effectRenderer.addEffect(new FreezingParticle(mc.world, x, y, z, FRAMES[this.random.nextInt(FRAMES.length)]));
                }
            }
        }
    }

    /** 26.4's {@code FreezingParticle}. */
    static final class FreezingParticle extends Particle {
        private final float rotSpeed;

        FreezingParticle(World world, double x, double y, double z, TextureAtlasSprite sprite) {
            super(world, x, y, z, 0.0D, 0.0D, 0.0D);
            this.motionX = (this.rand.nextFloat() * 2.0F - 1.0F) * 0.01D;
            this.motionY = (this.rand.nextFloat() * 2.0F - 1.0F) * 0.01D * 0.25D;
            this.motionZ = (this.rand.nextFloat() * 2.0F - 1.0F) * 0.01D;
            this.particleGravity = 0.125F;
            this.particleScale *= 0.81F;
            this.particleMaxAge = Math.max((int) ((int) (32.0F / (this.rand.nextFloat() * 0.8F + 0.2F)) * 0.9F), 10);
            this.rotSpeed = (this.rand.nextFloat() - 0.5F) * 0.1F;
            this.particleAngle = this.rotSpeed * 5.0F;
            setParticleTexture(sprite);
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
            this.prevParticleAngle = this.particleAngle;
            this.particleAngle += (float) (Math.PI * this.rotSpeed * 2.0D * Math.exp(this.particleAge / -8.0D));
            if (this.onGround) {
                this.particleMaxAge = this.particleAge + 20;
            }
            this.motionY -= 0.003D;
            this.motionY = Math.max(this.motionY, -0.14D);
            move(this.motionX, this.motionY, this.motionZ);
        }
    }
}
