package com.exiledradio.gamedropsbackport.babies.client.model;

import net.minecraft.entity.passive.AbstractHorse;

/**
 * 26.3's {@code BabyHorseModel} (horse, skeleton and zombie horse) and {@code BabyDonkeyModel} (donkey and mule)
 * with {@code AbstractEquineModel.setupAnim}: grazing, rearing, mouth, tail swish and walk.
 */
public final class BabyEquineModel extends BabyModel<AbstractHorse> {

    private final boolean donkey;
    private final ModelPart body;
    private final ModelPart headParts;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart tail;

    private BabyEquineModel(LayerDefinition layer, boolean donkey) {
        super(layer);
        this.donkey = donkey;
        body = root.getChild("body");
        ModelPart limbs = donkey ? body : root;
        headParts = limbs.getChild("head_parts");
        rightHindLeg = limbs.getChild("right_hind_leg");
        leftHindLeg = limbs.getChild("left_hind_leg");
        rightFrontLeg = limbs.getChild("right_front_leg");
        leftFrontLeg = limbs.getChild("left_front_leg");
        tail = body.getChild("tail");
    }

    public static BabyEquineModel horse() {
        return new BabyEquineModel(LayerDefinition.create(createHorseMesh(CubeDeformation.NONE), 64, 64), false);
    }

    public static BabyEquineModel donkey() {
        return new BabyEquineModel(createDonkeyLayer(), true);
    }

    static MeshDefinition createHorseMesh(CubeDeformation g) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 13).addBox(-4.0F, -3.5F, -7.0F, 8.0F, 7.0F, 14.0F, g), PartPose.offset(0.0F, 12.5F, 0.0F));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(24, 34).addBox(-1.5F, -1.5F, -1.0F, 3.0F, 3.0F, 8.0F, g),
                PartPose.offsetAndRotation(0.0F, -1.0F, 7.0F, -0.7418F, 0.0F, 0.0F));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(12, 46).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 9.0F, 3.0F, g),
                PartPose.offset(2.4F, 16.0F, 5.4F));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(0, 46).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 9.0F, 3.0F, g),
                PartPose.offset(-2.4F, 16.0F, 5.4F));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(12, 34).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 9.0F, 3.0F, g),
                PartPose.offset(2.4F, 16.0F, -5.4F));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(0, 34).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 9.0F, 3.0F, g),
                PartPose.offset(-2.4F, 16.0F, -5.4F));
        PartDefinition neck = root.addOrReplaceChild("head_parts",
                CubeListBuilder.create().texOffs(30, 0).addBox(-2.0F, -6.0F, -2.0F, 4.0F, 8.0F, 4.0F, g),
                PartPose.offsetAndRotation(0.0F, 10.0F, -6.0F, 0.6109F, 0.0F, 0.0F));
        PartDefinition head = neck.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.9484F, -6.705F, 6.0F, 4.0F, 9.0F, g),
                PartPose.offset(0.0F, -6.0516F, -0.2951F));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(0, 4).addBox(-1.0F, -2.5F, -0.8F, 2.0F, 3.0F, 1.0F, g),
                PartPose.offsetAndRotation(2.0F, -4.2484F, 1.9451F, 0.0F, 0.0F, 0.2618F));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -2.5F, -0.5F, 2.0F, 3.0F, 1.0F, g),
                PartPose.offsetAndRotation(-2.0F, -4.2484F, 1.645F, 0.0F, 0.0F, -0.2618F));
        return mesh;
    }

    static LayerDefinition createDonkeyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 13).addBox(-5.0F, -3.0F, -7.0F, 8.0F, 6.0F, 14.0F, new CubeDeformation(0.0F)),
                PartPose.offset(1.0F, 14.0F, 0.0F));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offset(0.0F, -1.5F, 6.5F));
        tail.addOrReplaceChild("tail_r1",
                CubeListBuilder.create().texOffs(24, 33).addBox(-2.5F, -1.0F, -0.5F, 3.0F, 3.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.7418F, 0.0F, 0.0F));
        body.addOrReplaceChild("left_hind_leg",
                CubeListBuilder.create().texOffs(12, 44).addBox(-2.5F, -1.5F, -1.5F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(2.25F, 3.5F, 5.25F));
        body.addOrReplaceChild("right_hind_leg",
                CubeListBuilder.create().texOffs(0, 44).addBox(-2.5F, -1.5F, -1.5F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-2.4F, 3.5F, 5.4F));
        body.addOrReplaceChild("left_front_leg",
                CubeListBuilder.create().texOffs(12, 33).addBox(-2.5F, -1.5F, -1.5F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(2.4F, 3.5F, -5.3F));
        body.addOrReplaceChild("right_front_leg",
                CubeListBuilder.create().texOffs(0, 33).addBox(-2.5F, -1.5F, -1.5F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-2.4F, 3.5F, -5.4F));
        PartDefinition neck = body.addOrReplaceChild("head_parts", CubeListBuilder.create(), PartPose.offset(0.0F, -3.0F, -5.0F));
        neck.addOrReplaceChild("neck_r1",
                CubeListBuilder.create().texOffs(30, 9).addBox(-3.0F, -6.0F, -3.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.3927F, 0.0F, 0.0F));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, -5.0F, -3.0F));
        head.addOrReplaceChild("head_r1",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -3.6F, -8.4F, 6.0F, 4.0F, 9.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -1.0F, 1.0F, 0.3927F, 0.0F, 0.0F));
        head.addOrReplaceChild("left_ear",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -6.5F, -0.3F, 2.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(2.0F, -3.5F, -1.0F, 0.48F, 0.0F, 0.48F));
        head.addOrReplaceChild("right_ear",
                CubeListBuilder.create().texOffs(22, 0).mirror().addBox(-2.0F, -6.5F, -0.3F, 2.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
                        .mirror(false),
                PartPose.offsetAndRotation(-2.0F, -3.5F, -1.0F, 0.48F, 0.0F, -0.48F));
        body.addOrReplaceChild("right_chest", CubeListBuilder.create(), PartPose.offset(-1.0F, 10.0F, 0.0F));
        body.addOrReplaceChild("left_chest", CubeListBuilder.create(), PartPose.offset(-1.0F, 10.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    protected void setupAnim(AbstractHorse e, float pos, float speed, float ageInTicks, float yRot, float xRot) {
        float eating = e.getGrassEatingAmount(partialTicks);
        float standing = e.getRearingAmount(partialTicks);
        float feedingAnim = e.getMouthOpennessAngle(partialTicks);
        float clampedYRot = clamp(yRot, -20.0F, 20.0F);
        float headRotXRad = xRot * RAD;
        if (speed > 0.2F) {
            headRotXRad += cos(pos * 0.8F) * 0.15F * speed;
        }
        float iStanding = 1.0F - standing;
        headParts.xRot = (float) (Math.PI / 6) + headRotXRad;
        headParts.yRot = clampedYRot * RAD;
        float waterMultiplier = e.isInWater() ? 0.2F : 1.0F;
        float legAnim1 = cos(waterMultiplier * pos * 0.6662F + (float) Math.PI);
        float legXRotAnim = legAnim1 * 0.8F * speed;
        float baseHeadAngle = (1.0F - Math.max(standing, eating))
                * ((float) (Math.PI / 6) + headRotXRad + feedingAnim * sin(ageInTicks) * 0.05F);
        headParts.xRot = standing * ((float) (Math.PI / 12) + headRotXRad)
                + eating * (2.1816616F + sin(ageInTicks) * 0.05F)
                + baseHeadAngle;
        headParts.yRot = standing * clampedYRot * RAD + (1.0F - Math.max(standing, eating)) * headParts.yRot;
        animateHeadPartsPlacement(eating, standing);
        body.xRot = standing * (float) (-Math.PI / 4) + iStanding * body.xRot;
        leftFrontLeg.y = leftFrontLeg.y - legStandingYOffset() * standing;
        leftFrontLeg.z = leftFrontLeg.z + legStandingZOffset() * standing;
        rightFrontLeg.y = leftFrontLeg.y;
        rightFrontLeg.z = leftFrontLeg.z;
        float standAngle = (donkey ? (float) (Math.PI / 3) : (float) (Math.PI / 12)) * standing;
        float bobValue = cos(ageInTicks * 0.6F + (float) Math.PI);
        float legStandingXRotOffset = donkey ? 0.0F : (float) (-Math.PI / 3);
        float rlegRot = (legStandingXRotOffset + bobValue) * standing + legXRotAnim * iStanding;
        float llegRot = (legStandingXRotOffset - bobValue) * standing - legXRotAnim * iStanding;
        leftHindLeg.xRot = standAngle - legAnim1 * 0.5F * speed * iStanding;
        rightHindLeg.xRot = standAngle + legAnim1 * 0.5F * speed * iStanding;
        leftFrontLeg.xRot = rlegRot;
        rightFrontLeg.xRot = llegRot;
        if (donkey) {
            // BabyDonkeyModel.offsetLegPositionWhenStanding
            leftHindLeg.y = lerp(standing, leftHindLeg.y, -0.3F);
            rightHindLeg.y = lerp(standing, leftHindLeg.y, -0.3F);
        }
        tail.xRot = (donkey ? (float) (-Math.PI / 4) : (float) (-Math.PI / 2)) + (float) (Math.PI / 6) + speed * 0.75F;
        tail.y += speed * AGE_SCALE;
        tail.z += speed * 2.0F * AGE_SCALE;
        if (e.tailCounter > 0) {
            tail.yRot = cos(ageInTicks * 0.7F);
        } else {
            tail.yRot = 0.0F;
        }
        if (donkey) {
            // BabyDonkeyModel.setupAnim: the head angle redone with the pitch fixed at -30 degrees.
            float fixedXRotRad = -30.0F * RAD;
            float base = (1.0F - Math.max(standing, eating))
                    * ((float) (Math.PI / 6) + fixedXRotRad + feedingAnim * sin(ageInTicks) * 0.05F);
            headParts.xRot = standing * ((float) (Math.PI / 12) + fixedXRotRad)
                    + eating * ((float) (Math.PI / 2) + sin(ageInTicks) * 0.05F)
                    + base;
        }
    }

    private float legStandingYOffset() {
        return donkey ? 1.0F : 4.0F;
    }

    private float legStandingZOffset() {
        return donkey ? 0.5F : 0.0F;
    }

    private void animateHeadPartsPlacement(float eating, float standing) {
        if (donkey) {
            headParts.y = lerp(eating, headParts.y, -1.2F);
            headParts.z = lerp(standing, headParts.z, -3.6F);
        } else {
            headParts.y = headParts.y + lerp(eating, lerp(standing, 0.0F, -2.0F), 2.0F);
            headParts.z = lerp(standing, headParts.z, -4.0F);
        }
    }
}
