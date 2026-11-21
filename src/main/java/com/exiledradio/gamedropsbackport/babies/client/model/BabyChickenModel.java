package com.exiledradio.gamedropsbackport.babies.client.model;

import net.minecraft.entity.passive.EntityChicken;

/** 26.3's {@code BabyChickenModel} with {@code ChickenModel.setupAnim}. The head is part of the body. */
public final class BabyChickenModel extends BabyModel<EntityChicken> {

    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart rightWing;
    private final ModelPart leftWing;

    public BabyChickenModel() {
        super(createBodyLayer());
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
        rightWing = root.getChild("right_wing");
        leftWing = root.getChild("left_wing");
    }

    static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.25F, -0.75F, 4.0F, 4.0F, 4.0F)
                        .texOffs(10, 8).addBox(-1.0F, -0.25F, -1.75F, 2.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 20.25F, -1.25F));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(2, 2).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 2.0F, 0.0F)
                        .texOffs(0, 1).addBox(-0.5F, 2.0F, -1.0F, 1.0F, 0.0F, 1.0F),
                PartPose.offset(1.0F, 22.0F, 0.5F));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 2).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 2.0F, 0.0F)
                        .texOffs(0, 0).addBox(-0.5F, 2.0F, -1.0F, 1.0F, 0.0F, 1.0F),
                PartPose.offset(-1.0F, 22.0F, 0.5F));
        root.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(6, 8).addBox(0.0F, 0.0F, -1.0F, 1.0F, 0.0F, 2.0F),
                PartPose.offset(2.0F, 20.0F, 0.0F));
        root.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(4, 8).addBox(-1.0F, 0.0F, -1.0F, 1.0F, 0.0F, 2.0F),
                PartPose.offset(-2.0F, 20.0F, 0.0F));
        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    protected void setupAnim(EntityChicken e, float pos, float speed, float ageInTicks, float yRot, float xRot) {
        float flap = lerp(partialTicks, e.oFlap, e.wingRotation);
        float flapSpeed = lerp(partialTicks, e.oFlapSpeed, e.destPos);
        float flapAngle = (sin(flap) + 1.0F) * flapSpeed;
        rightLeg.xRot = cos(pos * 0.6662F) * 1.4F * speed;
        leftLeg.xRot = cos(pos * 0.6662F + (float) Math.PI) * 1.4F * speed;
        rightWing.zRot = flapAngle;
        leftWing.zRot = -flapAngle;
    }
}
