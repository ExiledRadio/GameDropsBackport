package com.exiledradio.gamedropsbackport.camps;

import com.exiledradio.backportcore.locate.StructureLocate;
import com.exiledradio.backportcore.structure.JigsawPools;
import com.exiledradio.backportcore.structure.ModernPalette;
import com.exiledradio.backportcore.structure.ScatteredStructure;
import com.exiledradio.backportcore.structure.TerrainAdaptation;
import com.exiledradio.backportcore.worldgen.FlatWorlds;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.ModConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Stream 7: 26.3's abandoned camps and explorer maps.
 *
 * <ul>
 *   <li>The camps ({@link AbandonedCampStructure}): 26.3's structure set, 18 structures, pools and templates,
 *       unchanged ({@code tools/camps/generate.py}), laid out by 26.3's own jigsaw rules ({@link CampLayout}), placed by
 *       Backport Core's templates and scattered driver, where 26.3 places them for the same seed; trees from the
 *       pools' features ({@link CampTrees}); 26.3's chest, barrel and copper chest loot ({@link CampLoot});
 *       {@code /locate structure minecraft:abandoned_camp_<biome>} (Backport Core); the terrain's {@code beard_thin}
 *       where a world's generator adapts to structures.</li>
 *   <li>The sixteen explorer map items ({@link ExplorerMaps}), each made only where its structure exists, copied
 *       with empty maps in a crafting grid ({@link ExplorerMapCloning}).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID)
public final class CampsContent {

    /** Backport Core's kind name (today's structure set). */
    public static final String KIND = "abandoned_camp";
    public static final String SET = "minecraft:abandoned_camp";

    /**
     * Blocks of the camps' templates that no installed mod may have, by what stands in: Village &amp; Pillage's barrel
     * and Tricky Trials' copper chest keep their loot as chests; the soft blocks stream's white wool stairs, the tents'
     * cloth, are white wool without it.
     */
    static final String[][] FALLBACKS = {
            {"minecraft:barrel", "minecraft:chest"},
            {"minecraft:oxidized_copper_chest", "minecraft:chest"},
            {"minecraft:white_wool_stairs", "minecraft:white_wool"},
            {"minecraft:powder_snow", "minecraft:snow_block"},
            {"minecraft:water_cauldron", "minecraft:cauldron"},
    };

    private CampsContent() {
    }

    public static boolean enabled() {
        return ModConfig.camps.enabled;
    }

    /** @return true when new chunks get abandoned camps */
    public static boolean generates() {
        return enabled();
    }

    /** Pre-init. */
    public static void preInit() {
        ExplorerMaps.preInit();
        if (!enabled()) {
            return;
        }
        JigsawPools.source(GameDropsBackport.MOD_ID);
        ModernPalette.source(GameDropsBackport.MOD_ID);
        for (String[] f : FALLBACKS) {
            ModernPalette.fallback(f[0], f[1]);
        }
        // The soft blocks stream's cushion (an entity in 26.3), by its registry name.
        ModernPalette.entity("minecraft:cushion", new ResourceLocation(GameDropsBackport.MOD_ID, "cushion"));
        CampLoot.preInit();
        CampTrees.register();
    }

    /** Init. */
    public static void init() {
        if (!enabled()) {
            return;
        }
        MapGenStructureIO.registerStructure(AbandonedCampStructure.Start.class, AbandonedCampStructure.SAVE_NAME);
        MapGenStructureIO.registerStructureComponent(CampPiece.class, "GDB_AbandonedCampPiece");
        ScatteredStructure.register(KIND, world -> generates() && world.provider.getDimension() == 0 && FlatWorlds.structure(world, KIND)
                ? new AbandonedCampStructure() : null);
        List<String> ids = new ArrayList<String>();
        for (CampSettings.Variant v : CampSettings.get().variants) {
            ids.add(v.id);
        }
        StructureLocate.scattered(SET, KIND, (world, structure, x, z) -> {
            AbandonedCampStructure.Choice c = AbandonedCampStructure.choose(world, x, z);
            return c == null ? null : c.variant.id;
        }, ids.toArray(new String[0]));
        // 26.3's structure tags the explorer maps point at (#on_abandoned_camp_swamp, #on_mineshaft_maps, ...).
        for (String tag : STRUCTURE_TAGS) {
            JsonObject t = CampSettings.read("tags/worldgen/structure/" + tag + ".json");
            if (t != null) {
                List<String> members = new ArrayList<String>();
                for (JsonElement e : t.getAsJsonArray("values")) {
                    members.add(e.getAsString());
                }
                StructureLocate.tag("minecraft:" + tag, members.toArray(new String[0]));
            }
        }
        TerrainAdaptation.register(KIND, TerrainAdaptation.Kind.BEARD_THIN, (world, cx, cz, out) -> {
            ScatteredStructure s = ScatteredStructure.of(world, KIND);
            if (s instanceof AbandonedCampStructure) {
                ((AbandonedCampStructure) s).startsNear(world, cx, cz, out);
            }
        });
        MinecraftForge.EVENT_BUS.register(new CampLoot());
        MinecraftForge.EVENT_BUS.register(new ExplorerMaps.Events());
    }

    /** Post-init: the bee nests on the {@code _bees} trees, whoever places the tree. */
    public static void postInit() {
        if (enabled()) {
            CampBees.postInit();
        }
    }

    static final String[] STRUCTURE_TAGS = {"on_abandoned_camp_bamboo_jungle", "on_abandoned_camp_birch_forest",
            "on_abandoned_camp_cherry_grove", "on_abandoned_camp_dappled_forest", "on_abandoned_camp_flower_forest",
            "on_abandoned_camp_pale_garden", "on_abandoned_camp_swamp", "on_abandoned_camp_windswept_forest",
            "on_ancient_city_maps", "on_buried_trial_chambers_maps", "on_desert_pyramid_maps", "on_desert_village_maps",
            "on_jungle_pyramid_maps", "on_mineshaft_maps", "on_ocean_monument_maps", "on_ocean_ruin_warm_maps",
            "on_plains_village_maps", "on_savanna_village_maps", "on_snowy_village_maps", "on_swamp_hut_maps",
            "on_taiga_village_maps", "on_treasure_maps", "on_woodland_mansion_maps"};

    /** 26.3's map cloning for the explorer maps ({@link ExplorerMapCloning}). */
    @SubscribeEvent
    public static void recipes(RegistryEvent.Register<IRecipe> event) {
        if (enabled()) {
            event.getRegistry().register(new ExplorerMapCloning().setRegistryName(GameDropsBackport.MOD_ID, "explorer_map_cloning"));
        }
    }

    @SubscribeEvent
    public static void items(RegistryEvent.Register<Item> event) {
        if (enabled()) {
            for (Item item : ExplorerMaps.items()) {
                event.getRegistry().register(item);
            }
        }
    }
}
