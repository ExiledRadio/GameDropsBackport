package com.exiledradio.gamedropsbackport.frostbite;

import com.exiledradio.backportcore.advancement.MonsterHunter;
import com.exiledradio.backportcore.creative.CreativePlacement;
import com.exiledradio.backportcore.freeze.FreezeTicks;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.ModConfig;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.monster.EntityStray;
import net.minecraft.entity.passive.EntityParrot;
import net.minecraft.init.Biomes;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionHelper;
import net.minecraft.potion.PotionType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * 26.4 stream 2: the frostbite, ice balls, the Freezing effect and its potions.
 *
 * <h2>Registry names (26.4's)</h2>
 * Entities {@code frostbite} (id 16) and {@code ice_ball} (id 17); items {@code ice_ball} and the frostbite's spawn
 * egg; effect {@code freezing}; potions {@code freezing} (900 ticks) and {@code long_freezing} (1800), with their
 * splash, lingering and tipped arrow forms (1.12.2 scales those as 26.4: a quarter, an eighth).
 *
 * <h2>Brewing (26.4's 13 recipes; 1.12.2 makes the splash and lingering ones from these)</h2>
 * Awkward + ice ball = Freezing, water + ice ball = mundane, Freezing + redstone = long Freezing.
 *
 * <h2>Spawning (26.4's biome files, monster category, groups of 4)</h2>
 * Frostbites: snowy plains 10, ice spikes 25, frozen river 25, frozen ocean 25, and by name where their owners add
 * them: Update Aquatic's deep frozen ocean 25, Caves &amp; Cliffs' frozen peaks 25, jagged peaks 10 and snowy slopes
 * 10; ice caves 50 (stream 1). Strays newly in 26.4 (95): frozen river, frozen ocean and those four.
 */
@Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID)
public final class FrostbiteContent {

    public static final ResourceLocation FROSTBITE = new ResourceLocation(GameDropsBackport.MOD_ID, "frostbite");
    public static final ResourceLocation ICE_BALL = new ResourceLocation(GameDropsBackport.MOD_ID, "ice_ball");
    /** 26.4's egg is a texture (Core's SpawnEggModels); these tint 1.12.2's. */
    public static final int EGG_PRIMARY = 0x6E8FA8;
    public static final int EGG_SECONDARY = 0xCFE8F2;
    public static final int GROUP = 4;

    private static final List<SoundEvent> SOUNDS = new ArrayList<SoundEvent>();
    public static final SoundEvent ambient = sound("entity.frostbite.ambient");
    public static final SoundEvent death = sound("entity.frostbite.death");
    public static final SoundEvent hurt = sound("entity.frostbite.hurt");
    public static final SoundEvent step = sound("entity.frostbite.step");
    public static final SoundEvent throwSound = sound("entity.frostbite.throw");
    public static final SoundEvent convertedToZombie = sound("entity.frostbite.converted_to_zombie");
    public static final SoundEvent zombieConverted = sound("entity.zombie.converted_to_frostbite");
    public static final SoundEvent drownedConverted = sound("entity.drowned.converted_to_zombie");
    public static final SoundEvent huskConverted = sound("entity.husk.converted_to_zombie");
    public static final SoundEvent imitate = sound("entity.parrot.imitate.frostbite");
    public static final SoundEvent iceBallThrow = sound("entity.ice_ball.throw");
    public static final SoundEvent iceBallBreak = sound("entity.ice_ball.break");
    public static final SoundEvent applyFreezing = sound("event.mob_effect.freezing");

    public static ItemIceBall iceBall;
    public static PotionFreezing freezing;
    public static PotionType freezingPotion;
    public static PotionType longFreezingPotion;

    private FrostbiteContent() {
    }

    private static SoundEvent sound(String name) {
        ResourceLocation id = new ResourceLocation(GameDropsBackport.MOD_ID, name);
        SoundEvent event = new SoundEvent(id).setRegistryName(id);
        SOUNDS.add(event);
        return event;
    }

    public static boolean enabled() {
        return ModConfig.frostbite.enabled;
    }

    public static void preInit() {
        if (!enabled()) {
            return;
        }
        LootTableList.register(EntityFrostbite.LOOT);
        MinecraftForge.EVENT_BUS.register(new FrostbiteEvents());
        MonsterHunter.add(FROSTBITE);
    }

    /** Brewing, the parrot's sound and the spawns; once every mod's biomes exist. */
    public static void init() {
        if (!enabled()) {
            return;
        }
        FreezeTicks.registerEffect(freezing, PotionFreezing.DAMAGE_VALUE);
        Ingredient ball = Ingredient.fromItem(iceBall);
        PotionHelper.addMix(PotionTypes.WATER, ball, PotionTypes.MUNDANE);
        PotionHelper.addMix(PotionTypes.AWKWARD, ball, freezingPotion);
        PotionHelper.addMix(freezingPotion, Items.REDSTONE, longFreezingPotion);
        EntityParrot.registerMimicSound(EntityFrostbite.class, imitate);
    }

    public static void postInit() {
        if (!enabled()) {
            return;
        }
        frostbite(Biomes.ICE_PLAINS, 10);
        frostbite(Biomes.MUTATED_ICE_FLATS, 25);
        frostbite(Biomes.FROZEN_RIVER, 25);
        frostbite(Biomes.FROZEN_OCEAN, 25);
        frostbite(biome("updateaquaticbackport:deep_frozen_ocean"), 25);
        frostbite(biome("cavesandcliffsbackport:frozen_peaks"), 25);
        frostbite(biome("cavesandcliffsbackport:jagged_peaks"), 10);
        frostbite(biome("cavesandcliffsbackport:snowy_slopes"), 10);
        for (Biome b : new Biome[] {Biomes.FROZEN_RIVER, Biomes.FROZEN_OCEAN, biome("updateaquaticbackport:deep_frozen_ocean"),
                biome("cavesandcliffsbackport:frozen_peaks"), biome("cavesandcliffsbackport:jagged_peaks"),
                biome("cavesandcliffsbackport:snowy_slopes")}) {
            if (b != null && !has(b, EntityStray.class)) {
                b.getSpawnableList(EnumCreatureType.MONSTER).add(new Biome.SpawnListEntry(EntityStray.class, 95, GROUP, GROUP));
            }
        }
    }

    private static Biome biome(String id) {
        return ForgeRegistries.BIOMES.getValue(new ResourceLocation(id));
    }

    private static boolean has(Biome biome, Class<?> type) {
        for (Biome.SpawnListEntry e : biome.getSpawnableList(EnumCreatureType.MONSTER)) {
            if (e.entityClass == type) {
                return true;
            }
        }
        return false;
    }

    private static void frostbite(Biome biome, int weight) {
        if (biome != null && !has(biome, EntityFrostbite.class)) {
            biome.getSpawnableList(EnumCreatureType.MONSTER).add(new Biome.SpawnListEntry(EntityFrostbite.class, weight, GROUP, GROUP));
        }
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        iceBall = new ItemIceBall();
        iceBall.setRegistryName(ICE_BALL);
        iceBall.setTranslationKey(GameDropsBackport.MOD_ID + ".ice_ball");
        CreativePlacement.place(iceBall);
        event.getRegistry().register(iceBall);
    }

    @SubscribeEvent
    public static void registerEffects(RegistryEvent.Register<Potion> event) {
        if (!enabled()) {
            return;
        }
        freezing = new PotionFreezing();
        freezing.setRegistryName(new ResourceLocation(GameDropsBackport.MOD_ID, "freezing"));
        event.getRegistry().register(freezing);
    }

    @SubscribeEvent
    public static void registerPotions(RegistryEvent.Register<PotionType> event) {
        if (!enabled()) {
            return;
        }
        // The base name gives potion.effect.<mod>.freezing and its splash, lingering and tipped arrow names.
        freezingPotion = new PotionType(GameDropsBackport.MOD_ID + ".freezing", new PotionEffect(freezing, 900));
        freezingPotion.setRegistryName(new ResourceLocation(GameDropsBackport.MOD_ID, "freezing"));
        longFreezingPotion = new PotionType(GameDropsBackport.MOD_ID + ".freezing", new PotionEffect(freezing, 1800));
        longFreezingPotion.setRegistryName(new ResourceLocation(GameDropsBackport.MOD_ID, "long_freezing"));
        event.getRegistry().registerAll(freezingPotion, longFreezingPotion);
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (!enabled()) {
            return;
        }
        for (SoundEvent s : SOUNDS) {
            event.getRegistry().register(s);
        }
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!enabled()) {
            return;
        }
        // Entity ids 16-20 are the 26.4 frostbite stream's. Tracked as the zombie (80 blocks) and as thrown items.
        EntityRegistry.registerModEntity(FROSTBITE, EntityFrostbite.class, GameDropsBackport.MOD_ID + ".frostbite", 16,
                GameDropsBackport.instance, 80, 3, true, EGG_PRIMARY, EGG_SECONDARY);
        EntityRegistry.registerModEntity(ICE_BALL, EntityIceBall.class, GameDropsBackport.MOD_ID + ".ice_ball", 17,
                GameDropsBackport.instance, 64, 10, true);
        CreativePlacement.placeSpawnEgg(FROSTBITE);
    }
}
