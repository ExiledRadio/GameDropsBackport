package com.exiledradio.gamedropsbackport.sulfurcaves;

import com.exiledradio.gamedropsbackport.GameDropsBackport;

import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 26.2's sulfur caves over Caves &amp; Cliffs' 1.18 terrain, until that terrain knows the biome itself
 * ({@code docs/CORE_REQUESTS.md} SC-1): its climate noise is 1.18's, which 26.2 still uses, so 26.2's climate box picks
 * the same columns and depths vanilla would. Reached by reflection: the generator's {@code biomes3d}, its
 * {@code BiomeSource} and that source's {@code OverworldNoise}, from which we build our own {@code ColumnCache}.
 *
 * <p>Where the terrain's own biome at a quart is lush caves, dripstone caves or the deep dark, that biome stays: the
 * lush caves' box overlaps ours (humidity 0.7 and up, distance 0 to both), the others never do.
 */
final class ModernClimate {

    private static final String GENERATOR = ".terrain.ModernGenerator";
    private static final String CORE = "com.exiledradio.cavesandcliffsbackport.terrain.core.";

    private static boolean looked, broken;
    private static Field biomes3d, source, noiseField;
    private static Method sourceBiome, columnsGet, depth, biomeName;
    private static Constructor<?> columnCache;
    private static int colContinents, colErosion, colRidges, colOffset, colSize;

    /** Per world: {source, columns}; the columns are ours, the source the terrain's (used under its lock). */
    private static final Map<World, Object[]> STATE = new WeakHashMap<World, Object[]>();

    private ModernClimate() {
    }

    static boolean applies(World world) {
        return state(world) != null;
    }

    /** @return true for a world with Caves &amp; Cliffs' 1.18 terrain, whose own climate places the sulfur caves */
    /** @return whether the world is made by Caves &amp; Cliffs' 1.18 terrain (its own sulfur caves or not) */
    static boolean onModernTerrain(World world) {
        if (!(world instanceof WorldServer)) {
            return false;
        }
        Object generator = ((WorldServer) world).getChunkProvider().chunkGenerator;
        return generator != null && generator.getClass().getName().endsWith(GENERATOR);
    }

    static boolean modern(World world) {
        return state(world) != null;
    }

    @Nullable
    private static Object[] state(World world) {
        if (!(world instanceof WorldServer)) {
            return null;
        }
        Object generator = ((WorldServer) world).getChunkProvider().chunkGenerator;
        if (generator == null || !generator.getClass().getName().endsWith(GENERATOR) || SulfurCaves.terrainHasSulfurCaves(world)) {
            return null;
        }
        synchronized (ModernClimate.class) {
            if (broken) {
                return null;
            }
            Object[] s = STATE.get(world);
            if (s != null) {
                return s;
            }
            try {
                if (!looked) {
                    looked = true;
                    biomes3d = generator.getClass().getDeclaredField("biomes3d");
                    biomes3d.setAccessible(true);
                    source = biomes3d.getType().getDeclaredField("source");
                    source.setAccessible(true);
                    noiseField = source.getType().getDeclaredField("noise");
                    noiseField.setAccessible(true);
                    sourceBiome = source.getType().getMethod("biome", int.class, int.class, int.class);
                    Class<?> noise = Class.forName(CORE + "OverworldNoise");
                    Class<?> cache = Class.forName(CORE + "ColumnCache");
                    columnCache = cache.getConstructor(noise);
                    columnsGet = cache.getMethod("get", int.class, int.class, double[].class);
                    depth = noise.getMethod("depth", int.class, double.class);
                    colContinents = noise.getField("COL_CONTINENTS").getInt(null);
                    colErosion = noise.getField("COL_EROSION").getInt(null);
                    colRidges = noise.getField("COL_RIDGES").getInt(null);
                    colOffset = noise.getField("COL_OFFSET").getInt(null);
                    colSize = noise.getField("COL_SIZE").getInt(null);
                    biomeName = Class.forName(CORE + "Biomes").getMethod("name", int.class);
                }
                Object chunks = biomes3d.get(generator);
                Object src;
                synchronized (chunks) {
                    src = source.get(chunks);
                }
                s = new Object[] {chunks, src, columnCache.newInstance(noiseField.get(src))};
                STATE.put(world, s);
                return s;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                broken = true;
                GameDropsBackport.LOGGER.warn("Caves & Cliffs' climate is out of reach; its worlds get sulfur caves by our own regions.", e);
                return null;
            }
        }
    }

    /** The column's mask by 26.2's box, quart by quart, as 1.18's biome lookup reads it. */
    @Nullable
    static boolean[] mask(World world, int x, int z, int min, int height) {
        Object[] s = state(world);
        if (s == null) {
            return LegacyRegions.mask(world, x, z, min, height);
        }
        int qx = x >> 2, qz = z >> 2;
        double[] col = new double[colSize];
        try {
            synchronized (s[2]) {
                columnsGet.invoke(s[2], qx << 2, qz << 2, col);
            }
            double c = (float) col[colContinents], e = (float) col[colErosion], w = (float) col[colRidges];
            if (c < -0.19 || c > 0.55 || e < 0.45 || w < -1.1 || w > -0.85) {
                return null;
            }
            boolean[] mask = null;
            for (int qy = min >> 2; qy < (min + height) >> 2; qy++) {
                double d = (float) ((Double) depth.invoke(null, qy << 2, col[colOffset])).doubleValue();
                if (d < SulfurCaves.DEPTH_MIN || d > SulfurCaves.DEPTH_MAX) {
                    continue;
                }
                int id;
                synchronized (s[0]) {
                    id = (Integer) sourceBiome.invoke(s[1], qx, qy, qz);
                }
                String name = (String) biomeName.invoke(null, id);
                if ("lush_caves".equals(name) || "dripstone_caves".equals(name) || "deep_dark".equals(name)) {
                    continue;
                }
                if (mask == null) {
                    mask = new boolean[height];
                }
                for (int y = qy << 2; y < (qy << 2) + 4; y++) {
                    if (y >= min && y < min + height) {
                        mask[y - min] = true;
                    }
                }
            }
            return mask;
        } catch (ReflectiveOperationException | RuntimeException e) {
            synchronized (ModernClimate.class) {
                broken = true;
            }
            GameDropsBackport.LOGGER.warn("Caves & Cliffs' climate failed; its worlds get sulfur caves by our own regions.", e);
            return LegacyRegions.mask(world, x, z, min, height);
        }
    }
}
