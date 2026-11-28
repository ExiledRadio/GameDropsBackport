package com.exiledradio.gamedropsbackport.softblocks;

import com.exiledradio.backportcore.creative.CreativePlacement;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.ModConfig;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stream 6 (26.3): wool and concrete stairs and slabs in 16 colors, cushions and the straw bed. Everything is
 * registered from here, behind {@code softBlocks.enabled}.
 *
 * <h2>Other family mods (Rule 2)</h2>
 * <ul>
 *   <li>Village &amp; Pillage's stonecutter cuts concrete into these stairs (1) and slabs (2) by itself: it reads the
 *       crafting recipes of every {@code BlockStairs} and {@code BlockSlab} made from one rock block.</li>
 *   <li>Village &amp; Pillage's composter takes straw beds (26.3: 85%).</li>
 *   <li>Wool shapes dampen vibrations (Wild Update's sculk, through Backport Core's game events) once Core lets a mod
 *       add to {@code #dampens_vibrations} (docs/CORE_REQUESTS.md).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID)
public final class SoftBlocks {

    public static final String[] MATERIALS = {"wool", "concrete"};

    /** Before the sound fields: they add themselves to it. */
    private static final List<SoundEvent> SOUNDS = new ArrayList<SoundEvent>();

    public static final SoundEvent cushionPlace = sound("entity.cushion.place");
    public static final SoundEvent cushionBreak = sound("entity.cushion.break");
    public static final SoundEvent cushionSit = sound("entity.cushion.sit");
    public static final SoundEvent cushionGetUp = sound("entity.cushion.get_up");
    public static final SoundEvent strawBedBreakLeave = sound("block.straw_bed.break_leave");
    /** 26.3's break sound, still registered so a world saved before 26.4's change loads without a missing sound event. */
    public static final SoundEvent strawBedBreak = sound("block.straw_bed.break");
    /** 26.4 Pre-Release 2: the straw bed's break sound is break_leave ({@code block_sound_set/straw_bed.json}). */
    public static final SoundType STRAW_BED = new SoundType(1.0F, 1.0F, strawBedBreakLeave,
            sound("block.straw_bed.step"), sound("block.straw_bed.place"), sound("block.straw_bed.hit"),
            sound("block.straw_bed.fall"));

    /** material -> its stairs blocks (2 colors each), in dye order */
    public static final Map<String, BlockSoftStairs[]> STAIRS = new LinkedHashMap<String, BlockSoftStairs[]>();
    /** material -> its two half slabs (8 colors each) */
    public static final Map<String, BlockSoftSlab.Half[]> SLABS = new LinkedHashMap<String, BlockSoftSlab.Half[]>();
    public static final Map<String, BlockSoftSlab.Double> DOUBLE_SLABS = new LinkedHashMap<String, BlockSoftSlab.Double>();
    public static BlockStrawBed strawBed;
    public static ItemCushion cushion;

    private static final List<Block> BLOCKS = new ArrayList<Block>();
    private static final List<Item> ITEMS = new ArrayList<Item>();
    private static final List<EntityItem> UNPROTECT = new ArrayList<EntityItem>();

    private SoftBlocks() {
    }

    public static boolean enabled() {
        return ModConfig.softBlocks.enabled;
    }

    private static SoundEvent sound(String name) {
        ResourceLocation id = new ResourceLocation(GameDropsBackport.MOD_ID, name);
        SoundEvent e = new SoundEvent(id).setRegistryName(id);
        SOUNDS.add(e);
        return e;
    }

    public static List<Block> blocks() {
        return Collections.unmodifiableList(BLOCKS);
    }

    public static List<Item> items() {
        return Collections.unmodifiableList(ITEMS);
    }

    private static Block base(String material) {
        return material.equals("wool") ? Blocks.WOOL : Blocks.CONCRETE;
    }

    /** @return the stairs block that has this color */
    public static BlockSoftStairs stairs(String material, EnumDyeColor c) {
        return STAIRS.get(material)[c.getMetadata() / 2];
    }

    /** @return the half slab that has this color */
    public static BlockSoftSlab.Half slab(String material, EnumDyeColor c) {
        return SLABS.get(material)[c.getMetadata() / 8];
    }

    private static <B extends Block> B block(IForgeRegistry<Block> registry, String name, B block) {
        block.setRegistryName(new ResourceLocation(GameDropsBackport.MOD_ID, name));
        block.setTranslationKey(GameDropsBackport.MOD_ID + "." + name);
        registry.register(block);
        BLOCKS.add(block);
        return block;
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Block> r = event.getRegistry();
        for (String mat : MATERIALS) {
            Block base = base(mat);
            BlockSoftStairs[] stairs = new BlockSoftStairs[8];
            for (int i = 0; i < 8; i++) {
                EnumDyeColor[] colors = SoftColors.range(i * 2, 2);
                stairs[i] = block(r, SoftColors.modern(colors[0]) + "_" + mat + "_stairs", BlockSoftStairs.create(base, colors));
            }
            STAIRS.put(mat, stairs);
            final EnumDyeColor[] first = SoftColors.range(0, 8);
            final EnumDyeColor[] second = SoftColors.range(8, 8);
            final BlockSoftSlab.Half a = block(r, "white_" + mat + "_slab",
                    BlockSoftSlab.construct(first, () -> new BlockSoftSlab.Half(base, first)));
            final BlockSoftSlab.Half b = block(r, "light_gray_" + mat + "_slab",
                    BlockSoftSlab.construct(second, () -> new BlockSoftSlab.Half(base, second)));
            BlockSoftSlab.Double full = block(r, "white_" + mat + "_double_slab",
                    BlockSoftSlab.construct(EnumDyeColor.values(), () -> new BlockSoftSlab.Double(base, a, b)));
            a.full = full;
            b.full = full;
            SLABS.put(mat, new BlockSoftSlab.Half[]{a, b});
            DOUBLE_SLABS.put(mat, full);
        }
        strawBed = block(r, "straw_bed", new BlockStrawBed());

        // FireBlock.bootStrap (26.3): wool shapes and the straw bed burn like wool.
        for (Block block : BLOCKS) {
            if (block.getDefaultState().getMaterial() == net.minecraft.block.material.Material.CLOTH) {
                Blocks.FIRE.setFireInfo(block, 30, 60);
            }
        }
    }

    private static <I extends Item> I item(IForgeRegistry<Item> registry, ResourceLocation name, I item) {
        item.setRegistryName(name);
        registry.register(item);
        ITEMS.add(item);
        return item;
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Item> r = event.getRegistry();
        for (String mat : MATERIALS) {
            boolean wool = mat.equals("wool");
            String anchor = wool ? "minecraft:black_wool" : "minecraft:black_concrete";
            for (BlockSoftStairs b : STAIRS.get(mat)) {
                item(r, b.getRegistryName(), new ItemSoftBlock(b, mat + "_stairs", wool ? 100 : -1));
                CreativePlacement.placeAfter(b, CreativePlacement.ANY, CreativeTabs.BUILDING_BLOCKS, anchor);
            }
            for (BlockSoftSlab.Half b : SLABS.get(mat)) {
                item(r, b.getRegistryName(), new ItemSoftBlock(b, mat + "_slab", wool ? 50 : -1));
                CreativePlacement.placeAfter(b, CreativePlacement.ANY, CreativeTabs.BUILDING_BLOCKS, anchor);
            }
        }
        cushion = item(r, new ResourceLocation(GameDropsBackport.MOD_ID, "cushion"), new ItemCushion());
        cushion.setTranslationKey(GameDropsBackport.MOD_ID + ".cushion");
        CreativePlacement.placeAfter(cushion, CreativePlacement.ANY, CreativeTabs.DECORATIONS, "minecraft:black_carpet");
        Item bed = item(r, strawBed.getRegistryName(), new ItemStrawBed(strawBed));
        strawBed.item = bed;
        CreativePlacement.placeAfter(strawBed, CreativePlacement.ANY, CreativeTabs.DECORATIONS, "minecraft:black_bed");
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (enabled()) {
            event.getRegistry().registerAll(SOUNDS.toArray(new SoundEvent[0]));
        }
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<net.minecraftforge.fml.common.registry.EntityEntry> event) {
        if (enabled()) {
            // Entity id 11 (the stream's 11-15). 26.3: tracked 10 chunks out, never updated after it spawns.
            EntityRegistry.registerModEntity(new ResourceLocation(GameDropsBackport.MOD_ID, "cushion"), EntityCushion.class,
                    GameDropsBackport.MOD_ID + ".cushion", 11, GameDropsBackport.instance, 160, Integer.MAX_VALUE, false);
        }
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<IRecipe> r = event.getRegistry();
        for (String mat : MATERIALS) {
            Block base = base(mat);
            for (EnumDyeColor c : EnumDyeColor.values()) {
                ItemStack full = new ItemStack(base, 1, c.getMetadata());
                String n = SoftColors.modern(c) + "_" + mat;
                ItemStack stairs = new ItemStack(stairs(mat, c), 4, c.getMetadata());
                ItemStack slab = new ItemStack(slab(mat, c), 6, c.getMetadata());
                recipe(r, n + "_stairs", new ShapedOreRecipe(group(mat.equals("wool") ? "woolen_stairs" : "concrete_stairs"),
                        stairs, "#  ", "## ", "###", '#', full));
                recipe(r, n + "_slab", new ShapedOreRecipe(group(mat.equals("wool") ? "woolen_slab" : "concrete_slab"),
                        slab, "###", '#', full));
            }
        }
        for (EnumDyeColor c : EnumDyeColor.values()) {
            String n = SoftColors.modern(c);
            String dye = dyeOre(c);
            recipe(r, "dye_" + n + "_wool_stairs", new ShapelessOreRecipe(group("wool_stairs"),
                    new ItemStack(stairs("wool", c), 1, c.getMetadata()), dye, others(c, true)));
            recipe(r, "dye_" + n + "_wool_slab", new ShapelessOreRecipe(group("wool_slab"),
                    new ItemStack(slab("wool", c), 1, c.getMetadata()), dye, others(c, false)));
            recipe(r, n + "_cushion", new ShapedOreRecipe(group("cushion"), new ItemStack(cushion, 1, c.getMetadata()),
                    "###", '#', new ItemStack(slab("wool", c), 1, c.getMetadata())));
            List<ItemStack> cushions = new ArrayList<ItemStack>();
            for (EnumDyeColor o : EnumDyeColor.values()) {
                if (o != c) {
                    cushions.add(new ItemStack(cushion, 1, o.getMetadata()));
                }
            }
            recipe(r, "dye_" + n + "_cushion", new ShapelessOreRecipe(group("cushion_dye"), new ItemStack(cushion, 1, c.getMetadata()),
                    dye, net.minecraft.item.crafting.Ingredient.fromStacks(cushions.toArray(new ItemStack[0]))));
        }
        recipe(r, "straw_bed", new ShapedOreRecipe(null, new ItemStack(strawBed.item, 4), "XXX", 'X', Blocks.HAY_BLOCK));
    }

    /** The wool stairs (or slabs) of every color but {@code c}, as one ingredient. */
    private static net.minecraft.item.crafting.Ingredient others(EnumDyeColor c, boolean stairs) {
        List<ItemStack> list = new ArrayList<ItemStack>();
        for (EnumDyeColor o : EnumDyeColor.values()) {
            if (o != c) {
                list.add(new ItemStack(stairs ? stairs("wool", o) : slab("wool", o), 1, o.getMetadata()));
            }
        }
        return net.minecraft.item.crafting.Ingredient.fromStacks(list.toArray(new ItemStack[0]));
    }

    /** Forge's dye ore names: {@code dyeWhite}, {@code dyeLightGray}, ... (a dye's ore id is 15 - its color). */
    static String dyeOre(EnumDyeColor c) {
        String[] names = {"Black", "Red", "Green", "Brown", "Blue", "Purple", "Cyan", "LightGray", "Gray", "Pink",
                "Lime", "Yellow", "LightBlue", "Magenta", "Orange", "White"};
        return "dye" + names[c.getDyeDamage()];
    }

    private static ResourceLocation group(String name) {
        return new ResourceLocation(GameDropsBackport.MOD_ID, name);
    }

    private static void recipe(IForgeRegistry<IRecipe> r, String name, IRecipe recipe) {
        recipe.setRegistryName(new ResourceLocation(GameDropsBackport.MOD_ID, name));
        r.register(recipe);
    }

    /** 26.3's {@code sleep_in_straw_bed}: counted instead of {@code sleep_in_bed} when the bed is a straw bed. */
    public static net.minecraft.stats.StatBase sleepInStrawBed;
    public static final String SLEEP_STAT = "stat." + GameDropsBackport.MOD_ID + ".sleepInStrawBed";

    /** Init: the straw bed statistic, and the hand-offs to other family mods. */
    public static void init() {
        if (!enabled()) {
            return;
        }
        sleepInStrawBed = new net.minecraft.stats.StatBasic(SLEEP_STAT,
                new net.minecraft.util.text.TextComponentTranslation(SLEEP_STAT)).registerStat();
        if (Loader.isModLoaded("villagerbackport")) {
            try {
                Class.forName("com.exiledradio.villagerbackport.block.Compostables")
                        .getMethod("register", float.class, Item.class).invoke(null, 0.85F, strawBed.item);
            } catch (ReflectiveOperationException | RuntimeException e) {
                GameDropsBackport.LOGGER.warn("Could not add the straw bed to the composter.", e);
            }
        }
        // #dampens_vibrations: wool stairs and slabs (26.3), through Backport Core 1.4.4's GameEventTags.
        for (Block block : BLOCKS) {
            if (block.getDefaultState().getMaterial() == net.minecraft.block.material.Material.CLOTH && block != strawBed) {
                com.exiledradio.backportcore.gameevent.GameEventTags.addDampener(block);
            }
        }
    }

    /** The lightning-proof time of a dropped cushion: 20 ticks, then the item is ordinary again. */
    static void unprotectLater(EntityItem item) {
        synchronized (UNPROTECT) {
            UNPROTECT.add(item);
        }
        item.getEntityData().setLong(GameDropsBackport.MOD_ID + ":unprotect", item.world.getTotalWorldTime() + 20);
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) {
            return;
        }
        synchronized (UNPROTECT) {
            for (Iterator<EntityItem> it = UNPROTECT.iterator(); it.hasNext(); ) {
                EntityItem item = it.next();
                if (item.world != event.world) {
                    continue;
                }
                if (item.isDead || event.world.getTotalWorldTime()
                        >= item.getEntityData().getLong(GameDropsBackport.MOD_ID + ":unprotect")) {
                    item.setEntityInvulnerable(false);
                    it.remove();
                }
            }
        }
    }
}
