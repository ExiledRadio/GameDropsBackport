package com.exiledradio.gamedropsbackport.changes;

import com.exiledradio.gamedropsbackport.ModConfig;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.Random;

/**
 * What this mod's patches call ({@code docs/PATCHES.md}, P1-P5). Each hook gives 1.12.2's behavior while its setting is
 * off, so a patched class with the stream switched off works as vanilla's.
 */
public final class ChangeHooks {

    /** 1.12.2's bed restitution ({@code BlockBed.onLanded}). */
    static final double OLD_BED_BOUNCE = 0.6600000262260437D;
    /** 26.2's {@code bounceRestitution(0.75F)} for beds, as the float it is. */
    static final double BED_BOUNCE = 0.75F;

    private static final Random RANDOM = new Random();
    private static Field idleTime;

    private ChangeHooks() {
    }

    // P1 -------------------------------------------------------------------------------------------

    /** P1: the constant {@code BlockBed.onLanded} sends a falling entity back up with. */
    public static double bedBounce() {
        return Changes.on(ModConfig.changes.bedBounce) ? BED_BOUNCE : OLD_BED_BOUNCE;
    }

    // P2 -------------------------------------------------------------------------------------------

    /**
     * P2, at the head of {@code EntityEnderman.attackEntityFrom}: true when a riding enderman is hit by a projectile.
     * The patch then runs {@code EntityMob.attackEntityFrom} (26.3's {@code super.hurtServer}) instead of 1.12.2's 64
     * teleport tries, so the enderman is hurt. Other damage needs nothing here: a riding enderman's teleports are all
     * refused ({@link Changes#onEnderTeleport}).
     */
    public static boolean endermanTakesProjectile(EntityEnderman enderman, DamageSource source) {
        return Changes.on(ModConfig.changes.endermenRiding) && enderman.isRiding()
                && source instanceof EntityDamageSourceIndirect;
    }

    // P3 -------------------------------------------------------------------------------------------

    /**
     * P3, at the head of {@code EntityLiving.despawnEntity}: 26.3's {@code checkDespawn} for a persistent mob. 1.12.2
     * zeroes a persistent mob's idle time every tick, so it always wanders; 26.3 zeroes it only while a player is within
     * 32 blocks, so with nobody near it counts up and {@code EntityAIWander} (idle time 100 or more) stops.
     *
     * @return true to skip 1.12.2's own body (only for a persistent mob, while the setting is on)
     */
    public static boolean persistentDespawn(EntityLiving mob) {
        boolean custom = !mob.isNoDespawnRequired() && customPersistence(mob);
        if (!mob.isNoDespawnRequired() && !custom) {
            return false;
        }
        if (!Changes.on(ModConfig.changes.persistentMobsIdle)) {
            if (custom) {
                // 1.12.2's own persistent branch, for a mob 26.x keeps without the flag.
                setIdleTime(mob, 0);
                return true;
            }
            return false;
        }
        EntityPlayer player = mob.world.getClosestPlayerToEntity(mob, -1.0D);
        if (player != null && player.getDistanceSq(mob) < 32.0D * 32.0D) {
            setIdleTime(mob, 0);
        }
        return true;
    }

    /**
     * 26.1's {@code Mob.requiresCustomPersistence}: a mob on a lead or riding something never despawns (1.12.2 let a
     * leashed squid, ocelot or modded mob go, and a mob in a boat).
     */
    public static boolean customPersistence(EntityLiving mob) {
        return Changes.on(ModConfig.changes.leashedMobsStay) && (mob.getLeashed() || mob.isRiding());
    }

    static void setIdleTime(EntityLivingBase mob, int value) {
        try {
            if (idleTime == null) {
                idleTime = ReflectionHelper.findField(EntityLivingBase.class, "idleTime", "field_70708_bq");
            }
            idleTime.setInt(mob, value);
        } catch (Exception ignored) {
        }
    }

    // P4 -------------------------------------------------------------------------------------------

    /**
     * P4, in place of {@code EntityWitch.attackEntityWithRangedAttack}'s {@code shoot}: 26.1 throws at 0.45 instead
     * of 0.75 when the target is 2 blocks away or closer (horizontally), so the potion no longer flies over it.
     * {@code x} and {@code z} are the aim's horizontal offsets, from which 1.12.2 also takes the distance.
     */
    public static void witchShoot(EntityThrowable potion, double x, double y, double z, float velocity, float inaccuracy) {
        if (Changes.on(ModConfig.changes.witchCloseThrows) && Math.sqrt(x * x + z * z) <= 2.0D) {
            velocity = 0.45F;
        }
        potion.shoot(x, y, z, velocity, inaccuracy);
    }

    // P5 -------------------------------------------------------------------------------------------

    /**
     * P5, in place of the {@code setItemDamage} 1.12.2's zombies and skeletons use on the helmet that shields them from
     * the sun: 26.3's {@code hurtAndBreak}, so Unbreaking can save the point. Breaking is left to the code after the
     * call, which checks the damage as before.
     */
    public static void sunDamage(ItemStack helmet, int damage) {
        int amount = damage - helmet.getItemDamage();
        if (amount <= 0 || !Changes.on(ModConfig.changes.sunDamageEnchantments)) {
            helmet.setItemDamage(damage);
            return;
        }
        helmet.attemptDamageItem(amount, RANDOM, null);
    }
}
