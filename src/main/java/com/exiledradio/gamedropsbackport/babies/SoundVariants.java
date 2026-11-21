package com.exiledradio.gamedropsbackport.babies;

import com.exiledradio.gamedropsbackport.GameDropsBackport;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityMooshroom;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.projectile.EntityEgg;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 26.1's sound variants and baby sounds.
 *
 * <h2>Adult variants</h2>
 * Pigs ({@code classic}, {@code mini}, {@code big}), cows ({@code classic}, {@code moody}), chickens ({@code classic},
 * {@code picky}) and cats ({@code classic}, {@code royal}) each keep one variant for life, saved as
 * {@code gamedropsbackport:SoundVariant}. As 26.3 does it, a mob that spawns (naturally, from a spawn egg, by command)
 * picks one at random, all equally likely; a bred baby, a chicken from a thrown egg and a mob from a world made before
 * this mod are {@code classic}. A cat is Village &amp; Pillage's cat or 1.12.2's tamed ocelot (every ocelot gets
 * a variant, heard once tamed). Mooshrooms keep the
 * classic cow sounds.
 *
 * <h2>Baby sounds</h2>
 * Babies of every variant share one set: {@code entity.baby_pig}, {@code baby_chicken}, {@code baby_cat},
 * {@code baby_wolf}, and {@code baby_horse} (a horse's calls; any horse-like baby's steps and landing).
 *
 * <p>The swap happens in {@link PlaySoundAtEntityEvent} by the 1.12.2 event's name, at low priority so other
 * mods' sound changes come first. Pigs also play their eat sound when fed (26.1's {@code entity.pig.eat}).
 */
@Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID)
public final class SoundVariants {

    public static final String KEY = GameDropsBackport.MOD_ID + ":SoundVariant";
    private static final String BRED = GameDropsBackport.MOD_ID + ":Bred";

    static final String[] PIG = {"classic", "mini", "big"};
    static final String[] COW = {"classic", "moody"};
    static final String[] CHICKEN = {"classic", "picky"};
    static final String[] CAT = {"classic", "royal"};

    private static final Set<String> CAT_SOUNDS = new HashSet<String>(Arrays.asList(
            "ambient", "beg_for_food", "death", "eat", "hiss", "hurt", "purr", "purreow", "stray_ambient"));
    private static final Set<String> HORSE_CALLS = new HashSet<String>(Arrays.asList(
            "ambient", "angry", "breathe", "death", "eat", "hurt"));
    private static final Set<String> WOLF_SOUNDS = new HashSet<String>(Arrays.asList(
            "ambient", "death", "growl", "hurt", "pant", "step", "whine"));

    /** True while a thrown egg hatches its chickens (the rest of this server tick). */
    private static boolean eggHatching;

    private SoundVariants() {
    }

    // ------------------------------------------------------------------------------------------
    // Choosing

    /** @return the variant names of a mob with sound variants, or null */
    static String[] variantsOf(Entity e) {
        if (e instanceof EntityPig) {
            return PIG;
        }
        if (e instanceof EntityCow) {
            return e instanceof EntityMooshroom ? null : COW;
        }
        if (e instanceof EntityChicken) {
            return CHICKEN;
        }
        if (isCat(e)) {
            return CAT;
        }
        return null;
    }

    /** Village &amp; Pillage's cat, or an ocelot (1.12.2's cat once tamed). */
    static boolean isCat(Entity e) {
        return e instanceof EntityOcelot || "com.exiledradio.villagerbackport.cat.EntityCat".equals(e.getClass().getName());
    }

    /** @return this mob's variant, {@code classic} until one is chosen */
    public static String variant(Entity e) {
        String v = e.getEntityData().getString(KEY);
        return v.isEmpty() ? "classic" : v;
    }

    public static void setVariant(Entity e, String variant) {
        e.getEntityData().setString(KEY, variant);
    }

    @SubscribeEvent
    public static void onBaby(BabyEntitySpawnEvent event) {
        if (BabiesContent.on() && event.getChild() != null) {
            event.getChild().getEntityData().setBoolean(BRED, true);
        }
    }

    @SubscribeEvent
    public static void onEggImpact(ProjectileImpactEvent.Throwable event) {
        if (event.getThrowable() instanceof EntityEgg && !event.getThrowable().world.isRemote) {
            eggHatching = true;
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        eggHatching = false;
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinWorldEvent event) {
        Entity e = event.getEntity();
        if (!BabiesContent.on() || event.getWorld().isRemote || e instanceof EntityItem) {
            return;
        }
        String[] variants = variantsOf(e);
        if (variants == null || e.getEntityData().hasKey(KEY)) {
            return;
        }
        // A mob read from its chunk is already in it when it joins; a new one is added after this event.
        boolean classic = e.addedToChunk || e.getEntityData().getBoolean(BRED) || (eggHatching && e instanceof EntityChicken);
        e.getEntityData().removeTag(BRED);
        setVariant(e, classic ? "classic" : variants[e.world.rand.nextInt(variants.length)]);
    }

    // ------------------------------------------------------------------------------------------
    // Playing

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onSound(PlaySoundAtEntityEvent event) {
        if (!BabiesContent.on() || event.getSound() == null || event.getEntity() == null) {
            return;
        }
        ResourceLocation id = event.getSound().getRegistryName();
        if (id == null) {
            return;
        }
        String ours = replacement(event.getEntity(), id.getPath());
        if (ours != null) {
            SoundEvent sound = BabiesContent.sound(ours);
            if (sound != null) {
                event.setSound(sound);
            }
        }
    }

    /**
     * @param path a 1.12.2 sound event's path, such as {@code entity.pig.ambient}
     * @return the 26.3 event this mob plays instead, or null to keep it
     */
    static String replacement(Entity e, String path) {
        boolean baby = e instanceof EntityAgeable && ((EntityAgeable) e).isChild();
        if (e instanceof EntityPig) {
            return pick(path, "entity.pig.", baby, "baby_pig", variant(e), "ambient", "death", "hurt", "eat");
        }
        if (e instanceof EntityChicken) {
            return pick(path, "entity.chicken.", baby, "baby_chicken", variant(e), "ambient", "death", "hurt");
        }
        if (e instanceof EntityCow && !(e instanceof EntityMooshroom)) {
            // Cows have no baby sounds: a moody calf sounds moody.
            String v = variant(e);
            String name = suffix(path, "entity.cow.");
            return "moody".equals(v) && name != null ? "entity.cow_moody." + name : null;
        }
        if (isCat(e) && (!(e instanceof EntityOcelot) || ((EntityOcelot) e).isTamed())) {
            String name = suffix(path, "entity.cat.");
            if (name == null || !CAT_SOUNDS.contains(name)) {
                return null;
            }
            if (baby) {
                return "entity.baby_cat." + name;
            }
            return "royal".equals(variant(e)) ? "entity.cat_royal." + name : null;
        }
        if (e instanceof EntityWolf && baby) {
            String name = suffix(path, "entity.wolf.");
            return name != null && WOLF_SOUNDS.contains(name) ? "entity.baby_wolf." + name : null;
        }
        if (e instanceof AbstractHorse && baby) {
            String name = suffix(path, "entity.horse.");
            if ("step".equals(name) || "land".equals(name)) {
                return "entity.baby_horse." + name;
            }
            if (e instanceof EntityHorse && name != null && HORSE_CALLS.contains(name)) {
                return "entity.baby_horse." + name;
            }
        }
        return null;
    }

    /**
     * Pigs and chickens: a baby plays its baby set (steps included); an adult of a non-classic variant plays the
     * variant's sound for the named calls and keeps the classic step.
     */
    private static String pick(String path, String prefix, boolean baby, String babySet, String variant, String... calls) {
        String name = suffix(path, prefix);
        if (name == null) {
            return null;
        }
        if (baby) {
            return "entity." + babySet + "." + name;
        }
        if (!"classic".equals(variant) && Arrays.asList(calls).contains(name)) {
            return prefix.substring(0, prefix.length() - 1) + "_" + variant + "." + name;
        }
        return null;
    }

    private static String suffix(String path, String prefix) {
        return path.startsWith(prefix) ? path.substring(prefix.length()) : null;
    }

    /** {@code Pig.playEatingSound}: a fed pig, when 26.3's {@code Animal.mobInteract} would feed it. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onFeed(PlayerInteractEvent.EntityInteract event) {
        if (!BabiesContent.on() || event.isCanceled() || event.getWorld().isRemote || !(event.getTarget() instanceof EntityPig)) {
            return;
        }
        EntityPig pig = (EntityPig) event.getTarget();
        if (!pig.isBreedingItem(event.getItemStack())) {
            return;
        }
        boolean fed = (pig.getGrowingAge() == 0 && !pig.isInLove()) || (pig.isChild() && !AgeLock.isLocked(pig));
        if (!fed) {
            return;
        }
        String name = pig.isChild() ? "entity.baby_pig.eat"
                : "classic".equals(variant(pig)) ? "entity.pig.eat" : "entity.pig_" + variant(pig) + ".eat";
        SoundEvent sound = BabiesContent.sound(name);
        if (sound != null) {
            pig.world.playSound(null, pig.posX, pig.posY, pig.posZ, sound, SoundCategory.NEUTRAL, 1.0F,
                    (pig.world.rand.nextFloat() - pig.world.rand.nextFloat()) * 0.2F + (pig.isChild() ? 1.5F : 1.0F));
        }
    }
}
