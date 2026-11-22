package com.exiledradio.gamedropsbackport.sulfurcube;

import com.exiledradio.gamedropsbackport.GameDropsBackport;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Which items a sulfur cube swallows and what each makes it: 26.3's {@code #sulfur_cube_archetype/*} item tags
 * ({@code #sulfur_cube_swallowable} is their union), read once after every mod has registered.
 *
 * <p>Each 26.3 name resolves to 1.12.2's item and metadata where 1.12.2 has the block (the flattening's old names);
 * otherwise to the block of that name from this mod or another family mod, when that mod is installed (Rule 2), and
 * to nothing when none is. Item tags become 1.12.2's ore dictionary names or 1.12.2's colored blocks.
 */
public final class ArchetypeItems {

    /** Namespaces a 26.3 name is looked for in, in order, after 1.12.2's own items. */
    private static final String[] FAMILY = {GameDropsBackport.MOD_ID, "cavesandcliffsbackport", "wildupdatebackport",
            "trickytrialsbackport", "trailsandtalesbackport", "netherupdatebackport", "buzzybeesbackport",
            "updateaquaticbackport", "villagerbackport", "backportcore"};

    /** 26.3 names that are a different item or metadata in 1.12.2: "item@meta", meta -1 for any. */
    private static final Map<String, String[]> OLD = new HashMap<String, String[]>();

    static {
        old("stone", "stone@0");
        old("granite", "stone@1");
        old("polished_granite", "stone@2");
        old("diorite", "stone@3");
        old("polished_diorite", "stone@4");
        old("andesite", "stone@5");
        old("polished_andesite", "stone@6");
        old("dirt", "dirt@0");
        old("coarse_dirt", "dirt@1");
        old("podzol", "dirt@2");
        old("grass_block", "grass@-1");
        old("bricks", "brick_block@-1");
        old("stone_bricks", "stonebrick@0");
        old("mossy_stone_bricks", "stonebrick@1");
        old("cracked_stone_bricks", "stonebrick@2");
        old("chiseled_stone_bricks", "stonebrick@3");
        old("sandstone", "sandstone@0");
        old("chiseled_sandstone", "sandstone@1");
        old("cut_sandstone", "sandstone@2");
        old("red_sandstone", "red_sandstone@0");
        old("chiseled_red_sandstone", "red_sandstone@1");
        old("cut_red_sandstone", "red_sandstone@2");
        old("quartz_block", "quartz_block@0");
        old("chiseled_quartz_block", "quartz_block@1");
        old("quartz_pillar", "quartz_block@2");
        old("prismarine", "prismarine@0");
        old("prismarine_bricks", "prismarine@1");
        old("dark_prismarine", "prismarine@2");
        old("nether_bricks", "nether_brick@-1");
        old("red_nether_bricks", "red_nether_brick@-1");
        old("end_stone_bricks", "end_bricks@-1");
        old("nether_quartz_ore", "quartz_ore@-1");
        old("magma_block", "magma@-1");
        old("snow_block", "snow@-1");
        old("melon", "melon_block@-1");
        old("jack_o_lantern", "lit_pumpkin@-1");
        old("sponge", "sponge@0");
        old("wet_sponge", "sponge@1");
        old("brown_mushroom_block", "brown_mushroom_block@-1");
        old("red_mushroom_block", "red_mushroom_block@-1");
        old("redstone_lamp", "redstone_lamp@-1", "lit_redstone_lamp@-1");
        old("redstone_ore", "redstone_ore@-1", "lit_redstone_ore@-1");
    }

    /** Item tags of 26.3: their 1.12.2 forms ("ore:name" for an ore dictionary name), then family names. */
    private static final Map<String, String[]> TAGS = new HashMap<String, String[]>();

    static {
        TAGS.put("#planks", new String[]{"ore:plankWood", "planks@-1"});
        TAGS.put("#logs", new String[]{"ore:logWood", "log@-1", "log2@-1"});
        TAGS.put("#bamboo_blocks", new String[]{"bamboo_block", "stripped_bamboo_block"});
        TAGS.put("#wool", new String[]{"wool@-1"});
        TAGS.put("#concrete", new String[]{"concrete@-1"});
        TAGS.put("#concrete_powders", new String[]{"concrete_powder@-1"});
        TAGS.put("#terracotta", new String[]{"hardened_clay@-1", "stained_hardened_clay@-1"});
        String[] colors = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "silver", "cyan",
                "purple", "blue", "brown", "green", "red", "black"};
        String[] glazed = new String[colors.length];
        for (int i = 0; i < colors.length; i++) {
            glazed[i] = colors[i] + "_glazed_terracotta@-1";
        }
        TAGS.put("#glazed_terracotta", glazed);
        TAGS.put("#moss_blocks", new String[]{"moss_block", "pale_moss_block"});
        TAGS.put("#wart_blocks", new String[]{"nether_wart_block@-1", "warped_wart_block"});
        TAGS.put("#coal_ores", new String[]{"coal_ore@-1", "deepslate_coal_ore"});
        TAGS.put("#lapis_ores", new String[]{"lapis_ore@-1", "deepslate_lapis_ore"});
        TAGS.put("#redstone_ores", new String[]{"redstone_ore@-1", "lit_redstone_ore@-1", "deepslate_redstone_ore"});
        TAGS.put("#diamond_ores", new String[]{"diamond_ore@-1", "deepslate_diamond_ore"});
        TAGS.put("#emerald_ores", new String[]{"emerald_ore@-1", "deepslate_emerald_ore"});
        TAGS.put("#gold_ores", new String[]{"gold_ore@-1", "nether_gold_ore", "deepslate_gold_ore"});
        TAGS.put("#iron_ores", new String[]{"iron_ore@-1", "deepslate_iron_ore"});
        TAGS.put("#copper_ores", new String[]{"copper_ore", "deepslate_copper_ore"});
    }

    /** 26.3's item tags, in its files' order. */
    private static final Map<Archetype, String> LISTS = new EnumMap<Archetype, String>(Archetype.class);

    static {
        LISTS.put(Archetype.BOUNCY, "#planks bamboo_mosaic #logs #bamboo_blocks");
        LISTS.put(Archetype.EXPLOSIVE, "tnt");
        LISTS.put(Archetype.FAST_FLAT, "tube_coral_block brain_coral_block bubble_coral_block fire_coral_block "
                + "horn_coral_block dead_tube_coral_block dead_brain_coral_block dead_bubble_coral_block "
                + "dead_fire_coral_block dead_horn_coral_block sponge wet_sponge dried_kelp_block #moss_blocks resin_block "
                + "resin_bricks chiseled_resin_bricks melon hay_block pumpkin carved_pumpkin jack_o_lantern ochre_froglight "
                + "pearlescent_froglight verdant_froglight");
        LISTS.put(Archetype.FAST_SLIDING, "blue_ice packed_ice snow_block");
        LISTS.put(Archetype.HIGH_RESISTANCE, "soul_sand soul_soil");
        LISTS.put(Archetype.HOT, "magma_block");
        LISTS.put(Archetype.LIGHT, "#wool");
        LISTS.put(Archetype.REGULAR, "#concrete_powders mud muddy_mangrove_roots packed_mud coal_block dirt coarse_dirt "
                + "rooted_dirt podzol grass_block clay bone_block");
        LISTS.put(Archetype.SLOW_BOUNCY, "amethyst_block andesite basalt blackstone bricks calcite chiseled_cinnabar "
                + "chiseled_deepslate chiseled_nether_bricks chiseled_polished_blackstone chiseled_quartz_block "
                + "chiseled_red_sandstone chiseled_sandstone chiseled_stone_bricks chiseled_sulfur chiseled_tuff "
                + "chiseled_tuff_bricks cinnabar cinnabar_bricks cobbled_deepslate cobblestone cracked_deepslate_bricks "
                + "cracked_deepslate_tiles cracked_nether_bricks cracked_polished_blackstone_bricks cracked_stone_bricks "
                + "crimson_nylium crying_obsidian cut_red_sandstone cut_sandstone dark_prismarine deepslate deepslate_bricks "
                + "deepslate_tiles diamond_block diorite dripstone_block emerald_block end_stone end_stone_bricks "
                + "gilded_blackstone glowstone granite lapis_block mossy_cobblestone mossy_stone_bricks mud_bricks "
                + "nether_bricks netherrack observer obsidian polished_andesite polished_basalt polished_blackstone "
                + "polished_blackstone_bricks polished_cinnabar polished_deepslate polished_diorite polished_granite "
                + "polished_sulfur polished_tuff prismarine prismarine_bricks purpur_block purpur_pillar quartz_block "
                + "quartz_bricks nether_quartz_ore quartz_pillar red_nether_bricks red_sandstone redstone_lamp sandstone "
                + "sea_lantern smooth_basalt smooth_quartz smooth_red_sandstone smooth_sandstone smooth_stone stone "
                + "stone_bricks sulfur sulfur_bricks tuff tuff_bricks warped_nylium #concrete #coal_ores #lapis_ores "
                + "#redstone_ores #diamond_ores #emerald_ores #terracotta #glazed_terracotta");
        LISTS.put(Archetype.SLOW_FLAT, "iron_block gold_block raw_copper_block raw_gold_block raw_iron_block #gold_ores "
                + "#iron_ores #copper_ores netherite_block ancient_debris copper_block exposed_copper weathered_copper "
                + "oxidized_copper waxed_copper_block waxed_exposed_copper waxed_weathered_copper waxed_oxidized_copper "
                + "copper_bulb exposed_copper_bulb weathered_copper_bulb oxidized_copper_bulb waxed_copper_bulb "
                + "waxed_exposed_copper_bulb waxed_weathered_copper_bulb waxed_oxidized_copper_bulb cut_copper "
                + "exposed_cut_copper weathered_cut_copper oxidized_cut_copper waxed_cut_copper waxed_exposed_cut_copper "
                + "waxed_weathered_cut_copper waxed_oxidized_cut_copper chiseled_copper exposed_chiseled_copper "
                + "weathered_chiseled_copper oxidized_chiseled_copper waxed_chiseled_copper waxed_exposed_chiseled_copper "
                + "waxed_weathered_chiseled_copper waxed_oxidized_chiseled_copper");
        LISTS.put(Archetype.SLOW_SLIDING, "brown_mushroom_block red_mushroom_block mushroom_stem mycelium #wart_blocks "
                + "shroomlight");
        LISTS.put(Archetype.STICKY, "honeycomb_block");
    }

    /** item -> meta (or -1 for any) -> archetype */
    private static final Map<Item, Map<Integer, Archetype>> BY_ITEM = new HashMap<Item, Map<Integer, Archetype>>();
    private static boolean built;

    private ArchetypeItems() {
    }

    private static void old(String modern, String... forms) {
        OLD.put(modern, forms);
    }

    /** Reads the lists; runs once, after every mod has registered its items and ore names. */
    public static synchronized void build() {
        if (built) {
            return;
        }
        built = true;
        for (Map.Entry<Archetype, String> e : LISTS.entrySet()) {
            for (String name : e.getValue().split(" ")) {
                for (ItemStack stack : resolve(name)) {
                    Map<Integer, Archetype> metas = BY_ITEM.get(stack.getItem());
                    if (metas == null) {
                        metas = new HashMap<Integer, Archetype>();
                        BY_ITEM.put(stack.getItem(), metas);
                    }
                    int meta = stack.getMetadata() == OreDictionary.WILDCARD_VALUE ? -1 : stack.getMetadata();
                    if (!metas.containsKey(meta)) {
                        metas.put(meta, e.getKey());
                    }
                }
            }
        }
    }

    /** @return the stacks (metadata WILDCARD for any) a 26.3 name or tag stands for here; empty when none exists */
    static List<ItemStack> resolve(String name) {
        Set<ItemStack> out = new LinkedHashSet<ItemStack>();
        String[] forms = name.startsWith("#") ? TAGS.get(name) : OLD.get(name);
        if (forms == null) {
            forms = new String[]{name + "@0"};
        }
        for (String form : forms) {
            if (form.startsWith("ore:")) {
                out.addAll(OreDictionary.getOres(form.substring(4), false));
                continue;
            }
            int at = form.indexOf('@');
            if (at >= 0) {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("minecraft", form.substring(0, at)));
                int meta = Integer.parseInt(form.substring(at + 1));
                if (item != null && item != net.minecraft.init.Items.AIR) {
                    out.add(new ItemStack(item, 1, meta < 0 ? OreDictionary.WILDCARD_VALUE : meta));
                    continue;
                }
                form = form.substring(0, at);
                if (!name.startsWith("#") && !OLD.containsKey(name)) {
                    form = name;
                }
            }
            for (String ns : FAMILY) {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(ns, form));
                if (item != null && item != net.minecraft.init.Items.AIR) {
                    out.add(new ItemStack(item, 1, OreDictionary.WILDCARD_VALUE));
                    break;
                }
            }
        }
        return new java.util.ArrayList<ItemStack>(out);
    }

    /** @return what this stack makes a cube, or null when a cube does not swallow it */
    public static Archetype of(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        build();
        Map<Integer, Archetype> metas = BY_ITEM.get(stack.getItem());
        if (metas == null) {
            return null;
        }
        Archetype a = metas.get(stack.getMetadata());
        return a != null ? a : metas.get(-1);
    }

    /**
     * @return the archetype of what a cube holds: {@link #of}, or {@link Archetype#NONE} for something no archetype
     *     names (a skull put in by command), null for nothing
     */
    public static Archetype held(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Archetype a = of(stack);
        return a != null ? a : Archetype.NONE;
    }

    /** 26.3's {@code #sulfur_cube_swallowable}. */
    public static boolean swallowable(ItemStack stack) {
        return of(stack) != null;
    }

    /** @return every item some archetype names, for the dispenser behavior */
    public static Set<Item> items() {
        build();
        return Collections.unmodifiableSet(BY_ITEM.keySet());
    }
}
