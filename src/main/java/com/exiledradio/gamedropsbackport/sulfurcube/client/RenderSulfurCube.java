package com.exiledradio.gamedropsbackport.sulfurcube.client;

import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.sulfurcube.EntitySulfurCube;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * 26.3's {@code SulfurCubeRenderer} and {@code SulfurCubeInnerLayer}: the translucent outer gel around either the
 * inner core or the block the cube holds; squish while it hops (not while it holds a block), TNT's swell and flash
 * while primed.
 */
public class RenderSulfurCube extends RenderLiving<EntitySulfurCube> {

    private static final ResourceLocation OUTER = tex("sulfur_cube_outer");
    private static final ResourceLocation OUTER_SMALL = tex("sulfur_cube_outer_small");
    private static final ResourceLocation INNER = tex("sulfur_cube_inner");
    private static final ResourceLocation INNER_SMALL = tex("sulfur_cube_inner_small");

    private final ModelSulfurCube outer = ModelSulfurCube.outer();
    private final ModelSulfurCube smallOuter = ModelSulfurCube.smallOuter();
    private final ModelSulfurCube inner = ModelSulfurCube.inner();
    private final ModelSulfurCube smallInner = ModelSulfurCube.smallInner();

    public RenderSulfurCube(RenderManager manager) {
        super(manager, ModelSulfurCube.outer(), 0.25F);
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(GameDropsBackport.MOD_ID, "textures/entity/sulfur_cube/" + name + ".png");
    }

    @Override
    public void doRender(EntitySulfurCube cube, double x, double y, double z, float yaw, float partialTicks) {
        this.shadowSize = 0.25F * cube.getCubeSize();
        this.mainModel = cube.isChild() ? this.smallOuter : this.outer;
        super.doRender(cube, x, y, z, yaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntitySulfurCube cube) {
        return cube.isChild() ? OUTER_SMALL : OUTER;
    }

    private static float fuseLeft(EntitySulfurCube cube, float partialTicks) {
        return cube.isPrimed() ? cube.getFuse() - partialTicks + 1.0F : 0.0F;
    }

    @Override
    protected void preRenderCallback(EntitySulfurCube cube, float partialTicks) {
        // downscaleSlightly
        GlStateManager.scale(0.999F, 0.999F, 0.999F);
        GlStateManager.translate(0.0F, 0.001F, 0.0F);
        // applySizeAndSquish
        float size = cube.getCubeSize();
        float squish = cube.prevSquish + (cube.squish - cube.prevSquish) * partialTicks;
        float ss = cube.hasBodyItem() ? 0.0F : squish / (size * 0.5F + 1.0F);
        float w = 1.0F / (ss + 1.0F);
        GlStateManager.scale(w * size, 1.0F / w * size, w * size);
        float fuse = fuseLeft(cube, partialTicks);
        if (fuse < 10.0F && fuse > 0.0F) {
            float g = 1.0F - fuse / 10.0F;
            g = Math.max(0.0F, Math.min(1.0F, g));
            g *= g;
            g *= g;
            float s = 1.0F + g * 0.3F;
            GlStateManager.scale(s, s, s);
        }
        boolean baby = cube.isChild();
        float vOffset = baby ? 1.24F : 0.98F;
        float extra = baby ? 1.0F : 0.5F;
        float pixel = (cube.isInvisible() ? 0.0F : 1.0F) / 16.0F;
        GlStateManager.scale(extra, extra, extra);
        GlStateManager.translate(0.0F, vOffset - pixel, 0.0F);
    }

    @Override
    protected void renderModel(EntitySulfurCube cube, float limbSwing, float limbSwingAmount, float age, float yaw,
                               float pitch, float scale) {
        boolean visible = this.isVisible(cube);
        if (!visible) {
            super.renderModel(cube, limbSwing, limbSwingAmount, age, yaw, pitch, scale);
            return;
        }
        boolean baby = cube.isChild();
        ItemStack held = cube.getBodyItem();
        IBlockState state = held.isEmpty() ? null : stateOf(held);
        if (state != null) {
            this.renderBlock(cube, state, baby);
        } else {
            this.bindTexture(baby ? INNER_SMALL : INNER);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            (baby ? this.smallInner : this.inner).render(cube, limbSwing, limbSwingAmount, age, yaw, pitch, scale);
            GlStateManager.disableBlend();
        }
        // The outer gel, translucent, over it.
        this.bindTexture(baby ? OUTER_SMALL : OUTER);
        GlStateManager.enableNormalize();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        (baby ? this.smallOuter : this.outer).render(cube, limbSwing, limbSwingAmount, age, yaw, pitch, scale);
        GlStateManager.disableBlend();
        GlStateManager.disableNormalize();
    }

    private static IBlockState stateOf(ItemStack stack) {
        Block block = Block.getBlockFromItem(stack.getItem());
        if (block == net.minecraft.init.Blocks.AIR) {
            return null;
        }
        try {
            return block.getStateFromMeta(stack.getItem().getMetadata(stack.getMetadata()));
        } catch (RuntimeException e) {
            return block.getDefaultState();
        }
    }

    /** The held block, upright inside the gel, flashing white while the fuse burns (as primed TNT does). */
    private void renderBlock(EntitySulfurCube cube, IBlockState state, boolean baby) {
        BlockRendererDispatcher blocks = Minecraft.getMinecraft().getBlockRendererDispatcher();
        GlStateManager.pushMatrix();
        GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
        if (baby) {
            GlStateManager.scale(0.5F, 0.5F, 0.5F);
        }
        GlStateManager.translate(-0.5F, -0.518F, -0.5F);
        this.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        blocks.renderBlockBrightness(state, cube.getBrightness());
        float fuse = fuseLeft(Minecraft.getMinecraft().getRenderPartialTicks(), cube);
        if (fuse > 0.0F && (int) fuse / 5 % 2 == 0) {
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.DST_ALPHA);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 0.8F);
            GlStateManager.doPolygonOffset(-3.0F, -3.0F);
            GlStateManager.enablePolygonOffset();
            blocks.renderBlockBrightness(state, 1.0F);
            GlStateManager.doPolygonOffset(0.0F, 0.0F);
            GlStateManager.disablePolygonOffset();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.disableBlend();
            GlStateManager.enableLighting();
            GlStateManager.enableTexture2D();
        }
        GlStateManager.popMatrix();
    }

    private static float fuseLeft(float partialTicks, EntitySulfurCube cube) {
        return fuseLeft(cube, partialTicks);
    }
}
