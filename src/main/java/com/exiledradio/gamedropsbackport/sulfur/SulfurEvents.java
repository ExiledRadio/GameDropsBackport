package com.exiledradio.gamedropsbackport.sulfur;

import com.exiledradio.gamedropsbackport.GameDropsBackport;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * What sulfur spikes do outside their block: tridents break them, and a falling tip hurts as 26.3's falling
 * stalactite. Registered on the event bus only when the stream is on.
 */
public final class SulfurEvents {

    /** 26.3's {@code falling_stalactite}: as an anvil, a helmet takes a quarter of it and wears for it. */
    public static final DamageSource FALLING_STALACTITE = new DamageSource(GameDropsBackport.MOD_ID + ".fallingStalactite");

    /**
     * 1.12.2's falling block hurts as "a falling block"; a falling spike's tip is a falling stalactite in 26.3. The
     * hit is swapped for ours, same amount, when a falling spike is where it lands.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAttack(LivingAttackEvent event) {
        if (event.getSource() != DamageSource.FALLING_BLOCK) {
            return;
        }
        EntityLivingBase victim = event.getEntityLiving();
        for (EntityFallingBlock falling : victim.world.getEntitiesWithinAABB(EntityFallingBlock.class, victim.getEntityBoundingBox().grow(0.5))) {
            if (falling.getBlock() != null && BlockSulfurSpike.isSpike(falling.getBlock())) {
                event.setCanceled(true);
                victim.attackEntityFrom(FALLING_STALACTITE, event.getAmount());
                return;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onHurt(LivingHurtEvent event) {
        if (event.getSource() != FALLING_STALACTITE) {
            return;
        }
        EntityLivingBase entity = event.getEntityLiving();
        ItemStack helmet = entity.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
        if (!helmet.isEmpty()) {
            float amount = event.getAmount();
            helmet.damageItem((int) (amount * 4.0F + entity.getRNG().nextFloat() * amount * 2.0F), entity);
            event.setAmount(amount * 0.75F);
        }
    }

    /**
     * A thrown trident moving faster than 0.6 breaks the spike it hits (26.3's {@code onProjectileHit}). Any mod's
     * trident, by its entity name: Update Aquatic's when it is installed, nothing without one.
     */
    @SubscribeEvent
    public void onProjectile(ProjectileImpactEvent event) {
        RayTraceResult hit = event.getRayTraceResult();
        Entity projectile = event.getEntity();
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK || projectile == null || projectile.world.isRemote) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        if (!BlockSulfurSpike.isSpike(projectile.world.getBlockState(pos))) {
            return;
        }
        if (isTrident(projectile) && speed(projectile) > 0.6) {
            projectile.world.destroyBlock(pos, true);
        }
    }

    static boolean isTrident(Entity entity) {
        ResourceLocation id = EntityList.getKey(entity);
        return id != null && id.getPath().contains("trident");
    }

    static double speed(Entity e) {
        return Math.sqrt(e.motionX * e.motionX + e.motionY * e.motionY + e.motionZ * e.motionZ);
    }
}
