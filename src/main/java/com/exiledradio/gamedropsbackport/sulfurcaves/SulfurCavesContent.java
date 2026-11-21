package com.exiledradio.gamedropsbackport.sulfurcaves;

import com.exiledradio.backportcore.creative.CreativePlacement;
import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.gamedropsbackport.GameDropsBackport;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.storage.loot.LootEntryItem;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;

import javax.annotation.Nullable;

/**
 * Stream 4's registrations: the biome, the music event, a tuff copy where no family mod has tuff (Rule 1: Caves &amp;
 * Cliffs owns it, Tricky Trials copies it), Bounce in mineshaft chests, and the world generator.
 *
 * <p>Stream 2's blocks are found by name ({@link #block}); without them (sulfur off) nothing generates.
 */
@Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID)
public final class SulfurCavesContent {

    public static BiomeSulfurCaves biome;
    /** Our tuff, or null where a family mod has its own. */
    public static Block tuffCopy;
    public static SoundEvent music;
    static SoundType tuffSound;

    private SulfurCavesContent() {
    }

    public static void preInit() {
        if (SulfurCaves.enabled()) {
            MinecraftForge.EVENT_BUS.register(new SulfurSpawns());
            if (net.minecraftforge.fml.common.FMLCommonHandler.instance().getSide().isClient()) {
                clientInit();
            }
        }
    }

    private static void clientInit() {
        com.exiledradio.gamedropsbackport.sulfurcaves.client.SulfurCavesClient.init();
    }

    public static void init() {
        if (SulfurCaves.enabled()) {
            GameRegistry.registerWorldGenerator(new SulfurCavesGen(), 20);
        }
    }

    public static void postInit() {
        if (biome != null) {
            biome.getSpawnableList(net.minecraft.entity.EnumCreatureType.MONSTER).clear();
            biome.getSpawnableList(net.minecraft.entity.EnumCreatureType.MONSTER).addAll(BiomeSulfurCaves.monsters());
        }
    }

    /** @return true when stream 2's blocks are there: the caves cannot generate without them */
    public static boolean blocksPresent() {
        return block("sulfur") != null && block("cinnabar") != null && block("potent_sulfur") != null
                && block("sulfur_spike") != null;
    }

    /** A block of ours by name (stream 2's), or null. */
    @Nullable
    public static Block block(String path) {
        ResourceLocation id = new ResourceLocation(GameDropsBackport.MOD_ID, path);
        Block b = ForgeRegistries.BLOCKS.containsKey(id) ? ForgeRegistries.BLOCKS.getValue(id) : null;
        return b == Blocks.AIR ? null : b;
    }

    /** Tuff: Caves &amp; Cliffs', else Tricky Trials' copy, else ours. */
    public static IBlockState tuff() {
        Block b = BlockOwnership.block("tuff");
        if (b == null) {
            ResourceLocation tt = new ResourceLocation("trickytrialsbackport", "tuff");
            b = ForgeRegistries.BLOCKS.containsKey(tt) ? ForgeRegistries.BLOCKS.getValue(tt) : null;
        }
        if (b == null || b == Blocks.AIR) {
            b = tuffCopy;
        }
        return b == null ? Blocks.STONE.getDefaultState() : b.getDefaultState();
    }

    /** @return true when no family mod registers tuff, so we copy it */
    static boolean copiesTuff() {
        return !BlockOwnership.ownerProvides("tuff") && !Loader.isModLoaded("trickytrialsbackport");
    }

    static ResourceLocation id(String name) {
        return new ResourceLocation(GameDropsBackport.MOD_ID, name);
    }

    static SoundEvent sound(String name) {
        SoundEvent e = new SoundEvent(id(name));
        e.setRegistryName(id(name));
        return e;
    }

    private static SoundEvent tuffBreak, tuffStep, tuffPlace, tuffHit, tuffFall;

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!SulfurCaves.enabled() || !copiesTuff()) {
            return;
        }
        tuffBreak = sound("block.tuff.break");
        tuffStep = sound("block.tuff.step");
        tuffPlace = sound("block.tuff.place");
        tuffHit = sound("block.tuff.hit");
        tuffFall = sound("block.tuff.fall");
        tuffSound = new SoundType(1.0F, 1.0F, tuffBreak, tuffStep, tuffPlace, tuffHit, tuffFall);
        tuffCopy = new BlockTuff(tuffSound);
        tuffCopy.setRegistryName(id("tuff"));
        tuffCopy.setTranslationKey(GameDropsBackport.MOD_ID + ".tuff");
        event.getRegistry().register(tuffCopy);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (tuffCopy != null) {
            ItemBlock item = new ItemBlock(tuffCopy);
            item.setRegistryName(tuffCopy.getRegistryName());
            CreativePlacement.place(tuffCopy);
            event.getRegistry().register(item);
        }
    }

    @SubscribeEvent
    public static void registerBiomes(RegistryEvent.Register<Biome> event) {
        if (!SulfurCaves.enabled()) {
            return;
        }
        biome = new BiomeSulfurCaves();
        biome.setRegistryName(id("sulfur_caves"));
        event.getRegistry().register(biome);
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (!SulfurCaves.enabled()) {
            return;
        }
        music = sound("music.overworld.sulfur_caves");
        event.getRegistry().register(music);
        if (tuffBreak != null) {
            event.getRegistry().registerAll(tuffBreak, tuffStep, tuffPlace, tuffHit, tuffFall);
        }
    }

    /**
     * 26.3's {@code chests/abandoned_mineshaft}: Bounce in the rail pool (3 rolls), weight 10, beside rails 20, powered,
     * detector and activator rails 5 each and torches 15. 26.3 has no biome condition on it.
     */
    @SubscribeEvent
    public static void loot(LootTableLoadEvent event) {
        if (!SulfurCaves.enabled() || !LootTableList.CHESTS_ABANDONED_MINESHAFT.equals(event.getName())) {
            return;
        }
        Item disc = ForgeRegistries.ITEMS.getValue(id("music_disc_bounce"));
        if (disc == null || disc == Items.AIR) {
            return;
        }
        for (String name : new String[] {"pool2", "main", "pool0", "pool1"}) {
            LootPool pool = event.getTable().getPool(name);
            if (pool != null && pool.getEntry("minecraft:rail") != null) {
                pool.addEntry(new LootEntryItem(disc, 10, 0, new LootFunction[0], new LootCondition[0],
                        GameDropsBackport.MOD_ID + ":music_disc_bounce"));
                return;
            }
        }
        GameDropsBackport.LOGGER.warn("The mineshaft chests have no rail pool; Bounce is not added.");
    }

    /** Tuff (1.17's, Caves &amp; Cliffs'), as Tricky Trials copies it. */
    static final class BlockTuff extends Block {
        BlockTuff(SoundType sound) {
            super(Material.ROCK, MapColor.GRAY_STAINED_HARDENED_CLAY);
            setHardness(1.5F);
            // 1.12.2 stores resistance tripled and reports it fifthed: 10 here is a modern 6.
            setResistance(10.0F);
            setSoundType(sound);
            setHarvestLevel("pickaxe", 0);
        }
    }
}
