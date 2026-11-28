package com.exiledradio.gamedropsbackport.frostbite.client;

import com.exiledradio.gamedropsbackport.frostbite.EntityFrostbite;

import net.minecraft.client.model.ModelZombie;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.util.ResourceLocation;

/**
 * 26.4's {@code FrostbiteRenderer}: the zombie's renderer with {@code zombie/frostbite.png} and the outer layer
 * ({@code FrostbiteOuterLayer}: {@code zombie/frostbite_outer_layer.png}, inflated 0.25, adults only). A baby uses the
 * babies stream's 26.1 baby zombie model with {@code zombie/frostbite_baby.png} where baby models are on.
 */
public class RenderFrostbite extends RenderBiped<EntityZombie> {

    public static final ResourceLocation TEXTURE = new ResourceLocation("gamedropsbackport", "textures/entity/zombie/frostbite.png");
    public static final ResourceLocation OUTER = new ResourceLocation("gamedropsbackport", "textures/entity/zombie/frostbite_outer_layer.png");

    public RenderFrostbite(RenderManager manager) {
        super(manager, new ModelFrostbite(0.0F), 0.5F);
        addLayer(new LayerBipedArmor(this) {
            @Override
            protected void initArmor() {
                this.modelLeggings = new ModelZombie(0.5F, true);
                this.modelArmor = new ModelZombie(1.0F, true);
            }
        });
        addLayer(new Outer(this));
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityZombie entity) {
        return TEXTURE;
    }

    static final class Outer implements LayerRenderer<EntityZombie> {
        private final RenderFrostbite render;
        private final ModelFrostbite model = new ModelFrostbite(0.25F);

        Outer(RenderFrostbite render) {
            this.render = render;
        }

        @Override
        public void doRenderLayer(EntityZombie entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scale) {
            if (entity.isInvisible() || entity.isChild() || !(entity instanceof EntityFrostbite)) {
                return;
            }
            this.model.setModelAttributes(this.render.getMainModel());
            this.model.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTicks);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.render.bindTexture(OUTER);
            this.model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        }

        @Override
        public boolean shouldCombineTextures() {
            return true;
        }
    }
}
