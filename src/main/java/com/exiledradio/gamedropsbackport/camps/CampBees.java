package com.exiledradio.gamedropsbackport.camps;

import com.exiledradio.backportcore.structure.JigsawPools;
import com.exiledradio.backportcore.structure.ModernPalette;
import com.google.gson.JsonElement;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 26.3's {@code BeehiveDecorator} on the camps' {@code _bees} trees ({@code cherry_bees_005},
 * {@code fancy_oak_bees_002}, {@code birch_bees_002}, {@code super_birch_bees_0002}), with Buzzy Bees' bee nest.
 *
 * <p>1.12.2's tree generators do not report their blocks, so the logs and leaves the tree placed are found by
 * comparing its box before and after it grows. Then, as 26.3: with the feature's probability (the name's suffix,
 * {@code _005} is 0.05), the nest goes on the logs' layer {@code max(lowest leaf - 1, lowest log + 1)} (without
 * leaves, 1-3 above the lowest log, never above the top one), on a random free east, south or west side of one of
 * them where the nest and the block south of it are air, facing south, with 2-3 bees 0-598 ticks into their rest
 * ({@code Occupant.create}: minimum 600). No bee nest block installed (Buzzy Bees): no nest, the tree unchanged.
 */
public final class CampBees {

    /** 26.3's {@code SPAWN_DIRECTIONS}: the horizontal directions but north, in {@code Direction.Plane} order. */
    private static final EnumFacing[] SIDES = {EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST};
    /** The box a camp tree can fill, around its sapling spot: a fancy oak's branches, a tall birch's top. */
    private static final int REACH = 8;
    private static final int HEIGHT = 32;

    /** The camps' {@code _bees} trees. */
    static final String[] FEATURES = {"minecraft:cherry_bees_005", "minecraft:fancy_oak_bees_002", "minecraft:birch_bees_002",
            "minecraft:super_birch_bees_0002"};

    private CampBees() {
    }

    /** A tree placer with 26.3's {@code BeehiveDecorator} after it. */
    static final class Placer implements JigsawPools.FeaturePlacer {
        final JigsawPools.FeaturePlacer tree;
        final float probability;

        Placer(JigsawPools.FeaturePlacer tree, float probability) {
            this.tree = tree;
            this.probability = probability;
        }

        @Override
        public void place(World world, BlockPos pos, Random random, JsonElement feature) {
            grow(world, pos, random, this.probability, () -> this.tree.place(world, pos, random, feature));
        }
    }

    /**
     * Post-init: the nest goes on whichever tree places a {@code _bees} feature, ours or the one a sibling registered
     * later (Trails &amp; Tales' {@code cherry_bees_005}, its cherry without a nest).
     */
    static void postInit() {
        for (String id : FEATURES) {
            JigsawPools.FeaturePlacer current = JigsawPools.placer(id);
            if (current != null && !(current instanceof Placer)) {
                JigsawPools.feature(id, new Placer(current, probability(id)));
            }
        }
    }

    /** @return the {@code _bees_<digits>} feature's probability (0.05 for {@code _005}), or 0 for any other tree */
    public static float probability(String feature) {
        int at = feature.lastIndexOf("_bees_");
        if (at < 0) {
            return 0.0F;
        }
        String digits = feature.substring(at + "_bees_".length());
        if (digits.length() < 2 || !digits.matches("[0-9]+")) {
            return 0.0F;
        }
        return Float.parseFloat("0." + digits.substring(1));
    }

    /** Grows a tree with {@code grow} and then decorates it as a {@code BeehiveDecorator(probability)}. */
    public static void grow(World world, BlockPos pos, Random random, float probability, Runnable grow) {
        if (probability <= 0.0F || nest() == null) {
            grow.run();
            return;
        }
        Map<BlockPos, IBlockState> before = snapshot(world, pos);
        grow.run();
        List<BlockPos> logs = new ArrayList<BlockPos>();
        List<BlockPos> leaves = new ArrayList<BlockPos>();
        for (Map.Entry<BlockPos, IBlockState> e : snapshot(world, pos).entrySet()) {
            BlockPos p = e.getKey();
            IBlockState now = e.getValue();
            if (now == before.get(p)) {
                continue;
            }
            if (now.getBlock().isWood(world, p)) {
                logs.add(p);
            } else if (now.getBlock().isLeaves(now, world, p)) {
                leaves.add(p);
            }
        }
        decorate(world, random, probability, logs, leaves);
    }

    /** 26.3's {@code BeehiveDecorator.place}, the tree's logs and leaves in any order. */
    public static boolean decorate(World world, Random random, float probability, List<BlockPos> logs, List<BlockPos> leaves) {
        IBlockState nest = nest();
        if (nest == null || logs.isEmpty()) {
            return false;
        }
        // TreeFeature hands the decorators both lists sorted by height.
        Comparator<BlockPos> byY = Comparator.comparingInt(BlockPos::getY);
        logs = new ArrayList<BlockPos>(logs);
        leaves = new ArrayList<BlockPos>(leaves);
        logs.sort(byY);
        leaves.sort(byY);
        if (random.nextFloat() >= probability) {
            return false;
        }
        int hiveY = !leaves.isEmpty()
                ? Math.max(leaves.get(0).getY() - 1, logs.get(0).getY() + 1)
                : Math.min(logs.get(0).getY() + 1 + random.nextInt(3), logs.get(logs.size() - 1).getY());
        List<BlockPos> spots = new ArrayList<BlockPos>();
        for (BlockPos log : logs) {
            if (log.getY() == hiveY) {
                for (EnumFacing side : SIDES) {
                    spots.add(log.offset(side));
                }
            }
        }
        if (spots.isEmpty()) {
            return false;
        }
        Collections.shuffle(spots, random);
        for (BlockPos at : spots) {
            if (world.isAirBlock(at) && world.isAirBlock(at.south())) {
                world.setBlockState(at, nest, 2);
                fill(world, random, at);
                return true;
            }
        }
        return false;
    }

    /** The bee nest facing south, or null when no installed mod has one. */
    @Nullable
    public static IBlockState nest() {
        Map<String, String> props = new HashMap<String, String>();
        props.put("facing", "south");
        props.put("honey_level", "0");
        IBlockState state = ModernPalette.resolve("minecraft:bee_nest", props, Integer.MAX_VALUE).state;
        ResourceLocation id = state == null ? null : state.getBlock().getRegistryName();
        return id != null && "bee_nest".equals(id.getPath()) ? state : null;
    }

    /** 2-3 bees in the nest's saved {@code Bees}, as 26.3's {@code storeBee(Occupant.create(random.nextInt(599)))}. */
    static void fill(World world, Random random, BlockPos at) {
        TileEntity te = world.getTileEntity(at);
        ResourceLocation nestId = world.getBlockState(at).getBlock().getRegistryName();
        if (te == null || nestId == null) {
            return;
        }
        NBTTagCompound tag = te.writeToNBT(new NBTTagCompound());
        NBTTagList bees = tag.getTagList("Bees", 10);
        int n = 2 + random.nextInt(2);
        for (int i = 0; i < n; i++) {
            int ticks = random.nextInt(599);
            Entity bee = EntityList.createEntityByIDFromName(new ResourceLocation(nestId.getNamespace(), "bee"), world);
            NBTTagCompound data = new NBTTagCompound();
            if (bee == null || !bee.writeToNBTOptional(data)) {
                return;
            }
            NBTTagCompound entry = new NBTTagCompound();
            entry.setTag("EntityData", data);
            entry.setInteger("TicksInHive", ticks);
            entry.setInteger("MinOccupationTicks", 600);
            bees.appendTag(entry);
        }
        tag.setTag("Bees", bees);
        te.readFromNBT(tag);
        te.markDirty();
    }

    private static Map<BlockPos, IBlockState> snapshot(World world, BlockPos pos) {
        Map<BlockPos, IBlockState> out = new HashMap<BlockPos, IBlockState>();
        for (BlockPos p : BlockPos.getAllInBox(pos.add(-REACH, 0, -REACH), pos.add(REACH, HEIGHT, REACH))) {
            if (world.isBlockLoaded(p)) {
                out.put(p.toImmutable(), world.getBlockState(p));
            }
        }
        return out;
    }
}
