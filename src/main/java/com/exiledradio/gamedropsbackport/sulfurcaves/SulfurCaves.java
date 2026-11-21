package com.exiledradio.gamedropsbackport.sulfurcaves;

import com.exiledradio.backportcore.terrain.WorldHeights;
import com.exiledradio.backportcore.worldgen.FlatWorlds;
import com.exiledradio.gamedropsbackport.ModConfig;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.BiomeDictionary;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Where the sulfur caves are (decision 2). 26.2 adds them to {@code OverworldBiomeBuilder.addUndergroundBiomes} as an
 * underground biome: continentalness from the coast's -0.19 to the inland's 0.55, erosion 0.45 to 1 (levels 5 and 6),
 * weirdness -1.1 to -0.85, depth 0.2 to 0.9. 1.12.2 has no underground biomes, so a block's answer comes from:
 * <ul>
 *   <li><b>1.18 terrain</b> (Caves &amp; Cliffs' {@code cavesandcliffs} worlds): its biome, if it ever knows
 *       {@code minecraft:sulfur_caves}; until then the same climate box over its own climate noise
 *       ({@link ModernClimate}), at its real heights, wherever its own biome there is not lush caves, dripstone caves or
 *       the deep dark.</li>
 *   <li><b>Any other overworld</b>: our own regions ({@link LegacyRegions}), as rare and as large as 26.2's (measured on
 *       its climate noise), under low, dry land (never oceans, rivers, beaches, hills or mountains), at 26.2's depth
 *       below the column's ground: 26 to 115 blocks where the world goes down to y -64, two thirds of that (17 to 77) in
 *       a 0-255 world, the proportional placement. Caves &amp; Cliffs' lush and dripstone regions win where they
 *       meet.</li>
 *   <li><b>Superflat</b>: the preset's one biome everywhere, so only where that biome is the sulfur caves.</li>
 * </ul>
 * A column's answer is a mask over the world's height ({@link #mask}); a block's is that mask at its y.
 */
public final class SulfurCaves {

    public static final String BIOME = "minecraft:sulfur_caves";

    /** 26.2's depth range of underground biomes. */
    static final double DEPTH_MIN = 0.2, DEPTH_MAX = 0.9;

    private static final Map<World, Map<Long, boolean[]>> MASKS = new WeakHashMap<World, Map<Long, boolean[]>>();
    private static final Map<World, int[]> HEIGHTS = new WeakHashMap<World, int[]>();

    private SulfurCaves() {
    }

    public static boolean enabled() {
        return ModConfig.sulfurCaves.enabled;
    }

    /** @return true if this world can have sulfur caves at all: the overworld, with the stream on */
    public static boolean generates(World world) {
        return enabled() && world.provider.getDimension() == 0 && !world.isRemote;
    }

    /** @return true if this block is in the sulfur caves (server side) */
    public static boolean at(World world, BlockPos pos) {
        if (!generates(world)) {
            return false;
        }
        boolean[] mask = mask(world, pos.getX(), pos.getZ());
        int i = pos.getY() - minY(world);
        return mask != null && i >= 0 && i < mask.length && mask[i];
    }

    /**
     * @return the column's sulfur cave heights, indexed by y minus the world's bottom, or null where it has none. The
     *     column's chunk must be generated (the legacy depth is measured from its ground).
     */
    @Nullable
    public static boolean[] mask(World world, int x, int z) {
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

    /** Forgets a world's cached columns (a test that rebuilds terrain). */
    public static void forget(World world) {
        synchronized (MASKS) {
            MASKS.remove(world);
        }
    }

    @Nullable
    private static boolean[] compute(World world, int x, int z) {
        int min = minY(world), height = maxYExclusive(world) - min;
        if (FlatWorlds.isFlat(world)) {
            if (world.getBiomeProvider().getBiome(new BlockPos(x, 0, z), null) != SulfurCavesContent.biome) {
                return null;
            }
            boolean[] all = new boolean[height];
            java.util.Arrays.fill(all, true);
            return all;
        }
        String id = modernBiomeName(world);
        if (id != null) {
            // The 1.18 terrain names sulfur caves itself: its 3D biome is the answer.
            boolean[] mask = null;
            for (int y = min; y < min + height; y += 4) {
                if (BIOME.equals(modernBiome(world, new BlockPos(x, y, z)))) {
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
        if (ModernClimate.applies(world)) {
            return ModernClimate.mask(world, x, z, min, height);
        }
        return LegacyRegions.mask(world, x, z, min, height);
    }

    // ------------------------------------------------------------------------------------------------- heights

    public static int minY(World world) {
        return heights(world)[0];
    }

    public static int maxYExclusive(World world) {
        return heights(world)[1];
    }

    /** @return true where the world goes down to y -64 (Caves &amp; Cliffs' extended height): vanilla's heights */
    public static boolean extended(World world) {
        return minY(world) < 0;
    }

    /**
     * @return a 26.2 height in this world: itself where the world goes down to -64, else mapped proportionally,
     *     {@code (y + 64) * 256 / 384}
     */
    public static int worldY(World world, int y262) {
        if (extended(world)) {
            return y262;
        }
        int min = minY(world), height = maxYExclusive(world) - min;
        return min + Math.floorDiv((y262 + 64) * height, 384);
    }

    /** A column's ground: its highest block that blocks movement and is not a tree, else -1 below the bottom. */
    static int ground(World world, int x, int z) {
        Chunk chunk = world.getChunk(x >> 4, z >> 4);
        int top = Math.min(chunk.getTopFilledSegment() + 15, maxYExclusive(world) - 1);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int y = top; y >= minY(world); y--) {
            IBlockState state = chunk.getBlockState(m.setPos(x, y, z));
            if (state.getMaterial().blocksMovement() && !state.getBlock().isLeaves(state, world, m)
                    && !state.getBlock().isWood(world, m)) {
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

    private static Method biomeId;
    private static boolean looked;
    private static Boolean knowsSulfurCaves;

    /** @return non-null when the world's 1.18 terrain has a 3D biome named {@link #BIOME}: then it is the answer */
    @Nullable
    private static String modernBiomeName(World world) {
        if (modernBiome(world, BlockPos.ORIGIN) == null) {
            return null;
        }
        if (knowsSulfurCaves == null) {
            boolean knows;
            try {
                Class<?> biomes = Class.forName("com.exiledradio.cavesandcliffsbackport.terrain.core.Biomes", false,
                        SulfurCaves.class.getClassLoader());
                knows = (Integer) biomes.getMethod("byName", String.class).invoke(null, "sulfur_caves") >= 0;
            } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
                knows = false;
            }
            knowsSulfurCaves = knows;
        }
        return knowsSulfurCaves ? BIOME : null;
    }

    /** @return the 1.18 terrain's biome id at a block, or null in a world without it (Core's {@code ModernGeneration}) */
    @Nullable
    static String modernBiome(World world, BlockPos pos) {
        Method m = method();
        if (m == null) {
            return null;
        }
        try {
            return (String) m.invoke(null, world, pos);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    @Nullable
    private static synchronized Method method() {
        if (!looked) {
            looked = true;
            try {
                biomeId = Class.forName("com.exiledradio.backportcore.terrain.ModernGeneration")
                        .getMethod("biomeId", World.class, BlockPos.class);
            } catch (ReflectiveOperationException | LinkageError e) {
                biomeId = null;
            }
        }
        return biomeId;
    }

    // ------------------------------------------------------------------------------------------------- biomes

    /** The 1.12 stand-in for 26.2's high erosion and moderate continentalness: low land, not water or high ground. */
    static boolean lowland(@Nullable Biome biome) {
        if (biome == null || BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN)
                || BiomeDictionary.hasType(biome, BiomeDictionary.Type.RIVER)
                || BiomeDictionary.hasType(biome, BiomeDictionary.Type.BEACH)
                || BiomeDictionary.hasType(biome, BiomeDictionary.Type.HILLS)
                || BiomeDictionary.hasType(biome, BiomeDictionary.Type.MOUNTAIN)
                || BiomeDictionary.hasType(biome, BiomeDictionary.Type.NETHER)
                || BiomeDictionary.hasType(biome, BiomeDictionary.Type.END)) {
            return false;
        }
        return biome.getBaseHeight() < 0.45F;
    }

    /** @return true for blocks the bands replace: what 26.2's material rules and its stone features would have made */
    static boolean bandReplaceable(IBlockState state) {
        Block b = state.getBlock();
        if (b == Blocks.STONE || b == Blocks.DIRT || b == Blocks.GRAVEL || b instanceof net.minecraft.block.BlockOre
                || b instanceof net.minecraft.block.BlockRedstoneOre) {
            return true;
        }
        String path = b.getRegistryName() == null ? "" : b.getRegistryName().getPath();
        return path.equals("deepslate") || path.equals("tuff") || path.endsWith("_ore") && !path.contains("nether");
    }
}
