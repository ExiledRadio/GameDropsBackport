package com.exiledradio.gamedropsbackport.babies;

import com.exiledradio.backportcore.mechanics.AgeLockable;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.network.NetworkHandler;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntitySkeletonHorse;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityZombieHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 26.1's age lock ({@code AgeableMob}): a golden dandelion used on a baby stops it growing up, and a second one
 * lets it grow again; skeleton and zombie horses never grow up and no longer panic when hurt.
 *
 * <ul>
 *   <li>Works on every {@link EntityAgeable} baby (any mod's) except villagers and skeleton and zombie horses
 *       (26.3's {@code cannot_be_age_locked}), and on every mob that implements Backport Core's
 *       {@link AgeLockable} (Wild Update's tadpole, always a baby). Each use resets the baby's age to the start
 *       (-24000, or its {@link AgeLockable#startAge()}: a snifflet's -48000, a tadpole's 0), uses one dandelion
 *       (not in creative), plays {@code item.golden_dandelion.use} or {@code .unuse}, and starts a 2-second
 *       cooldown on that mob. A locked baby never despawns.</li>
 *   <li>While locked the age stays at the start, and food that only grows a baby ({@link EntityAnimal}'s breeding
 *       item) is refused and not used up (an {@code AgeLockable} owner refuses its own). Saved as
 *       {@link AgeLockable#LOCKED} ({@code AgeLocked}) in the mob's Forge data.</li>
 *   <li>For 2 seconds after a use, particles drift down from above the mob (locked) or up (unlocked).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID)
public final class AgeLock {

    public static final String LOCKED = AgeLockable.LOCKED;
    /** The age a baby is held at, recorded the first tick it is held (26.1: babies of undead horses keep theirs). */
    private static final String HELD_AGE = GameDropsBackport.MOD_ID + ":HeldAge";
    public static final int BABY_START_AGE = -24000;
    public static final int COOLDOWN_TICKS = 40;

    /** Mob -> world time of its last use, for the cooldown. */
    private static final Map<Entity, Long> LAST_USE = new WeakHashMap<Entity, Long>();

    private AgeLock() {
    }

    // ------------------------------------------------------------------------------------------

    public static boolean isLocked(Entity entity) {
        return AgeLockable.isLocked(entity);
    }

    /** @return true for a mob the lock knows: an {@link EntityAgeable} or an {@link AgeLockable} */
    public static boolean lockable(Entity entity) {
        return entity instanceof EntityLiving && (entity instanceof EntityAgeable || entity instanceof AgeLockable);
    }

    /** @return whether a {@link #lockable} mob is a baby */
    public static boolean isBaby(EntityLiving mob) {
        return mob instanceof AgeLockable ? ((AgeLockable) mob).isBaby() : ((EntityAgeable) mob).isChild();
    }

    /** @return a {@link #lockable} mob's age, in its own terms */
    static int age(EntityLiving mob) {
        return mob instanceof AgeLockable ? ((AgeLockable) mob).getAge() : ((EntityAgeable) mob).getGrowingAge();
    }

    static void setAge(EntityLiving mob, int age) {
        if (mob instanceof AgeLockable) {
            ((AgeLockable) mob).setAge(age);
        } else {
            ((EntityAgeable) mob).setGrowingAge(age);
        }
    }

    /** @return the age a {@link #lockable} mob's babies start at (26.3's {@code getBabyStartAge}) */
    public static int startAge(EntityLiving mob) {
        return mob instanceof AgeLockable ? ((AgeLockable) mob).startAge() : BABY_START_AGE;
    }

    /** @return true for mobs 26.3 lists in {@code cannot_be_age_locked} */
    public static boolean cannotBeLocked(Entity entity) {
        return entity instanceof EntityVillager || isUndeadHorse(entity);
    }

    public static boolean isUndeadHorse(Entity entity) {
        return entity instanceof EntitySkeletonHorse || entity instanceof EntityZombieHorse;
    }

    /** @return true while this baby's age must not move */
    static boolean held(EntityLiving mob) {
        return isBaby(mob) && (isLocked(mob) || isUndeadHorse(mob));
    }

    /** Toggles the lock, as a golden dandelion does. Server side. */
    public static void toggle(EntityLiving mob, EntityPlayer player, ItemStack stack) {
        boolean locked = !isLocked(mob);
        NBTTagCompound data = mob.getEntityData();
        if (locked) {
            data.setBoolean(LOCKED, true);
        } else {
            data.removeTag(LOCKED);
        }
        data.removeTag(HELD_AGE);
        setAge(mob, startAge(mob));
        LAST_USE.put(mob, mob.world.getTotalWorldTime());
        if (player != null && !player.capabilities.isCreativeMode) {
            stack.shrink(1);
        }
        if (locked) {
            mob.enablePersistence();
        }
        SoundEvent sound = BabiesContent.sound(locked ? "item.golden_dandelion.use" : "item.golden_dandelion.unuse");
        // 26.4 Pre-Release 1: a silent mob makes no sound here either.
        if (sound != null && !mob.isSilent()) {
            mob.world.playSound(null, mob.posX, mob.posY, mob.posZ, sound, SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
        // Every player who sees the mob, the one using the dandelion included.
        NetworkHandler.channel.sendToAllTracking(new AgeLockMessage(mob.getEntityId(), locked, true), mob);
    }

    static boolean coolingDown(Entity mob) {
        Long last = LAST_USE.get(mob);
        return last != null && mob.world.getTotalWorldTime() - last < COOLDOWN_TICKS;
    }

    // ------------------------------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!BabiesContent.on() || !lockable(event.getTarget())) {
            return;
        }
        EntityLiving mob = (EntityLiving) event.getTarget();
        ItemStack stack = event.getItemStack();
        if (!isBaby(mob)) {
            return;
        }
        if (!stack.isEmpty() && stack.getItem() == BabiesContent.goldenDandelionItem && !cannotBeLocked(mob)) {
            if (!event.getWorld().isRemote && !coolingDown(mob)) {
                toggle(mob, event.getEntityPlayer(), stack);
            }
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.SUCCESS);
            return;
        }
        // Animal.mobInteract: food that would only grow a locked baby is not eaten.
        if (isLocked(mob) && mob instanceof EntityAnimal && ((EntityAnimal) mob).isBreedingItem(stack)) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.PASS);
        }
    }

    /** Fired before {@code onLivingUpdate} adds one to the age: the age is set one lower so it stays put. */
    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!BabiesContent.on() || !lockable(event.getEntity()) || event.getEntity().world.isRemote) {
            return;
        }
        EntityLiving mob = (EntityLiving) event.getEntity();
        if (!held(mob)) {
            return;
        }
        NBTTagCompound data = mob.getEntityData();
        int age;
        if (data.hasKey(HELD_AGE)) {
            age = data.getInteger(HELD_AGE);
        } else {
            age = isLocked(mob) ? startAge(mob) : age(mob);
            data.setInteger(HELD_AGE, age);
        }
        setAge(mob, age - 1);
    }

    /** Skeleton and zombie horses lose 1.12.2's panic task (26.1: they no longer panic when hurt). */
    @SubscribeEvent
    public static void onJoin(EntityJoinWorldEvent event) {
        if (!BabiesContent.on() || event.getWorld().isRemote || !isUndeadHorse(event.getEntity())) {
            return;
        }
        removePanic(((EntityLiving) event.getEntity()).tasks);
    }

    static void removePanic(EntityAITasks tasks) {
        List<EntityAIBase> panic = new ArrayList<EntityAIBase>();
        for (EntityAITasks.EntityAITaskEntry entry : tasks.taskEntries) {
            if (entry.action instanceof EntityAIPanic) {
                panic.add(entry.action);
            }
        }
        for (EntityAIBase task : panic) {
            tasks.removeTask(task);
        }
    }

    /** A player who starts seeing a locked baby learns it is locked (its client refuses food the same way). */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (BabiesContent.on() && lockable(event.getTarget()) && isLocked(event.getTarget())
                && event.getEntityPlayer() instanceof EntityPlayerMP) {
            NetworkHandler.channel.sendTo(new AgeLockMessage(event.getTarget().getEntityId(), true, false),
                    (EntityPlayerMP) event.getEntityPlayer());
        }
    }
}
