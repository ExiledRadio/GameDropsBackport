package com.exiledradio.gamedropsbackport.sulfurcube.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

/**
 * 26.3's {@code SulfurCubeModel} and {@code SmallSulfurCubeModel}: one centered box. The adult's outer gel is 18
 * pixels and its inner core 16 (a 128 texture); the baby's are 10 and 8 (a 64 texture).
 */
public class ModelSulfurCube extends ModelBase {

    private final ModelRenderer cube;

    public ModelSulfurCube(int texSize, int v, int box) {
        this.textureWidth = texSize;
        this.textureHeight = texSize;
        this.cube = new ModelRenderer(this, 0, v);
        float h = box / 2.0F;
        this.cube.addBox(-h, -h, -h, box, box, box);
    }

    public static ModelSulfurCube outer() {
        return new ModelSulfurCube(128, 0, 18);
    }

    public static ModelSulfurCube inner() {
        return new ModelSulfurCube(128, 36, 16);
    }

    public static ModelSulfurCube smallOuter() {
        return new ModelSulfurCube(64, 0, 10);
    }

    public static ModelSulfurCube smallInner() {
        return new ModelSulfurCube(64, 20, 8);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float age, float yaw, float pitch, float scale) {
        this.cube.render(scale);
    }
}
