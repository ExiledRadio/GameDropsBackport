package com.exiledradio.gamedropsbackport.icecaves;

import com.exiledradio.backportcore.locate.BiomeLocate;
import com.exiledradio.backportcore.terrain.TerrainHeights;

import net.minecraft.util.math.BlockPos;

/**
 * {@code /locate biome minecraft:ice_caves}: our patches under snowy oceans, at 26.4's depth below the ground, through
 * Backport Core's biome search (as the sulfur caves). In Caves &amp; Cliffs' 1.18 terrain the climate box is not
 * searchable this way, and the search answers with the terrain's own biomes.
 */
final class IceCavesLocate {

    private IceCavesLocate() {
    }

    static void register() {
        BiomeLocate.source(new BiomeLocate.SamplerFactory() {
            @Override
            public BiomeLocate.Sampler forWorld(net.minecraft.world.WorldServer world) {
                return sampler(this, world);
            }
        });
    }

    @javax.annotation.Nullable
    static BiomeLocate.Sampler sampler(BiomeLocate.SamplerFactory self, net.minecraft.world.WorldServer world) {
        {
            if (!IceCaves.generates(world) || IceCaves.Modern.applies(world)) {
                return null;
            }
            // Everything else is the other sources' (the sulfur caves', Caves & Cliffs'), else 1.12.2's layers.
            BiomeLocate.Sampler layers = BiomeLocate.next(self, world);
            TerrainHeights terrain = TerrainHeights.forWorld(world);
            long seed = world.getSeed();
            return (x, y, z) -> {
                int[] p = IceCaves.patchAt(seed, x, z);
                if (p != null && IceCaves.cold(world.getBiomeProvider().getBiome(new BlockPos(p[0], 0, p[1]), null))
                        && IceCaves.cold(world.getBiomeProvider().getBiome(new BlockPos(x, 0, z), null))) {
                    int ground = terrain != null ? terrain.height(x, z) : world.getSeaLevel();
                    if (y - 32 <= ground - IceCaves.depthBlocks(world, IceCaves.DEPTH_MIN)
                            && y + 31 >= ground - IceCaves.depthBlocks(world, IceCaves.DEPTH_MAX)) {
                        return IceCaves.BIOME;
                    }
                }
                return layers.biome(x, y, z);
            };
        }
    }
}
