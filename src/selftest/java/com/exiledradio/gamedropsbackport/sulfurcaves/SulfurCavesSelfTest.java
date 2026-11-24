package com.exiledradio.gamedropsbackport.sulfurcaves;

import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.network.NetworkHandler;
import com.exiledradio.gamedropsbackport.selftest.Check;
import com.exiledradio.gamedropsbackport.selftest.SelfTest;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.monster.EntityCaveSpider;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.Random;

/**
 * Stream 4: registrations, heights, the spring templates, each feature on a prepared site, and a real sulfur cave
 * generated in new chunks (bands only inside the mask, spikes, sulfur against cinnabar, spawns). Appends the tuff
 * copy to {@code selftest-states.txt} and {@code selftest-items.txt} for {@code tools/check_assets.py}.
 */
public final class SulfurCavesSelfTest implements SelfTest {

    @Override
    public String name() {
        return "sulfurcaves";
    }

    @Override
    public void run(Check check) throws Exception {
        if (!check.ok(SulfurCaves.enabled(), "sulfur caves enabled") || !check.ok(SulfurCavesContent.blocksPresent(), "stream 2's blocks")) {
            return;
        }
        registry(check);
        heights(check);
        templates(check);
        pool(check);
        cluster(check);
        spring(check);
        natural(check);
        dump(check);
    }

    private static Block block(String name) {
        return SulfurCavesContent.block(name);
    }

    // ------------------------------------------------------------------------------------------------ registry

    private void registry(Check check) throws Exception {
        Biome biome = ForgeRegistries.BIOMES.getValue(new ResourceLocation(GameDropsBackport.MOD_ID, "sulfur_caves"));
        check.ok(biome != null && biome == SulfurCavesContent.biome, "biome registered");
        check.eq(0.8F, biome.getDefaultTemperature(), "biome temperature");
        check.eq(0.4F, biome.getRainfall(), "biome downfall");
        check.ok(!net.minecraftforge.common.BiomeManager.getBiomes(net.minecraftforge.common.BiomeManager.BiomeType.WARM)
                .stream().anyMatch(e -> e.biome == biome), "biome not in the 1.12 biome map");
        List<Biome.SpawnListEntry> monsters = biome.getSpawnableList(EnumCreatureType.MONSTER);
        check.ok(monsters.stream().anyMatch(e -> e.entityClass == EntityCaveSpider.class), "cave spiders spawn");
        check.ok(monsters.stream().noneMatch(e -> e.entityClass == EntitySpider.class), "no spiders");
        int total = 0;
        for (Biome.SpawnListEntry e : monsters) {
            if (!e.entityClass.getName().contains("SulfurCube")) {
                total += e.itemWeight;
            }
        }
        check.eq(211, total, "monster weights without the sulfur cube (26.3: 50+50+25+20+50+10+1+5)");
        check.eq(1, biome.getSpawnableList(EnumCreatureType.AMBIENT).size(), "one ambient spawn (bats)");
        check.ok(SulfurCavesContent.music != null && ForgeRegistries.SOUND_EVENTS.containsKey(
                new ResourceLocation(GameDropsBackport.MOD_ID, "music.overworld.sulfur_caves")), "music event");
        check.ok(SulfurCavesContent.copiesTuff() == (SulfurCavesContent.tuffCopy != null), "tuff copied exactly when no family mod has it");
        check.ok(SulfurCavesContent.tuff().getBlock() != Blocks.STONE, "tuff resolves to a tuff block");
        check.ok(NetworkHandler.channel != null, "network channel");

        LootTable table = check.world.getLootTableManager().getLootTableFromLocation(LootTableList.CHESTS_ABANDONED_MINESHAFT);
        boolean found = false;
        for (String name : new String[] {"pool2", "main", "pool0", "pool1"}) {
            LootPool pool = table.getPool(name);
            if (pool != null && pool.getEntry("minecraft:rail") != null) {
                found = pool.getEntry(GameDropsBackport.MOD_ID + ":music_disc_bounce") != null;
            }
        }
        check.ok(found, "Bounce in the mineshaft chests' rail pool");
    }

    private void heights(Check check) {
        WorldServer w = check.world;
        check.ok(!SulfurCaves.extended(w), "a 0-255 dev world");
        check.eq(0, SulfurCaves.worldY(w, -64), "26.2's -64 is y 0");
        check.eq(213, SulfurCaves.worldY(w, 256), "26.2's 256 is y 213");
        check.eq(42, SulfurCaves.worldY(w, 0), "26.2's 0 is y 42");
    }

    private void templates(Check check) {
        int[][] sizes = {{8, 11, 9}, {9, 11, 8}, {9, 11, 9}, {9, 11, 9}, {11, 11, 11}, {12, 11, 11}, {12, 11, 12}, {13, 11, 12},
            {12, 11, 13}, {16, 11, 16}};
        String[] names = {"small_1", "small_2", "small_3", "small_4", "medium_1", "medium_2", "medium_3", "large_1", "large_2",
            "extra_large_1"};
        for (int i = 0; i < names.length; i++) {
            Springs.Template t = Springs.template(names[i]);
            if (!check.ok(t != null, "template " + names[i])) {
                continue;
            }
            check.eq(sizes[i][0] + "x" + sizes[i][1] + "x" + sizes[i][2], t.sx + "x" + t.sy + "x" + t.sz, names[i] + " size");
            check.ok(t.blocks.size() > 0 && t.blocks.size() <= t.sx * t.sy * t.sz, names[i] + " blocks inside its box");
        }
    }

    // ------------------------------------------------------------------------------------------------ features on sites

    /** A site far from anything: a box of {@code fill} from y0 to y1, air above to y2, at chunk-aligned x, z. */
    private static Features site(WorldServer w, int x, int z, int y0, int y1, int y2, IBlockState fill, long seed) {
        for (int cx = (x >> 4) - 1; cx <= (x >> 4) + 2; cx++) {
            for (int cz = (z >> 4) - 1; cz <= (z >> 4) + 2; cz++) {
                w.getChunk(cx, cz);
            }
        }
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 32; dx++) {
            for (int dz = 0; dz < 32; dz++) {
                for (int y = y0; y <= y2; y++) {
                    w.setBlockState(m.setPos(x + dx, y, z + dz), y <= y1 ? fill : Blocks.AIR.getDefaultState(), 2 | 16);
                }
            }
        }
        return new Features(w, x, z, new Random(seed));
    }

    private static int count(WorldServer w, int x, int z, int y0, int y1, Block block) {
        int n = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 32; dx++) {
            for (int dz = 0; dz < 32; dz++) {
                for (int y = y0; y <= y1; y++) {
                    if (w.getBlockState(m.setPos(x + dx, y, z + dz)).getBlock() == block) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

    private void pool(Check check) {
        WorldServer w = check.world;
        int x = 40000, z = 40000;
        Features f = site(w, x, z, 40, 60, 75, block("sulfur").getDefaultState(), 1);
        boolean placed = false;
        for (int i = 0; i < 20 && !placed; i++) {
            placed = f.lake(new BlockPos(x + 16, 60, z + 16));
        }
        check.ok(placed, "a sulfur pool places on a sulfur floor");
        check.ok(count(w, x, z, 50, 60, Blocks.WATER) > 10, "the pool holds water");
        check.eq(0, count(w, x, z, 61, 75, Blocks.WATER), "no water above the floor");
        BlockPos floor = f.scan(new BlockPos(x + 16, 60, z + 16), net.minecraft.util.EnumFacing.DOWN, 4, Features.Scan.ANY,
                Features.Scan.SOLID_UNDER_WATER);
        if (floor != null) {
            f.set(floor, Features.with(f.potent.getDefaultState(), "potent_sulfur_state", "wet"));
            check.eq(f.potent, w.getBlockState(floor).getBlock(), "potent sulfur under the pool");
        }
        // A pool never touches sulfur spikes.
        Features g = site(w, x + 64, z, 40, 60, 75, block("sulfur").getDefaultState(), 2);
        for (int dx = 0; dx < 32; dx++) {
            for (int dz = 0; dz < 32; dz++) {
                w.setBlockState(new BlockPos(x + 64 + dx, 61, z + dz), f.spike.getDefaultState(), 2 | 16);
            }
        }
        boolean any = false;
        for (int i = 0; i < 20; i++) {
            any |= g.lake(new BlockPos(x + 64 + 16, 60, z + 16));
        }
        check.ok(!any, "no pool beside sulfur spikes");
    }

    private void cluster(Check check) {
        WorldServer w = check.world;
        int x = 40000, z = 40128;
        Features f = site(w, x, z, 40, 60, 80, block("sulfur").getDefaultState(), 3);
        for (int dx = 0; dx < 32; dx++) {
            for (int dz = 0; dz < 32; dz++) {
                w.setBlockState(new BlockPos(x + dx, 68, z + dz), block("cinnabar").getDefaultState(), 2 | 16);
            }
        }
        for (int i = 0; i < 4; i++) {
            new Speleothems(f).cluster(new BlockPos(x + 16, 63, z + 16));
        }
        int spikes = count(w, x, z, 61, 67, f.spike);
        check.ok(spikes > 10, "spike clusters grow spikes (" + spikes + ")");
        // Every spike rests on sulfur or cinnabar or on another spike of its own direction.
        int loose = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 32; dx++) {
            for (int dz = 0; dz < 32; dz++) {
                for (int y = 61; y <= 67; y++) {
                    IBlockState s = w.getBlockState(m.setPos(x + dx, y, z + dz));
                    if (s.getBlock() != f.spike) {
                        continue;
                    }
                    boolean up = s.toString().contains("vertical_direction=up");
                    IBlockState root = w.getBlockState(up ? m.down() : m.up());
                    if (!(root.getBlock() == f.spike || f.in(root.getBlock()))) {
                        loose++;
                    }
                }
            }
        }
        check.eq(0, loose, "spikes without support");
        int single = 0;
        for (int i = 0; i < 40; i++) {
            BlockPos p = new BlockPos(x + 4 + i % 24, 61, z + 4 + i / 2);
            if (w.isAirBlock(p) && f.get(p.down()).getBlock() == f.sulfur) {
                new Speleothems(f).single(p);
                if (w.getBlockState(p).getBlock() == f.spike) {
                    single++;
                }
            }
        }
        check.ok(single > 0, "a single sulfur spike grows");
    }

    private void spring(Check check) {
        WorldServer w = check.world;
        int x = 40000, z = 40256;
        // Ground of stone to y 70, flat, air above; a cave at y 40-44 under it.
        Features f = site(w, x, z, 30, 70, 90, Blocks.STONE.getDefaultState(), 4);
        for (int dx = 12; dx < 20; dx++) {
            for (int dz = 12; dz < 20; dz++) {
                for (int y = 40; y <= 44; y++) {
                    w.setBlockState(new BlockPos(x + dx, y, z + dz), Blocks.AIR.getDefaultState(), 2 | 16);
                }
            }
        }
        boolean placed = new Springs(f).rootSystem(new BlockPos(x + 16, 44, z + 16));
        check.ok(placed, "a rooted sulfur spring places under flat ground");
        int water = count(w, x, z, 60, 75, Blocks.WATER), roots = count(w, x, z, 45, 62, f.sulfur);
        int tuff = count(w, x, z, 60, 75, SulfurCavesContent.tuff().getBlock());
        check.ok(water > 0, "the spring holds water (" + water + ")");
        check.ok(tuff > 20, "tuff around the spring (" + tuff + ")");
        check.ok(roots > 20, "sulfur roots up from the cave (" + roots + ")");
        check.eq(f.sulfur, w.getBlockState(new BlockPos(x + 16, 44, z + 16)).getBlock(), "hanging sulfur under the ceiling");
        check.ok(count(w, x, z, 55, 75, f.potent) >= 1, "potent sulfur in the spring");
        // Uneven ground: the level test fails.
        Features g = site(w, x + 64, z, 30, 70, 90, Blocks.STONE.getDefaultState(), 5);
        for (int y = 71; y <= 80; y++) {
            for (int dz = 0; dz < 32; dz++) {
                w.setBlockState(new BlockPos(x + 64 + 24, y, z + dz), Blocks.STONE.getDefaultState(), 2 | 16);
            }
        }
        for (int dx = 12; dx < 20; dx++) {
            for (int dz = 12; dz < 20; dz++) {
                for (int y = 40; y <= 44; y++) {
                    w.setBlockState(new BlockPos(x + 64 + dx, y, z + dz), Blocks.AIR.getDefaultState(), 2 | 16);
                }
            }
        }
        check.ok(!new Springs(g).rootSystem(new BlockPos(x + 64 + 16, 44, z + 16)), "no spring where a wall rises 8 out");
    }

    // ------------------------------------------------------------------------------------------------ natural

    private void natural(Check check) {
        WorldServer w = check.world;
        long seed = w.getSeed();
        int[] p = null;
        for (int r = 1; r <= 40 && p == null; r++) {
            int[] q = LegacyRegions.nearest(seed, r * 3000, r * 2000, 4);
            if (q != null && q[2] >= 40 && SulfurCaves.lowland(w.getBiomeProvider().getBiome(new BlockPos(q[0], 0, q[1]), null))) {
                p = q;
            }
        }
        if (!check.ok(p != null, "a legacy patch under low land nearby")) {
            return;
        }
        GameDropsBackport.LOGGER.info("SELFTEST sulfur caves patch at {} {} radius {}", p[0], p[1], p[2]);
        // /locate biome finds sulfur caves without loading chunks, on a column that has them, and not far off: patches
        // are common enough to come across.
        com.exiledradio.backportcore.locate.BiomeLocate.Found found = com.exiledradio.backportcore.locate.BiomeLocate.nearest(w,
                new BlockPos(p[0] + 300, 64, p[1]), "minecraft:sulfur_caves");
        check.ok(found != null && LegacyRegions.inPatch(seed, found.pos.getX(), found.pos.getZ())
                        && SulfurCaves.lowland(w.getBiomeProvider().getBiome(found.pos, null))
                        && Math.abs(found.pos.getX() - p[0] - 300) <= 2000 && Math.abs(found.pos.getZ() - p[1]) <= 2000,
                "/locate biome minecraft:sulfur_caves finds a sulfur cave column within 2000 blocks (" + (found == null ? "none" : found.pos) + ")");
        int patches = 0;
        for (int cx = 0; cx < 40; cx++) {
            for (int cz = 0; cz < 40; cz++) {
                if (LegacyRegions.patch(seed, cx, cz) != null) {
                    patches++;
                }
            }
        }
        check.ok(patches > 300 && patches < 500, "one cell in 4 holds a patch (" + patches + " of 1600)");
        int c0 = (p[0] >> 4) - 4, c1 = (p[0] >> 4) + 4, d0 = (p[1] >> 4) - 4, d1 = (p[1] >> 4) + 4;
        for (int cx = c0; cx <= c1 + 1; cx++) {
            for (int cz = d0; cz <= d1 + 1; cz++) {
                w.getChunk(cx, cz);
            }
        }
        int sulfur = 0, cinnabar = 0, outside = 0, spikes = 0, inMask = 0, potent = 0, tuff = 0;
        Block s = block("sulfur"), c = block("cinnabar"), sp = block("sulfur_spike"), po = block("potent_sulfur");
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int min = SulfurCaves.minY(w);
        for (int x = (c0 + 1) * 16; x < c1 * 16; x++) {
            for (int z = (d0 + 1) * 16; z < d1 * 16; z++) {
                boolean[] mask = SulfurCaves.mask(w, x, z);
                int ground = SulfurCaves.ground(w, x, z);
                for (int y = 1; y < 128; y++) {
                    Block b = w.getBlockState(m.setPos(x, y, z)).getBlock();
                    boolean in = mask != null && mask[y - min];
                    if (in) {
                        inMask++;
                    }
                    if (b == s || b == c) {
                        if (b == s) {
                            sulfur++;
                        } else {
                            cinnabar++;
                        }
                        // Pool barriers, roots and springs put sulfur outside the mask; only the bands make cinnabar
                        // underground.
                        if (b == c && !in && !near(mask, y - min, 12) && y < ground - 12) {
                            outside++;
                        }
                    } else if (b == sp) {
                        spikes++;
                    } else if (b == po) {
                        potent++;
                    } else if (b == SulfurCavesContent.tuff().getBlock() && y > ground - 12) {
                        tuff++;
                    }
                }
            }
        }
        GameDropsBackport.LOGGER.info("SELFTEST sulfur caves census: mask {} sulfur {} cinnabar {} spikes {} potent {} surface tuff {} far outside {}",
                inMask, sulfur, cinnabar, spikes, potent, tuff, outside);
        check.ok(inMask > 1000, "the patch has sulfur cave blocks (" + inMask + ")");
        check.ok(sulfur > 500 && cinnabar > 500, "bands of sulfur and cinnabar");
        double ratio = (double) sulfur / Math.max(1, cinnabar);
        check.ok(ratio > 0.6 && ratio < 1.8, "sulfur against cinnabar " + String.format("%.2f", ratio));
        check.ok(spikes > 0, "sulfur spikes in the caves (" + spikes + ")");
        check.eq(0, outside, "cinnabar underground far from the sulfur caves");

        // Core's underground biomes: every sulfur cave column of the populated inside is registered at its heights.
        int columns = 0, registered = 0, wrong = 0;
        BlockPos regPos = null;
        for (int x = (c0 + 1) * 16; x < c1 * 16; x += 3) {
            for (int z = (d0 + 1) * 16; z < d1 * 16; z += 3) {
                boolean[] mask = SulfurCaves.mask(w, x, z);
                for (int i = 0; i < 128 - min; i += 5) {
                    boolean want = mask != null && i < mask.length && mask[i];
                    Biome got = com.exiledradio.backportcore.worldgen.UndergroundBiomes.at(w, new BlockPos(x, min + i, z));
                    if (want) {
                        columns++;
                        if (got == SulfurCavesContent.biome) {
                            registered++;
                            regPos = new BlockPos(x, min + i, z);
                        }
                    } else if (got != null) {
                        wrong++;
                    }
                }
            }
        }
        // A column's mask is measured from its ground; one measured again now (the cache keeps 8192 columns) can differ by a
        // block where later features changed the ground. Registered spans are what population laid the bands by.
        GameDropsBackport.LOGGER.info("SELFTEST sulfur caves underground biomes: {} of {} mask samples registered, {} outside",
                registered, columns, wrong);
        check.ok(columns > 50 && registered >= columns * 0.99, "sulfur cave blocks are Core underground biomes (" + registered + " of " + columns + ")");
        check.eq(0, wrong, "no underground biome outside the mask");
        if (regPos != null) {
            int[] colors = com.exiledradio.backportcore.water.WaterColors.at(w, regPos);
            check.ok(colors != null && colors[0] == 0x34BF89, "water color there is the sulfur caves' #34bf89 ("
                    + (colors == null ? "none" : Integer.toHexString(colors[0])) + ")");
        }

        // Deferred work: the area's edge windows left spikes and tuff waiting; one more ring of chunks runs some.
        int waiting = com.exiledradio.backportcore.worldgen.DeferredPlacement.pending(w);
        check.ok(waiting > 0, "spikes past the population windows wait in Core's queue (" + waiting + ")");
        for (int cx = c0 - 1; cx <= c1 + 2; cx++) {
            for (int cz = d0 - 1; cz <= d1 + 2; cz++) {
                w.getChunk(cx, cz);
            }
        }
        com.exiledradio.backportcore.worldgen.DeferredPlacement.flush(w);
        int after = com.exiledradio.backportcore.worldgen.DeferredPlacement.pending(w);
        GameDropsBackport.LOGGER.info("SELFTEST sulfur caves deferred work: {} waiting, {} after one more ring", waiting, after);
        check.ok(after < waiting, "the next ring of chunks finishes waiting work (" + waiting + " then " + after
                + " with the ring's own)");
        deferredTuff(check, (c0 + 2) * 16 + 7, (d0 + 2) * 16 + 7);

        // Spawns: inside, the biome's list.
        BlockPos in = null;
        for (int x = p[0] - 8; x <= p[0] + 8 && in == null; x++) {
            boolean[] mask = SulfurCaves.mask(w, x, p[1]);
            if (mask != null) {
                for (int i = 0; i < mask.length; i++) {
                    if (mask[i]) {
                        in = new BlockPos(x, min + i, p[1]);
                        break;
                    }
                }
            }
        }
        if (check.ok(in != null, "a block in the sulfur caves")) {
            check.ok(SulfurCaves.at(w, in), "at() agrees with the mask");
            // Our handlers alone: on the bus, Caves & Cliffs' local mob cap (also LOW) can cancel first when the test world's
            // cap is full, and then nothing spawns at all.
            net.minecraftforge.event.world.WorldEvent.PotentialSpawns event = new net.minecraftforge.event.world.WorldEvent.PotentialSpawns(
                    w, EnumCreatureType.MONSTER, in, new java.util.ArrayList<Biome.SpawnListEntry>(
                    w.getChunkProvider().getPossibleCreatures(EnumCreatureType.MONSTER, in)));
            new SulfurSpawns().spawns(event);
            com.exiledradio.gamedropsbackport.sulfurcube.SulfurCubes.onPotentialSpawns(event);
            List<Biome.SpawnListEntry> list = event.getList();
            check.ok(list.stream().anyMatch(e -> e.entityClass == EntityCaveSpider.class)
                    && list.stream().noneMatch(e -> e.entityClass == EntitySpider.class), "spawns there are the sulfur caves': "
                    + list.stream().map(e -> e.entityClass.getSimpleName()).collect(java.util.stream.Collectors.toList()));
            BlockPos sky = new BlockPos(in.getX(), 200, in.getZ());
            check.ok(!SulfurCaves.at(w, sky), "not in the sky above");
        }
    }

    /** The deferred spring tuff on a prepared column of loaded, populated chunks: down through air to stone. */
    private void deferredTuff(Check check, int x, int z) {
        WorldServer w = check.world;
        BlockPos top = new BlockPos(x, 200, z);
        w.setBlockState(top, Blocks.AIR.getDefaultState(), 2);
        w.setBlockState(top.down(), Blocks.AIR.getDefaultState(), 2);
        w.setBlockState(top.down(2), Blocks.STONE.getDefaultState(), 2);
        w.setBlockState(top.down(3), Blocks.STONE.getDefaultState(), 2);
        int before = com.exiledradio.backportcore.worldgen.DeferredPlacement.pending(w);
        Deferred.tuff(w, top);
        check.eq(before + 1, com.exiledradio.backportcore.worldgen.DeferredPlacement.pending(w), "a spring's far tuff is queued");
        com.exiledradio.backportcore.worldgen.DeferredPlacement.flush(w);
        check.eq(SulfurCavesContent.tuff().getBlock(), w.getBlockState(top.down(2)).getBlock(), "and placed on the first solid block below");
        check.eq(Blocks.AIR, w.getBlockState(top.down()).getBlock(), "not in the air above it");
        w.setBlockState(top.down(2), Blocks.AIR.getDefaultState(), 2);
        w.setBlockState(top.down(3), Blocks.AIR.getDefaultState(), 2);
    }

    private static boolean near(boolean[] mask, int i, int d) {
        if (mask == null) {
            return false;
        }
        for (int j = Math.max(0, i - d); j < Math.min(mask.length, i + d + 1); j++) {
            if (mask[j]) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------------------------------ assets

    private void dump(Check check) throws Exception {
        if (SulfurCavesContent.tuffCopy == null) {
            return;
        }
        File dir = check.world.getMinecraftServer().getDataDirectory();
        try (PrintWriter states = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(dir, "selftest-states.txt"), true), "UTF-8"));
             PrintWriter items = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(dir, "selftest-items.txt"), true), "UTF-8"))) {
            states.println(GameDropsBackport.MOD_ID + ":tuff\tnormal\tvariants");
            items.println(GameDropsBackport.MOD_ID + ":tuff\t0\ttuff\ttile." + GameDropsBackport.MOD_ID + ".tuff.name");
        }
    }
}
