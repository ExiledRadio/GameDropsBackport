package com.exiledradio.gamedropsbackport.iceblocks;

import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.ModConfig;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.storage.loot.LootEntryItem;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;

/**
 * 26.4 streams 3 and 4: the ice and snow building blocks, the calcite set and chiseled soulshale.
 *
 * <h2>Registry names (26.4's)</h2>
 * <ul>
 *   <li>{@code packed_ice_bricks}, {@code packed_ice_brick_stairs/_slab/_wall}, {@code chiseled_packed_ice};</li>
 *   <li>{@code snow_bricks}, {@code snow_brick_stairs/_slab/_wall};</li>
 *   <li>with Caves &amp; Cliffs' calcite: {@code calcite_pillar}, {@code calcite_stairs/_slab/_wall},
 *       {@code polished_calcite} (+ {@code _stairs/_slab/_wall}), {@code polished_calcite_bricks}
 *       (+ {@code polished_calcite_brick_stairs/_slab/_wall});</li>
 *   <li>with Wild Update's ancient cities: {@code chiseled_soulshale}.</li>
 * </ul>
 * Each slab also has our {@code <x>_double_slab}.
 *
 * <h2>Numbers (26.4 Pre-Release 1, {@code Blocks.java})</h2>
 * Packed ice bricks: 0.5/0.5, packed ice's sounds and chime, friction 0.6 (not slippery), pickaxe needed. Snow bricks:
 * 0.6/0.6, snow's sounds, shovel needed. The calcite set copies calcite: 0.75/0.75, calcite's sounds, the base drum,
 * pickaxe needed. Chiseled soulshale: 2.0/6.0, its own sounds, any tool. Shapes copy their block. Every block drops
 * itself, none melts. Recipes as 26.4's crafting; a 1.14 stonecutter derives its cuts from them (26.4 has no
 * stonecutter recipe for snow bricks, and none of ours is a 2x2 of snow bricks' kind).
 */
@Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID)
public final class IceBlocksContent {

    public static final String CAVES_AND_CLIFFS = "cavesandcliffsbackport";
    public static final String WILD_UPDATE = "wildupdatebackport";

    public static Block packedIceBricks;
    public static Shapes packedIceBrickShapes;
    public static Block chiseledPackedIce;
    public static Block snowBricks;
    public static Shapes snowBrickShapes;
    @Nullable public static Block calcitePillar;
    @Nullable public static Shapes calciteShapes;
    @Nullable public static Block polishedCalcite;
    @Nullable public static Shapes polishedCalciteShapes;
    @Nullable public static Block polishedCalciteBricks;
    @Nullable public static Shapes polishedCalciteBrickShapes;
    @Nullable public static Block chiseledSoulshale;

    private IceBlocksContent() {
    }

    public static boolean enabled() {
        return ModConfig.iceBlocks.enabled;
    }

    /** The calcite set exists only beside Caves &amp; Cliffs' calcite (Rule 2). */
    public static boolean calcite() {
        return Loader.isModLoaded(CAVES_AND_CLIFFS);
    }

    /** Chiseled soulshale comes only from Wild Update's ancient cities. */
    public static boolean soulshale() {
        return Loader.isModLoaded(WILD_UPDATE);
    }

    public static void preInit() {
        if (enabled()) {
            MinecraftForge.EVENT_BUS.register(new Loot());
        }
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Block> r = event.getRegistry();
        SoundType packedIce = Reg.soundType("packed_ice");
        packedIceBricks = Reg.block(r, "packed_ice_bricks",
                new BlockIceStone(Material.PACKED_ICE, MapColor.ICE, packedIce, 0.5F, 0.5F, true), "pickaxe");
        packedIceBrickShapes = Shapes.register(r, "packed_ice_brick", packedIceBricks, true);
        chiseledPackedIce = Reg.block(r, "chiseled_packed_ice",
                new BlockIceStone(Material.PACKED_ICE, MapColor.ICE, packedIce, 0.5F, 0.5F, true), "pickaxe");

        snowBricks = Reg.block(r, "snow_bricks",
                new BlockIceStone(Material.CRAFTED_SNOW, MapColor.SNOW, SoundType.SNOW, 0.6F, 0.6F, true), "shovel");
        snowBrickShapes = Shapes.register(r, "snow_brick", snowBricks, true);

        if (calcite()) {
            SoundType sound = Reg.soundType("calcite");
            MapColor white = MapColor.WHITE_STAINED_HARDENED_CLAY;
            // Caves & Cliffs' calcite may register after us: the shapes copy this stand-in, which has its numbers.
            Block stand = new BlockIceStone(Material.ROCK, white, sound, 0.75F, 0.75F, true);
            stand.setHarvestLevel("pickaxe", 0);
            calcitePillar = Reg.block(r, "calcite_pillar", new BlockIceStone.Pillar(Material.ROCK, white, sound, 0.75F, 0.75F), "pickaxe");
            calciteShapes = Shapes.register(r, "calcite", stand, true);
            polishedCalcite = Reg.block(r, "polished_calcite", new BlockIceStone(Material.ROCK, white, sound, 0.75F, 0.75F, true), "pickaxe");
            polishedCalciteShapes = Shapes.register(r, "polished_calcite", polishedCalcite, true);
            polishedCalciteBricks = Reg.block(r, "polished_calcite_bricks",
                    new BlockIceStone(Material.ROCK, white, sound, 0.75F, 0.75F, true), "pickaxe");
            polishedCalciteBrickShapes = Shapes.register(r, "polished_calcite_brick", polishedCalciteBricks, true);
        }
        if (soulshale()) {
            // Any tool or none; 26.4's WARPED_STEM map color is the family's cyan.
            chiseledSoulshale = Reg.block(r, "chiseled_soulshale",
                    new BlockIceStone(new Material(MapColor.CYAN), MapColor.CYAN, Reg.soundType("chiseled_soulshale"), 2.0F, 6.0F, false), null);
            // 26.4 lists it in no creative tab: only commands and ancient city chests.
            chiseledSoulshale.setCreativeTab(null);
        }
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Item> r = event.getRegistry();
        Reg.blockItem(r, packedIceBricks);
        packedIceBrickShapes.registerItems(r);
        Reg.blockItem(r, chiseledPackedIce);
        Reg.blockItem(r, snowBricks);
        snowBrickShapes.registerItems(r);
        if (calcitePillar != null) {
            Reg.blockItem(r, calcitePillar);
            calciteShapes.registerItems(r);
            Reg.blockItem(r, polishedCalcite);
            polishedCalciteShapes.registerItems(r);
            Reg.blockItem(r, polishedCalciteBricks);
            polishedCalciteBrickShapes.registerItems(r);
        }
        if (chiseledSoulshale != null) {
            Reg.blockItemNoTab(r, chiseledSoulshale);
        }
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (!enabled()) {
            return;
        }
        for (SoundEvent sound : Reg.sounds()) {
            event.getRegistry().register(sound);
        }
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<IRecipe> r = event.getRegistry();
        shaped(r, "packed_ice_bricks", new ItemStack(packedIceBricks, 4), "##", "##", '#', Blocks.PACKED_ICE);
        shapes(r, packedIceBricks, packedIceBrickShapes);
        shaped(r, "chiseled_packed_ice", new ItemStack(chiseledPackedIce), "#", "#", '#', packedIceBrickShapes.slab);
        shaped(r, "snow_bricks", new ItemStack(snowBricks, 4), "##", "##", '#', Blocks.SNOW);
        shapes(r, snowBricks, snowBrickShapes);
        Block calcite = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(CAVES_AND_CLIFFS, "calcite"));
        if (calcitePillar != null && calcite != null && calcite != Blocks.AIR) {
            shaped(r, "calcite_pillar", new ItemStack(calcitePillar, 2), "#", "#", '#', calcite);
            shapes(r, calcite, calciteShapes);
            shaped(r, "polished_calcite", new ItemStack(polishedCalcite, 4), "##", "##", '#', calcite);
            shapes(r, polishedCalcite, polishedCalciteShapes);
            shaped(r, "polished_calcite_bricks", new ItemStack(polishedCalciteBricks, 4), "##", "##", '#', polishedCalcite);
            shapes(r, polishedCalciteBricks, polishedCalciteBrickShapes);
        }
    }

    /** Vanilla's three shape recipes: 6 to 4 stairs, 3 to 6 slabs, 6 to 6 walls. */
    private static void shapes(IForgeRegistry<IRecipe> r, Block base, Shapes s) {
        shaped(r, s.prefix + "_stairs", new ItemStack(s.stairs, 4), "#  ", "## ", "###", '#', base);
        shaped(r, s.prefix + "_slab", new ItemStack(s.slab, 6), "###", '#', base);
        shaped(r, s.prefix + "_wall", new ItemStack(s.wall, 6), "###", "###", '#', base);
    }

    private static void shaped(IForgeRegistry<IRecipe> r, String name, ItemStack out, Object... recipe) {
        ShapedOreRecipe recipeObject = new ShapedOreRecipe(new ResourceLocation(GameDropsBackport.MOD_ID, "iceblocks"), out, recipe);
        recipeObject.setRegistryName(Reg.id(name));
        r.register(recipeObject);
    }

    /**
     * 26.4's {@code chests/ancient_city}: chiseled soulshale, weight 3, one, in the main pool (5-10 rolls). Wild Update
     * builds the pool at load; this runs after it.
     */
    public static final class Loot {

        public static final ResourceLocation CITY = new ResourceLocation(WILD_UPDATE, "chests/ancient_city");
        public static final String POOL = WILD_UPDATE + ":ancient_city";

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void load(LootTableLoadEvent event) {
            if (chiseledSoulshale == null || !CITY.equals(event.getName())) {
                return;
            }
            LootPool pool = event.getTable().getPool(POOL);
            if (pool != null && pool.getEntry("gamedropsbackport:chiseled_soulshale") == null) {
                pool.addEntry(new LootEntryItem(Item.getItemFromBlock(chiseledSoulshale), 3, 0, new LootFunction[0],
                        new LootCondition[0], "gamedropsbackport:chiseled_soulshale"));
            }
        }
    }
}
