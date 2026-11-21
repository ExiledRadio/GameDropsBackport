package com.exiledradio.gamedropsbackport.babies.client.model;

import net.minecraft.entity.monster.EntityPolarBear;

/** 26.3's {@code BabyPolarBearModel} with {@code PolarBearModel.setupAnim} (standing up). */
public final class BabyPolarBearModel extends BabyQuadrupedModel<EntityPolarBear> {

    public BabyPolarBearModel() {
        super(polarBear());
    }

    @Override
    protected void setupAnim(EntityPolarBear e, float pos, float speed, float ageInTicks, float yRot, float xRot) {
        super.setupAnim(e, pos, speed, ageInTicks, yRot, xRot);
        float stand = e.getStandingAnimationScale(partialTicks);
        float standScale = stand * stand;
        body.xRot -= standScale * (float) Math.PI * 0.35F;
        body.y += standScale * AGE_SCALE * 2.0F;
        rightFrontLeg.y -= standScale * AGE_SCALE * 20.0F;
        rightFrontLeg.z += standScale * AGE_SCALE * 4.0F;
        rightFrontLeg.xRot -= standScale * (float) Math.PI * 0.45F;
        leftFrontLeg.y = rightFrontLeg.y;
        leftFrontLeg.z = rightFrontLeg.z;
        leftFrontLeg.xRot -= standScale * (float) Math.PI * 0.45F;
        head.y -= standScale * 24.0F;
        head.z += standScale * 13.0F;
        head.xRot += standScale * (float) Math.PI * 0.15F;
    }
}
