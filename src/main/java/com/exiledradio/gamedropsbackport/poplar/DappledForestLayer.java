package com.exiledradio.gamedropsbackport.poplar;

import com.exiledradio.gamedropsbackport.GameDropsBackport;

import net.minecraft.init.Biomes;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.ChunkGeneratorSettings;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.GenLayerBiome;
import net.minecraft.world.gen.layer.GenLayerRiverMix;
import net.minecraft.world.gen.layer.IntCache;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.WorldTypeEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Places the dappled forest on 1.12.2's biome layers where 26.3 places it: plains of the cool climate.
 *
 * <h2>Why this way</h2>
 * 26.3 put the dappled forest in {@code OverworldBiomeBuilder.MIDDLE_BIOMES_VARIANT[1][0]}: the variant (weirdness above
 * 0) of the cool temperature band's driest slot, where plains grow otherwise, so dappled forests are plains ground
 * beside taigas, snowy plains and old growth forests. 1.12.2's biome layers have that band too: {@code GenLayerBiome}
 * draws each cool cell (climate 3, beside the icy one) from the cool list, forest, extreme hills, taiga or plains. So
 * (decision 1 of the plan, Tricky Trials' pale garden pattern) a cool cell that {@code GenLayerBiome} made plains
 * becomes a dappled forest one time in {@link #CHANCE}, for weirdness's half and the driest of plains' two humidity
 * slots. The plains of the warm and desert climates stay plains. Everything else is untouched, so 1.12.2's map is the
 * same around it; the dappled forest then passes through vanilla's own zooms, edges, rivers and shores as any biome.
 * Chunks already generated keep their biome.
 *
 * <h2>How</h2>
 * Forge's {@link WorldTypeEvent.InitBiomeGens} hands over the final layers; the chain is walked down (through
 * {@link GenLayerRiverMix}'s biome input and every {@code parent}) to vanilla's {@link GenLayerBiome}, and the layer
 * above it is given {@link Dappled} in its place, which reads the biome and the climate it came from at the same
 * cells. A world type with no {@code GenLayerBiome} (a single biome world, other mods' own generators) gets none.
 *
 * <p>Caves &amp; Cliffs' 1.18 terrain places biomes by climate and does not use these layers; it can place
 * {@code gamedropsbackport:dappled_forest} where 26.3 does and decorate it through {@link PoplarFeatures#vegetation}.
 */
public final class DappledForestLayer {

    /** A cool plains cell in this many becomes dappled forest. */
    static final int CHANCE = 3;
    /** {@code GenLayerBiome}'s climate for the cool list. */
    static final int COOL = 3;

    private static Field parent;
    private static Field biomeChain;
    private static Field settings;

    private DappledForestLayer() {
    }

    /** Pre-init: the event is on Forge's terrain bus, which annotations do not reach. */
    public static void register() {
        MinecraftForge.TERRAIN_GEN_BUS.register(DappledForestLayer.class);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInitBiomeGens(WorldTypeEvent.InitBiomeGens event) {
        BiomeDappledForest dappled = PoplarContent.biome;
        GenLayer[] layers = event.getNewBiomeGens();
        if (dappled == null || layers == null || layers.length == 0) {
            return;
        }
        try {
            if (parent == null) {
                parent = ObfuscationReflectionHelper.findField(GenLayer.class, "field_75909_a");
                biomeChain = ObfuscationReflectionHelper.findField(GenLayerRiverMix.class, "field_75910_b");
                settings = ObfuscationReflectionHelper.findField(GenLayerBiome.class, "field_175973_g");
            }
            if (insert(layers[0], dappled, event.getSeed())) {
                return;
            }
        } catch (RuntimeException | IllegalAccessException e) {
            GameDropsBackport.LOGGER.warn("Could not add the dappled forest to this world's biome layers.", e);
            return;
        }
        GameDropsBackport.LOGGER.debug("No GenLayerBiome in {}'s biome layers: no dappled forest.", event.getWorldType().getName());
    }

    /** Walks down to the layer whose input is {@link GenLayerBiome} and gives it {@link Dappled} in between. */
    static boolean insert(GenLayer top, BiomeDappledForest dappled, long seed) throws IllegalAccessException {
        Map<GenLayer, Boolean> seen = new IdentityHashMap<GenLayer, Boolean>();
        GenLayer at = top;
        while (at != null && seen.put(at, Boolean.TRUE) == null) {
            GenLayer below = at instanceof GenLayerRiverMix ? (GenLayer) biomeChain.get(at) : (GenLayer) parent.get(at);
            if (below instanceof Dappled) {
                return true;
            }
            if (below instanceof GenLayerBiome) {
                ChunkGeneratorSettings s = (ChunkGeneratorSettings) settings.get(below);
                if (s != null && s.fixedBiome >= 0) {
                    return false;
                }
                Dappled layer = new Dappled((GenLayerBiome) below, (GenLayer) parent.get(below), dappled);
                layer.initWorldGenSeed(seed);
                if (at instanceof GenLayerRiverMix) {
                    biomeChain.set(at, layer);
                } else {
                    parent.set(at, layer);
                }
                return true;
            }
            at = below;
        }
        return false;
    }

    /** {@link GenLayerBiome}'s output, with one cool plains cell in {@link #CHANCE} made dappled forest. */
    static final class Dappled extends GenLayer {
        private final GenLayerBiome biomes;
        @Nullable
        private final GenLayer climate;
        private final int plains;
        private final int dappled;

        Dappled(GenLayerBiome biomes, @Nullable GenLayer climate, BiomeDappledForest dappled) {
            super(26300L);
            this.biomes = biomes;
            this.climate = climate;
            this.parent = biomes;
            this.plains = Biome.getIdForBiome(Biomes.PLAINS);
            this.dappled = Biome.getIdForBiome(dappled);
        }

        @Override
        public int[] getInts(int x, int z, int width, int depth) {
            int[] in = this.biomes.getInts(x, z, width, depth);
            if (this.climate == null) {
                return in;
            }
            int[] cl = this.climate.getInts(x, z, width, depth);
            int[] out = IntCache.getIntCache(width * depth);
            for (int dz = 0; dz < depth; dz++) {
                for (int dx = 0; dx < width; dx++) {
                    int i = dx + dz * width;
                    out[i] = in[i];
                    if (in[i] == this.plains && (cl[i] & -3841) == COOL) {
                        initChunkSeed(x + dx, z + dz);
                        if (nextInt(CHANCE) == 0) {
                            out[i] = this.dappled;
                        }
                    }
                }
            }
            return out;
        }
    }
}
