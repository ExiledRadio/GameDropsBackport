package com.exiledradio.gamedropsbackport.babies.client.model;

import net.minecraft.entity.passive.EntitySheep;

/** 26.3's {@code BabySheepModel} with {@code SheepModel.setupAnim} (grazing head). Body and wool layers alike. */
public final class BabySheepModel extends BabyQuadrupedModel<EntitySheep> {

    public BabySheepModel() {
        super(sheep());
    }

    @Override
    protected void setupAnim(EntitySheep e, float pos, float speed, float ageInTicks, float yRot, float xRot) {
        super.setupAnim(e, pos, speed, ageInTicks, yRot, xRot);
        head.y += e.getHeadRotationPointY(partialTicks) * 9.0F * AGE_SCALE;
        head.xRot = e.getHeadRotationAngleX(partialTicks);
    }
}
