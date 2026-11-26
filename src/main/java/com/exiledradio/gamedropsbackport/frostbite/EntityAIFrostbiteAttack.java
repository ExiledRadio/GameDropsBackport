package com.exiledradio.gamedropsbackport.frostbite;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIZombieAttack;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/**
 * The frostbite's two attack goals, as 26.4's {@code RangedZombie}: the ranged goal (priority 1) runs while
 * {@link #shouldDoRangedAttack} holds, the melee goal ({@link Melee}, priority 2) otherwise.
 *
 * <h2>Ranged ({@code RangedZombieRangedAttackGoal}, a {@code RangedAttackGoal})</h2>
 * Speed 1.0, radius 16, keeps walking toward the target ({@code stopWhenInRange} false). The first throw waits
 * {@code floor(lerp(dist/16, min, max))} ticks, each later one {@code floor(dist/16 x (max - min) + min)}; a throw
 * needs line of sight.
 *
 * <h2>When ranged ({@code shouldDoRangedAttack})</h2>
 * An ice ball in the main hand and a living target within 16 blocks; then ranged from 5 blocks out (3 once it is
 * ranged already), and closer only when the target is out of melee reach and the path toward it stops short of it.
 */
public class EntityAIFrostbiteAttack extends EntityAIBase {

    public static final double MELEE_MODE_DISTANCE_SQR = 9.0D;
    public static final double RANGED_MODE_DISTANCE_SQR = 25.0D;

    private final EntityFrostbite mob;
    private EntityLivingBase target;
    private int attackTime = -1;
    private int seeTime;
    private boolean ranged;

    public EntityAIFrostbiteAttack(EntityFrostbite mob) {
        this.mob = mob;
        setMutexBits(3);
    }

    public boolean isRanged() {
        return this.ranged;
    }

    boolean shouldDoRangedAttack() {
        if (!this.mob.holdsIceBall()) {
            return false;
        }
        EntityLivingBase t = this.mob.getAttackTarget();
        if (t == null || !t.isEntityAlive()) {
            return false;
        }
        double d = this.mob.getDistanceSq(t);
        if (d > EntityFrostbite.MAX_RANGE * EntityFrostbite.MAX_RANGE) {
            return false;
        }
        if (d >= (this.ranged ? MELEE_MODE_DISTANCE_SQR : RANGED_MODE_DISTANCE_SQR)) {
            return true;
        }
        if (d <= meleeReachSqr(t)) {
            return false;
        }
        Path path = this.mob.getNavigator().getPath() != null ? this.mob.getNavigator().getPath() : this.mob.getNavigator().getPathToEntityLiving(t);
        if (path == null || path.getFinalPathPoint() == null) {
            return false;
        }
        BlockPos end = new BlockPos(path.getFinalPathPoint().x, path.getFinalPathPoint().y, path.getFinalPathPoint().z);
        // !canReach(): 1.12.2's path keeps no target, so its end is compared with the target's block.
        return end.distanceSq(new BlockPos(t)) > 1.0D;
    }

    private double meleeReachSqr(EntityLivingBase t) {
        return this.mob.width * 2.0F * this.mob.width * 2.0F + t.width;
    }

    @Override
    public boolean shouldExecute() {
        EntityLivingBase t = this.mob.getAttackTarget();
        if (t == null || !t.isEntityAlive()) {
            return false;
        }
        this.target = t;
        return shouldDoRangedAttack();
    }

    @Override
    public boolean shouldContinueExecuting() {
        return (shouldExecute() || this.target.isEntityAlive() && !this.mob.getNavigator().noPath()) && shouldDoRangedAttack();
    }

    @Override
    public void startExecuting() {
        this.ranged = true;
    }

    @Override
    public void resetTask() {
        this.target = null;
        this.seeTime = 0;
        this.attackTime = -1;
        this.ranged = false;
    }

    @Override
    public void updateTask() {
        double distSqr = this.mob.getDistanceSq(this.target.posX, this.target.getEntityBoundingBox().minY, this.target.posZ);
        boolean sees = this.mob.getEntitySenses().canSee(this.target);
        this.seeTime = sees ? this.seeTime + 1 : 0;
        this.mob.getNavigator().tryMoveToEntityLiving(this.target, 1.0D);
        this.mob.getLookHelper().setLookPositionWithEntity(this.target, 30.0F, 30.0F);
        float radius = EntityFrostbite.MAX_RANGE;
        int min = this.mob.intervalMin(), max = this.mob.intervalMax();
        if (--this.attackTime == 0) {
            if (!sees) {
                return;
            }
            float dist = MathHelper.sqrt(distSqr) / radius;
            this.mob.attackEntityWithRangedAttack(this.target, MathHelper.clamp(dist, 0.1F, 1.0F));
            this.attackTime = MathHelper.floor(dist * (max - min) + min);
        } else if (this.attackTime < 0) {
            double f = Math.sqrt(distSqr) / radius;
            this.attackTime = MathHelper.floor(min + f * (max - min));
        }
    }

    /** {@code RangedZombieMeleeAttackGoal}: the zombie's attack (arms raised) every {@value EntityFrostbite#MELEE_ATTACK_TICKS} ticks. */
    public static class Melee extends EntityAIZombieAttack {
        private final EntityFrostbite frostbite;

        public Melee(EntityFrostbite mob) {
            super(mob, 1.0D, false);
            this.frostbite = mob;
        }

        @Override
        protected void checkAndPerformAttack(EntityLivingBase enemy, double distToEnemySqr) {
            double reach = getAttackReachSqr(enemy);
            if (distToEnemySqr <= reach && this.attackTick <= 0) {
                this.attackTick = EntityFrostbite.MELEE_ATTACK_TICKS;
                this.attacker.swingArm(EnumHand.MAIN_HAND);
                this.attacker.attackEntityAsMob(enemy);
            }
        }
    }
}
