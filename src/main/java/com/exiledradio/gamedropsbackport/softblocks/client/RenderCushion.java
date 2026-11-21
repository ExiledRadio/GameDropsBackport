package com.exiledradio.gamedropsbackport.softblocks.client;

import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.softblocks.EntityCushion;
import com.exiledradio.gamedropsbackport.softblocks.SoftColors;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

/** 26.3's {@code CushionRenderer} and {@code CushionModel}: one 16 x 4 x 16 box on a 64 x 64 texture per color. */
public class RenderCushion extends Render<EntityCushion> {

    private static final ResourceLocation[] TEXTURES = new ResourceLocation[16];

    static {
        for (EnumDyeColor c : EnumDyeColor.values()) {
            TEXTURES[c.getMetadata()] = new ResourceLocation(GameDropsBackport.MOD_ID,
                    "textures/entity/cushion/" + SoftColors.modern(c) + "_cushion.png");
        }
    }

    private final Model model = new Model();

    public RenderCushion(RenderManager manager) {
        super(manager);
        this.shadowSize = 0.0F;
    }

    @Override
    public void doRender(EntityCushion entity, double x, double y, double z, float yaw, float partialTicks) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(180.0F - EnumFacing.fromAngle(entity.rotationYaw).getHorizontalAngle(), 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.translate(0.0D, -0.25D, 0.0D);
        bindEntityTexture(entity);
        if (this.renderOutlines) {
            GlStateManager.enableColorMaterial();
            GlStateManager.enableOutlineMode(getTeamColor(entity));
        }
        this.model.cushion.render(0.0625F);
        if (this.renderOutlines) {
            GlStateManager.disableOutlineMode();
            GlStateManager.disableColorMaterial();
        }
        GlStateManager.popMatrix();
        super.doRender(entity, x, y, z, yaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityCushion entity) {
        return TEXTURES[entity.getColor().getMetadata()];
    }

    static final class Model extends ModelBase {
        final ModelRenderer cushion;

        Model() {
            this.textureWidth = 64;
            this.textureHeight = 64;
            this.cushion = new ModelRenderer(this, 0, 0);
            this.cushion.setRotationPoint(23.0F, 4.0F, -7.0F);
            this.cushion.addBox(-31.0F, -4.0F, -1.0F, 16, 4, 16, -0.005F);
        }
    }
}
