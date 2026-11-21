package com.exiledradio.gamedropsbackport.babies.client.model;

import net.minecraft.entity.EntityLivingBase;

/** 26.3's {@code QuadrupedModel} for its baby layers: head, body and four legs. */
public class BabyQuadrupedModel<T extends EntityLivingBase> extends BabyModel<T> {

    protected final ModelPart head;
    protected final ModelPart body;
    protected final ModelPart rightHindLeg;
    protected final ModelPart leftHindLeg;
    protected final ModelPart rightFrontLeg;
    protected final ModelPart leftFrontLeg;

    public BabyQuadrupedModel(LayerDefinition layer) {
        super(layer);
        head = root.getChild("head");
        body = root.getChild("body");
        rightHindLeg = root.getChild("right_hind_leg");
        leftHindLeg = root.getChild("left_hind_leg");
        rightFrontLeg = root.getChild("right_front_leg");
        leftFrontLeg = root.getChild("left_front_leg");
    }

    @Override
    protected void setupAnim(T entity, float pos, float speed, float ageInTicks, float yRot, float xRot) {
        quadruped(head, rightHindLeg, leftHindLeg, rightFrontLeg, leftFrontLeg, pos, speed, yRot, xRot);
    }

    /** 26.3's {@code BabyCowModel.createBodyLayer} (cow and mooshroom). */
    public static LayerDefinition cow() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 18).addBox(-3.0F, -4.569F, -4.8333F, 6.0F, 6.0F, 5.0F)
                        .texOffs(8, 29).addBox(3.0F, -5.569F, -3.8333F, 1.0F, 2.0F, 1.0F)
                        .texOffs(4, 29).mirror().addBox(-4.0F, -5.569F, -3.8333F, 1.0F, 2.0F, 1.0F).mirror(false)
                        .texOffs(12, 29).addBox(-2.0F, -1.569F, -5.8333F, 4.0F, 3.0F, 1.0F),
                PartPose.offset(0.0F, 13.569F, -5.1667F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -7.0F, -1.0F, 8.0F, 6.0F, 12.0F),
                PartPose.offset(3.0F, 19.0F, -5.0F));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(22, 18).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
                PartPose.offset(-2.5F, 18.0F, -3.5F));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(34, 18).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
                PartPose.offset(2.5F, 18.0F, -3.5F));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(22, 27).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
                PartPose.offset(-2.5F, 18.0F, 3.5F));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(34, 27).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
                PartPose.offset(2.5F, 18.0F, 3.5F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** 26.3's {@code BabyPigModel.createBodyLayer(CubeDeformation.NONE)}. */
    public static LayerDefinition pig() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -3.0F, -4.5F, 7.0F, 6.0F, 9.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 19.0F, 0.5F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 15).addBox(-3.5F, -5.0F, -5.0F, 7.0F, 6.0F, 6.0F, new CubeDeformation(0.025F))
                        .texOffs(6, 27).addBox(-1.5F, -1.975F, -6.0F, 3.0F, 2.0F, 1.0F, new CubeDeformation(0.015F)),
                PartPose.offset(0.0F, 19.0F, -2.0F));
        root.addOrReplaceChild("left_front_leg",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(2.5F, 22.0F, -3.0F));
        root.addOrReplaceChild("right_front_leg",
                CubeListBuilder.create().texOffs(23, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-2.5F, 22.0F, -3.0F));
        root.addOrReplaceChild("left_hind_leg",
                CubeListBuilder.create().texOffs(0, 4).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(2.5F, 22.0F, 4.0F));
        root.addOrReplaceChild("right_hind_leg",
                CubeListBuilder.create().texOffs(23, 4).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-2.5F, 22.0F, 4.0F));
        return LayerDefinition.create(mesh, 32, 32);
    }

    /** 26.3's {@code BabySheepModel.createBodyLayer}, also its wool layer's geometry. */
    public static LayerDefinition sheep() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 10).addBox(-3.0F, -2.0F, -4.5F, 6.0F, 4.0F, 9.0F),
                PartPose.offset(0.0F, 17.0F, 0.5F));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -4.5F, -3.5F, 5.0F, 5.0F, 5.0F),
                PartPose.offset(0.0F, 15.5F, -2.5F));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(0, 23).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offset(-2.0F, 19.0F, 3.0F));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(24, 12).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offset(2.0F, 19.0F, 3.0F));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(8, 23).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offset(-2.0F, 19.0F, -2.0F));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(24, 5).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offset(2.0F, 19.0F, -2.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** 26.3's {@code BabyPolarBearModel.createBodyLayer}. */
    public static LayerDefinition polarBear() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 9).addBox(-4.0F, -3.5F, -6.0F, 8.0F, 7.0F, 12.0F),
                PartPose.offset(0.0F, 17.5F, 0.0F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.0F, -2.625F, -4.25F, 6.0F, 5.0F, 4.0F)
                        .texOffs(20, 3).addBox(-2.0F, 0.375F, -6.25F, 4.0F, 2.0F, 2.0F)
                        .texOffs(20, 0).addBox(-4.0F, -3.625F, -2.75F, 2.0F, 2.0F, 1.0F)
                        .texOffs(26, 0).addBox(2.0F, -3.625F, -2.75F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 18.625F, -5.75F));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(0, 34).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(-2.5F, 21.5F, 4.5F));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(12, 34).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(2.5F, 21.5F, 4.5F));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(0, 28).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(-2.5F, 21.5F, -4.5F));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(12, 28).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(2.5F, 21.5F, -4.5F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
