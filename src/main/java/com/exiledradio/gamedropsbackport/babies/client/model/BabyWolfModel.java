package com.exiledradio.gamedropsbackport.babies.client.model;

import net.minecraft.entity.passive.EntityWolf;

/** 26.3's {@code BabyWolfModel} with {@code WolfModel.setupAnim}: tail wag, sitting, begging tilt, shaking dry. */
public final class BabyWolfModel extends BabyModel<EntityWolf> {

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart tail;

    public BabyWolfModel() {
        super(createBodyLayer());
        head = root.getChild("head");
        body = root.getChild("body");
        rightHindLeg = root.getChild("right_hind_leg");
        leftHindLeg = root.getChild("left_hind_leg");
        rightFrontLeg = root.getChild("right_front_leg");
        leftFrontLeg = root.getChild("left_front_leg");
        tail = root.getChild("tail");
    }

    static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 12).addBox(-2.99F, -3.25F, -3.0F, 6.0F, 5.0F, 5.0F, new CubeDeformation(0.025F))
                        .texOffs(17, 12).addBox(-1.5F, -0.24F, -5.0F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 18.25F, -4.0F));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(0, 5).addBox(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(-2.0F, -4.25F, -0.5F));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(20, 5).addBox(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(2.0F, -4.25F, -0.5F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -2.0F, -4.0F, 6.0F, 4.0F, 8.0F),
                PartPose.offset(0.0F, 19.0F, 0.0F));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(0, 22).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(-1.5F, 21.0F, 3.0F));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(8, 22).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(1.5F, 21.0F, 3.0F));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(-1.5F, 21.0F, -3.0F));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(20, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(1.5F, 21.0F, -3.0F));
        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 19.0F, 3.0F, -0.5236F, 0.0F, 0.0F));
        tail.addOrReplaceChild("tail_r1",
                CubeListBuilder.create().texOffs(22, 16).addBox(-1.0F, -5.7F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -0.6F, 0.2F, -3.1F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    protected void setupAnim(EntityWolf e, float pos, float speed, float ageInTicks, float yRot, float xRot) {
        if (e.isAngry()) {
            tail.yRot = 0.0F;
        } else {
            tail.yRot = cos(pos * 0.6662F) * 1.4F * speed;
        }
        if (e.isSitting()) {
            setSittingPose();
        } else {
            rightHindLeg.xRot = cos(pos * 0.6662F) * 1.4F * speed;
            leftHindLeg.xRot = cos(pos * 0.6662F + (float) Math.PI) * 1.4F * speed;
            rightFrontLeg.xRot = cos(pos * 0.6662F + (float) Math.PI) * 1.4F * speed;
            leftFrontLeg.xRot = cos(pos * 0.6662F) * 1.4F * speed;
        }
        // shakeOffWater: WolfModel's body roll, then BabyWolfModel's head and tail roll.
        body.zRot = e.getShakeAngle(partialTicks, -0.16F);
        head.zRot = e.getInterestedAngle(partialTicks) + e.getShakeAngle(partialTicks, 0.0F);
        tail.zRot = e.getShakeAngle(partialTicks, -0.2F);
        head.xRot = xRot * RAD;
        head.yRot = yRot * RAD;
        tail.xRot = e.getTailRotation();
    }

    private void setSittingPose() {
        body.y += 4.0F * AGE_SCALE;
        body.z -= 2.0F * AGE_SCALE;
        body.xRot = (float) (Math.PI / 4);
        tail.y += 9.0F * AGE_SCALE;
        tail.z -= 2.0F * AGE_SCALE;
        rightHindLeg.y += 6.7F * AGE_SCALE;
        rightHindLeg.z -= 5.0F * AGE_SCALE;
        rightHindLeg.xRot = (float) (Math.PI * 3.0 / 2.0);
        leftHindLeg.y += 6.7F * AGE_SCALE;
        leftHindLeg.z -= 5.0F * AGE_SCALE;
        leftHindLeg.xRot = (float) (Math.PI * 3.0 / 2.0);
        rightFrontLeg.xRot = 5.811947F;
        rightFrontLeg.x += 0.01F * AGE_SCALE;
        rightFrontLeg.y += 1.0F * AGE_SCALE;
        leftFrontLeg.xRot = 5.811947F;
        leftFrontLeg.x -= 0.01F * AGE_SCALE;
        leftFrontLeg.y += 1.0F * AGE_SCALE;
        // BabyWolfModel.setSittingPose
        body.xRot--;
    }
}
