package com.exiledradio.gamedropsbackport.camps;

import com.exiledradio.backportcore.locate.BiomeLocate;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Today's biome id at a block, as 26.3's {@code Structure.isValidBiome} and its loot's {@code location_check} see it.
 *
 * <ul>
 *   <li>Caves &amp; Cliffs' 1.18 terrain has three-dimensional noise biomes. Its generator keeps its terrain core's
 *       {@code BiomeSource} behind {@code biomes3d}, reached by reflection as Tricky Trials' trial chambers and Trails
 *       &amp; Tales' trail ruins reach it; the quart's biome is the answer.</li>
 *   <li>Elsewhere, 1.12.2's one biome per column, by the id Backport Core's {@code /locate biome} knows it by
 *       (1.18's merges: birch forest hills is birch forest, mega taiga is old growth pine taiga, ...).</li>
 * </ul>
 */
final class CampBiomes {

    private CampBiomes() {
    }

    /** @return today's id ({@code minecraft:forest}) at the block, or null */
    @Nullable
    static String at(World world, BlockPos pos) {
        String noise = noiseBiome(world, pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2);
        if (noise != null) {
            return noise;
        }
        Biome biome = world.getBiomeProvider().getBiome(pos, null);
        return biome == null ? null : BiomeLocate.id(biome);
    }

    /**
     * {@code couldStructureExistInColumn}: some quart of the column has one of the biomes. Without 3D biomes the column
     * has its one biome.
     */
    static boolean column(World world, int x, int z, java.util.Set<String> biomes) {
        if (noiseBiome(world, x >> 2, 0, z >> 2) != null) {
            for (int qy = -16; qy <= 80; qy++) {
                if (biomes.contains(noiseBiome(world, x >> 2, qy, z >> 2))) {
                    return true;
                }
            }
            return false;
        }
        String here = at(world, new BlockPos(x, 64, z));
        return here != null && biomes.contains(here);
    }

    private static boolean noiseLooked;
    private static Field biomes3d, source;
    private static Method sourceBiome, biomeName;

    @Nullable
    static String noiseBiome(World world, int qx, int qy, int qz) {
        if (!(world instanceof WorldServer)) {
            return null;
        }
        Object generator = ((WorldServer) world).getChunkProvider().chunkGenerator;
        if (generator == null || !generator.getClass().getName().equals("com.exiledradio.cavesandcliffsbackport.terrain.ModernGenerator")) {
            return null;
        }
        try {
            synchronized (CampBiomes.class) {
                if (!noiseLooked) {
                    noiseLooked = true;
                    biomes3d = generator.getClass().getDeclaredField("biomes3d");
                    biomes3d.setAccessible(true);
                    source = biomes3d.getType().getDeclaredField("source");
                    source.setAccessible(true);
                    sourceBiome = source.getType().getMethod("biome", int.class, int.class, int.class);
                    biomeName = Class.forName(source.getType().getPackage().getName() + ".Biomes").getMethod("name", int.class);
                }
            }
            if (sourceBiome == null) {
                return null;
            }
            Object chunks = biomes3d.get(generator);
            synchronized (chunks) {
                Object src = source.get(chunks);
                int id = (Integer) sourceBiome.invoke(src, qx, qy, qz);
                return "minecraft:" + biomeName.invoke(null, id);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            synchronized (CampBiomes.class) {
                if (sourceBiome != null) {
                    GameDropsBackport.LOGGER.warn("Caves & Cliffs' noise biome is out of reach; abandoned camps check the column's biome.", e);
                }
                sourceBiome = null;
            }
            return null;
        }
    }
}
