package com.exiledradio.gamedropsbackport.icecaves;

import com.exiledradio.backportcore.worldgen.UndergroundBiomes;
import com.exiledradio.gamedropsbackport.selftest.Check;
import com.exiledradio.gamedropsbackport.selftest.SelfTest;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;

import java.util.Random;

/**
 * 26.4 stream 1's generation: the bands' thresholds, the crevasse, the large icicle and the icicle cluster on prepared
 * sites, and a real patch of our own regions generated in new chunks (bands, the biome registered under ground, and
 * none outside the mask).
 */
public final class IceCavesGenSelfTest implements SelfTest {

    @Override
    public String name() {
        return "icecavesgen";
    }

    @Override
    public void run(Check check) throws Exception {
        if (!check.ok(IceCaves.enabled(), "ice caves enabled")) {
            return;
        }
        IBlockState p = Blocks.PACKED_ICE.getDefaultState(), c = Blocks.BONE_BLOCK.getDefaultState();
        check.eq(c, IceCavesGen.band(-0.55, p, c), "band -0.55: calcite");
        check.eq(p, IceCavesGen.band(0.0, p, c), "band 0: packed ice");
        check.eq(c, IceCavesGen.band(0.25, p, c), "band 0.25: calcite");
        check.eq(p, IceCavesGen.band(0.4, p, c), "band 0.4: packed ice");
        check.eq(c, IceCavesGen.band(0.6, p, c), "band 0.6: calcite");
        check.eq(null, IceCavesGen.band(0.8, p, c), "band 0.8: stone stays");
        check.eq(null, IceCavesGen.band(-0.7, p, c), "band -0.7: stone stays");

        WorldServer w = check.world;
        // Off Caves & Cliffs' 1.18 terrain with its 26.4 edition, our own regions and our /locate answer.
        check.ok(!IceCaves.terrainHasIceCaves(w), "a world off the 1.18 terrain's 26.4 edition gets our own ice caves");
        check.ok(IceCavesLocate.sampler(new com.exiledradio.backportcore.locate.BiomeLocate.SamplerFactory() {
            @Override
            public com.exiledradio.backportcore.locate.BiomeLocate.Sampler forWorld(WorldServer world) {
                return null;
            }
        }, w) != null, "our /locate answers the ice caves there");
        BlockPos base = w.getSpawnPoint().add(400, 0, 400);
        int cx = base.getX() >> 4, cz = base.getZ() >> 4;
        for (int dx = 0; dx < 2; dx++) {
            for (int dz = 0; dz < 2; dz++) {
                w.getChunk(cx + dx, cz + dz);
            }
        }
        int ox = cx * 16, oz = cz * 16;
        crevasse(check, w, ox, oz);
        largeIcicle(check, w, ox, oz);
        cluster(check, w, ox, oz);
        realPatch(check, w);
    }

    private static void fill(WorldServer w, int x0, int y0, int z0, int x1, int y1, int z1, IBlockState s) {
        for (BlockPos q : BlockPos.getAllInBox(new BlockPos(x0, y0, z0), new BlockPos(x1, y1, z1))) {
            w.setBlockState(q, s, 2);
        }
    }

    private static void crevasse(Check check, WorldServer w, int ox, int oz) {
        int y = 150;
        fill(w, ox, y - 12, oz, ox + 31, y, oz + 31, Blocks.PACKED_ICE.getDefaultState());
        fill(w, ox, y + 1, oz, ox + 31, y + 10, oz + 31, Blocks.AIR.getDefaultState());
        IceCavesGen.Gen g = new IceCavesGen.Gen(w, ox, oz, new Random(5));
        g.crevasse(new BlockPos(ox + 16, y, oz + 16));
        int brittle = 0, water = 0;
        for (BlockPos q : BlockPos.getAllInBox(new BlockPos(ox, y - 12, oz), new BlockPos(ox + 31, y, oz + 31))) {
            IBlockState s = w.getBlockState(q);
            if (s.getBlock() == IceCavesContent.brittleIce) {
                brittle++;
            } else if (s.getBlock() == Blocks.WATER) {
                water++;
            }
        }
        check.ok(brittle >= 10, "crevasse: a brittle ice cover of at least 10 columns (" + brittle + ")");
        check.ok(water > 0, "crevasse: water under the cover (" + water + ")");
        fill(w, ox, y - 12, oz, ox + 31, y + 10, oz + 31, Blocks.AIR.getDefaultState());
    }

    private static void largeIcicle(Check check, WorldServer w, int ox, int oz) {
        int y = 100;
        fill(w, ox, y - 6, oz, ox + 31, y + 26, oz + 31, Blocks.STONE.getDefaultState());
        fill(w, ox + 4, y, oz + 4, ox + 27, y + 20, oz + 27, Blocks.AIR.getDefaultState());
        int packed = 0;
        for (long seed = 1; seed < 20 && packed == 0; seed++) {
            IceCavesGen.Gen g = new IceCavesGen.Gen(w, ox, oz, new Random(seed));
            g.largeIcicle(new BlockPos(ox + 16, y + 10, oz + 16));
            for (BlockPos q : BlockPos.getAllInBox(new BlockPos(ox + 4, y, oz + 4), new BlockPos(ox + 27, y + 20, oz + 27))) {
                if (w.getBlockState(q).getBlock() == Blocks.PACKED_ICE) {
                    packed++;
                }
            }
        }
        check.ok(packed > 0, "large icicle: packed ice spikes in a stone cave (" + packed + ")");
        fill(w, ox, y - 6, oz, ox + 31, y + 26, oz + 31, Blocks.AIR.getDefaultState());
    }

    private static void cluster(Check check, WorldServer w, int ox, int oz) {
        int y = 60;
        fill(w, ox, y + 8, oz, ox + 31, y + 10, oz + 31, Blocks.PACKED_ICE.getDefaultState());
        fill(w, ox, y, oz, ox + 31, y + 7, oz + 31, Blocks.AIR.getDefaultState());
        fill(w, ox, y - 2, oz, ox + 31, y - 1, oz + 31, Blocks.STONE.getDefaultState());
        int icicles = 0, attached = 0, stalagmites = 0;
        for (long seed = 1; seed < 8; seed++) {
            IceCavesGen.Gen g = new IceCavesGen.Gen(w, ox, oz, new Random(seed));
            g.cluster(new BlockPos(ox + 16, y + 3, oz + 16));
        }
        for (BlockPos q : BlockPos.getAllInBox(new BlockPos(ox, y, oz), new BlockPos(ox + 31, y + 7, oz + 31))) {
            IBlockState s = w.getBlockState(q);
            if (s.getBlock() == IceCavesContent.icicle) {
                icicles++;
                if (IceCavesContent.icicle.getActualState(s, w, q).getValue(BlockIcicle.ATTACHED)) {
                    attached++;
                }
                if (s.getValue(com.exiledradio.backportcore.speleothem.BlockSpeleothem.VERTICAL_DIRECTION) == net.minecraft.util.EnumFacing.UP) {
                    stalagmites++;
                }
            }
        }
        check.ok(icicles > 0, "icicle cluster: icicles under packed ice (" + icicles + ")");
        check.ok(attached > 0, "icicle cluster: their bases attached (" + attached + ")");
        check.eq(0, stalagmites, "icicle cluster: ceiling only");
        fill(w, ox, y - 2, oz, ox + 31, y + 10, oz + 31, Blocks.AIR.getDefaultState());
    }

    /** A patch of our regions near spawn under a cold biome, generated in new chunks. */
    private static void realPatch(Check check, WorldServer w) {
        long seed = w.getSeed();
        BlockPos spawn = w.getSpawnPoint();
        int[] found = null;
        for (int r = 0; r < 12 && found == null; r++) {
            for (int a = -r; a <= r && found == null; a++) {
                for (int b = -r; b <= r && found == null; b++) {
                    if (Math.max(Math.abs(a), Math.abs(b)) != r) {
                        continue;
                    }
                    int cellX = Math.floorDiv(spawn.getX(), IceCaves.CELL) + a, cellZ = Math.floorDiv(spawn.getZ(), IceCaves.CELL) + b;
                    int[] p = IceCaves.patch(seed, cellX, cellZ);
                    if (p != null && IceCaves.cold(w.getBiomeProvider().getBiome(new BlockPos(p[0], 0, p[1]), null))) {
                        found = p;
                    }
                }
            }
        }
        if (found == null) {
            check.ok(true, "no cold patch within 12 cells of spawn in this world (seed " + seed + "): the census needs one");
            return;
        }
        int pcx = (found[0] - 16) >> 4, pcz = (found[1] - 16) >> 4;
        for (int dx = -1; dx <= 2; dx++) {
            for (int dz = -1; dz <= 2; dz++) {
                w.getChunk(pcx + dx, pcz + dz);
            }
        }
        boolean[] mask = IceCaves.mask(w, found[0], found[1]);
        check.ok(mask != null, "the patch's center column is in the ice caves");
        int around = 0;
        for (int x = found[0] - 32; x < found[0] + 32; x += 4) {
            for (int z = found[1] - 32; z < found[1] + 32; z += 4) {
                if (IceCaves.mask(w, x, z) != null) {
                    around++;
                }
            }
        }
        com.exiledradio.gamedropsbackport.GameDropsBackport.LOGGER.info("SELFTEST icecaves patch {} r={} masks around={} centre biome {}",
                java.util.Arrays.toString(found), found[2], around, w.getBiomeProvider().getBiome(new BlockPos(found[0], 0, found[1]), null).getRegistryName());
        int ice = 0, inMask = 0, registered = 0;
        int min = IceCaves.minY(w);
        for (int x = pcx * 16 + 8; x < pcx * 16 + 24; x++) {
            for (int z = pcz * 16 + 8; z < pcz * 16 + 24; z++) {
                boolean[] m = IceCaves.mask(w, x, z);
                if (m == null) {
                    continue;
                }
                for (int i = 0; i < m.length; i++) {
                    if (!m[i]) {
                        continue;
                    }
                    inMask++;
                    IBlockState s = w.getBlockState(new BlockPos(x, min + i, z));
                    if (s.getBlock() == Blocks.PACKED_ICE || IceCavesGen.is(s, "calcite")) {
                        ice++;
                    }
                }
                Biome b = UndergroundBiomes.biomeAt(w, new BlockPos(x, min + firstTrue(m), z));
                if (b == IceCavesContent.biome) {
                    registered++;
                }
            }
        }
        check.ok(inMask > 0 && ice > inMask / 5, "the bands: packed ice and calcite in the patch (" + ice + " of " + inMask + ")");
        check.ok(registered > 0, "the columns are Core underground biomes (" + registered + ")");
    }

    private static int firstTrue(boolean[] m) {
        for (int i = 0; i < m.length; i++) {
            if (m[i]) {
                return i;
            }
        }
        return 0;
    }
}
