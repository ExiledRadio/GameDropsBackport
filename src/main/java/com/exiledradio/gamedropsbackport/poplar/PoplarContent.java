package com.exiledradio.gamedropsbackport.poplar;

import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.ModConfig;
import com.exiledradio.gamedropsbackport.poplar.wood.AxeStripping;
import com.exiledradio.gamedropsbackport.poplar.wood.WoodKind;
import com.exiledradio.gamedropsbackport.poplar.wood.WoodSet;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapelessOreRecipe;

/**
 * Stream 5: poplar and the dappled forest (26.3).
 *
 * <ul>
 *   <li>The poplar wood set ({@link WoodSet}; 26.3's {@code BlockSetType.POPLAR} uses the defaults, which are 1.12.2's
 *       wooden sounds), red, orange and yellow poplar leaves, the poplar sapling (a red, orange or yellow poplar),
 *       the potted sapling and the boat ({@code boat.BoatContent}; the chest boat with Wild Update).</li>
 *   <li>The red shrub ({@link BlockRedShrub}) and the shelf mushroom ({@link BlockShelfMushroom}).</li>
 *   <li>The dappled forest ({@link BiomeDappledForest}, placed by {@link DappledForestLayer}); its trees, fallen
 *       poplars and ground cover are all in {@link PoplarFeatures}, for other terrain to call.</li>
 * </ul>
 *
 * <p>The hanging sign and the shelf are Trails &amp; Tales' and Tricky Trials' blocks, which those mods add for every
 * wood in the game (Rule 2), so they are not registered here ({@code docs/CORE_REQUESTS.md}).
 *
 * <p>Map colors are 26.3's, by 1.12.2's names: planks and stripped wood {@code COLOR_LIGHT_GRAY}
 * ({@link MapColor#SILVER}), bark {@code PODZOL} ({@link MapColor#OBSIDIAN}), leaves red, orange
 * ({@link MapColor#ADOBE}) and yellow, the sapling {@code METAL} ({@link MapColor#IRON}).
 */
@Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID)
public final class PoplarContent {

    public static final WoodKind POPLAR = new WoodKind("poplar", MapColor.SILVER, MapColor.OBSIDIAN);

    public static final SoundType POPLAR_LEAVES = Content.soundType("poplar_leaves", 1.0F, 1.0F);
    public static final SoundEvent POPLAR_LEAVES_AMBIENT = Content.sound("block.poplar_leaves.ambient");
    /**
     * 26.3's {@code SoundType.RED_SHRUB}: break and place only. Its step, hit and fall are {@code EMPTY}; they are
     * never played (it breaks at once, and steps and falls sound the block under it), so vanilla's grass events
     * stand in for them.
     */
    public static final SoundType RED_SHRUB = new SoundType(1.0F, 1.0F, Content.sound("block.red_shrub.break"),
            SoundEvents.BLOCK_GRASS_STEP, Content.sound("block.red_shrub.place"), SoundEvents.BLOCK_GRASS_HIT,
            SoundEvents.BLOCK_GRASS_FALL);
    /** 26.3's {@code SoundType.SHELF_MUSHROOM}: its hit is {@code EMPTY}, never played (it breaks at once). */
    public static final SoundType SHELF_MUSHROOM = new SoundType(1.0F, 1.0F, Content.sound("block.shelf_mushroom.break"),
            Content.sound("block.shelf_mushroom.step"), Content.sound("block.shelf_mushroom.place"),
            SoundEvents.BLOCK_GRASS_HIT, Content.sound("block.shelf_mushroom.fall"));
    public static final SoundEvent SHELF_MUSHROOM_BOUNCE = Content.sound("block.shelf_mushroom.bounce");

    /** Null when the stream is off. */
    public static WoodSet set;
    public static BlockPoplarLeaves redLeaves;
    public static BlockPoplarLeaves orangeLeaves;
    public static BlockPoplarLeaves yellowLeaves;
    public static BlockPoplarSapling sapling;
    public static BlockPotted pottedSapling;
    public static BlockRedShrub redShrub;
    public static BlockShelfMushroom shelfMushroom;
    /** Null when the stream is off or the biome is switched off. */
    public static BiomeDappledForest biome;

    private PoplarContent() {
    }

    public static boolean enabled() {
        return ModConfig.poplar.enabled;
    }

    /**
     * Pre-init: this mod ships these first (26.3), so it owns them in Backport Core's table; on a client, the models and
     * renderers (a class a dedicated server never loads).
     */
    public static void preInit(boolean client) {
        if (enabled()) {
            if (client) {
                com.exiledradio.gamedropsbackport.poplar.client.PoplarClient.init();
            }
            DappledForestLayer.register();
            BlockOwnership.declare(GameDropsBackport.MOD_ID, "poplar_log", "poplar_wood", "stripped_poplar_log",
                    "stripped_poplar_wood", "poplar_planks", "poplar_stairs", "poplar_slab", "poplar_double_slab",
                    "poplar_fence", "poplar_fence_gate", "poplar_door", "poplar_trapdoor", "poplar_pressure_plate",
                    "poplar_button", "poplar_sign", "poplar_wall_sign", "red_poplar_leaves", "orange_poplar_leaves",
                    "yellow_poplar_leaves", "poplar_sapling", "potted_poplar_sapling", "red_shrub", "shelf_mushroom");
        }
    }

    /** Init: compost chances to Village &amp; Pillage's composter, the wandering trader's poplar trades. */
    public static void init() {
        if (enabled()) {
            Compost.init();
            PoplarTrades.init();
            if (biome != null) {
                biome.addFox();
            }
        }
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        set = WoodSet.tree(POPLAR);
        set.registerBlocks(event.getRegistry());
        redLeaves = leaves(event, "red_poplar_leaves", BlockPoplarLeaves.Color.RED);
        orangeLeaves = leaves(event, "orange_poplar_leaves", BlockPoplarLeaves.Color.ORANGE);
        yellowLeaves = leaves(event, "yellow_poplar_leaves", BlockPoplarLeaves.Color.YELLOW);
        sapling = Content.block(event.getRegistry(), "poplar_sapling", new BlockPoplarSapling(), null);
        Content.ignoreProperties(sapling, BlockPoplarSapling.STAGE);
        pottedSapling = Content.block(event.getRegistry(), "potted_poplar_sapling", new BlockPotted(), null);
        pottedSapling.setCreativeTab(null);
        redShrub = Content.block(event.getRegistry(), "red_shrub", new BlockRedShrub(), null);
        shelfMushroom = Content.block(event.getRegistry(), "shelf_mushroom", new BlockShelfMushroom(), null);

        // 26.3's FireBlock table: leaves 30/60, the red shrub 60/100.
        for (Block b : new Block[] {redLeaves, orangeLeaves, yellowLeaves}) {
            Blocks.FIRE.setFireInfo(b, 30, 60);
        }
        Blocks.FIRE.setFireInfo(redShrub, 60, 100);
        AxeStripping.add(set.stripping());
    }

    private static BlockPoplarLeaves leaves(RegistryEvent.Register<Block> event, String name, BlockPoplarLeaves.Color color) {
        BlockPoplarLeaves leaves = Content.block(event.getRegistry(), name, new BlockPoplarLeaves(color), "hoe");
        Content.ignoreProperties(leaves, BlockPoplarLeaves.CHECK_DECAY, BlockPoplarLeaves.DECAYABLE);
        return leaves;
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        set.registerItems(event.getRegistry());
        Item red = Content.registerBlockItem(event.getRegistry(), redLeaves);
        Item orange = Content.registerBlockItem(event.getRegistry(), orangeLeaves);
        Item yellow = Content.registerBlockItem(event.getRegistry(), yellowLeaves);
        Item saplingItem = Content.blockItem(event.getRegistry(), sapling, new ItemBlock(sapling) {
            @Override
            public int getItemBurnTime(ItemStack stack) {
                return 100;
            }
        });
        for (BlockPoplarLeaves leaves : new BlockPoplarLeaves[] {redLeaves, orangeLeaves, yellowLeaves}) {
            leaves.setSapling(saplingItem);
            OreDictionary.registerOre("treeLeaves", leaves);
        }
        pottedSapling.setPlant(saplingItem);
        Item shrub = Content.registerBlockItem(event.getRegistry(), redShrub);
        Item mushroom = Content.registerBlockItem(event.getRegistry(), shelfMushroom);

        OreDictionary.registerOre("treeSapling", sapling);
        // 26.3's compostable components: leaves, sapling and red shrub "low" (30%), the shelf mushroom "medium" (65%).
        Compost.add(0.3F, red, orange, yellow, saplingItem, shrub);
        Compost.add(0.65F, mushroom);
    }

    // Normal priority, as the family's other wood sets: at LOW, Quark's any-planks trapdoor recipe (registered
    // earlier) matched the trapdoor's grid first.
    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        set.registerRecipes(event.getRegistry());
        // 26.3's rabbit_stew_from_shelf_mushroom (group rabbit_stew).
        ResourceLocation id = Content.id("rabbit_stew_from_shelf_mushroom");
        event.getRegistry().register(new ShapelessOreRecipe(id, new ItemStack(Items.RABBIT_STEW), Items.BAKED_POTATO,
                Items.COOKED_RABBIT, Items.BOWL, Items.CARROT, shelfMushroom).setRegistryName(id));
    }

    @SubscribeEvent
    public static void registerBiomes(RegistryEvent.Register<Biome> event) {
        if (!enabled() || !ModConfig.poplar.generateBiome) {
            return;
        }
        biome = new BiomeDappledForest();
        biome.setRegistryName(Content.id("dappled_forest"));
        event.getRegistry().register(biome);
        BiomeDappledForest.addTypes(biome);
    }
}
