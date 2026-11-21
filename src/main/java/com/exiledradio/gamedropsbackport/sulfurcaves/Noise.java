package com.exiledradio.gamedropsbackport.sulfurcaves;

import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

/**
 * The two noises of this stream: a 2D value noise for the legacy regions' edges, and the bands' 3D gradient.
 *
 * <p>26.2's {@code sulfur_cave_gradient} is a normal noise with octaves -5 and -3 (1/32 and 1/8 per block,
 * amplitude modifiers 1, 0, 1). 26.x rebuilt the noise library ({@code NoiseStack}, its own normalization), so this
 * is the same shape from improved Perlin noise rather than its exact values: two octaves at those frequencies,
 * weighted 4:1 as an octave noise weighs them, each the sum of two offset samples as a normal noise is, scaled so its
 * spread gives sulfur and cinnabar equal shares (the wiki's "roughly equal amounts").
 */
final class Noise {

    /** Scales the summed octaves so the bands come out sulfur 38%, cinnabar 38%, stone 24% (2.4 million samples). */
    static final double SCALE = 1.05;

    private static final Map<Long, Perlin[]> GRADIENTS = new WeakHashMap<Long, Perlin[]>();

    private Noise() {
    }

    /** 2D value noise, 0 to 1. */
    static double value2(long seed, double x, double z) {
        int x0 = (int) Math.floor(x), z0 = (int) Math.floor(z);
        double fx = smooth(x - x0), fz = smooth(z - z0);
        double a = lerp(fx, lattice(seed, x0, z0), lattice(seed, x0 + 1, z0));
        double b = lerp(fx, lattice(seed, x0, z0 + 1), lattice(seed, x0 + 1, z0 + 1));
        return lerp(fz, a, b);
    }

    /** The bands' gradient at a block, centered on 0. */
    static double gradient(long worldSeed, int x, int y, int z) {
        Perlin[] p;
        synchronized (GRADIENTS) {
            Long key = worldSeed;
            p = GRADIENTS.get(key);
            if (p == null) {
                Random r = new Random(worldSeed ^ 0x5017FC4BE5L);
                p = new Perlin[] {new Perlin(r), new Perlin(r), new Perlin(r), new Perlin(r)};
                GRADIENTS.put(key, p);
            }
        }
        double f1 = 1 / 32.0, f2 = 1 / 8.0, k = 1.0181268882175227;
        double low = p[0].sample(x * f1, y * f1, z * f1) + p[1].sample(x * f1 * k, y * f1 * k, z * f1 * k);
        double high = p[2].sample(x * f2, y * f2, z * f2) + p[3].sample(x * f2 * k, y * f2 * k, z * f2 * k);
        return (0.8 * low + 0.2 * high) * SCALE;
    }

    private static double smooth(double t) {
        return t * t * (3 - 2 * t);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static double lattice(long seed, int x, int z) {
        long h = LegacyRegions.mix(seed, x, z);
        return (h >>> 11) * 0x1.0p-53;
    }

    /** Ken Perlin's improved noise, -1 to 1, with a random offset. */
    static final class Perlin {
        private final int[] perm = new int[512];
        private final double ox, oy, oz;

        Perlin(Random random) {
            ox = random.nextDouble() * 256;
            oy = random.nextDouble() * 256;
            oz = random.nextDouble() * 256;
            for (int i = 0; i < 256; i++) {
                perm[i] = i;
            }
            for (int i = 0; i < 256; i++) {
                int j = random.nextInt(256 - i) + i;
                int t = perm[i];
                perm[i] = perm[j];
                perm[j] = t;
                perm[i + 256] = perm[i];
            }
        }

        double sample(double x, double y, double z) {
            x += ox;
            y += oy;
            z += oz;
            int xi = (int) Math.floor(x), yi = (int) Math.floor(y), zi = (int) Math.floor(z);
            x -= xi;
            y -= yi;
            z -= zi;
            xi &= 255;
            yi &= 255;
            zi &= 255;
            double u = fade(x), v = fade(y), w = fade(z);
            int a = perm[xi] + yi, aa = perm[a] + zi, ab = perm[a + 1] + zi;
            int b = perm[xi + 1] + yi, ba = perm[b] + zi, bb = perm[b + 1] + zi;
            return lerp(w, lerp(v, lerp(u, grad(perm[aa], x, y, z), grad(perm[ba], x - 1, y, z)),
                            lerp(u, grad(perm[ab], x, y - 1, z), grad(perm[bb], x - 1, y - 1, z))),
                    lerp(v, lerp(u, grad(perm[aa + 1], x, y, z - 1), grad(perm[ba + 1], x - 1, y, z - 1)),
                            lerp(u, grad(perm[ab + 1], x, y - 1, z - 1), grad(perm[bb + 1], x - 1, y - 1, z - 1))));
        }

        private static double fade(double t) {
            return t * t * t * (t * (t * 6 - 15) + 10);
        }

        private static double grad(int hash, double x, double y, double z) {
            int h = hash & 15;
            double u = h < 8 ? x : y;
            double v = h < 4 ? y : h == 12 || h == 14 ? x : z;
            return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
        }
    }
}
