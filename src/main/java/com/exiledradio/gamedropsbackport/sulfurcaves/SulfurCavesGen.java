package com.exiledradio.gamedropsbackport.sulfurcaves;

import com.exiledradio.backportcore.worldgen.FlatWorlds;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Random;

/**
 * The sulfur caves in newly generated chunks, over the 16x16 columns of a chunk's population window, in 26.2's order:
 * <ol>
 *   <li><b>Bands</b> ({@code overworld/sulfur_cave_bands}, first in the underground material rule): by the
 *       {@code sulfur_cave_gradient} noise, cinnabar from -0.4 to -0.1, sulfur from 0 to 0.4, cinnabar from 0.4 up,
 *       stone between. 26.2 lays them before any feature, so its ores, stone blobs and dirt never land in them; here
 *       they come after 1.12's (or the 1.18 terrain's) features, so they also take the stone, deepslate, tuff, dirt,
 *       gravel and ores those put there ({@link SulfurCaves#bandReplaceable}).</li>
 *   <li>Lakes step: {@code rooted_sulfur_spring}, then {@code sulfur_pool} ({@link Features}).</li>
 *   <li>Underground decoration: {@code sulfur_spike_cluster}, then {@code sulfur_spike}.</li>
 * </ol>
 * The other features 26.2 lists for the biome are vanilla's everywhere (ores, geodes, lava lakes, springs, glow
 * lichen), which 1.12 or the family's backports already place.
 *
 * <p>Writes stay inside the chunk and the three around its population window, which 1.12 guarantees are generated: a
 * try that would reach further (a spike thrown more than 8 blocks out, the outermost tuff of a large spring) skips that
 * block.
 */
public final class SulfurCavesGen implements IWorldGenerator {

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator generator, IChunkProvider provider) {
        if (!SulfurCaves.generates(world) || !SulfurCavesContent.blocksPresent()) {
            return;
        }
        if (FlatWorlds.isFlat(world) && !FlatWorlds.decoration(world)) {
            return;
        }
        populate(world, chunkX, chunkZ);
    }

    /** @return how many blocks the bands changed */
    public static int populate(World world, int chunkX, int chunkZ) {
        int x0 = chunkX * 16 + 8, z0 = chunkZ * 16 + 8;
        boolean any = false;
        for (int x = x0; x < x0 + 16 && !any; x += 4) {
            for (int z = z0; z < z0 + 16 && !any; z += 4) {
                any = SulfurCaves.mask(world, x, z) != null || SulfurCaves.mask(world, x + 3, z + 3) != null;
            }
        }
        if (!any) {
            return 0;
        }
        // Caves & Cliffs' Game Drops edition lays the bands in its surface rules, before its ores and deepslate.
        int changed = SulfurCaves.terrainHasSulfurCaves(world) ? 0 : bands(world, x0, z0);
        Random rand = new Random(LegacyRegions.mix(world.getSeed() ^ 0x26_02_5CL, chunkX, chunkZ));
        Features f = new Features(world, chunkX * 16, chunkZ * 16, rand);
        f.rootedSprings(x0, z0);
        f.pools(x0, z0);
        f.spikeClusters(x0, z0);
        f.spikes(x0, z0);
        return changed;
    }

    static int bands(World world, int x0, int z0) {
        long seed = world.getSeed();
        int min = SulfurCaves.minY(world);
        IBlockState sulfur = SulfurCavesContent.block("sulfur").getDefaultState();
        IBlockState cinnabar = SulfurCavesContent.block("cinnabar").getDefaultState();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int changed = 0;
        for (int x = x0; x < x0 + 16; x++) {
            for (int z = z0; z < z0 + 16; z++) {
                boolean[] mask = SulfurCaves.mask(world, x, z);
                if (mask == null) {
                    continue;
                }
                for (int i = 0; i < mask.length; i++) {
                    if (!mask[i]) {
                        continue;
                    }
                    int y = min + i;
                    IBlockState band = band(Noise.gradient(seed, x, y, z), sulfur, cinnabar);
                    if (band == null || !SulfurCaves.bandReplaceable(world.getBlockState(m.setPos(x, y, z)))) {
                        continue;
                    }
                    world.setBlockState(m, band, 2 | 16);
                    changed++;
                }
            }
        }
        return changed;
    }

    /** 26.2's {@code sulfur_cave_bands}: null keeps the stone. */
    static IBlockState band(double v, IBlockState sulfur, IBlockState cinnabar) {
        if (v >= -0.4 && v <= -0.1) {
            return cinnabar;
        }
        if (v >= 0.0 && v <= 0.4) {
            return sulfur;
        }
        return v >= 0.4 ? cinnabar : null;
    }
}
