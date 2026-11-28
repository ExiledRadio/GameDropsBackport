package com.exiledradio.gamedropsbackport.icecaves;

import com.exiledradio.backportcore.trade.TradeRegistry;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.ModConfig;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundEvent;
import net.minecraft.village.MerchantRecipe;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.Random;

/**
 * 26.4 stream 1: the ice caves and their blocks.
 *
 * <h2>Registry names (26.4's)</h2>
 * {@code icicle}, {@code ice_crystal}, {@code brittle_ice}, in this mod's domain. The biome and its generation are
 * {@link IceCaves} (where) and {@code IceCavesGen} (what).
 *
 * <h2>Trades</h2>
 * Village &amp; Pillage's wandering trader, through Backport Core's {@link TradeRegistry}, common pool: 1 ice crystal
 * for 2 emeralds and 2 icicles for 1 emerald, 5 uses each, 1 experience.
 */
@Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID)
public final class IceCavesContent {

    public static BlockIcicle icicle;
    public static BlockIceCrystal iceCrystal;
    public static BlockBrittleIce brittleIce;
    public static BiomeIceCaves biome;

    private IceCavesContent() {
    }

    public static boolean enabled() {
        return ModConfig.iceCaves.enabled;
    }

    public static void preInit() {
        if (enabled()) {
            MinecraftForge.EVENT_BUS.register(new IceCavesEvents());
            if (net.minecraftforge.fml.common.FMLCommonHandler.instance().getSide().isClient()) {
                com.exiledradio.gamedropsbackport.icecaves.client.IceCavesClientWorld.init();
            }
        }
    }

    public static void init() {
        if (!enabled()) {
            return;
        }
        net.minecraftforge.fml.common.registry.GameRegistry.registerWorldGenerator(new IceCavesGen(), 21);
        IceCavesLocate.register();
        TradeRegistry.addTraderTrade(TradeRegistry.TraderPool.ORDINARY, sell(iceCrystal, 1, 2, 5));
        TradeRegistry.addTraderTrade(TradeRegistry.TraderPool.ORDINARY, sell(icicle, 2, 1, 5));
    }

    private static TradeRegistry.TraderTrade sell(final Block block, final int count, final int emeralds, final int uses) {
        return new TradeRegistry.TraderTrade() {
            @Override
            public boolean available() {
                return block != null;
            }

            @Override
            public MerchantRecipe offer(Random random) {
                return new MerchantRecipe(new ItemStack(Items.EMERALD, emeralds), ItemStack.EMPTY, new ItemStack(block, count), 0, uses);
            }
        };
    }

    public static void postInit() {
        if (biome != null) {
            biome.getSpawnableList(net.minecraft.entity.EnumCreatureType.MONSTER).clear();
            biome.getSpawnableList(net.minecraft.entity.EnumCreatureType.MONSTER).addAll(BiomeIceCaves.monsters());
        }
    }

    @SubscribeEvent
    public static void registerBiomes(RegistryEvent.Register<net.minecraft.world.biome.Biome> event) {
        if (!enabled()) {
            return;
        }
        biome = new BiomeIceCaves();
        biome.setRegistryName(new net.minecraft.util.ResourceLocation(GameDropsBackport.MOD_ID, "ice_caves"));
        event.getRegistry().register(biome);
        // 26.4's water_color and water_fog_color, for whichever mod draws 1.13's water (Update Aquatic).
        com.exiledradio.backportcore.water.WaterColors.register(biome, BiomeIceCaves.WATER, BiomeIceCaves.FOG);
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Block> r = event.getRegistry();
        icicle = Reg.block(r, "icicle", new BlockIcicle(Reg.soundType("icicle"), Reg.sound("block.icicle.land")), "pickaxe");
        iceCrystal = Reg.block(r, "ice_crystal", new BlockIceCrystal(Reg.soundType("ice_crystal"), Reg.sound("block.ice_crystal.ambient")),
                "pickaxe");
        brittleIce = Reg.block(r, "brittle_ice", new BlockBrittleIce(Reg.soundType("brittle_ice"), Reg.sound("block.brittle_ice.crack")),
                "pickaxe");
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Item> r = event.getRegistry();
        Reg.blockItem(r, brittleIce);
        Reg.blockItem(r, icicle);
        Reg.blockItem(r, iceCrystal);
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
}
