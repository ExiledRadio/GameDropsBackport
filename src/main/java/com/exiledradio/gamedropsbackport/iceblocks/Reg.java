package com.exiledradio.gamedropsbackport.iceblocks;

import com.exiledradio.backportcore.creative.CreativePlacement;
import com.exiledradio.gamedropsbackport.GameDropsBackport;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.IProperty;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * How this stream names and registers its blocks, items and sounds, and what the client and the self-test need to
 * know about them (item models, ignored properties, multipart files). Names only, never client types.
 *
 * <p>The family's {@code common.Content} pattern, kept inside this stream's package: the plan's streams share no
 * classes.
 */
public final class Reg {

    private static final Map<Item, String> ITEM_MODELS = new LinkedHashMap<Item, String>();
    private static final List<Block> BLOCKS = new ArrayList<Block>();
    private static final List<Block> MULTIPART_FILES = new ArrayList<Block>();
    private static final Map<Block, IProperty<?>[]> IGNORED = new LinkedHashMap<Block, IProperty<?>[]>();
    private static final Map<String, SoundEvent> SOUNDS = new LinkedHashMap<String, SoundEvent>();

    private Reg() {
    }

    public static ResourceLocation id(String name) {
        return new ResourceLocation(GameDropsBackport.MOD_ID, name);
    }

    /** Names and registers a block; @param tool the harvest tool, or null */
    public static <B extends Block> B block(IForgeRegistry<Block> registry, String name, B block, String tool) {
        block.setRegistryName(id(name));
        block.setTranslationKey(GameDropsBackport.MOD_ID + "." + name);
        if (tool != null) {
            block.setHarvestLevel(tool, 0);
        }
        registry.register(block);
        BLOCKS.add(block);
        return block;
    }

    public static ItemBlock blockItem(IForgeRegistry<Item> registry, Block block) {
        return blockItem(registry, block, new ItemBlock(block));
    }

    /** Registers an item for a block under the block's name, placed next to vanilla's block of that name. */
    public static <I extends Item> I blockItem(IForgeRegistry<Item> registry, Block block, I item) {
        item.setRegistryName(block.getRegistryName());
        CreativePlacement.place(block);
        registry.register(item);
        ITEM_MODELS.put(item, block.getRegistryName().getPath());
        return item;
    }

    /** {@link #blockItem} for a block 26.4 lists in no creative tab. */
    public static ItemBlock blockItemNoTab(IForgeRegistry<Item> registry, Block block) {
        ItemBlock item = new ItemBlock(block);
        item.setRegistryName(block.getRegistryName());
        item.setCreativeTab(null);
        registry.register(item);
        ITEM_MODELS.put(item, block.getRegistryName().getPath());
        return item;
    }

    public static <I extends Item> I item(IForgeRegistry<Item> registry, String name, I item) {
        item.setRegistryName(id(name));
        item.setTranslationKey(GameDropsBackport.MOD_ID + "." + name);
        CreativePlacement.place(item);
        registry.register(item);
        ITEM_MODELS.put(item, name);
        return item;
    }

    /** item -> model name under {@code models/item/} (metadata 0) */
    public static Map<Item, String> itemModels() {
        return Collections.unmodifiableMap(ITEM_MODELS);
    }

    public static List<Block> blocks() {
        return Collections.unmodifiableList(BLOCKS);
    }

    /** A block whose blockstate file is multipart (walls), for the asset check. */
    public static void multipartFile(Block block) {
        MULTIPART_FILES.add(block);
    }

    public static List<Block> multipartFiles() {
        return Collections.unmodifiableList(MULTIPART_FILES);
    }

    /** Properties the client's state mapper leaves out (slabs' one-value variant). */
    public static void ignoreProperties(Block block, IProperty<?>... properties) {
        IGNORED.put(block, properties);
    }

    public static Map<Block, IProperty<?>[]> ignoredProperties() {
        return Collections.unmodifiableMap(IGNORED);
    }

    /** @return our sound event {@code gamedropsbackport:<name>}, made on first use, registered in the sound event */
    public static synchronized SoundEvent sound(String name) {
        SoundEvent event = SOUNDS.get(name);
        if (event == null) {
            event = new SoundEvent(id(name));
            event.setRegistryName(id(name));
            SOUNDS.put(name, event);
        }
        return event;
    }

    public static synchronized List<SoundEvent> sounds() {
        return new ArrayList<SoundEvent>(SOUNDS.values());
    }

    /** @return a sound type from our five events {@code block.<name>.break/step/place/hit/fall} */
    public static SoundType soundType(String name) {
        return new SoundType(1.0F, 1.0F, sound("block." + name + ".break"), sound("block." + name + ".step"),
                sound("block." + name + ".place"), sound("block." + name + ".hit"), sound("block." + name + ".fall"));
    }
}
