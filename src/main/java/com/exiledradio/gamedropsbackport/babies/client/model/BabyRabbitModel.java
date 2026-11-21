package com.exiledradio.gamedropsbackport.babies.client.model;

import net.minecraft.entity.passive.EntityRabbit;

import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

/**
 * 26.3's {@code BabyRabbitModel} with {@code RabbitModel.setupAnim}: the keyframed hop and idle head tilt. 1.12's
 * rabbit has no animation states, so {@code Rabbit.setupAnimationStates} runs here, once per entity tick seen.
 */
public final class BabyRabbitModel extends BabyModel<EntityRabbit> {

    private static final Random RANDOM = new Random();

    private final ModelPart head;
    private final KeyframeAnimation hopAnimation;
    private final KeyframeAnimation idleHeadTiltAnimation;
    private final Map<EntityRabbit, States> states = new WeakHashMap<EntityRabbit, States>();

    /** A rabbit's client animation state ({@code Rabbit}'s fields). */
    private static final class States {
        final AnimationState hop = new AnimationState();
        final AnimationState idleHeadTilt = new AnimationState();
        int idleAnimationTimeout;
        int lastTick = Integer.MIN_VALUE;
    }

    public BabyRabbitModel() {
        super(createBodyLayer());
        head = root.getChild("body").getChild("head");
        hopAnimation = BabyRabbitAnimation.HOP.bake(root);
        idleHeadTiltAnimation = BabyRabbitAnimation.IDLE_HEAD_TILT.bake(root);
    }

    static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 23.0F, 1.6F));
        body.addOrReplaceChild("body_r1", CubeListBuilder.create().texOffs(0, 8).addBox(-2.0F, -2.0F, -3.0F, 4.0F, 3.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, -2.0F, -1.6F, -0.5236F, 0.0F, 0.0F));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offset(0.0F, -2.2F, 2.0F));
        tail.addOrReplaceChild("tail_r1", CubeListBuilder.create().texOffs(0, 21).addBox(-1.4F, -2.0268F, -1.0177F, 3.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(-0.1F, 0.0F, 0.0F, -0.5236F, 0.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -3.0F, -3.0F, 5.0F, 4.0F, 4.0F), PartPose.offset(0.0F, -5.0F, -2.6F));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(18, 0).addBox(-1.0F, -3.5F, -0.5F, 2.0F, 4.0F, 1.0F),
                PartPose.offset(-1.5F, -3.5F, -0.5F));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(24, 0).addBox(-1.0F, -3.5F, -0.5F, 2.0F, 4.0F, 1.0F),
                PartPose.offset(1.5F, -3.5F, -0.5F));
        PartDefinition frontLegs = body.addOrReplaceChild("frontlegs", CubeListBuilder.create(), PartPose.offset(0.0F, -2.5F, -2.6F));
        PartDefinition leftFrontLeg = frontLegs.addOrReplaceChild("left_front_leg", CubeListBuilder.create(),
                PartPose.offsetAndRotation(1.0F, 1.0F, -0.5F, 0.3927F, 0.0F, 0.0F));
        leftFrontLeg.addOrReplaceChild("left_front_leg_r1",
                CubeListBuilder.create().texOffs(18, 8).addBox(-0.5F, -1.5F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 1.0F, 0.0F, -0.3927F, 0.0F, 0.0F));
        PartDefinition rightFrontLeg = frontLegs.addOrReplaceChild("right_front_leg", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-1.0F, 1.0F, -0.5F, 0.3927F, 0.0F, 0.0F));
        rightFrontLeg.addOrReplaceChild("right_front_leg_r1",
                CubeListBuilder.create().texOffs(14, 8).addBox(-0.5F, -1.5F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 1.0F, 0.0F, -0.3927F, 0.0F, 0.0F));
        PartDefinition backLegs = root.addOrReplaceChild("backlegs", CubeListBuilder.create(), PartPose.offset(0.0F, 23.0F, 2.0F));
        PartDefinition leftBackLeg = backLegs.addOrReplaceChild("left_hind_leg", CubeListBuilder.create(),
                PartPose.offsetAndRotation(1.5F, 0.5F, 0.5F, 0.0F, 3.1416F, 0.0F));
        leftBackLeg.addOrReplaceChild("left_haunch", CubeListBuilder.create().texOffs(10, 17).addBox(-2.0F, -0.5F, 0.0F, 2.0F, 1.0F, 3.0F),
                PartPose.offsetAndRotation(1.0F, 0.0F, 0.5F, 0.0F, -0.7854F, 0.0F));
        PartDefinition rightBackLeg = backLegs.addOrReplaceChild("right_hind_leg", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-1.5F, 0.5F, 0.5F, 0.0F, 3.1416F, 0.0F));
        rightBackLeg.addOrReplaceChild("right_haunch", CubeListBuilder.create().texOffs(0, 17).addBox(-2.0F, -0.5F, 0.0F, 2.0F, 1.0F, 3.0F),
                PartPose.offsetAndRotation(0.5F, 0.0F, -0.9F, 0.0F, 0.7854F, 0.0F));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    protected void setupAnim(EntityRabbit e, float pos, float speed, float ageInTicks, float yRot, float xRot) {
        States s = tick(e);
        if (!s.idleHeadTilt.isStarted()) {
            head.yRot = yRot * RAD;
            head.xRot = xRot * RAD;
        }
        hopAnimation.apply(s.hop, ageInTicks);
        idleHeadTiltAnimation.apply(s.idleHeadTilt, ageInTicks);
    }

    /** {@code Rabbit.setupAnimationStates}, from the client's {@code baseTick}. */
    private States tick(EntityRabbit e) {
        States s = states.get(e);
        if (s == null) {
            s = new States();
            states.put(e, s);
        }
        int now = e.ticksExisted;
        if (s.lastTick == now) {
            return s;
        }
        int elapsed = s.lastTick == Integer.MIN_VALUE ? 1 : Math.max(1, now - s.lastTick);
        s.lastTick = now;
        boolean jumping = e.getJumpCompletion(0.0F) > 0.0F;
        if (s.idleAnimationTimeout <= 0 && !e.getLeashed() && !e.isAIDisabled()) {
            s.idleAnimationTimeout = RANDOM.nextInt(40) + 180;
            s.idleHeadTilt.start(now);
        } else if (jumping) {
            s.hop.startIfStopped(now);
            s.idleHeadTilt.stop();
        } else {
            s.idleAnimationTimeout -= elapsed;
            s.hop.stop();
        }
        if (e.getLeashed()) {
            // Rabbit.setLeashData stops the tilt.
            s.idleHeadTilt.stop();
        }
        return s;
    }
}
