package com.exiledradio.gamedropsbackport.camps;

import com.exiledradio.backportcore.structure.JigsawElement;
import com.exiledradio.backportcore.structure.JigsawPools;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.google.gson.JsonElement;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.IntBinaryOperator;

/**
 * 26.3's jigsaw assembly ({@code JigsawPlacement.addPieces} and its {@code Placer}) for abandoned camps, draw for
 * draw: the start piece's rotation and element, the start projected to {@code WORLD_SURFACE_WG} at its middle, then
 * breadth first to the structure's {@code size}, each jigsaw (shuffled) trying its pool's elements and then the
 * fallback's (shuffled), each in shuffled rotations, with 26.3's expansion hack ({@code use_expansion_hack}), inside a
 * box {@code max_distance_from_center} out from the start, or the parent's own box for a joint inside it.
 *
 * <p>Two 26.3 rules Backport Core's 1.16.5 {@code Jigsaw} lacks: a feature element's jigsaw has no name, so it
 * attaches to any target ({@code JigsawBlock.canAttach}: the camps' {@code minecraft:down} joints grow trees), and
 * shuffles are {@code Util.shuffle} (n - 1 draws). No camp jigsaw has a placement or selection priority, so the
 * queue is first in, first out. Every camp element is rigid: the ground is asked only for the start.
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
     * The children of a stub made from {@code random}, the start piece first.
     *
     * @param minY the world's lowest block; @param maxY its highest
     */
    public static List<CampPiece> assemble(CampSettings.Variant v, Stub stub, Random random, int minY, int maxY) {
        List<CampPiece> pieces = new ArrayList<CampPiece>();
        CampPiece first = stub.start;
        pieces.add(first);
        if (v.maxDepth > 0) {
            BlockPos c = stub.position;
            int h = v.maxDistance;
            int low = Math.max(c.getY() - h, minY);
            int high = Math.min(c.getY() + h + 1, maxY + 1) - 1;
            Space cube = new Space(new StructureBoundingBox(c.getX() - h, low, c.getZ() - h, c.getX() + h, high, c.getZ() + h));
            cube.taken.add(new StructureBoundingBox(first.getBoundingBox()));
            Placer placer = new Placer(v, pieces, random);
            placer.tryPlacingChildren(first, cube, 0);
            State next;
            while ((next = placer.queue.pollFirst()) != null) {
                placer.tryPlacingChildren(next.piece, next.free, next.depth);
            }
        }
        // StructureStart.placeInChunk: every piece's pivot is the first piece's middle at its floor.
        StructureBoundingBox b = first.getBoundingBox();
        BlockPos pivot = new BlockPos(b.minX + (b.maxX - b.minX + 1) / 2, b.minY, b.minZ + (b.maxZ - b.minZ + 1) / 2);
        for (CampPiece piece : pieces) {
            piece.pivot(pivot);
        }
        return pieces;
    }

    // ---- free space: whole-block boxes

    static final class Space {
        @Nullable
        final StructureBoundingBox outside;
        final List<StructureBoundingBox> taken = new ArrayList<StructureBoundingBox>();

        Space(@Nullable StructureBoundingBox outside) {
            this.outside = outside;
        }

        /** "The box, shrunk by a quarter block, is all inside the free shape." */
        boolean fits(StructureBoundingBox box) {
            if (this.outside != null && !(this.outside.minX <= box.minX && box.maxX <= this.outside.maxX
                    && this.outside.minY <= box.minY && box.maxY <= this.outside.maxY
                    && this.outside.minZ <= box.minZ && box.maxZ <= this.outside.maxZ)) {
                return false;
            }
            for (StructureBoundingBox t : this.taken) {
                if (t.intersectsWith(box)) {
                    return false;
                }
            }
            return true;
        }
    }

    static final class State {
        final CampPiece piece;
        final Space free;
        final int depth;

        State(CampPiece piece, Space free, int depth) {
            this.piece = piece;
            this.free = free;
            this.depth = depth;
        }
    }

    /** @return true for a {@code feature_pool_element}, whose one jigsaw has no name in 26.3 */
    static boolean isFeature(JigsawElement element) {
        JsonElement type = element.json() == null ? null : element.json().get("element_type");
        return type != null && type.getAsString().endsWith("feature_pool_element");
    }

    /** 26.3's {@code JigsawBlock.canAttach}; a nameless (feature) target attaches to any. */
    static boolean canAttach(JigsawElement.Connector source, JigsawElement.Connector target, boolean targetNameless) {
        boolean rollable = "rollable".equals(source.joint);
        return source.front == target.front.getOpposite() && (rollable || source.top == target.top)
                && (targetNameless || source.target.equals(target.name));
    }

    /** {@code Util.shuffle} on a copy: n - 1 draws. */
    static <T> List<T> shuffled(List<T> in, Random random) {
        List<T> out = new ArrayList<T>(in);
        Collections.shuffle(out, random);
        return out;
    }

    /** {@code JigsawPlacement.Placer}. */
    static final class Placer {
        final CampSettings.Variant v;
        final List<CampPiece> pieces;
        final Random random;
        final ArrayDeque<State> queue = new ArrayDeque<State>();

        Placer(CampSettings.Variant v, List<CampPiece> pieces, Random random) {
            this.v = v;
            this.pieces = pieces;
            this.random = random;
        }

        void tryPlacingChildren(CampPiece parent, Space outerFree, int depth) {
            JigsawElement element = parent.element();
            BlockPos position = parent.position();
            Rotation rotation = parent.rotation();
            boolean parentRigid = element.rigid();
            Space inner = null;
            StructureBoundingBox parentBox = parent.getBoundingBox();
            int parentMinY = parentBox.minY;

            connectorLoop:
            for (JigsawElement.Connector connector : shuffled(element.connectors(position, rotation), this.random)) {
                EnumFacing front = connector.front;
                BlockPos at = connector.pos;
                BlockPos target = at.offset(front);
                int connectorY = at.getY() - parentMinY;
                ResourceLocation poolId = new ResourceLocation(connector.pool);
                JigsawPools.Pool pool = JigsawPools.pool(poolId);
                if (pool == null || (pool.size() == 0 && !poolId.equals(JigsawPools.EMPTY))) {
                    GameDropsBackport.LOGGER.warn("Empty or non-existent pool: {}", poolId);
                    continue;
                }
                ResourceLocation fallbackId = pool.fallback();
                JigsawPools.Pool fallback = JigsawPools.pool(fallbackId);
                if (fallback == null || (fallback.size() == 0 && !fallbackId.equals(JigsawPools.EMPTY))) {
                    GameDropsBackport.LOGGER.warn("Empty or non-existent fallback pool: {}", fallbackId);
                    continue;
                }
                Space free;
                if (parentBox.isVecInside(target)) {
                    if (inner == null) {
                        inner = new Space(new StructureBoundingBox(parentBox));
                    }
                    free = inner;
                } else {
                    free = outerFree;
                }
                List<JigsawElement> candidates = new ArrayList<JigsawElement>();
                if (depth != this.v.maxDepth) {
                    candidates.addAll(shuffled(pool.elements(), this.random));
                }
                candidates.addAll(shuffled(fallback.elements(), this.random));

                for (JigsawElement candidate : candidates) {
                    if (candidate.isEmpty()) {
                        break;
                    }
                    boolean nameless = isFeature(candidate);
                    for (Rotation r : shuffled(Arrays.asList(Rotation.values()), this.random)) {
                        List<JigsawElement.Connector> theirs = shuffled(candidate.connectors(BlockPos.ORIGIN, r), this.random);
                        StructureBoundingBox hackBox = candidate.box(BlockPos.ORIGIN, r);
                        int expandTo = 0;
                        if (this.v.expansionHack && hackBox.getYSize() <= 16) {
                            for (JigsawElement.Connector j : theirs) {
                                if (!hackBox.isVecInside(j.pos.offset(j.front))) {
                                    continue;
                                }
                                JigsawPools.Pool child = JigsawPools.pool(new ResourceLocation(j.pool));
                                int size = child == null ? 0 : child.maxSize();
                                JigsawPools.Pool childFallback = child == null ? null : JigsawPools.pool(child.fallback());
                                int fallbackSize = childFallback == null ? 0 : childFallback.maxSize();
                                expandTo = Math.max(expandTo, Math.max(size, fallbackSize));
                            }
                        }
                        for (JigsawElement.Connector their : theirs) {
                            if (!canAttach(connector, their, nameless)) {
                                continue;
                            }
                            BlockPos theirPos = their.pos;
                            BlockPos offset = target.subtract(theirPos);
                            StructureBoundingBox childBox = candidate.box(offset, r);
                            boolean childRigid = candidate.rigid();
                            int theirY = theirPos.getY();
                            int delta = connectorY - theirY + front.getYOffset();
                            // Every camp element is rigid; a terrain-matching one would ask the ground here.
                            int targetMinY = parentRigid && childRigid ? parentMinY + delta : at.getY() - theirY;
                            int dy = targetMinY - childBox.minY;
                            StructureBoundingBox moved = new StructureBoundingBox(childBox);
                            moved.offset(0, dy, 0);
                            BlockPos childPos = offset.add(0, dy, 0);
                            if (expandTo > 0) {
                                int newSize = Math.max(expandTo + 1, moved.maxY - moved.minY);
                                moved.maxY = Math.max(moved.maxY, moved.minY + newSize);
                            }
                            if (free.fits(moved)) {
                                free.taken.add(new StructureBoundingBox(moved));
                                int parentGround = parent.groundLevelDelta();
                                int childGround = childRigid ? parentGround - delta : candidate.groundLevelDelta();
                                CampPiece child = new CampPiece(candidate, childPos, childGround, r, moved);
                                int sourceGroundY;
                                if (parentRigid) {
                                    sourceGroundY = parentMinY + connectorY;
                                } else if (childRigid) {
                                    sourceGroundY = targetMinY + theirY;
                                } else {
                                    sourceGroundY = at.getY() + delta / 2;
                                }
                                parent.addJunction(target.getX(), sourceGroundY - connectorY + parentGround, target.getZ());
                                child.addJunction(at.getX(), sourceGroundY - theirY + childGround, at.getZ());
                                this.pieces.add(child);
                                if (depth + 1 <= this.v.maxDepth) {
                                    this.queue.addLast(new State(child, free, depth + 1));
                                }
                                continue connectorLoop;
                            }
                        }
                    }
                }
            }
        }
    }
}
