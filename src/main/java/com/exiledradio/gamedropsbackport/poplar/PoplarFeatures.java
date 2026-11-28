package com.exiledradio.gamedropsbackport.poplar;

import com.exiledradio.backportcore.waterlog.Waterlogging;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDeadBush;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.BlockVine;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.WorldGenTaiga2;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 26.3's poplar trees, fallen poplars and the dappled forest's vegetal step, on any 1.12.2 world.
 *
 * <h2>The poplar ({@code worldgen/feature/red_poplar}, orange, yellow)</h2>
 * {@code TreeFeature} with {@code poplar_trunk_placer(7, 4, 0; above branches 4, branches 1-4)},
 * {@code poplar_foliage_placer(radius 5-8 weighted 5/5/1/1, height 5-6, side holes 0.15)} and
 * {@code two_layers_feature_size(upper 2)}, step for step and in the same order of random draws: a 7-11 high trunk
 * with dirt under it (grass and mycelium become dirt), one to four one-block branches four below the top, a tall
 * rhombus of leaves around the upper trunk with logs reaching out inside it, then the decorators: leaf litter on the
 * ground around it (the {@code _leaf_litter} trees, worldgen only) and shelf mushrooms on its lower trunk (40%).
 *
 * <h2>The fallen poplar ({@code fallen_poplar_tree})</h2>
 * {@code FallenTreeFeature}: a one-block stump, and two to three blocks away a 2-5 long log lying on the ground in one
 * of four directions, with brown mushrooms on top (10% a block) and shelf mushrooms on its sides (80%).
 *
 * <h2>The vegetal step ({@link #vegetation})</h2>
 * {@code trees_dappled_forest} (6 tries: red 200, orange 240, yellow 90 poplars with leaf litter, spruce 27, fallen
 * poplar 120), {@code brown_mushroom_dappled_forest} (one chunk in two, 96 tries), {@code patch_red_shrub} (8 tries)
 * and {@code patch_grass_forest} (2 patches of 32 tries). The spruce is 1.12.2's own. Positions are drawn in the
 * 16x16 square offset by 8, as 1.12.2 decorates, so nothing reaches an ungenerated chunk. Nothing here depends on
 * how the biome was placed or on the world's height, so other terrain (Caves &amp; Cliffs' 1.18 generator) can call
 * {@link #vegetation}, {@link #tree} and {@link #fallenTree} directly.
 */
public final class PoplarFeatures {

    /** {@code poplar_trunk_placer}'s base height: the shortest poplar. */
    public static final int BASE_HEIGHT = 7;
    private static final int HEIGHT_RAND_A = 4;
    private static final int HEIGHT_RAND_B = 0;
    private static final int ABOVE_BRANCHES = 4;
    private static final float SIDE_HOLE_CHANCE = 0.15F;
    private static final float SHELF_STANDING = 0.4F;
    private static final float SHELF_FALLEN = 0.8F;

    /** {@code trees_dappled_forest}'s weights, in its order: red, orange, yellow (with leaf litter), spruce, fallen. */
    private static final int[] TREE_WEIGHTS = {200, 240, 90, 27, 120};

    private static final EnumFacing[] ALL = {EnumFacing.DOWN, EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH,
            EnumFacing.WEST, EnumFacing.EAST};
    private static final EnumFacing[] HORIZONTAL = {EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST};

    private PoplarFeatures() {
    }

    private static int between(Random rand, int min, int max) {
        return min + rand.nextInt(max - min + 1);
    }

    /** {@code TrapezoidInt(min, max, plateau 0)}: two draws, peaked in the middle. */
    private static int trapezoid(Random rand, int min, int max) {
        int range = max - min;
        int start = range / 2;
        int end = range - start;
        return min + between(rand, 0, end) + between(rand, 0, start);
    }

    // ===========================================================================================
    // The poplar
    // ===========================================================================================

    /** What a tree placed: its trunk blocks (with the dirt under it) and its leaves. */
    private static final class Tree {
        final Set<BlockPos> trunks = new HashSet<BlockPos>();
        final Set<BlockPos> foliage = new HashSet<BlockPos>();
        final World world;
        final IBlockState log;
        final IBlockState leaves;

        Tree(World world, IBlockState log, IBlockState leaves) {
            this.world = world;
            this.log = log;
            this.leaves = leaves;
        }

        void trunk(BlockPos pos, IBlockState state) {
            this.trunks.add(pos.toImmutable());
            this.world.setBlockState(pos, state, 3);
        }

        /**
         * {@code FoliagePlacer.tryPlaceLeaf}: leaves keep a water source there (26.3 sets {@code waterlogged}); flowing
         * water is replaced.
         */
        void leaf(BlockPos pos, IBlockState state) {
            this.foliage.add(pos.toImmutable());
            Waterlogging.placeKeepingWater(this.world, pos, state, 3);
        }
    }

    /**
     * Places one poplar with its trunk at {@code origin}, as {@code TreeFeature.place}.
     *
     * @param leafLitter the worldgen trees' {@code _leaf_litter} form, with leaf litter around its foot
     * @return false if it did not fit (nothing is placed then)
     */
    public static boolean tree(World world, Random rand, BlockPos origin, BlockPoplarLeaves.Color color, boolean leafLitter) {
        if (PoplarContent.set == null) {
            return false;
        }
        // With Dynamic Trees - Backport, a dynamic poplar of this color grows here instead, with this tree's decorators.
        if (com.exiledradio.backportcore.compat.DynamicTrees.grow(world, origin, rand, "dtbackport:" + color.name + "poplar")) {
            List<BlockPos> trunk = dynamicTrunk(world, origin);
            if (leafLitter) {
                leafLitter(world, rand, trunk, 96, 4, 2, false);
                leafLitter(world, rand, trunk, 150, 2, 2, true);
            }
            // Dynamic Trees - Backport's slimmer poplar (its Decorators, 6629b8c): the trunk's bottom two logs only.
            shelfMushrooms(world, rand, trunk, SHELF_STANDING, DT_SHELF_MIN, DT_SHELF_MAX);
            fullTrunkUnderMushrooms(world, trunk);
            return true;
        }
        IBlockState log = PoplarContent.set.log.getDefaultState().withProperty(BlockRotatedPillar.AXIS, EnumFacing.Axis.Y);
        Block leavesBlock = color == BlockPoplarLeaves.Color.RED ? PoplarContent.redLeaves
                : color == BlockPoplarLeaves.Color.ORANGE ? PoplarContent.orangeLeaves : PoplarContent.yellowLeaves;
        IBlockState leaves = leavesBlock.getDefaultState().withProperty(BlockPoplarLeaves.DECAYABLE, true)
                .withProperty(BlockPoplarLeaves.CHECK_DECAY, false);
        Tree tree = new Tree(world, log, leaves);

        // TreeFeature.doPlace
        int treeHeight = BASE_HEIGHT + rand.nextInt(HEIGHT_RAND_A + 1) + rand.nextInt(HEIGHT_RAND_B + 1);
        int foliageHeight = between(rand, 5, 6);
        int leafRadius = radius(rand);
        if (origin.getY() < 1 || origin.getY() + treeHeight + 1 > world.getHeight()) {
            return false;
        }
        if (maxFreeHeight(world, treeHeight, origin) < treeHeight) {
            return false;
        }
        BlockPos attachment = trunk(world, rand, treeHeight, origin, tree);
        foliage(world, rand, tree, attachment, foliageHeight, leafRadius);

        if (tree.trunks.isEmpty() && tree.foliage.isEmpty()) {
            return false;
        }
        List<BlockPos> logs = sortedByY(tree.trunks);
        if (leafLitter) {
            leafLitter(world, rand, logs, 96, 4, 2, false);
            leafLitter(world, rand, logs, 150, 2, 2, true);
        }
        shelfMushrooms(world, rand, logs, SHELF_STANDING, 1, 4);
        return true;
    }

    /** The weighted radius list: 5 (weight 5), 6 (5), 7 (1), 8 (1). */
    private static int radius(Random rand) {
        int i = rand.nextInt(12);
        return i < 5 ? 5 : i < 10 ? 6 : i < 11 ? 7 : 8;
    }

    /** {@code getMaxFreeTreeHeight} with {@code two_layers_feature_size(limit 1, lower 0, upper 2)}, vines ignored. */
    private static int maxFreeHeight(World world, int maxHeight, BlockPos origin) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int y = 0; y <= maxHeight + 1; y++) {
            int r = y < 1 ? 0 : 2;
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    at.setPos(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (!isFree(world, at)) {
                        return y - 2;
                    }
                }
            }
        }
        return maxHeight;
    }

    /** {@code PoplarTrunkPlacer.placeTrunk}: @return the foliage attachment point */
    private static BlockPos trunk(World world, Random rand, int treeHeight, BlockPos origin, Tree tree) {
        soilBeneath(world, origin.down(), tree);
        int branchY = treeHeight - ABOVE_BRANCHES;
        for (int y = 0; y < treeHeight; y++) {
            BlockPos at = origin.up(y);
            placeLog(world, at, tree, EnumFacing.Axis.Y);
            // Every level shuffles all six directions (vanilla draws them whether or not it branches there).
            List<EnumFacing> directions = shuffledHorizontal(rand);
            if (branchY - 1 == y) {
                int branches = between(rand, 1, 4);
                for (int i = 0; i < branches; i++) {
                    EnumFacing d = directions.get(i);
                    placeLog(world, at.offset(d), tree, d.getAxis());
                }
            }
        }
        return origin.up(branchY);
    }

    /** {@code Direction.allShuffled} ({@code Util.shuffle}), then the horizontal ones in that order. */
    private static List<EnumFacing> shuffledHorizontal(Random rand) {
        List<EnumFacing> all = new ArrayList<EnumFacing>();
        Collections.addAll(all, ALL);
        for (int i = all.size(); i > 1; i--) {
            int swap = rand.nextInt(i);
            all.set(i - 1, all.set(swap, all.get(i - 1)));
        }
        List<EnumFacing> out = new ArrayList<EnumFacing>(4);
        for (EnumFacing f : all) {
            if (f.getAxis().isHorizontal()) {
                out.add(f);
            }
        }
        return out;
    }

    /** {@code soil_beneath_tree}: dirt, unless it is already dirt, coarse dirt, podzol, mud or moss. */
    private static void soilBeneath(World world, BlockPos pos, Tree tree) {
        IBlockState s = world.getBlockState(pos);
        Block b = s.getBlock();
        if (b == Blocks.DIRT || named(b, "rooted_dirt", "mud", "muddy_mangrove_roots", "moss_block", "pale_moss_block")) {
            return;
        }
        tree.trunk(pos, Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.DIRT));
    }

    private static void placeLog(World world, BlockPos pos, Tree tree, EnumFacing.Axis axis) {
        if (validTreePos(world, pos)) {
            tree.trunk(pos, tree.log.withProperty(BlockRotatedPillar.AXIS, axis));
        }
    }

    /** {@code PoplarFoliagePlacer.createFoliage} (offset 0, a single trunk). */
    private static void foliage(World world, Random rand, Tree tree, BlockPos at, int foliageHeight, int leafRadius) {
        int r = leafRadius - 1;
        boolean flip = rand.nextBoolean();
        int h = foliageHeight;
        row(world, rand, tree, at, r - 2, h - 1, h, flip);
        row(world, rand, tree, at, r - 1, h - 2, h, flip);
        row(world, rand, tree, at, r - 1, h - 3, h, flip);
        for (int y = h - 4; y >= 1; y--) {
            row(world, rand, tree, at, r, y, h, flip);
        }
        replaceLeavesWithLog(world, tree, at, r, h - 4, h, flip);
        row(world, rand, tree, at, r - 1, 0, h, flip);
        row(world, rand, tree, at, MathHelper.clamp(r - 2, 1, 2), -1, h, flip);
    }

    private static void row(World world, Random rand, Tree tree, BlockPos origin, int r, int y, int h, boolean flip) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (!skip(rand, dx, y, dz, r, h, flip)) {
                    at.setPos(origin.getX() + dx, origin.getY() + y, origin.getZ() + dz);
                    tryLeaf(world, tree, at);
                }
            }
        }
    }

    private static boolean skip(Random rand, int dx, int y, int dz, int r, int h, boolean flip) {
        boolean partial = partialRow(h, y);
        int cut = cornerCut(dx, dz, r, partial, flip);
        int ax = Math.abs(dx);
        int az = Math.abs(dz);
        if (partial && (ax == r || az == r)) {
            return true;
        }
        int hole = rand.nextFloat() <= SIDE_HOLE_CHANCE ? 1 : 0;
        return !inRhombus(r, ax, az, cut, hole);
    }

    private static void replaceLeavesWithLog(World world, Tree tree, BlockPos origin, int r, int y, int h, boolean flip) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int az = Math.abs(dz);
                int ax = Math.abs(dx);
                if (inRhombus(r, ax, az, cornerCut(dx, dz, r, partialRow(h, y), flip), 2)
                        && (az == 0 && r - ax >= 4 || ax == 0 && r - az >= 4)) {
                    at.setPos(origin.getX() + dx, origin.getY() + y, origin.getZ() + dz);
                    if (world.getBlockState(at) == tree.leaves) {
                        tree.leaf(at, tree.log.withProperty(BlockRotatedPillar.AXIS, az == 0 ? EnumFacing.Axis.X : EnumFacing.Axis.Z));
                    }
                }
            }
        }
    }

    private static int cornerCut(int dx, int dz, int r, boolean partial, boolean flip) {
        boolean small = flip ? (dx > 0 && dz > 0 || dz < 0 && dx < 0) : (dx > 0 && dz < 0 || dz > 0 && dx < 0);
        return small ? r - 1 : (partial ? r + 1 : r);
    }

    private static boolean inRhombus(int r, int ax, int az, int cut, int extra) {
        return ax + az <= r * 2 - (cut + extra);
    }

    private static boolean partialRow(int h, int y) {
        return h - 1 == y || h - 2 == y;
    }

    /** {@code FoliagePlacer.tryPlaceLeaf}: not over persistent (player-placed) leaves, on a valid tree position. */
    private static void tryLeaf(World world, Tree tree, BlockPos pos) {
        IBlockState s = world.getBlockState(pos);
        if (s.getBlock() instanceof net.minecraft.block.BlockLeaves && s.getPropertyKeys().contains(net.minecraft.block.BlockLeaves.DECAYABLE)
                && !s.getValue(net.minecraft.block.BlockLeaves.DECAYABLE)) {
            return;
        }
        if (validTreePos(world, pos)) {
            tree.leaf(pos, tree.leaves);
        }
    }

    // ===========================================================================================
    // Decorators
    // ===========================================================================================

    /** {@code TreeDecorator.Context}'s order: by height, stable, from vanilla's hash set order. */
    private static List<BlockPos> sortedByY(Set<BlockPos> positions) {
        List<BlockPos> list = new ArrayList<BlockPos>(positions);
        Collections.sort(list, new Comparator<BlockPos>() {
            @Override
            public int compare(BlockPos a, BlockPos b) {
                return Integer.compare(a.getY(), b.getY());
            }
        });
        return list;
    }

    /**
     * {@code PlaceOnGroundDecorator} with leaf litter: {@code tries} random spots in the box around the tree's lowest
     * layer (widened by {@code radius}, {@code height} up and down); a spot with a full solid block, air (or vine)
     * above, and nothing but leaves over it gets a leaf litter of one of the provider's states.
     *
     * @param four the second provider, which has four-piece litter too (16 states, not 12)
     */
    private static void leafLitter(World world, Random rand, List<BlockPos> logs, int tries, int radius, int height, boolean four) {
        Block litter = LeafLitter.block();
        if (litter == null || logs.isEmpty()) {
            return;
        }
        BlockPos first = logs.get(0);
        int minY = first.getY();
        int minX = first.getX();
        int maxX = minX;
        int minZ = first.getZ();
        int maxZ = minZ;
        for (BlockPos p : logs) {
            if (p.getY() == minY) {
                minX = Math.min(minX, p.getX());
                maxX = Math.max(maxX, p.getX());
                minZ = Math.min(minZ, p.getZ());
                maxZ = Math.max(maxZ, p.getZ());
            }
        }
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int i = 0; i < tries; i++) {
            at.setPos(between(rand, minX - radius, maxX + radius), between(rand, minY - height, minY + height),
                    between(rand, minZ - radius, maxZ + radius));
            BlockPos above = at.up();
            IBlockState aboveState = world.getBlockState(above);
            IBlockState ground = world.getBlockState(at);
            if ((aboveState.getBlock().isAir(aboveState, world, above) || aboveState.getBlock() == Blocks.VINE)
                    && ground.isOpaqueCube() && ground.isFullCube()
                    && motionBlockingNoLeaves(world, at.getX(), at.getZ()) <= above.getY()) {
                int pick = rand.nextInt(four ? 16 : 12);
                world.setBlockState(above, litterState(litter, HORIZONTAL[pick % 4], pick / 4 + 1), 3);
            }
        }
    }

    /** A leaf litter state by property name, so the owner's block and our copy both work. */
    private static IBlockState litterState(Block litter, EnumFacing facing, int amount) {
        IBlockState s = litter.getDefaultState();
        for (IProperty<?> p : s.getPropertyKeys()) {
            if ("facing".equals(p.getName()) && p.getAllowedValues().contains(facing)) {
                s = s.withProperty(BlockHorizontal.FACING, facing);
            } else if ("segment_amount".equals(p.getName()) && p instanceof PropertyInteger
                    && p.getAllowedValues().contains(amount)) {
                s = s.withProperty((PropertyInteger) p, amount);
            }
        }
        return s;
    }

    /**
     * {@code ShelfMushroomDecorator}: with chance {@code probability}, shelf mushrooms on the lowest logs. A standing
     * tree: two neighboring sides picked once, each log {@code minDy} to {@code maxDy} above the trunk's bottom (26.3's 1
     * to 4; a Dynamic Trees poplar's bottom two, 0 to 1) tries them in order (a quarter each) and takes the first that is
     * free, not wet and has no mushroom under it. A fallen log: both long sides of every log, a quarter each, never
     * beside another mushroom.
     */
    public static void shelfMushrooms(World world, Random rand, List<BlockPos> logs, float probability, int minDy, int maxDy) {
        if (PoplarContent.shelfMushroom == null || rand.nextFloat() >= probability || logs.isEmpty()) {
            return;
        }
        BlockPos first = logs.get(0);
        BlockPos last = logs.get(logs.size() - 1);
        if (first.getY() == last.getY()) {
            EnumFacing[] sides = first.getX() != last.getX() ? new EnumFacing[] {EnumFacing.NORTH, EnumFacing.SOUTH}
                    : new EnumFacing[] {EnumFacing.EAST, EnumFacing.WEST};
            for (BlockPos log : logs) {
                for (EnumFacing f : sides) {
                    if (!(rand.nextFloat() > 0.25F)) {
                        BlockPos at = log.offset(f);
                        if (mushroomSpot(world, at) && !mushroomBeside(world, at) && !mushroomBeside(world, log)) {
                            placeMushroom(world, rand, at, f);
                        }
                    }
                }
            }
            return;
        }
        EnumFacing a = HORIZONTAL[rand.nextInt(4)];
        EnumFacing[] sides = {a, a.rotateY()};
        int baseY = first.getY();
        for (BlockPos log : logs) {
            int dy = log.getY() - baseY;
            if (dy < minDy || dy > maxDy) {
                continue;
            }
            for (EnumFacing f : sides) {
                if (!(rand.nextFloat() > 0.25F)) {
                    BlockPos at = log.offset(f);
                    if (mushroomSpot(world, at) && world.getBlockState(at.down()).getBlock() != PoplarContent.shelfMushroom) {
                        placeMushroom(world, rand, at, f);
                        break;
                    }
                }
            }
        }
    }

    /** A Dynamic Trees trunk from its first log up: the blocks with a {@code radius}, as a branch has. */
    private static List<BlockPos> dynamicTrunk(World world, BlockPos origin) {
        List<BlockPos> out = new ArrayList<BlockPos>();
        BlockPos p = origin;
        while (out.size() < 32 && radiusProperty(world.getBlockState(p)) != null) {
            out.add(p);
            p = p.up();
        }
        return out;
    }

    @javax.annotation.Nullable
    private static IProperty<?> radiusProperty(IBlockState state) {
        for (IProperty<?> prop : state.getPropertyKeys()) {
            if (prop.getName().equals("radius") && prop.getValueClass() == Integer.class) {
                return prop;
            }
        }
        return null;
    }

    /**
     * The radius at least the {@code i}th trunk log (from the bottom) grows to, with mushrooms up to log {@code top} of
     * {@code height}: 8 to the mushrooms, two less a log above, never the top two logs; -1 once nothing more changes.
     */
    public static int wantedRadius(int i, int top, int height) {
        if (top < 0 || i >= height - 2) {
            return -1;
        }
        int want = i <= top ? 8 : 8 - 2 * (i - top);
        return want <= 1 ? -1 : want;
    }

    /** A Dynamic Trees poplar's shelf mushrooms: its bottom two logs (Dynamic Trees - Backport's slimmer poplar). */
    public static final int DT_SHELF_MIN = 0;
    public static final int DT_SHELF_MAX = 1;

    /**
     * A shelf mushroom sits against a whole log; a Dynamic Trees trunk is thinner, so the logs up to the highest mushroom
     * grow a full log thick, and each log above two sizes thinner than the one below it, until the trunk's own thickness
     * takes over; the top two logs (and the leaves they feed) stay as grown. Dynamic Trees - Backport's
     * {@code Decorators} does the same for the poplars it grows itself. Only ever thicker (Dynamic Trees only thickens
     * a branch).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static void fullTrunkUnderMushrooms(World world, List<BlockPos> trunk) {
        int top = -1;
        for (int i = 0; i < trunk.size(); i++) {
            for (EnumFacing f : HORIZONTAL) {
                if (PoplarContent.shelfMushroom != null && world.getBlockState(trunk.get(i).offset(f)).getBlock() == PoplarContent.shelfMushroom) {
                    top = i;
                }
            }
        }
        if (top < 0) {
            return;
        }
        for (int i = 0; i < trunk.size(); i++) {
            int want = wantedRadius(i, top, trunk.size());
            if (want < 0) {
                break;
            }
            IBlockState s = world.getBlockState(trunk.get(i));
            IProperty prop = radiusProperty(s);
            if (prop != null && prop.getAllowedValues().contains(want) && (Integer) s.getValue(prop) < want) {
                world.setBlockState(trunk.get(i), s.withProperty(prop, want), 2);
            }
        }
    }

    /** {@code isBlockReplaceableWithShelfMushroom}: replaceable, and no water there or beside it. */
    private static boolean mushroomSpot(World world, BlockPos pos) {
        if (!world.getBlockState(pos).getBlock().isReplaceable(world, pos)) {
            return false;
        }
        for (BlockPos p : new BlockPos[] {pos, pos.east(), pos.west(), pos.north(), pos.south()}) {
            Block b = world.getBlockState(p).getBlock();
            if (b == Blocks.WATER || b == Blocks.FLOWING_WATER) {
                return false;
            }
        }
        return true;
    }

    private static boolean mushroomBeside(World world, BlockPos pos) {
        for (EnumFacing f : HORIZONTAL) {
            if (world.getBlockState(pos.offset(f)).getBlock() == PoplarContent.shelfMushroom) {
                return true;
            }
        }
        return false;
    }

    private static void placeMushroom(World world, Random rand, BlockPos pos, EnumFacing facing) {
        world.setBlockState(pos, PoplarContent.shelfMushroom.getDefaultState()
                .withProperty(BlockShelfMushroom.AGE, rand.nextInt(2)).withProperty(BlockHorizontal.FACING, facing), 3);
    }

    // ===========================================================================================
    // The fallen poplar
    // ===========================================================================================

    /** {@code FallenTreeFeature.place} for {@code fallen_poplar_tree}: always true, as vanilla's. */
    public static boolean fallenTree(World world, Random rand, BlockPos origin) {
        if (PoplarContent.set == null) {
            return false;
        }
        IBlockState log = PoplarContent.set.log.getDefaultState();
        world.setBlockState(origin, log.withProperty(BlockRotatedPillar.AXIS, EnumFacing.Axis.Y), 3);
        EnumFacing direction = HORIZONTAL[rand.nextInt(4)];
        int length = between(rand, 4, 7) - 2;
        BlockPos.MutableBlockPos start = new BlockPos.MutableBlockPos(origin.offset(direction, 2 + rand.nextInt(2)));
        // setGroundHeightForFallenLogStartPos
        start.setY(start.getY() + 1);
        for (int i = 0; i < 6; i++) {
            if (validTreePos(world, start) && overSolidGround(world, start)) {
                break;
            }
            start.setY(start.getY() - 1);
        }
        if (!canPlaceFallenLog(world, length, start.toImmutable(), direction)) {
            return true;
        }
        Set<BlockPos> fallen = new HashSet<BlockPos>();
        BlockPos at = start.toImmutable();
        for (int i = 0; i < length; i++) {
            world.setBlockState(at, log.withProperty(BlockRotatedPillar.AXIS, direction.getAxis()), 3);
            fallen.add(at);
            at = at.offset(direction);
        }
        List<BlockPos> logs = sortedByY(fallen);
        // attached_to_logs: a brown mushroom on top, 10% a log, in a shuffled order.
        List<BlockPos> shuffled = new ArrayList<BlockPos>(logs);
        for (int i = shuffled.size(); i > 1; i--) {
            int swap = rand.nextInt(i);
            shuffled.set(i - 1, shuffled.set(swap, shuffled.get(i - 1)));
        }
        for (BlockPos p : shuffled) {
            rand.nextInt(1);
            BlockPos up = p.up();
            if (rand.nextFloat() <= 0.1F && world.isAirBlock(up)) {
                world.setBlockState(up, Blocks.BROWN_MUSHROOM.getDefaultState(), 3);
            }
        }
        shelfMushrooms(world, rand, logs, SHELF_FALLEN, 1, 4);
        return true;
    }

    private static boolean canPlaceFallenLog(World world, int length, BlockPos start, EnumFacing direction) {
        int gap = 0;
        BlockPos at = start;
        for (int i = 0; i < length; i++) {
            if (!validTreePos(world, at)) {
                return false;
            }
            if (!overSolidGround(world, at)) {
                if (++gap > 2) {
                    return false;
                }
            } else {
                gap = 0;
            }
            at = at.offset(direction);
        }
        return true;
    }

    private static boolean overSolidGround(World world, BlockPos pos) {
        BlockPos below = pos.down();
        return world.getBlockState(below).getBlockFaceShape(world, below, EnumFacing.UP) == BlockFaceShape.SOLID;
    }

    // ===========================================================================================
    // The dappled forest's vegetal step
    // ===========================================================================================

    /**
     * The dappled forest's 26.3 vegetal step for the chunk whose decoration corner is {@code corner} (1.12.2's
     * {@code chunkPos}; positions are drawn from {@code corner + 8}).
     */
    public static void vegetation(World world, Random rand, BlockPos corner, Biome biome) {
        BlockPos base = corner.add(8, 0, 8);
        // trees_dappled_forest: count 6, in_square, no water above, OCEAN_FLOOR, biome.
        for (int i = 0; i < 6; i++) {
            int x = base.getX() + rand.nextInt(16);
            int z = base.getZ() + rand.nextInt(16);
            int floor = oceanFloor(world, x, z);
            if (worldSurface(world, x, z) - floor > 0) {
                continue;
            }
            BlockPos pos = new BlockPos(x, floor, z);
            if (world.getBiome(pos) != biome) {
                continue;
            }
            int pick = rand.nextInt(677);
            int kind = 0;
            for (int w : TREE_WEIGHTS) {
                if (pick < w) {
                    break;
                }
                pick -= w;
                kind++;
            }
            if (kind == 3) {
                if (((net.minecraft.block.BlockBush) Blocks.SAPLING).canBlockStay(world, pos, Blocks.SAPLING.getDefaultState())
                        && !com.exiledradio.backportcore.compat.DynamicTrees.grow(world, pos, rand, "dynamictrees:spruce")) {
                    // Dynamic Trees grows its own spruce here when installed, as it does in every other biome.
                    new WorldGenTaiga2(false).generate(world, rand, pos);
                }
                continue;
            }
            if (!saplingSurvives(world, pos)) {
                continue;
            }
            if (kind == 4) {
                fallenTree(world, rand, pos);
            } else {
                tree(world, rand, pos, BlockPoplarLeaves.Color.values()[kind], true);
            }
        }
        // brown_mushroom_dappled_forest: one chunk in two, in_square, MOTION_BLOCKING, biome, 96 tries around it.
        if (rand.nextFloat() < 0.5F) {
            int x = base.getX() + rand.nextInt(16);
            int z = base.getZ() + rand.nextInt(16);
            BlockPos center = new BlockPos(x, motionBlocking(world, x, z), z);
            if (world.getBiome(center) == biome) {
                patch(world, rand, center, 96, Blocks.BROWN_MUSHROOM.getDefaultState());
            }
        }
        // patch_red_shrub: in_square, WORLD_SURFACE_WG, biome, 8 tries.
        if (PoplarContent.redShrub != null) {
            int x = base.getX() + rand.nextInt(16);
            int z = base.getZ() + rand.nextInt(16);
            BlockPos center = new BlockPos(x, worldSurface(world, x, z), z);
            if (world.getBiome(center) == biome) {
                patch(world, rand, center, 8, PoplarContent.redShrub.getDefaultState());
            }
        }
        // patch_grass_forest: 2 patches (in_square, WORLD_SURFACE_WG, biome) of 32 tries of short grass.
        for (int i = 0; i < 2; i++) {
            int x = base.getX() + rand.nextInt(16);
            int z = base.getZ() + rand.nextInt(16);
            BlockPos center = new BlockPos(x, worldSurface(world, x, z), z);
            if (world.getBiome(center) == biome) {
                patch(world, rand, center, 32,
                        Blocks.TALLGRASS.getDefaultState().withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.GRASS));
            }
        }
    }

    /** {@code count, offset(trapezoid 7, 3, 7), air}, then {@code simple_block}: placed where it can stand. */
    private static void patch(World world, Random rand, BlockPos center, int tries, IBlockState state) {
        Block block = state.getBlock();
        for (int i = 0; i < tries; i++) {
            int dx = trapezoid(rand, -7, 7);
            int dy = trapezoid(rand, -3, 3);
            int dz = trapezoid(rand, -7, 7);
            BlockPos at = center.add(dx, dy, dz);
            if (at.getY() < 1 || at.getY() >= world.getHeight() || !world.isAirBlock(at)) {
                continue;
            }
            if (block instanceof net.minecraft.block.BlockBush
                    ? ((net.minecraft.block.BlockBush) block).canBlockStay(world, at, state) : block.canPlaceBlockAt(world, at)) {
                world.setBlockState(at, state, 2);
            }
        }
    }

    /** {@code would_survive poplar_sapling}. */
    private static boolean saplingSurvives(World world, BlockPos pos) {
        BlockPoplarSapling sapling = PoplarContent.sapling;
        return sapling != null && sapling.canBlockStay(world, pos, sapling.getDefaultState());
    }

    // ===========================================================================================
    // Heightmaps and tags, as 1.12.2 blocks
    // ===========================================================================================

    /** {@code OCEAN_FLOOR}: one above the highest block that blocks motion (leaves too), fluids not counted. */
    static int oceanFloor(World world, int x, int z) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(x, worldSurface(world, x, z), z);
        while (at.getY() > 0) {
            at.setY(at.getY() - 1);
            IBlockState s = world.getBlockState(at);
            if (s.getMaterial().blocksMovement() && !s.getMaterial().isLiquid()) {
                return at.getY() + 1;
            }
        }
        return 0;
    }

    /** {@code MOTION_BLOCKING}: as {@link #oceanFloor}, fluids counted. */
    static int motionBlocking(World world, int x, int z) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(x, worldSurface(world, x, z), z);
        while (at.getY() > 0) {
            at.setY(at.getY() - 1);
            IBlockState s = world.getBlockState(at);
            if (s.getMaterial().blocksMovement() || s.getMaterial().isLiquid()) {
                return at.getY() + 1;
            }
        }
        return 0;
    }

    /** {@code MOTION_BLOCKING_NO_LEAVES}. */
    static int motionBlockingNoLeaves(World world, int x, int z) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(x, worldSurface(world, x, z), z);
        while (at.getY() > 0) {
            at.setY(at.getY() - 1);
            IBlockState s = world.getBlockState(at);
            if ((s.getMaterial().blocksMovement() || s.getMaterial().isLiquid()) && s.getMaterial() != Material.LEAVES) {
                return at.getY() + 1;
            }
        }
        return 0;
    }

    /** {@code WORLD_SURFACE_WG}: one above the highest block that is not air. */
    static int worldSurface(World world, int x, int z) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(x, world.getHeight(x, z), z);
        while (at.getY() < world.getHeight() && !world.isAirBlock(at)) {
            at.setY(at.getY() + 1);
        }
        while (at.getY() > 0 && world.isAirBlock(at.down())) {
            at.setY(at.getY() - 1);
        }
        return at.getY();
    }

    /** {@code TreeFeature.validTreePos}: air, or a block in {@code #replaceable_by_trees}. */
    static boolean validTreePos(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Block b = state.getBlock();
        if (b.isAir(state, world, pos) || b.isLeaves(state, world, pos)) {
            return true;
        }
        return b instanceof BlockFlower || b instanceof BlockTallGrass || b instanceof BlockDoublePlant
                || b instanceof BlockDeadBush || b instanceof BlockVine || b == PoplarContent.shelfMushroom
                || state.getMaterial() == Material.WATER
                || named(b, "glow_lichen", "hanging_roots", "seagrass", "tall_seagrass", "leaf_litter", "bush",
                "firefly_bush", "pitcher_plant", "short_dry_grass", "tall_dry_grass", "warped_roots", "crimson_roots",
                "nether_sprouts", "pale_moss_carpet", "open_eyeblossom", "closed_eyeblossom", "torchflower",
                "golden_dandelion", "cornflower", "lily_of_the_valley", "wither_rose");
    }

    /** {@code TrunkPlacer.isFree}: a valid tree position or a log. */
    private static boolean isFree(World world, BlockPos pos) {
        if (validTreePos(world, pos)) {
            return true;
        }
        IBlockState s = world.getBlockState(pos);
        Block b = s.getBlock();
        return b instanceof BlockLog || b.isWood(world, pos) || b.canSustainLeaves(s, world, pos);
    }

    static boolean named(@Nullable Block b, String... paths) {
        if (b == null || b.getRegistryName() == null) {
            return false;
        }
        String path = b.getRegistryName().getPath();
        for (String p : paths) {
            if (p.equals(path)) {
                return true;
            }
        }
        return false;
    }
}
