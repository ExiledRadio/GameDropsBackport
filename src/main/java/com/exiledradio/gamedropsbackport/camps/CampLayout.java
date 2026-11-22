package com.exiledradio.gamedropsbackport.camps;

import com.exiledradio.backportcore.structure.JigsawElement;
import com.exiledradio.backportcore.structure.JigsawPools;
import com.exiledradio.backportcore.structure.ModernJigsaw;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import java.util.function.IntBinaryOperator;

/**
 * 26.3's jigsaw assembly ({@code JigsawStructure.findGenerationPoint} and {@code JigsawPlacement.addPieces}) for
 * abandoned camps, draw for draw: the start piece's rotation and element, the start projected to
 * {@code WORLD_SURFACE_WG} at its middle, then Backport Core's {@link ModernJigsaw} (26.x's {@code Placer}) to the
 * structure's {@code size}, inside a box {@code max_distance_from_center} out from the start, with 26.3's expansion
 * hack and its nameless feature jigsaws (the camps' {@code minecraft:down} joints grow trees). No camp jigsaw has a
 * placement or selection priority. Every camp element is rigid: the ground is asked only for the start.
 */
public final class CampLayout {

    private CampLayout() {
    }

    /** {@code Structure.GenerationStub}: where the biome is checked, and the start piece. */
    public static final class Stub {
        /** The start piece's middle at the start's height. */
        public final BlockPos position;
        final CampPiece start;

        Stub(BlockPos position, CampPiece start) {
            this.position = position;
            this.start = start;
        }
    }

    /**
     * {@code JigsawStructure.findGenerationPoint} up to the stub. Draws from {@code random} exactly as 26.3 does.
     *
     * @param random  {@code StructureRandoms.start(seed, chunkX, chunkZ)}, today's {@code makeRandom}
     * @param surface {@code getFirstFreeHeight(x, z, WORLD_SURFACE_WG)}
     * @param column  {@code couldStructureExistInColumn} at the start piece's middle, asked before the ground is; null
     *                for every column
     * @return null where 26.3 has no start
     */
    @Nullable
    public static Stub stub(CampSettings.Variant v, int chunkX, int chunkZ, Random random, IntBinaryOperator surface,
                            @Nullable java.util.function.BiPredicate<Integer, Integer> column) {
        BlockPos origin = new BlockPos(chunkX << 4, v.startHeight, chunkZ << 4);
        Rotation rotation = Rotation.values()[random.nextInt(4)];
        JigsawPools.Pool pool = JigsawPools.pool(v.startPool);
        if (pool == null || pool.size() == 0) {
            GameDropsBackport.LOGGER.warn("The abandoned camp start pool {} is missing.", v.startPool);
            return null;
        }
        JigsawElement element = pool.random(random);
        if (element.isEmpty()) {
            return null;
        }
        CampPiece first = new CampPiece(element, origin, element.groundLevelDelta(), rotation, element.box(origin, rotation));
        StructureBoundingBox box = first.getBoundingBox();
        int midX = (box.maxX + box.minX) / 2;
        int midZ = (box.maxZ + box.minZ) / 2;
        if (v.projectToSurface && column != null && !column.test(midX, midZ)) {
            return null;
        }
        int y = v.projectToSurface ? origin.getY() + surface.applyAsInt(midX, midZ) : origin.getY();
        int floor = box.minY + first.groundLevelDelta();
        first.offset(0, y - floor, 0);
        return new Stub(new BlockPos(midX, y, midZ), first);
    }

    /**
     * The children of a stub made from {@code random}, the start piece first: Backport Core's {@link ModernJigsaw}
     * with 26.3's rules (priorities, nameless features, the expansion hack as the structure says).
     *
     * @param minY the world's lowest block; @param maxY its highest
     */
    public static List<CampPiece> assemble(CampSettings.Variant v, Stub stub, Random random, int minY, int maxY) {
        BlockPos c = stub.position;
        int h = v.maxDistance;
        int low = Math.max(c.getY() - h, minY);
        int high = Math.min(c.getY() + h + 1, maxY + 1) - 1;
        return ModernJigsaw.of(CampPiece::new).maxDepth(v.maxDepth).expansionHack(v.expansionHack).priorities(true)
                .namelessFeatures(true)
                .assemble(stub.start, new StructureBoundingBox(c.getX() - h, low, c.getZ() - h, c.getX() + h, high, c.getZ() + h), random);
    }
}
