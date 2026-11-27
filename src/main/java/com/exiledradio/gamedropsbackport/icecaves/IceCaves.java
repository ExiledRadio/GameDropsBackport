package com.exiledradio.gamedropsbackport.icecaves;

import com.exiledradio.backportcore.terrain.WorldHeights;
import com.exiledradio.backportcore.worldgen.FlatWorlds;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.ModConfig;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Where the ice caves are. 26.4 adds them to {@code OverworldBiomeBuilder.addUndergroundBiomes}: temperature -1 to
 * -0.8, humidity -1 to 0.7, continentalness -1.05 to -0.19 (deep ocean and ocean), any erosion, weirdness 0.5 to 1.1,
 * depth 0.2 to 0.9: under the coldest oceans. 1.12.2 has no underground biomes, so a block's answer comes from:
 * <ul>
 *   <li><b>1.18 terrain with 26.4's ice caves</b> (Caves &amp; Cliffs' worlds made with its {@code DEEP_FREEZE}
 *       edition): its own 3D biome. The terrain places them by 26.4's climate, lays their bands and names the biome
 *       ({@link #terrainHasIceCaves}).</li>
 *   <li><b>1.18 terrain</b> made before that edition: the box over its own climate noise at its real heights
 *       (continentalness, weirdness, depth), with the temperature band read from its surface biome: frozen and deep
 *       frozen ocean, 1.18's coldest oceans (temperature below -0.45). Its lush caves, dripstone caves and deep dark
 *       stay.</li>
 *   <li><b>Any other overworld</b>: our own patches ({@link #patchAt}), one 512-block cell in 3, a ragged disc of radius
 *       20 to 69, under snowy oceans (frozen ocean, Update Aquatic's deep frozen ocean; without Update Aquatic, whose
 *       frozen oceans 1.12.2 hardly makes, under any snowy biome), at 26.4's depth below the column's ground: 26 to 115
 *       blocks where the world goes down to -64, two thirds of that (17 to 77) in a 0-255 world.</li>
 *   <li><b>Superflat</b>: the preset's biome everywhere, so only where that biome is the ice caves.</li>
 * </ul>
 */
public final class IceCaves {

    public static final String BIOME = "minecraft:ice_caves";
    static final double DEPTH_MIN = 0.2, DEPTH_MAX = 0.9;
    static final int CELL = 512, CHANCE = 3, MIN_RADIUS = 20, RADIUS_SPREAD = 50;

    private static final Map<World, Map<Long, boolean[]>> MASKS = new WeakHashMap<World, Map<Long, boolean[]>>();
    private static final Map<World, int[]> HEIGHTS = new WeakHashMap<World, int[]>();

    private IceCaves() {
    }

    public static boolean enabled() {
        return ModConfig.iceCaves.enabled;
    }

    public static boolean generates(World world) {
        return enabled() && world.provider.getDimension() == 0 && !world.isRemote;
    }

    public static boolean at(World world, BlockPos pos) {
        if (!generates(world)) {
            return false;
        }
        if (terrainHasIceCaves(world)) {
            // One block's 3D biome: a lookup (mob spawning asks for blocks all around every player).
            return BIOME.equals(com.exiledradio.gamedropsbackport.sulfurcaves.SulfurCaves.modernBiome(world, pos));
        }
        boolean[] mask = mask(world, pos.getX(), pos.getZ());
        int i = pos.getY() - minY(world);
        return mask != null && i >= 0 && i < mask.length && mask[i];
    }

    @Nullable
    public static boolean[] mask(World world, int x, int z) {
        if (terrainHasIceCaves(world)) {
            // The 1.18 terrain's biomes come in 4x4x4 cells: one mask serves the cell's sixteen columns.
            x &= ~3;
            z &= ~3;
        }
        long key = (long) x << 32 | (z & 0xFFFFFFFFL);
        Map<Long, boolean[]> cache;
        synchronized (MASKS) {
            cache = MASKS.get(world);
            if (cache == null) {
                cache = new LinkedHashMap<Long, boolean[]>(1024, 0.75F, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<Long, boolean[]> eldest) {
                        return size() > 8192;
                    }
                };
                MASKS.put(world, cache);
            }
            if (cache.containsKey(key)) {
                return cache.get(key);
            }
        }
        boolean[] mask = compute(world, x, z);
        synchronized (MASKS) {
            cache.put(key, mask);
        }
        return mask;
    }

    public static void forget(World world) {
        synchronized (MASKS) {
            MASKS.remove(world);
        }
        synchronized (EDITIONS) {
            EDITIONS.remove(world);
        }
    }

    @Nullable
    private static boolean[] compute(World world, int x, int z) {
        int min = minY(world), height = maxYExclusive(world) - min;
        if (FlatWorlds.isFlat(world)) {
            if (world.getBiomeProvider().getBiome(new BlockPos(x, 0, z), null) != IceCavesContent.biome) {
                return null;
            }
            boolean[] all = new boolean[height];
            java.util.Arrays.fill(all, true);
            return all;
        }
        if (terrainHasIceCaves(world)) {
            // The 1.18 terrain names the ice caves itself: its 3D biome is the answer.
            boolean[] mask = null;
            for (int y = min; y < min + height; y += 4) {
                if (BIOME.equals(com.exiledradio.gamedropsbackport.sulfurcaves.SulfurCaves.modernBiome(world, new BlockPos(x, y, z)))) {
                    if (mask == null) {
                        mask = new boolean[height];
                    }
                    for (int i = y - min; i < Math.min(height, y - min + 4); i++) {
                        mask[i] = true;
                    }
                }
            }
            return mask;
        }
        boolean[] modern = Modern.mask(world, x, z, min, height);
        if (modern != null || Modern.applies(world)) {
            return modern;
        }
        return legacy(world, x, z, min, height);
    }

    // ------------------------------------------------------------------------------------------------- our patches

    @Nullable
    static boolean[] legacy(World world, int x, int z, int min, int height) {
        long seed = world.getSeed();
        int[] p = patchAt(seed, x, z);
        if (p == null || !cold(world.getBiomeProvider().getBiome(new BlockPos(p[0], 0, p[1]), null))
                || !cold(world.getBiomeProvider().getBiome(new BlockPos(x, 0, z), null))) {
            return null;
        }
        int ground = ground(world, x, z);
        int top = ground - depthBlocks(world, DEPTH_MIN), bottom = ground - depthBlocks(world, DEPTH_MAX);
        bottom = Math.max(bottom, min + 1);
        if (top <= bottom) {
            return null;
        }
        boolean[] mask = new boolean[height];
        for (int y = bottom; y <= top; y++) {
            mask[y - min] = true;
        }
        return mask;
    }

    /** 26.x depth to blocks below the ground: 128 per unit, two thirds of that in a 0-255 world. */
    static int depthBlocks(World world, double depth) {
        return (int) Math.round(depth * (extended(world) ? 128.0 : 128.0 * 256.0 / 384.0));
    }

    /** The 1.12 stand-in for 26.4's temperature and continentalness: a snowy ocean (any snowy biome without Update Aquatic). */
    static boolean cold(@Nullable Biome biome) {
        if (biome == null || !biome.isSnowyBiome() && biome.getDefaultTemperature() > 0.0F) {
            return false;
        }
        if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.NETHER) || BiomeDictionary.hasType(biome, BiomeDictionary.Type.END)) {
            return false;
        }
        return !Loader.isModLoaded("updateaquaticbackport") || BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN);
    }

    @Nullable
    static int[] patch(long seed, int cellX, int cellZ) {
        long h = mix(seed ^ 0x1CEC4E5L, cellX, cellZ);
        if (Long.remainderUnsigned(h, CHANCE) != 0) {
            return null;
        }
        long h2 = mix(h, 11, 17);
        int x = cellX * CELL + (int) Long.remainderUnsigned(h2, CELL);
        int z = cellZ * CELL + (int) Long.remainderUnsigned(h2 >>> 20, CELL);
        int r = MIN_RADIUS + (int) Long.remainderUnsigned(h2 >>> 40, RADIUS_SPREAD);
        return new int[] {x, z, r};
    }

    @Nullable
    static int[] patchAt(long seed, int x, int z) {
        int cx = Math.floorDiv(x, CELL), cz = Math.floorDiv(z, CELL);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int[] p = patch(seed, cx + dx, cz + dz);
                if (p == null) {
                    continue;
                }
                double ddx = x - p[0], ddz = z - p[1];
                double d = Math.sqrt(ddx * ddx + ddz * ddz) / p[2];
                if (d < 1.0 + (IceNoise.value2(seed ^ 0x1CE5L, x / 24.0, z / 24.0) - 0.5) * 0.66) {
                    return p;
                }
            }
        }
        return null;
    }

    static long mix(long seed, int a, int b) {
        long h = seed ^ (a * 0x9E3779B97F4A7C15L) ^ (b * 0xC2B2AE3D27D4EB4FL);
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        return h ^ h >>> 33;
    }

    // ------------------------------------------------------------------------------------------------- heights

    public static int minY(World world) {
        return heights(world)[0];
    }

    public static int maxYExclusive(World world) {
        return heights(world)[1];
    }

    public static boolean extended(World world) {
        return minY(world) < 0;
    }

    /** @return a 26.4 height in this world: itself where it goes down to -64, else {@code (y + 64) * 256 / 384} */
    public static int worldY(World world, int y264) {
        if (extended(world)) {
            return y264;
        }
        int min = minY(world), height = maxYExclusive(world) - min;
        return min + Math.floorDiv((y264 + 64) * height, 384);
    }

    /** A column's ground: its highest block that blocks movement (the sea floor under an ocean). */
    static int ground(World world, int x, int z) {
        Chunk chunk = world.getChunk(x >> 4, z >> 4);
        int top = Math.min(chunk.getTopFilledSegment() + 15, maxYExclusive(world) - 1);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int y = top; y >= minY(world); y--) {
            IBlockState state = chunk.getBlockState(m.setPos(x, y, z));
            if (state.getMaterial().blocksMovement() && !state.getBlock().isLeaves(state, world, m) && !state.getBlock().isWood(world, m)
                    && state.getMaterial() != net.minecraft.block.material.Material.ICE && state.getMaterial() != net.minecraft.block.material.Material.PACKED_ICE) {
                return y;
            }
        }
        return minY(world) - 1;
    }

    private static int[] heights(World world) {
        synchronized (HEIGHTS) {
            int[] h = HEIGHTS.get(world);
            if (h != null) {
                return h;
            }
        }
        int[] h;
        try (AutoCloseable full = WorldHeights.enterFullHeight()) {
            h = new int[] {WorldHeights.minY(world), WorldHeights.maxYExclusive(world)};
        } catch (Exception e) {
            h = new int[] {WorldHeights.minY(world), WorldHeights.maxYExclusive(world)};
        }
        synchronized (HEIGHTS) {
            HEIGHTS.put(world, h);
        }
        return h;
    }

    // ------------------------------------------------------------------------------------------------- 1.18 terrain

    /** A world's terrain and its editions never change: asked once per world. */
    private static final Map<World, Boolean> EDITIONS = new WeakHashMap<World, Boolean>();
    private static Method editions;
    private static int deepFreeze = -1;

    /**
     * @return true when the world is Caves &amp; Cliffs' 1.18 terrain made with its 26.4 edition
     *     ({@code Editions.DEEP_FREEZE}): the terrain places the ice caves by 26.4's climate, lays their bands and
     *     names the biome, so our climate box ({@link Modern}), the bands at population and our locate step aside. A
     *     1.18 world made before that edition keeps them.
     */
    public static boolean terrainHasIceCaves(World world) {
        if (!(world instanceof WorldServer)) {
            return false;
        }
        synchronized (EDITIONS) {
            Boolean known = EDITIONS.get(world);
            if (known != null) {
                return known;
            }
        }
        boolean has = askTerrain(world);
        synchronized (EDITIONS) {
            EDITIONS.put(world, has);
        }
        return has;
    }

    private static boolean askTerrain(World world) {
        Object generator = ((WorldServer) world).getChunkProvider().chunkGenerator;
        if (generator == null || !generator.getClass().getName().equals("com.exiledradio.cavesandcliffsbackport.terrain.ModernGenerator")) {
            return false;
        }
        try {
            synchronized (IceCaves.class) {
                if (deepFreeze < 0) {
                    deepFreeze = 0;
                    deepFreeze = Class.forName("com.exiledradio.cavesandcliffsbackport.terrain.core.Editions", false,
                            generator.getClass().getClassLoader()).getField("DEEP_FREEZE").getInt(null);
                    editions = generator.getClass().getMethod("editions");
                }
            }
            return deepFreeze != 0 && (((Integer) editions.invoke(generator)) & deepFreeze) != 0;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            // A Caves & Cliffs without the edition.
            return false;
        }
    }

    /** 26.4's box over Caves &amp; Cliffs' 1.18 climate noise, reached by reflection (as the sulfur caves stream does). */
    static final class Modern {
        private static final String GENERATOR = ".terrain.ModernGenerator";
        private static final String CORE = "com.exiledradio.cavesandcliffsbackport.terrain.core.";
        private static boolean looked, broken;
        private static Field biomes3d, source, noiseField;
        private static Method sourceBiome, columnsGet, depth, biomeName;
        private static Constructor<?> columnCache;
        private static int colContinents, colRidges, colOffset, colSize;
        private static final Map<World, Object[]> STATE = new WeakHashMap<World, Object[]>();

        static boolean applies(World world) {
            return state(world) != null;
        }

        @Nullable
        private static Object[] state(World world) {
            if (!(world instanceof WorldServer)) {
                return null;
            }
            Object generator = ((WorldServer) world).getChunkProvider().chunkGenerator;
            if (generator == null || !generator.getClass().getName().endsWith(GENERATOR)) {
                return null;
            }
            synchronized (Modern.class) {
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
                    GameDropsBackport.LOGGER.warn("Caves & Cliffs' climate is out of reach; its worlds get ice caves by our own patches.", e);
                    return null;
                }
            }
        }

        @Nullable
        static boolean[] mask(World world, int x, int z, int min, int height) {
            Object[] s = state(world);
            if (s == null) {
                return null;
            }
            int qx = x >> 2, qz = z >> 2;
            double[] col = new double[colSize];
            try {
                synchronized (s[2]) {
                    columnsGet.invoke(s[2], qx << 2, qz << 2, col);
                }
                double c = (float) col[colContinents], w = (float) col[colRidges];
                if (c < -1.05 || c > -0.19 || w < 0.5 || w > 1.1) {
                    return null;
                }
                String surface;
                synchronized (s[0]) {
                    surface = (String) biomeName.invoke(null, (Integer) sourceBiome.invoke(s[1], qx, 63 >> 2, qz));
                }
                if (!"frozen_ocean".equals(surface) && !"deep_frozen_ocean".equals(surface)) {
                    return null;
                }
                boolean[] mask = null;
                for (int qy = min >> 2; qy < (min + height) >> 2; qy++) {
                    double d = (float) ((Double) depth.invoke(null, qy << 2, col[colOffset])).doubleValue();
                    if (d < DEPTH_MIN || d > DEPTH_MAX) {
                        continue;
                    }
                    String name;
                    synchronized (s[0]) {
                        name = (String) biomeName.invoke(null, (Integer) sourceBiome.invoke(s[1], qx, qy, qz));
                    }
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
                synchronized (Modern.class) {
                    broken = true;
                }
                GameDropsBackport.LOGGER.warn("Caves & Cliffs' climate failed; its worlds get ice caves by our own patches.", e);
                return null;
            }
        }
    }
}
