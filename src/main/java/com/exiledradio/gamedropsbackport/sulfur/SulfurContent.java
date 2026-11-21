package com.exiledradio.gamedropsbackport.sulfur;

import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.ModConfig;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraftforge.registries.IForgeRegistry;

/**
 * Stream 2: 26.2's sulfur and cinnabar blocks, potent sulfur, sulfur spikes and the music disc Bounce.
 *
 * <h2>Registry names (26.3's, for the streams that use these blocks by name)</h2>
 * <ul>
 *   <li>{@code sulfur}, {@code polished_sulfur}, {@code sulfur_bricks}, {@code chiseled_sulfur}, and the same four
 *       for cinnabar;</li>
 *   <li>{@code <x>_stairs}, {@code <x>_slab} (+ our {@code <x>_double_slab}), {@code <x>_wall} for x in sulfur,
 *       polished_sulfur, sulfur_brick, cinnabar, polished_cinnabar, cinnabar_brick;</li>
 *   <li>{@code potent_sulfur}, {@code sulfur_spike}, {@code music_disc_bounce}.</li>
 * </ul>
 * All in this mod's domain ({@code gamedropsbackport:}).
 *
 * <h2>Numbers (26.3)</h2>
 * Every stone block: hardness 1.5, blast resistance 6, pickaxe needed, three sound types (sulfur, cinnabar, potent
 * sulfur); the spike: 1.5 and 3, no tool needed. Crafting as 26.3; a 1.14 stonecutter derives its cuts from these
 * recipes by itself.
 */
@Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID)
public final class SulfurContent {

    public static Block sulfur;
    public static Block polishedSulfur;
    public static Block sulfurBricks;
    public static Block chiseledSulfur;
    public static Block cinnabar;
    public static Block polishedCinnabar;
    public static Block cinnabarBricks;
    public static Block chiseledCinnabar;
    public static Shapes sulfurShapes;
    public static Shapes polishedSulfurShapes;
    public static Shapes sulfurBrickShapes;
    public static Shapes cinnabarShapes;
    public static Shapes polishedCinnabarShapes;
    public static Shapes cinnabarBrickShapes;
    public static BlockPotentSulfur potentSulfur;
    public static BlockSulfurSpike sulfurSpike;
    public static ItemSulfurDisc bounce;

    private SulfurContent() {
    }

    public static boolean enabled() {
        return ModConfig.sulfur.enabled;
    }

    /** From the mod's pre-init. */
    public static void preInit() {
        if (enabled()) {
            MinecraftForge.EVENT_BUS.register(new SulfurEvents());
        }
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Block> r = event.getRegistry();
        SoundType sulfurSound = Reg.soundType("sulfur");
        SoundType cinnabarSound = Reg.soundType("cinnabar");

        sulfur = Reg.block(r, "sulfur", new BlockSulfurStone(MapColor.YELLOW, sulfurSound), "pickaxe");
        sulfurShapes = Shapes.register(r, "sulfur", sulfur, true, true, true);
        polishedSulfur = Reg.block(r, "polished_sulfur", new BlockSulfurStone(MapColor.YELLOW, sulfurSound), "pickaxe");
        polishedSulfurShapes = Shapes.register(r, "polished_sulfur", polishedSulfur, true, true, true);
        sulfurBricks = Reg.block(r, "sulfur_bricks", new BlockSulfurStone(MapColor.YELLOW, sulfurSound), "pickaxe");
        sulfurBrickShapes = Shapes.register(r, "sulfur_brick", sulfurBricks, true, true, true);
        chiseledSulfur = Reg.block(r, "chiseled_sulfur", new BlockSulfurStone(MapColor.YELLOW, sulfurSound), "pickaxe");

        cinnabar = Reg.block(r, "cinnabar", new BlockSulfurStone(MapColor.RED, cinnabarSound), "pickaxe");
        cinnabarShapes = Shapes.register(r, "cinnabar", cinnabar, true, true, true);
        polishedCinnabar = Reg.block(r, "polished_cinnabar", new BlockSulfurStone(MapColor.RED, cinnabarSound), "pickaxe");
        polishedCinnabarShapes = Shapes.register(r, "polished_cinnabar", polishedCinnabar, true, true, true);
        cinnabarBricks = Reg.block(r, "cinnabar_bricks", new BlockSulfurStone(MapColor.RED, cinnabarSound), "pickaxe");
        cinnabarBrickShapes = Shapes.register(r, "cinnabar_brick", cinnabarBricks, true, true, true);
        chiseledCinnabar = Reg.block(r, "chiseled_cinnabar", new BlockSulfurStone(MapColor.RED, cinnabarSound), "pickaxe");

        potentSulfur = Reg.block(r, "potent_sulfur", new BlockPotentSulfur(Reg.soundType("potent_sulfur")), "pickaxe");
        sulfurSpike = Reg.block(r, "sulfur_spike", new BlockSulfurSpike(Reg.soundType("sulfur_spike"), sulfur), "pickaxe");
        GameRegistry.registerTileEntity(TilePotentSulfur.class, Reg.id("potent_sulfur"));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Item> r = event.getRegistry();
        Reg.blockItem(r, sulfur);
        sulfurShapes.registerItems(r);
        Reg.blockItem(r, polishedSulfur);
        polishedSulfurShapes.registerItems(r);
        Reg.blockItem(r, sulfurBricks);
        sulfurBrickShapes.registerItems(r);
        Reg.blockItem(r, chiseledSulfur);
        Reg.blockItem(r, cinnabar);
        cinnabarShapes.registerItems(r);
        Reg.blockItem(r, polishedCinnabar);
        polishedCinnabarShapes.registerItems(r);
        Reg.blockItem(r, cinnabarBricks);
        cinnabarBrickShapes.registerItems(r);
        Reg.blockItem(r, chiseledCinnabar);
        Reg.blockItem(r, potentSulfur);
        Reg.blockItem(r, sulfurSpike, new ItemSulfurSpike(sulfurSpike));
        bounce = Reg.item(r, "music_disc_bounce", new ItemSulfurDisc("bounce", Reg.sound("music_disc.bounce")));
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
        stone(r, "sulfur", sulfur, polishedSulfur, sulfurBricks, chiseledSulfur, sulfurShapes, polishedSulfurShapes, sulfurBrickShapes);
        stone(r, "cinnabar", cinnabar, polishedCinnabar, cinnabarBricks, chiseledCinnabar, cinnabarShapes, polishedCinnabarShapes,
                cinnabarBrickShapes);
        Object[] nine = new Object[9];
        java.util.Arrays.fill(nine, sulfur);
        ShapelessOreRecipe potent = new ShapelessOreRecipe(Reg.id("sulfur"), new ItemStack(potentSulfur), nine);
        potent.setRegistryName(Reg.id("potent_sulfur"));
        r.register(potent);
        shaped(r, "sulfur_from_sulfur_spikes", new ItemStack(sulfur), "##", "##", '#', sulfurSpike);
    }

    private static void stone(IForgeRegistry<IRecipe> r, String name, Block base, Block polished, Block bricks, Block chiseled,
                              Shapes baseShapes, Shapes polishedShapes, Shapes brickShapes) {
        shapes(r, base, baseShapes);
        shaped(r, "polished_" + name, new ItemStack(polished, 4), "##", "##", '#', base);
        shapes(r, polished, polishedShapes);
        shaped(r, name + "_bricks", new ItemStack(bricks, 4), "##", "##", '#', polished);
        shapes(r, bricks, brickShapes);
        shaped(r, "chiseled_" + name, new ItemStack(chiseled), "#", "#", '#', baseShapes.slab);
    }

    /** Vanilla's three shape recipes: 6 to 4 stairs, 3 to 6 slabs, 6 to 6 walls. */
    private static void shapes(IForgeRegistry<IRecipe> r, Block base, Shapes s) {
        shaped(r, s.prefix + "_stairs", new ItemStack(s.stairs, 4), "#  ", "## ", "###", '#', base);
        shaped(r, s.prefix + "_slab", new ItemStack(s.slab, 6), "###", '#', base);
        shaped(r, s.prefix + "_wall", new ItemStack(s.wall, 6), "###", "###", '#', base);
    }

    private static void shaped(IForgeRegistry<IRecipe> r, String name, ItemStack out, Object... recipe) {
        ShapedOreRecipe recipeObject = new ShapedOreRecipe(new ResourceLocation(GameDropsBackport.MOD_ID, "sulfur"), out, recipe);
        recipeObject.setRegistryName(Reg.id(name));
        r.register(recipeObject);
    }
}
