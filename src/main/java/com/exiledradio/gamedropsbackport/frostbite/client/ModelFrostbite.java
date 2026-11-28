package com.exiledradio.gamedropsbackport.frostbite.client;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelZombie;

/**
 * 26.4's frostbite body ({@code HumanoidModel.createBodyLayerWithCustomLeftArmsAndLegs}, the drowned's): the zombie's
 * humanoid on a 64x64 texture, with the left arm at UV (32, 48) and the left leg at (16, 48) instead of mirrored. The
 * outer layer is the same mesh inflated 0.25.
 */
public class ModelFrostbite extends ModelZombie {

    public ModelFrostbite(float inflate) {
        super(inflate, true);
        this.bipedLeftArm = new ModelRenderer(this, 32, 48);
        this.bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, inflate);
        this.bipedLeftArm.setRotationPoint(5.0F, 2.0F, 0.0F);
        this.bipedLeftLeg = new ModelRenderer(this, 16, 48);
        this.bipedLeftLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, inflate);
        this.bipedLeftLeg.setRotationPoint(1.9F, 12.0F, 0.0F);
    }
}
