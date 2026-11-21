package com.exiledradio.gamedropsbackport.sulfurcaves;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * Sulfur cave regions in a world without 1.18 terrain.
 *
 * <p>26.2's climate box, measured over its climate noise (Caves &amp; Cliffs' exact 1.18 noise, seeds 1, 42 and 1144,
 * 16384 blocks square): 0.007% to 0.022% of all columns, 0.01% to 0.03% of land, in 6 to 12 patches of a median 2 to 18
 * chunks and at most 72. Here: one 1024-block cell in 25 holds a patch, a disc of radius 20 to 69 blocks around a
 * random point of the cell with a ragged edge (a 2D value noise moves the edge in or out by up to a third), only where
 * the center's biome and the column's are low land ({@link SulfurCaves#lowland}). That is 0.02% of columns, in patches
 * of 5 to 58 chunks.
 *
 * <p>Depth: 26.2's 0.2 to 0.9, about 26 to 115 blocks under the ground, two thirds of that in a 0-255 world.
 */
final class LegacyRegions {

    static final int CELL = 1024;
    static final int CHANCE = 25;
    static final int MIN_RADIUS = 20, RADIUS_SPREAD = 50;

    private LegacyRegions() {
    }

    /** A patch: its center and radius, or null where the cell has none. */
    @Nullable
    static int[] patch(long seed, int cellX, int cellZ) {
        long h = mix(seed ^ 0x5EB1F0C4A3E5L, cellX, cellZ);
        if (Long.remainderUnsigned(h, CHANCE) != 0) {
            return null;
        }
        long h2 = mix(h, 7, 13);
        int x = cellX * CELL + (int) Long.remainderUnsigned(h2, CELL);
        int z = cellZ * CELL + (int) Long.remainderUnsigned(h2 >>> 20, CELL);
        int r = MIN_RADIUS + (int) Long.remainderUnsigned(h2 >>> 40, RADIUS_SPREAD);
        return new int[] {x, z, r};
    }

    /** @return true if the column is inside a patch by the noise alone (no biome or depth check) */
    static boolean inPatch(long seed, int x, int z) {
        return patchAt(seed, x, z) != null;
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
                if (d < 1.0 + (Noise.value2(seed ^ 0x51F0L, x / 24.0, z / 24.0) - 0.5) * 0.66) {
                    return p;
                }
            }
        }
        return null;
    }

    /** The patch nearest a column, searching the cells around it; for tests and {@code /locate}-like lookups. */
    @Nullable
    static int[] nearest(long seed, int x, int z, int cells) {
        int cx = Math.floorDiv(x, CELL), cz = Math.floorDiv(z, CELL);
        int[] best = null;
        double bestD = Double.MAX_VALUE;
        for (int dx = -cells; dx <= cells; dx++) {
            for (int dz = -cells; dz <= cells; dz++) {
                int[] p = patch(seed, cx + dx, cz + dz);
                if (p != null) {
                    double d = (double) (p[0] - x) * (p[0] - x) + (double) (p[1] - z) * (p[1] - z);
                    if (d < bestD) {
                        bestD = d;
                        best = p;
                    }
                }
            }
        }
        return best;
    }

    @Nullable
    static boolean[] mask(World world, int x, int z, int min, int height) {
        long seed = world.getSeed();
        int[] p = patchAt(seed, x, z);
        if (p == null) {
            return null;
        }
        if (!SulfurCaves.lowland(world.getBiomeProvider().getBiome(new BlockPos(p[0], 0, p[1]), null))
                || !SulfurCaves.lowland(world.getBiomeProvider().getBiome(new BlockPos(x, 0, z), null))) {
            return null;
        }
        int ground = SulfurCaves.ground(world, x, z);
        // 26.2's depth is about (ground - y) / 128; a 0-255 world keeps two thirds of each depth.
        double scale = SulfurCaves.extended(world) ? 128.0 : 128.0 * 256.0 / 384.0;
        int top = ground - (int) Math.ceil(SulfurCaves.DEPTH_MIN * scale);
        int bottom = ground - (int) Math.floor(SulfurCaves.DEPTH_MAX * scale);
        boolean[] mask = null;
        for (int y = Math.max(bottom, min + 1); y <= top && y < min + height; y++) {
            if (ccRegion(world, x, y, z)) {
                continue;
            }
            if (mask == null) {
                mask = new boolean[height];
            }
            mask[y - min] = true;
        }
        return mask;
    }

    private static Method lush, dripstone;
    private static boolean looked;

    /** Caves &amp; Cliffs' own lush and dripstone regions in 1.12 terrain win where they meet ours. */
    private static boolean ccRegion(World world, int x, int y, int z) {
        synchronized (LegacyRegions.class) {
            if (!looked) {
                looked = true;
                try {
                    Class<?> c = Class.forName("com.exiledradio.cavesandcliffsbackport.common.CaveRegions", false,
                            LegacyRegions.class.getClassLoader());
                    lush = c.getMethod("lush", World.class, int.class, int.class, int.class);
                    dripstone = c.getMethod("dripstone", World.class, int.class, int.class, int.class);
                } catch (ReflectiveOperationException | LinkageError e) {
                    lush = null;
                    dripstone = null;
                }
            }
        }
        if (lush == null) {
            return false;
        }
        try {
            return (Boolean) lush.invoke(null, world, x, y, z) || (Boolean) dripstone.invoke(null, world, x, y, z);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    static long mix(long seed, int a, int b) {
        long h = seed ^ (a * 0x9E3779B97F4A7C15L) ^ (b * 0xC2B2AE3D27D4EB4FL);
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return h;
    }
}
