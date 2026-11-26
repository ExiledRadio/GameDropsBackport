package com.exiledradio.gamedropsbackport.jukebox;

import com.exiledradio.backportcore.creative.CreativePlacement;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.ModConfig;

import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemRecord;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootEntryItem;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.conditions.RandomChance;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 26.4 stream 6: jukebox playlists ({@link Playlists}) and the music disc Flurry ("Hyper Potions - Flurry", 154 s,
 * comparator 7, uncommon), with a 30% chance in igloo basement chests (a pool of its own, one roll). The jukebox's
 * 26.4 textures (a bottom of its own) are the look and feel stream's, under 26.4.
 */
@Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID)
public final class JukeboxContent {

    public static final ResourceLocation FLURRY = new ResourceLocation(GameDropsBackport.MOD_ID, "music_disc_flurry");
    public static final SoundEvent music = new SoundEvent(new ResourceLocation(GameDropsBackport.MOD_ID, "music_disc.flurry"))
            .setRegistryName(new ResourceLocation(GameDropsBackport.MOD_ID, "music_disc.flurry"));

    public static Disc flurry;

    private JukeboxContent() {
    }

    public static boolean enabled() {
        return ModConfig.jukebox.enabled;
    }

    public static void preInit() {
        if (!enabled()) {
            return;
        }
        MinecraftForge.EVENT_BUS.register(new Playlists.Events());
        MinecraftForge.EVENT_BUS.register(new Loot());
    }

    /** Flurry on 1.12.2's record item; its comparator signal through Core's {@code comparatorSignal()} (P41). */
    public static final class Disc extends ItemRecord {
        public static final int SIGNAL = 7;

        Disc() {
            super("flurry", music);
        }

        public int comparatorSignal() {
            return SIGNAL;
        }

        @Override
        public EnumRarity getRarity(ItemStack stack) {
            return EnumRarity.UNCOMMON;
        }
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        flurry = new Disc();
        flurry.setRegistryName(FLURRY);
        flurry.setTranslationKey(GameDropsBackport.MOD_ID + ".music_disc_flurry");
        CreativePlacement.place(flurry);
        event.getRegistry().register(flurry);
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (enabled()) {
            event.getRegistry().register(music);
        }
    }

    /** {@code chests/igloo_chest}'s 26.4 pool: one roll, 30%, the disc. */
    public static final class Loot {
        @SubscribeEvent
        public void load(LootTableLoadEvent event) {
            if (flurry == null || !LootTableList.CHESTS_IGLOO_CHEST.equals(event.getName())) {
                return;
            }
            LootEntry disc = new LootEntryItem(flurry, 1, 0, new LootFunction[0], new LootCondition[0], "gamedropsbackport:music_disc_flurry");
            event.getTable().addPool(new LootPool(new LootEntry[] {disc}, new LootCondition[] {new RandomChance(0.3F)},
                    new RandomValueRange(1), new RandomValueRange(0), "gamedropsbackport:flurry"));
        }
    }
}
