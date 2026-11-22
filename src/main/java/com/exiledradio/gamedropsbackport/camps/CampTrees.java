package com.exiledradio.gamedropsbackport.camps;

import com.exiledradio.backportcore.structure.JigsawPools;
import com.exiledradio.backportcore.structure.ModernPalette;
import net.minecraft.block.Block;
import net.minecraft.block.BlockOldLeaf;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.IGrowable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraft.world.gen.feature.WorldGenBigTree;
import net.minecraft.world.gen.feature.WorldGenBirchTree;
import net.minecraft.world.gen.feature.WorldGenMegaJungle;
import net.minecraft.world.gen.feature.WorldGenMegaPineTree;
import net.minecraft.world.gen.feature.WorldGenSavannaTree;
import net.minecraft.world.gen.feature.WorldGenTaiga1;
import net.minecraft.world.gen.feature.WorldGenTaiga2;
import net.minecraft.world.gen.feature.WorldGenTrees;
import net.minecraft.world.gen.feature.WorldGenerator;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

/**
 * The trees of the camps' {@code feature_pool_element}s ({@code abandoned_camp/trees/*}), 26.3's placed features by
 * the nearest 1.12.2 tree generator:
 *
 * <ul>
 *   <li>oak, fancy oak, birch, tall ("super") birch, spruce, pine, mega spruce and pine, jungle, mega jungle and
 *       acacia: 1.12.2's own generators, which make the same shapes. The {@code _bees} variants get 26.3's bee nest
 *       decorator at post-init ({@link CampBees}) where Buzzy Bees is installed.</li>
 *   <li>cherry, pale oak and the three poplars: their owners' trees, grown from the owner's sapling (found by today's
 *       name through Backport Core's palette). The owning stream may register a placer of its own for these feature
 *       names with {@link JigsawPools#feature}; registered later, it wins. Without the owner, no tree. The
 *       {@code cherry_bees_005} tree gets its nest as the vanilla {@code _bees} trees do, whoever places it.</li>
 *   <li>bamboo ({@code bamboo_no_podzol}): 26.3's {@code BambooFeature}, a stalk of 5-16, with the owner's bamboo.</li>
 * </ul>
 * Each placer, like the {@code _checked} filters, places nothing where the sapling could not stand.
 */
final class CampTrees {

    private CampTrees() {
    }

    static void register() {
        IBlockState jungleLog = Blocks.LOG.getDefaultState().withProperty(BlockOldLog.VARIANT, BlockPlanks.EnumType.JUNGLE);
        IBlockState jungleLeaf = Blocks.LEAVES.getDefaultState().withProperty(BlockOldLeaf.VARIANT, BlockPlanks.EnumType.JUNGLE)
                .withProperty(net.minecraft.block.BlockLeaves.CHECK_DECAY, false);
        Map<String, Supplier<WorldGenerator>> vanilla = new HashMap<String, Supplier<WorldGenerator>>();
        vanilla.put("minecraft:oak_checked", () -> new WorldGenTrees(false));
        vanilla.put("minecraft:fancy_oak_checked", () -> new WorldGenBigTree(false));
        vanilla.put("minecraft:fancy_oak_bees_002", () -> new WorldGenBigTree(false));
        vanilla.put("minecraft:birch_checked", () -> new WorldGenBirchTree(false, false));
        vanilla.put("minecraft:birch_bees_002", () -> new WorldGenBirchTree(false, false));
        vanilla.put("minecraft:super_birch_bees_0002", () -> new WorldGenBirchTree(false, true));
        vanilla.put("minecraft:spruce_checked", () -> new WorldGenTaiga2(false));
        vanilla.put("minecraft:spruce_on_snow", () -> new WorldGenTaiga2(false));
        vanilla.put("minecraft:pine_checked", WorldGenTaiga1::new);
        vanilla.put("minecraft:mega_spruce_checked", () -> new WorldGenMegaPineTree(false, true));
        vanilla.put("minecraft:mega_pine_checked", () -> new WorldGenMegaPineTree(false, false));
        vanilla.put("minecraft:acacia_checked", () -> new WorldGenSavannaTree(false));
        vanilla.put("minecraft:mega_jungle_tree_checked", () -> new WorldGenMegaJungle(false, 10, 20, jungleLog, jungleLeaf));
        for (Map.Entry<String, Supplier<WorldGenerator>> e : vanilla.entrySet()) {
            Supplier<WorldGenerator> gen = e.getValue();
            JigsawPools.feature(e.getKey(), (world, pos, random, feature) -> grow(world, pos, random, gen.get()));
        }
        // 26.3's jungle_tree: 4 + 0-8 tall with vines and cocoa, 1.12.2's small jungle tree.
        JigsawPools.feature("minecraft:jungle_tree", (world, pos, random, feature) ->
                grow(world, pos, random, new WorldGenTrees(false, 4 + random.nextInt(7), jungleLog, jungleLeaf, true)));

        sapling("minecraft:cherry_checked", "minecraft:cherry_sapling", false);
        sapling("minecraft:cherry_bees_005", "minecraft:cherry_sapling", false);
        sapling("minecraft:pale_oak_checked", "minecraft:pale_oak_sapling", true);
        sapling("minecraft:red_poplar", "minecraft:poplar_sapling", false);
        sapling("minecraft:orange_poplar", "minecraft:poplar_sapling", false);
        sapling("minecraft:yellow_poplar", "minecraft:poplar_sapling", false);
        JigsawPools.feature("minecraft:bamboo_in_structure", (world, pos, random, feature) -> bamboo(world, pos, random));
    }

    /** {@code would_survive(sapling)}: the ground under the spot takes a sapling. */
    static boolean soil(World world, BlockPos pos) {
        if (!world.isAirBlock(pos) && !world.getBlockState(pos).getBlock().isReplaceable(world, pos)) {
            return false;
        }
        Block b = world.getBlockState(pos.down()).getBlock();
        String path = b.getRegistryName() == null ? "" : b.getRegistryName().getPath();
        // 1.12.2's podzol and coarse dirt are dirt; pale moss and mud are their owners'.
        return b == Blocks.GRASS || b == Blocks.DIRT || b == Blocks.FARMLAND || path.endsWith("moss_block") || path.equals("mud");
    }

    static void grow(World world, BlockPos pos, Random random, WorldGenerator gen) {
        if (!soil(world, pos)) {
            return;
        }
        if (gen instanceof WorldGenAbstractTree) {
            ((WorldGenAbstractTree) gen).generate(world, random, pos);
        } else {
            gen.generate(world, random, pos);
        }
    }

    /** Places the owner's sapling (four for a two-by-two tree) and grows it, as bone meal would. */
    static void sapling(String feature, String saplingName, boolean twoByTwo) {
        JigsawPools.feature(feature, (world, pos, random, json) -> growSapling(world, pos, random, saplingName, twoByTwo));
    }

    private static void growSapling(World world, BlockPos pos, Random random, String saplingName, boolean twoByTwo) {
        if (!soil(world, pos)) {
            return;
        }
        IBlockState sapling = ModernPalette.resolve(saplingName).state;
        if (sapling == null || !(sapling.getBlock() instanceof IGrowable)) {
            return;
        }
        BlockPos[] spots = twoByTwo ? new BlockPos[] {pos, pos.east(), pos.south(), pos.south().east()} : new BlockPos[] {pos};
        for (BlockPos p : spots) {
            if (!world.isAirBlock(p) && !world.getBlockState(p).getBlock().isReplaceable(world, p)) {
                return;
            }
        }
        for (BlockPos p : spots) {
            world.setBlockState(p, sapling, 2);
        }
        Block block = sapling.getBlock();
        for (int i = 0; i < 4 && world.getBlockState(pos).getBlock() == block; i++) {
            if (block instanceof BlockSapling) {
                ((BlockSapling) block).generateTree(world, pos, world.getBlockState(pos), random);
            } else {
                ((IGrowable) block).grow(world, random, pos, world.getBlockState(pos));
            }
        }
        for (BlockPos p : spots) {
            if (world.getBlockState(p).getBlock() == block) {
                world.setBlockToAir(p);
            }
        }
    }

    /** 26.3's {@code BambooFeature} without podzol: 5-16 stalks, the top three leafed. */
    static void bamboo(World world, BlockPos pos, Random random) {
        if (!world.isAirBlock(pos)) {
            return;
        }
        IBlockState trunk = state("age", "1", "leaves", "none", "stage", "0");
        if (trunk == null || !soil(world, pos)) {
            return;
        }
        int height = random.nextInt(12) + 5;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(pos);
        for (int k = 0; k < height && world.isAirBlock(at); k++) {
            world.setBlockState(at, trunk, 2);
            at.move(net.minecraft.util.EnumFacing.UP);
        }
        if (at.getY() - pos.getY() >= 3) {
            set(world, at, state("age", "1", "leaves", "large", "stage", "1"));
            set(world, at.move(net.minecraft.util.EnumFacing.DOWN), state("age", "1", "leaves", "large", "stage", "0"));
            set(world, at.move(net.minecraft.util.EnumFacing.DOWN), state("age", "1", "leaves", "small", "stage", "0"));
        }
    }

    private static void set(World world, BlockPos pos, IBlockState state) {
        if (state != null) {
            world.setBlockState(pos, state, 2);
        }
    }

    private static IBlockState state(String... props) {
        Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; i + 1 < props.length; i += 2) {
            map.put(props[i], props[i + 1]);
        }
        return ModernPalette.resolve("minecraft:bamboo", map, Integer.MAX_VALUE).state;
    }
}
