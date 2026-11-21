package com.exiledradio.gamedropsbackport.babies.client.model;

import net.minecraft.entity.passive.EntityLlama;

/** 26.3's {@code BabyLlamaModel} with {@code LlamaModel.setupAnim}. Babies never show chests. */
public final class BabyLlamaModel extends BabyModel<EntityLlama> {

    private final ModelPart head;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightChest;
    private final ModelPart leftChest;

    public BabyLlamaModel() {
        super(createBodyLayer(CubeDeformation.NONE));
        head = root.getChild("head");
        rightChest = root.getChild("right_chest");
        leftChest = root.getChild("left_chest");
        rightHindLeg = root.getChild("right_hind_leg");
        leftHindLeg = root.getChild("left_hind_leg");
        rightFrontLeg = root.getChild("right_front_leg");
        leftFrontLeg = root.getChild("left_front_leg");
    }

    static LayerDefinition createBodyLayer(CubeDeformation g) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.0F, -9.0F, -4.0F, 6.0F, 11.0F, 4.0F, g)
                        .texOffs(0, 15).addBox(-1.5F, -7.0F, -7.0F, 3.0F, 3.0F, 3.0F, g)
                        .texOffs(20, 4).addBox(0.5F, -11.0F, -3.0F, 2.0F, 2.0F, 2.0F, g)
                        .texOffs(20, 0).addBox(-2.5F, -11.0F, -3.0F, 2.0F, 2.0F, 2.0F, g),
                PartPose.offset(0.0F, 12.0F, -4.0F));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(0, 45).addBox(-1.4F, -0.5F, -1.5F, 3.0F, 8.0F, 3.0F, g),
                PartPose.offset(-2.5F, 16.5F, 4.5F));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(12, 45).addBox(-1.6F, -0.5F, -1.5F, 3.0F, 8.0F, 3.0F, g),
                PartPose.offset(2.5F, 16.5F, 4.5F));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(0, 34).addBox(-1.4F, -0.5F, -1.5F, 3.0F, 8.0F, 3.0F, g),
                PartPose.offset(-2.5F, 16.5F, -3.5F));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(12, 34).addBox(-1.6F, -0.5F, -1.5F, 3.0F, 8.0F, 3.0F, g),
                PartPose.offset(2.5F, 16.5F, -3.5F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 15).addBox(-4.0F, -3.0F, -8.5F, 8.0F, 6.0F, 13.0F, g),
                PartPose.offset(0.0F, 14.0F, 2.5F));
        root.addOrReplaceChild("right_chest", CubeListBuilder.create().texOffs(45, 28).addBox(-3.0F, 0.0F, 0.0F, 8.0F, 8.0F, 3.0F, g),
                PartPose.offsetAndRotation(-8.5F, 4.0F, 3.0F, 0.0F, (float) (Math.PI / 2), 0.0F));
        root.addOrReplaceChild("left_chest", CubeListBuilder.create().texOffs(45, 41).addBox(-3.0F, 0.0F, 0.0F, 8.0F, 8.0F, 3.0F, g),
                PartPose.offsetAndRotation(5.5F, 4.0F, 3.0F, 0.0F, (float) (Math.PI / 2), 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    protected void setupAnim(EntityLlama e, float pos, float speed, float ageInTicks, float yRot, float xRot) {
        quadruped(head, rightHindLeg, leftHindLeg, rightFrontLeg, leftFrontLeg, pos, speed, yRot, xRot);
        // LlamaRenderer: hasChest = !isBaby && hasChest.
        rightChest.visible = false;
        leftChest.visible = false;
    }
}
