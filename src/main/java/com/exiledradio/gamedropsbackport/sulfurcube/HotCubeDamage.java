package com.exiledradio.gamedropsbackport.sulfurcube;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import javax.annotation.Nullable;

/**
 * 26.2's {@code sulfur_cube_hot} damage, as the hot archetype deals it on contact (26.3's
 * {@code SulfurCube.applyContactDamage}, {@code attribute_to_source} false: the cube is the source only when it hurts a
 * player). The damage type's tags, for 1.12.2:
 * <ul>
 * <li>{@code is_fire}: fire damage, so fire-immune mobs (blazes, wither skeletons, ...) and Fire Resistance take none,
 * and Fire Protection reduces it;</li>
 * <li>{@code bypasses_shield}: no damage location, so 1.12.2's shield check never blocks it (armor still counts, as
 * {@code bypasses_armor} does not hold it);</li>
 * <li>{@code no_knockback}, {@code no_wolf_retaliation}: no attacker is handed to the victim, so it is not knocked
 * back and a tamed wolf does not turn on the cube;</li>
 * <li>{@code burn_from_stepping}: Frost Walker boots make their wearer immune (26.2's Frost Walker
 * {@code damage_immunity}), as they do on 1.12.2's magma blocks;</li>
 * <li>{@code panic_environmental_causes}: a mob that panics (one with 1.12.2's {@link EntityAIPanic}) runs off, as
 * 26.2's {@code PanicGoal} does.</li>
 * </ul>
 * The damage type's {@code scaling} ({@code when_caused_by_living_non_player}) scales a player's damage with
 * difficulty, as an {@link EntityDamageSource} from the cube does; its exhaustion, 0.1, is 1.12.2's default.
 */
public final class HotCubeDamage {

    private HotCubeDamage() {
    }

    /** The player's source: from the cube (death message, difficulty scaling), with no attacker or location. */
    static final class FromCube extends EntityDamageSource {
        FromCube(Entity cube) {
            super(SulfurCubes.HOT_DAMAGE, cube);
            this.setFireDamage();
        }

        /** {@code no_knockback}, {@code no_wolf_retaliation}: no attacker for knockback, revenge or Thorns. */
        @Override
        @Nullable
        public Entity getTrueSource() {
            return null;
        }

        @Override
        @Nullable
        public Entity getImmediateSource() {
            return null;
        }

        /** {@code bypasses_shield}: 1.12.2's shield blocks only damage that has a location. */
        @Override
        @Nullable
        public Vec3d getDamageLocation() {
            return null;
        }

        /** The cube, for the death message and difficulty scaling (which read the field, not the getters). */
        public Entity cube() {
            return this.damageSourceEntity;
        }
    }

    /** The source the hot archetype hurts {@code target} with. */
    public static DamageSource source(Entity cube, Entity target) {
        return target instanceof EntityPlayer ? new FromCube(cube) : new DamageSource(SulfurCubes.HOT_DAMAGE).setFireDamage();
    }

    /**
     * Fire-immune mobs ({@code is_fire}: 26.2's {@code isInvulnerableTo} for a {@code fireImmune()} type; 1.12.2's
     * {@code attackEntityFrom} leaves that to the caller, as its magma block does) and Frost Walker boots
     * ({@code burn_from_stepping}), both as 1.12.2's magma block asks.
     */
    public static boolean immune(Entity target) {
        return target.isImmuneToFire()
                || target instanceof EntityLivingBase && EnchantmentHelper.hasFrostWalkerEnchantment((EntityLivingBase) target);
    }

    /** Contact damage: 1, unless Frost Walker protects; a mob that was hurt and can panic runs. @return hurt */
    public static boolean hurt(Entity cube, Entity target) {
        if (immune(target)) {
            return false;
        }
        boolean hurt = target.attackEntityFrom(source(cube, target), 1.0F);
        if (hurt && target instanceof EntityCreature) {
            panic((EntityCreature) target);
        }
        return hurt;
    }

    /**
     * 26.2's {@code PanicGoal} on {@code panic_environmental_causes}: 1.12.2's {@link EntityAIPanic} panics only with
     * an attacker or while burning, so its run is started here: a random spot 5 blocks out, 4 up or down (its
     * {@code findRandomPosition}), at the goal's own speed. @return whether the mob was sent running
     */
    public static boolean panic(EntityCreature mob) {
        EntityAIPanic goal = panicGoal(mob);
        if (goal == null || mob.isAIDisabled()) {
            return false;
        }
        Vec3d to = RandomPositionGenerator.findRandomTarget(mob, 5, 4);
        if (to == null) {
            return false;
        }
        double speed = ReflectionHelper.<Double, EntityAIPanic>getPrivateValue(EntityAIPanic.class, goal, "speed", "field_75265_b");
        return mob.getNavigator().tryMoveToXYZ(to.x, to.y, to.z, speed);
    }

    @Nullable
    public static EntityAIPanic panicGoal(EntityLiving mob) {
        for (EntityAITasks.EntityAITaskEntry entry : mob.tasks.taskEntries) {
            EntityAIBase action = entry.action;
            if (action instanceof EntityAIPanic) {
                return (EntityAIPanic) action;
            }
        }
        return null;
    }
}
