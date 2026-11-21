package com.exiledradio.gamedropsbackport.babies.client.model;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;

import com.google.common.collect.ImmutableSet;

/**
 * 26.3's baby humanoids on {@code HumanoidModel.setupAnim} with {@code AnimationUtils.animateZombieArms}:
 * {@code BabyZombieModel} (zombie, husk), {@code BabyZombieVillagerModel}, {@code BabyZombifiedPiglinModel}
 * (with its ear flap), and the baby armor meshes, which pose themselves the same way.
 * 1.12 has no swimming, elytra or two-handed poses; arms hold items with the {@code ITEM} pose.
 */
public final class BabyHumanoidModel extends BabyModel<EntityZombie> implements HeadedModel {

    private static final CubeDeformation BABY_OUTER_ARMOR_DEFORMATION = new CubeDeformation(-0.1F, 0.5F, 0.3F);
    private static final CubeDeformation BABY_INNER_ARMOR_DEFORMATION = new CubeDeformation(-0.1F, 0.3F, 0.3F);
    private static final CubeDeformation BABY_PIGLIN_ARMOR_DEFORMATION = new CubeDeformation(0.7F);
    private static final PartPose BABY_PIGLIN_ARMOR_ARM_OFFSET = new PartPose(0.5F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

    private final boolean piglin;
    private final boolean translucent;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart rightEar;
    private final ModelPart leftEar;

    private BabyHumanoidModel(LayerDefinition layer, boolean piglin, boolean translucent) {
        super(layer);
        this.piglin = piglin;
        this.translucent = translucent;
        head = root.getChild("head");
        body = root.getChild("body");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
        rightEar = piglin ? head.getChild("right_ear") : null;
        leftEar = piglin ? head.getChild("left_ear") : null;
    }

    public static BabyHumanoidModel zombie() {
        return new BabyHumanoidModel(createZombieLayer(CubeDeformation.NONE), false, false);
    }

    public static BabyHumanoidModel zombieVillager() {
        return new BabyHumanoidModel(createZombieVillagerLayer(), false, false);
    }

    public static BabyHumanoidModel zombifiedPiglin() {
        // AbstractPiglinModel renders translucent.
        return new BabyHumanoidModel(createPiglinLayer(), true, true);
    }

    /** {@code HumanoidModel.createBabyArmorMeshSet}: the model for one slot, humanoid or piglin. */
    public static BabyHumanoidModel armor(EntityEquipmentSlot slot, boolean piglin) {
        CubeDeformation outer = piglin ? BABY_PIGLIN_ARMOR_DEFORMATION : BABY_OUTER_ARMOR_DEFORMATION;
        CubeDeformation inner = piglin ? BABY_PIGLIN_ARMOR_DEFORMATION : BABY_INNER_ARMOR_DEFORMATION;
        PartPose armOffset = piglin ? BABY_PIGLIN_ARMOR_ARM_OFFSET : PartPose.ZERO;
        MeshDefinition mesh = createBabyArmorMesh(slot == EntityEquipmentSlot.LEGS ? inner : outer, armOffset);
        PartDefinition root = mesh.getRoot();
        switch (slot) {
            case HEAD:
                root.retainPartsAndChildren(ImmutableSet.of("head"));
                break;
            case CHEST:
                root.retainExactParts(ImmutableSet.of("body", "left_arm", "right_arm"));
                break;
            case LEGS:
                root.retainExactParts(ImmutableSet.of("left_leg", "right_leg", "waist"));
                break;
            default:
                root.retainExactParts(ImmutableSet.<String>of("left_foot", "right_foot"));
                break;
        }
        if (piglin) {
            // AbstractPiglinModel.removeEars
            PartDefinition head = root.getChild("head");
            head.addOrReplaceChild("left_ear", CubeListBuilder.create(), PartPose.ZERO);
            head.addOrReplaceChild("right_ear", CubeListBuilder.create(), PartPose.ZERO);
        }
        return new BabyHumanoidModel(LayerDefinition.create(mesh, 64, 64), piglin, false);
    }

    @Override
    public ModelPart getHead() {
        return head;
    }

    public ModelPart getArm(EnumHandSide side) {
        return side == EnumHandSide.LEFT ? leftArm : rightArm;
    }

    /** 26.3's {@code BabyZombieModel.createBodyLayer} (zombie and husk). */
    static LayerDefinition createZombieLayer(CubeDeformation g) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-2.0F, -2.5F, -1.0F, 4.0F, 5.0F, 2.0F, g),
                PartPose.offset(0.0F, 17.5F, 0.0F));
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(3, 3).addBox(-3.0F, -6.25F, -3.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(35, 3).addBox(-3.0F, -6.15F, -3.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.25F)),
                PartPose.offset(0.0F, 15.25F, 0.0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(36, 16).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 5.0F, 2.0F, g),
                PartPose.offset(-3.0F, 15.5F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(28, 16).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 5.0F, 2.0F, g),
                PartPose.offset(3.0F, 15.5F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(8, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, g),
                PartPose.offset(-1.0F, 20.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, g),
                PartPose.offset(1.0F, 20.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** 26.3's {@code BabyZombieVillagerModel.createBodyLayer}. */
    static LayerDefinition createZombieVillagerLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 15).addBox(-2.0F, -2.75F, -1.5F, 4.0F, 5.0F, 3.0F)
                        .texOffs(16, 22).addBox(-2.0F, -2.75F, -1.5F, 4.0F, 6.0F, 3.0F, new CubeDeformation(0.1F)),
                PartPose.offset(0.0F, 18.75F, 0.0F));
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -3.5F, 8.0F, 8.0F, 7.0F), PartPose.offset(0.0F, 16.0F, 0.0F));
        head.addOrReplaceChild("hat",
                CubeListBuilder.create().texOffs(0, 31).addBox(-4.0F, -4.0F, -3.5F, 8.0F, 8.0F, 7.0F, new CubeDeformation(0.3F)),
                PartPose.offset(0.0F, -4.0F, 0.0F));
        head.addOrReplaceChild("hat_rim", CubeListBuilder.create().texOffs(0, 46).addBox(-7.0F, -0.5F, -6.0F, 14.0F, 1.0F, 12.0F),
                PartPose.offset(0.0F, -4.5F, 0.0F));
        head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(23, 0).addBox(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, -1.0F, -4.0F));
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(24, 15).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-3.0F, 15.5F, 0.0F));
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(16, 15).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(3.0F, 15.5F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(8, 23).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(-1.0F, 21.5F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 23).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(1.0F, 21.5F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** 26.3's {@code BabyPiglinModel.createBodyLayer}, which {@code BabyZombifiedPiglinModel} uses. */
    static LayerDefinition createPiglinLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 13).addBox(-3.0F, -3.0F, -1.0F, 6.0F, 5.0F, 3.0F),
                PartPose.offset(0.0F, 18.0F, -0.5F));
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(21, 30).addBox(-1.5F, -3.0F, -4.5F, 3.0F, 3.0F, 1.0F)
                        .texOffs(0, 0).addBox(-4.5F, -6.0F, -3.5F, 9.0F, 6.0F, 7.0F),
                PartPose.offset(0.0F, 15.0F, 0.0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create(), PartPose.offset(4.2F, -4.0F, 0.0F));
        leftEar.addOrReplaceChild("left_ear_r1", CubeListBuilder.create().texOffs(0, 21).addBox(-0.5F, -3.0F, -2.0F, 1.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(1.0F, 1.75F, 0.0F, 0.0F, 0.0F, -0.6109F));
        PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create(), PartPose.offset(-4.2F, -4.0F, 0.0F));
        rightEar.addOrReplaceChild("right_ear_r1", CubeListBuilder.create().texOffs(18, 13).addBox(-0.5F, -3.0F, -2.0F, 1.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(-1.0F, 1.75F, 0.0F, 0.0F, 0.0F, 0.6109F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(28, 13).addBox(-1.0F, 0.0F, -1.5F, 2.0F, 5.0F, 3.0F),
                PartPose.offset(4.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(10, 30).addBox(-1.0F, 0.0F, -1.5F, 2.0F, 5.0F, 3.0F),
                PartPose.offset(-4.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(22, 23).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F),
                PartPose.offset(-1.5F, 20.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(10, 23).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F),
                PartPose.offset(1.5F, 20.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** 26.3's {@code HumanoidModel.createBabyArmorMesh}, part names as the jar has them. */
    private static MeshDefinition createBabyArmorMesh(CubeDeformation g, PartPose armOffset) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -7.0F, -4.5F, 9.0F, 8.0F, 8.0F, g), PartPose.offset(0.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 17).addBox(-3.0F, -3.0F, -1.5F, 6.0F, 5.0F, 3.0F, g),
                PartPose.offset(0.0F, 18.0F, 0.0F));
        root.addOrReplaceChild("waist", CubeListBuilder.create().texOffs(0, 36).addBox(-3.0F, -1.2F, -1.49F, 5.9F, 2.0F, 2.9F, g.extend(-0.1F)),
                PartPose.offset(0.0F, 19.0F, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(30, 25).addBox(-1.0F, 0.0F, -1.53F, 2.0F, 5.0F, 3.0F, g),
                PartPose.offset(-3.5F - armOffset.x, 15.5F + armOffset.y, 0.0F + armOffset.z));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(30, 17).addBox(-1.0F, 0.0F, -1.53F, 2.0F, 5.0F, 3.0F, g),
                PartPose.offset(3.5F + armOffset.x, 15.5F + armOffset.y, 0.0F + armOffset.z));
        root.addOrReplaceChild("inner_body", CubeListBuilder.create().texOffs(0, 17).addBox(-3.0F, -3.0F, -1.5F, 6.0F, 5.0F, 3.0F, g),
                PartPose.offset(0.0F, 18.0F, 0.0F));
        PartDefinition leftLegDef = root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(18, 24).addBox(-2.0F, -0.2F, -2.0F, 3.0F, 4.0F, 3.0F, g.extend(-0.1F)),
                PartPose.offset(1.5F, 20.0F, 0.5F));
        PartDefinition rightLegDef = root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(18, 17).addBox(-1.0F, -0.2F, -2.0F, 3.0F, 4.0F, 3.0F, g.extend(-0.1F)),
                PartPose.offset(-1.5F, 20.0F, 0.5F));
        // As in the jar, the left leg carries "right_foot" and the right leg "left_foot".
        leftLegDef.addOrReplaceChild("right_foot", CubeListBuilder.create().texOffs(0, 25).addBox(-2.0F, 2.9F, -2.0F, 3.0F, 1.0F, 3.0F, g),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        rightLegDef.addOrReplaceChild("left_foot",
                CubeListBuilder.create().texOffs(0, 29).mirror().addBox(-1.0F, 2.9F, -2.0F, 3.0F, 1.0F, 3.0F, g).mirror(false),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        return mesh;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch, float scale) {
        if (translucent) {
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        }
        super.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        if (translucent) {
            GlStateManager.disableBlend();
        }
    }

    @Override
    protected void setupAnim(EntityZombie e, float pos, float speed, float ageInTicks, float yRot, float xRot) {
        head.xRot = xRot * RAD;
        head.yRot = yRot * RAD;
        rightArm.xRot = cos(pos * 0.6662F + (float) Math.PI) * 2.0F * speed * 0.5F;
        leftArm.xRot = cos(pos * 0.6662F) * 2.0F * speed * 0.5F;
        rightLeg.xRot = cos(pos * 0.6662F) * 1.4F * speed;
        leftLeg.xRot = cos(pos * 0.6662F + (float) Math.PI) * 1.4F * speed;
        rightLeg.yRot = 0.005F;
        leftLeg.yRot = -0.005F;
        rightLeg.zRot = 0.005F;
        leftLeg.zRot = -0.005F;
        if (e.isRiding()) {
            rightArm.xRot += (float) (-Math.PI / 5);
            leftArm.xRot += (float) (-Math.PI / 5);
            rightLeg.xRot = -1.4137167F;
            rightLeg.yRot = (float) (Math.PI / 10);
            rightLeg.zRot = 0.07853982F;
            leftLeg.xRot = -1.4137167F;
            leftLeg.yRot = (float) (-Math.PI / 10);
            leftLeg.zRot = -0.07853982F;
        }
        EnumHandSide mainArm = e.getPrimaryHand();
        poseArm(rightArm, !handItem(e, EnumHandSide.RIGHT, mainArm).isEmpty());
        poseArm(leftArm, !handItem(e, EnumHandSide.LEFT, mainArm).isEmpty());
        setupAttackAnimation(e, mainArm);
        if (e.isSneaking()) {
            body.xRot = 0.5F;
            rightArm.xRot += 0.4F;
            leftArm.xRot += 0.4F;
            rightLeg.z += 4.0F;
            leftLeg.z += 4.0F;
            head.y += 4.2F;
            body.y += 3.2F;
            leftArm.y += 3.2F;
            rightArm.y += 3.2F;
        }
        bobModelPart(rightArm, ageInTicks, 1.0F);
        bobModelPart(leftArm, ageInTicks, -1.0F);
        if (piglin) {
            // AbstractPiglinModel.setupAnim, BabyZombifiedPiglinModel's 5 degree ears.
            float defaultAngle = 5.0F * RAD;
            float frequency = ageInTicks * 0.1F + pos * 0.5F;
            float amplitude = 0.08F + speed * 0.4F;
            leftEar.zRot = -defaultAngle - cos(frequency * 1.2F) * amplitude;
            rightEar.zRot = defaultAngle + cos(frequency) * amplitude;
        }
    }

    static net.minecraft.item.ItemStack handItem(EntityZombie e, EnumHandSide side, EnumHandSide mainArm) {
        return side == mainArm ? e.getHeldItemMainhand() : e.getHeldItemOffhand();
    }

    /** {@code ArmPose.EMPTY} or {@code ArmPose.ITEM}. */
    private static void poseArm(ModelPart arm, boolean holding) {
        if (holding) {
            arm.xRot = arm.xRot * 0.5F - (float) (Math.PI / 10);
        }
        arm.yRot = 0.0F;
    }

    /** {@code HumanoidModel.setupAttackAnimation} (a plain swing), then {@code AnimationUtils.animateZombieArms}. */
    private void setupAttackAnimation(EntityZombie e, EnumHandSide mainArm) {
        float swing = swingProgress;
        if (swing > 0.0F) {
            EnumHand hand = e.swingingHand == null ? EnumHand.MAIN_HAND : e.swingingHand;
            EnumHandSide swingArm = hand == EnumHand.MAIN_HAND ? mainArm : mainArm.opposite();
            body.yRot = sin(MathHelper.sqrt(swing) * (float) (Math.PI * 2)) * 0.2F;
            if (swingArm == EnumHandSide.LEFT) {
                body.yRot *= -1.0F;
            }
            rightArm.z = sin(body.yRot) * 5.0F * AGE_SCALE;
            rightArm.x = -cos(body.yRot) * 5.0F * AGE_SCALE;
            leftArm.z = -sin(body.yRot) * 5.0F * AGE_SCALE;
            leftArm.x = cos(body.yRot) * 5.0F * AGE_SCALE;
            rightArm.yRot += body.yRot;
            leftArm.yRot += body.yRot;
            leftArm.xRot += body.yRot;
            float eased = 1.0F - (1.0F - swing) * (1.0F - swing) * (1.0F - swing) * (1.0F - swing);
            float aa = sin(eased * (float) Math.PI);
            float bb = sin(swing * (float) Math.PI) * -(head.xRot - 0.7F) * 0.75F;
            ModelPart attackArm = getArm(swingArm);
            attackArm.xRot -= aa * 1.2F + bb;
            attackArm.yRot += body.yRot * 2.0F;
            attackArm.zRot += sin(swing * (float) Math.PI) * -0.4F;
        }
        boolean raiseArms = e.getHeldItemMainhand().isEmpty();
        float armDrop = raiseArms ? (float) -Math.PI / (e.isArmsRaised() ? 1.5F : 2.25F) : 0.0F;
        float attackYRotModifier = (raiseArms ? 1.0F : -1.0F) * sin(swing * (float) Math.PI);
        float attackXRotModifier = sin((1.0F - (1.0F - swing) * (1.0F - swing)) * (float) Math.PI);
        float xRotArms = armDrop + attackYRotModifier * 1.2F - attackXRotModifier * 0.4F;
        float yRotArms = 0.1F - attackYRotModifier * 0.6F;
        rightArm.xRot = xRotArms;
        rightArm.yRot = raiseArms ? -yRotArms : yRotArms;
        rightArm.zRot = 0.0F;
        leftArm.xRot = xRotArms;
        leftArm.yRot = raiseArms ? yRotArms : -yRotArms;
        leftArm.zRot = 0.0F;
    }

    private static void bobModelPart(ModelPart part, float ageInTicks, float scale) {
        part.zRot += scale * (cos(ageInTicks * 0.09F) * 0.05F + 0.05F);
        part.xRot += scale * sin(ageInTicks * 0.067F) * 0.05F;
    }
}
