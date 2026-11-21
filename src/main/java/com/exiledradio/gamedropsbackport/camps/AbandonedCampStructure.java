package com.exiledradio.gamedropsbackport.camps;

import com.exiledradio.backportcore.locate.BiomeLocate;
import com.exiledradio.backportcore.structure.ScatteredStructure;
import com.exiledradio.backportcore.structure.StructureRandoms;
import com.exiledradio.backportcore.terrain.TerrainHeights;
import com.exiledradio.backportcore.terrain.WorldHeights;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.IntBinaryOperator;

/**
 * 26.3's abandoned camps: structure set {@code abandoned_camp} (its {@code random_spread} grid, from vanilla's file)
 * of 18 jigsaw structures, one per biome. A candidate chunk picks among them as 26.3's
 * {@code ChunkGenerator.createStructures} does: weighted draws from {@code setLargeFeatureSeed}'s random, each
 * structure tried with a fresh start random and dropped from the draw when its stub's biome is not its own, until one
 * starts or none is left. Built from the populate step by Backport Core's scattered driver.
 */
public final class AbandonedCampStructure extends ScatteredStructure {

    public static final String SAVE_NAME = "GDB_AbandonedCamp";

    public AbandonedCampStructure() {
        super(SAVE_NAME, CampSettings.get().grid);
        // A tent and its camp stay within about 30 blocks of the start chunk's corner.
        this.range = 3;
    }

    /** What starts in a chunk: the variant and its stub. */
    public static final class Choice {
        public final CampSettings.Variant variant;
        public final CampLayout.Stub stub;
        final Random random;

        Choice(CampSettings.Variant variant, CampLayout.Stub stub, Random random) {
            this.variant = variant;
            this.stub = stub;
            this.random = random;
        }
    }

    /** {@code getFirstFreeHeight(x, z, WORLD_SURFACE_WG)} of the world's terrain, without loading chunks. */
    static IntBinaryOperator surface(World world) {
        TerrainHeights heights = TerrainHeights.forWorld(world);
        int sea = world.getSeaLevel();
        return (x, z) -> {
            if (heights != null) {
                return heights.height(x, z);
            }
            Chunk c = world.getChunkProvider().getLoadedChunk(x >> 4, z >> 4);
            return c == null ? sea : c.getHeightValue(x & 15, z & 15);
        };
    }

    /**
     * 26.3's choice for a candidate chunk (whatever the grid says), or null where none of the 18 starts.
     * Loads nothing.
     */
    @Nullable
    public static Choice choose(World world, int chunkX, int chunkZ) {
        CampSettings s = CampSettings.get();
        List<CampSettings.Variant> options = new ArrayList<CampSettings.Variant>(s.variants);
        Random pick = StructureRandoms.start(world.getSeed(), chunkX, chunkZ);
        int total = 0;
        for (CampSettings.Variant v : options) {
            total += v.weight;
        }
        IntBinaryOperator surface = surface(world);
        while (!options.isEmpty()) {
            int choice = pick.nextInt(total);
            int index = 0;
            for (CampSettings.Variant v : options) {
                choice -= v.weight;
                if (choice < 0) {
                    break;
                }
                index++;
            }
            CampSettings.Variant v = options.get(index);
            Random random = StructureRandoms.start(world.getSeed(), chunkX, chunkZ);
            // A variant whose biome no installed mod has cannot start: 26.3 would try it and find the biome wrong.
            CampLayout.Stub stub = !possible(v) ? null : CampLayout.stub(v, chunkX, chunkZ, random, surface,
                    (x, z) -> CampBiomes.column(world, x, z, v.biomes));
            if (stub != null) {
                String biome = CampBiomes.at(world, stub.position);
                if (biome != null && v.biomes.contains(biome)) {
                    return new Choice(v, stub, random);
                }
            }
            options.remove(index);
            total -= v.weight;
        }
        return null;
    }

    private static volatile java.util.Set<String> knownBiomes;

    /** @return true if some biome of this world (a registered biome, or a terrain mod's own) is the variant's */
    public static boolean possible(CampSettings.Variant v) {
        java.util.Set<String> ids = knownBiomes;
        if (ids == null) {
            // Every biome is registered before the first world loads.
            ids = knownBiomes = new java.util.HashSet<String>(BiomeLocate.ids());
        }
        for (String b : v.biomes) {
            if (ids.contains(b)) {
                return true;
            }
        }
        return false;
    }

    /** The pieces of the camp starting in this chunk, start piece first; empty where none starts. */
    public static List<CampPiece> layout(World world, int chunkX, int chunkZ) {
        Choice c = choose(world, chunkX, chunkZ);
        if (c == null) {
            return new ArrayList<CampPiece>();
        }
        return CampLayout.assemble(c.variant, c.stub, c.random, WorldHeights.minY(world), WorldHeights.maxYExclusive(world) - 1);
    }

    @Override
    protected boolean canStartAt(World world, int chunkX, int chunkZ) {
        return CampsContent.generates() && choose(world, chunkX, chunkZ) != null;
    }

    /** True while this places starts (iterating its start map), during which a new chunk must not add to the map. */
    private boolean building;

    @Override
    public synchronized boolean generateStructure(World world, Random random, ChunkPos chunk) {
        this.building = true;
        try {
            return super.generateStructure(world, random, chunk);
        } finally {
            this.building = false;
        }
    }

    /** How far, in chunks, a start's pieces and the terrain's 12-block reach around them go from its start chunk. */
    private static final int REACH_CHUNKS = 4;

    /** Backport Core's terrain adaptation planner ({@code beard_thin}). Loads no chunks. */
    public void startsNear(World world, int cx, int cz, Consumer<StructureStart> out) {
        this.world = world;
        if (!CampsContent.generates()) {
            return;
        }
        int s = placement().spacing();
        for (int gx = Math.floorDiv(cx - REACH_CHUNKS, s); gx <= Math.floorDiv(cx + REACH_CHUNKS, s); gx++) {
            for (int gz = Math.floorDiv(cz - REACH_CHUNKS, s); gz <= Math.floorDiv(cz + REACH_CHUNKS, s); gz++) {
                ChunkPos c = placement().candidate(world.getSeed(), gx * s, gz * s);
                if (Math.abs(c.x - cx) > REACH_CHUNKS || Math.abs(c.z - cz) > REACH_CHUNKS) {
                    continue;
                }
                long key = ChunkPos.asLong(c.x, c.z);
                StructureStart start = this.structureMap.get(key);
                if (start == null) {
                    if (!canSpawnStructureAtCoords(c.x, c.z)) {
                        continue;
                    }
                    if (this.building) {
                        start = getStructureStart(c.x, c.z);
                    } else {
                        generate(world, c.x, c.z, null);
                        start = this.structureMap.get(key);
                    }
                }
                if (start != null && start.isSizeableStructure() && reaches(start.getBoundingBox(), cx, cz)) {
                    out.accept(start);
                }
            }
        }
    }

    static boolean reaches(StructureBoundingBox b, int cx, int cz) {
        int x = cx << 4;
        int z = cz << 4;
        return b != null && b.maxX + 12 >= x && b.minX - 12 <= x + 15 && b.maxZ + 12 >= z && b.minZ - 12 <= z + 15;
    }

    @Override
    protected StructureStart getStructureStart(int chunkX, int chunkZ) {
        return new Start(this.world, chunkX, chunkZ);
    }

    public static final class Start extends StructureStart {
        /** For loading. */
        public Start() {
        }

        Start(World world, int chunkX, int chunkZ) {
            super(chunkX, chunkZ);
            this.components.addAll(layout(world, chunkX, chunkZ));
            updateBoundingBox();
        }

        @Override
        public void generateStructure(World world, Random random, StructureBoundingBox box) {
            // 1.12.2's MapGenStructure clips each chunk to y 1-512; a world that goes below 0 builds its whole height.
            StructureBoundingBox chunk = new StructureBoundingBox(box.minX, Math.min(box.minY, WorldHeights.minY(world)), box.minZ,
                    box.maxX, box.maxY, box.maxZ);
            super.generateStructure(world, random, chunk);
        }

        /** @return the pieces, start piece first */
        public List<StructureComponent> pieces() {
            return this.components;
        }
    }
}
