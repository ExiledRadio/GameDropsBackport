package com.exiledradio.gamedropsbackport.babies;

import com.exiledradio.backportcore.trade.TradeRegistry;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.ModConfig;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFlower;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreIngredient;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraftforge.common.crafting.CompoundIngredient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stream 1 (26.1, Tiny Takeover): the golden dandelion and its potted form, name tag recipes and trades, and the
 * sound events of the adult sound variants, the new baby sounds and the trumpet.
 *
 * <ul>
 *   <li>Golden dandelion: a dandelion in a ring of 8 gold nuggets; yellow dye; a flower pot plant. Flammability
 *       60/100, as a dandelion.</li>
 *   <li>Name tag: paper and any metal nugget (iron, gold, or any mod's copper nugget, by ore name).</li>
 *   <li>Wandering trader (Village &amp; Pillage reads Core's trade registry; without it nothing does): a golden
 *       dandelion for 2 emeralds, 12 uses, and a name tag for 1 emerald, 5 uses, both in the ordinary pool
 *       (26.3's {@code wandering_trader/common}).</li>
 *   <li>Woodland mansion chests no longer hold name tags (26.1).</li>
 * </ul>
 * Nothing here is registered while {@code babies.enabled} is off.
 */
@Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID)
public final class BabiesContent {

    public static BlockGoldenDandelion goldenDandelion;
    public static BlockPottedGoldenDandelion pottedGoldenDandelion;
    public static Item goldenDandelionItem;

    /** Our sound events, by vanilla event name (such as {@code entity.pig_mini.ambient}). */
    static final Map<String, SoundEvent> SOUNDS = new LinkedHashMap<String, SoundEvent>();

    /** Every sound event this stream adds, in 26.3's names. */
    static final List<String> SOUND_NAMES = Arrays.asList(
            "item.golden_dandelion.use", "item.golden_dandelion.unuse",
            "block.note_block.trumpet", "block.note_block.trumpet_exposed",
            "block.note_block.trumpet_weathered", "block.note_block.trumpet_oxidized",
            "entity.cat_royal.ambient", "entity.cat_royal.beg_for_food", "entity.cat_royal.death",
            "entity.cat_royal.eat", "entity.cat_royal.hiss", "entity.cat_royal.hurt", "entity.cat_royal.purr",
            "entity.cat_royal.purreow", "entity.cat_royal.stray_ambient",
            "entity.baby_cat.ambient", "entity.baby_cat.beg_for_food", "entity.baby_cat.death",
            "entity.baby_cat.eat", "entity.baby_cat.hiss", "entity.baby_cat.hurt", "entity.baby_cat.purr",
            "entity.baby_cat.purreow", "entity.baby_cat.stray_ambient",
            "entity.chicken_picky.ambient", "entity.chicken_picky.death", "entity.chicken_picky.hurt",
            "entity.baby_chicken.ambient", "entity.baby_chicken.death", "entity.baby_chicken.hurt",
            "entity.baby_chicken.step",
            "entity.cow_moody.ambient", "entity.cow_moody.death", "entity.cow_moody.hurt", "entity.cow_moody.step",
            "entity.pig.eat",
            "entity.pig_mini.ambient", "entity.pig_mini.death", "entity.pig_mini.eat", "entity.pig_mini.hurt",
            "entity.pig_big.ambient", "entity.pig_big.death", "entity.pig_big.eat", "entity.pig_big.hurt",
            "entity.baby_pig.ambient", "entity.baby_pig.death", "entity.baby_pig.eat", "entity.baby_pig.hurt",
            "entity.baby_pig.step",
            "entity.baby_horse.ambient", "entity.baby_horse.angry", "entity.baby_horse.breathe",
            "entity.baby_horse.death", "entity.baby_horse.eat", "entity.baby_horse.hurt", "entity.baby_horse.land",
            "entity.baby_horse.step",
            "entity.baby_wolf.ambient", "entity.baby_wolf.death", "entity.baby_wolf.growl", "entity.baby_wolf.hurt",
            "entity.baby_wolf.pant", "entity.baby_wolf.step", "entity.baby_wolf.whine");

    private BabiesContent() {
    }

    public static boolean on() {
        return ModConfig.babies.enabled;
    }

    /** @return our sound event for a 26.3 event name, or null when the stream is off */
    public static SoundEvent sound(String name) {
        return SOUNDS.get(name);
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!on()) {
            return;
        }
        goldenDandelion = new BlockGoldenDandelion();
        name(goldenDandelion, "golden_dandelion");
        event.getRegistry().register(goldenDandelion);
        pottedGoldenDandelion = new BlockPottedGoldenDandelion();
        name(pottedGoldenDandelion, "potted_golden_dandelion");
        event.getRegistry().register(pottedGoldenDandelion);
        Blocks.FIRE.setFireInfo(goldenDandelion, 60, 100);
    }

    private static void name(Block block, String name) {
        block.setRegistryName(GameDropsBackport.MOD_ID, name);
        block.setTranslationKey(GameDropsBackport.MOD_ID + "." + name);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!on()) {
            return;
        }
        goldenDandelionItem = new ItemBlock(goldenDandelion).setRegistryName(goldenDandelion.getRegistryName());
        goldenDandelionItem.setTranslationKey(goldenDandelion.getTranslationKey().substring(5));
        goldenDandelionItem.setCreativeTab(CreativeTabs.DECORATIONS);
        event.getRegistry().register(goldenDandelionItem);
        pottedGoldenDandelion.setPlant(goldenDandelionItem);
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (!on()) {
            return;
        }
        for (String name : SOUND_NAMES) {
            ResourceLocation id = new ResourceLocation(GameDropsBackport.MOD_ID, name);
            SoundEvent sound = new SoundEvent(id).setRegistryName(id);
            SOUNDS.put(name, sound);
            event.getRegistry().register(sound);
        }
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!on()) {
            return;
        }
        ItemStack dandelion = new ItemStack(Blocks.YELLOW_FLOWER, 1, BlockFlower.EnumFlowerType.DANDELION.getMeta());
        shaped(event, "golden_dandelion", new ItemStack(goldenDandelionItem),
                "###", "#I#", "###", '#', "nuggetGold", 'I', dandelion);
        shapeless(event, "yellow_dye_from_golden_dandelion",
                new ItemStack(Items.DYE, 1, EnumDyeColor.YELLOW.getDyeDamage()), goldenDandelionItem);
        shaped(event, "name_tag", new ItemStack(Items.NAME_TAG), " X", "# ", '#', Items.PAPER, 'X', metalNuggets());
    }

    /** 26.3's {@code #minecraft:metal_nuggets}: copper, iron and gold nuggets, any mod's, by ore name. */
    private static Ingredient metalNuggets() {
        List<Ingredient> parts = new ArrayList<Ingredient>();
        for (String ore : new String[]{"nuggetCopper", "nuggetIron", "nuggetGold"}) {
            parts.add(new OreIngredient(ore));
        }
        return new CompoundIngredient(parts) {
        };
    }

    private static void shaped(RegistryEvent.Register<IRecipe> event, String name, ItemStack out, Object... recipe) {
        ResourceLocation id = new ResourceLocation(GameDropsBackport.MOD_ID, name);
        event.getRegistry().register(new ShapedOreRecipe(id, out, recipe).setRegistryName(id));
    }

    private static void shapeless(RegistryEvent.Register<IRecipe> event, String name, ItemStack out, Object... recipe) {
        ResourceLocation id = new ResourceLocation(GameDropsBackport.MOD_ID, name);
        event.getRegistry().register(new ShapelessOreRecipe(id, out, recipe).setRegistryName(id));
    }

    /** 26.1 took the name tag out of the woodland mansion's chests. */
    @SubscribeEvent
    public static void onLootTable(LootTableLoadEvent event) {
        if (!on() || !LootTableList.CHESTS_WOODLAND_MANSION.equals(event.getName())) {
            return;
        }
        for (String name : new String[]{"main", "pool1", "pool2", "pool3"}) {
            LootPool pool = event.getTable().getPool(name);
            if (pool != null) {
                pool.removeEntry("minecraft:name_tag");
            }
        }
    }

    /** From init: the wandering trader's two trades (read by Village &amp; Pillage, if installed). */
    public static void init() {
        if (!on()) {
            return;
        }
        TradeRegistry.addTraderTrade(TradeRegistry.TraderPool.ORDINARY, new TradeRegistry.TraderTrade() {
            @Override
            public MerchantRecipe offer(java.util.Random random) {
                return new MerchantRecipe(new ItemStack(Items.EMERALD, 2), ItemStack.EMPTY,
                        new ItemStack(goldenDandelionItem), 0, 12);
            }
        });
        TradeRegistry.addTraderTrade(TradeRegistry.TraderPool.ORDINARY, new TradeRegistry.TraderTrade() {
            @Override
            public MerchantRecipe offer(java.util.Random random) {
                return new MerchantRecipe(new ItemStack(Items.EMERALD), ItemStack.EMPTY,
                        new ItemStack(Items.NAME_TAG), 0, 5);
            }
        });
    }
}
