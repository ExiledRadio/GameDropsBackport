package com.exiledradio.gamedropsbackport.sulfurcaves;

import com.exiledradio.backportcore.waterlog.Waterlogging;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * 26.3's sulfur cave features, placed as its {@code placed_feature} files say, inside the population area: the chunk
 * from {@code ax, az} and its three neighbors to +x and +z, 32 blocks square.
 */
final class Features {

    final World world;
    final int ax, az;
    final Random rand;
    final int minY, maxY;
    final Block sulfur, cinnabar, potent, spike;

    Features(World world, int ax, int az, Random rand) {
        this.world = world;
        this.ax = ax;
        this.az = az;
        this.rand = rand;
        this.minY = SulfurCaves.minY(world);
        this.maxY = SulfurCaves.maxYExclusive(world);
        this.sulfur = SulfurCavesContent.block("sulfur");
        this.cinnabar = SulfurCavesContent.block("cinnabar");
        this.potent = SulfurCavesContent.block("potent_sulfur");
        this.spike = SulfurCavesContent.block("sulfur_spike");
    }

    // ------------------------------------------------------------------------------------------------ access

    boolean in(BlockPos p) {
        return p.getX() >= ax && p.getX() < ax + 32 && p.getZ() >= az && p.getZ() < az + 32 && p.getY() >= minY
                && p.getY() < maxY;
    }

    /** The block, or bedrock outside the area (never replaced, never air). */
    IBlockState get(BlockPos p) {
        return in(p) ? world.getBlockState(p) : Blocks.BEDROCK.getDefaultState();
    }

    void set(BlockPos p, IBlockState state) {
        if (in(p)) {
            world.setBlockState(p, state, 2 | 16);
        }
    }

    boolean air(BlockPos p) {
        return in(p) && world.isAirBlock(p);
    }

    /** 26.x's {@code solid} predicate: the legacy "solid" of a block. */
    boolean solid(BlockPos p) {
        IBlockState s = get(p);
        return s.getMaterial().isSolid();
    }

    boolean water(IBlockState s) {
        return s.getMaterial() == Material.WATER;
    }

    boolean emptyOrWater(BlockPos p) {
        IBlockState s = get(p);
        return in(p) && (s.getBlock().isAir(s, world, p) || s.getBlock() == Blocks.WATER || s.getBlock() == Blocks.FLOWING_WATER);
    }

    boolean in(Block b) {
        return b == sulfur || b == cinnabar;
    }

    boolean inSulfurCaves(BlockPos p) {
        return in(p) && SulfurCaves.at(world, p);
    }

    /** {@code height_range} uniform from the bottom to y 256, in this world's heights. */
    int height() {
        int lo = minY, hi = Math.min(SulfurCaves.worldY(world, 256), maxY - 1);
        return lo + rand.nextInt(hi - lo + 1);
    }

    int between(int lo, int hi) {
        return lo + rand.nextInt(hi - lo + 1);
    }

    // ------------------------------------------------------------------------------------------------ rooted_sulfur_spring

    /**
     * Count 1-2, in square, height uniform; scan up through air at most 12 to a solid ceiling, one down; in the biome;
     * then {@code root_system} with the sulfur spring as its tree ({@link Springs}).
     */
    void rootedSprings(int x0, int z0) {
        int tries = between(1, 2);
        for (int t = 0; t < tries; t++) {
            BlockPos p = new BlockPos(x0 + rand.nextInt(16), height(), z0 + rand.nextInt(16));
            p = scan(p, EnumFacing.UP, 12, Scan.AIR, Scan.SOLID);
            if (p == null) {
                continue;
            }
            p = p.down();
            if (inSulfurCaves(p)) {
                new Springs(this).rootSystem(p);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ sulfur_pool

    /**
     * Count 256, in square, height uniform; on a solid block; scan up (anything) at most 32 to air, one down; that block
     * must be sulfur; in the biome. Then a water lake with a sulfur barrier, then wet potent sulfur on the first solid
     * block with water above, scanning down at most 4.
     */
    void pools(int x0, int z0) {
        for (int t = 0; t < 256; t++) {
            BlockPos p = new BlockPos(x0 + rand.nextInt(16), height(), z0 + rand.nextInt(16));
            if (!solid(p)) {
                continue;
            }
            p = scan(p, EnumFacing.UP, 32, Scan.ANY, Scan.AIR);
            if (p == null) {
                continue;
            }
            p = p.down();
            if (get(p).getBlock() != sulfur || !inSulfurCaves(p)) {
                continue;
            }
            if (lake(p)) {
                BlockPos floor = scan(p, EnumFacing.DOWN, 4, Scan.ANY, Scan.SOLID_UNDER_WATER);
                if (floor != null) {
                    set(floor, with(potent.getDefaultState(), "potent_sulfur_state", "wet"));
                }
            }
        }
    }

    /** 26.3's {@code LakeFeature} with water, a sulfur barrier, never touching sulfur spikes. */
    boolean lake(BlockPos origin) {
        if (origin.getY() <= minY + 4) {
            return false;
        }
        origin = origin.add(-8, -4, -8);
        boolean[] grid = new boolean[2048];
        int spots = rand.nextInt(4) + 4;
        for (int i = 0; i < spots; i++) {
            double xr = rand.nextDouble() * 6.0 + 3.0;
            double yr = rand.nextDouble() * 4.0 + 2.0;
            double zr = rand.nextDouble() * 6.0 + 3.0;
            double xp = rand.nextDouble() * (16.0 - xr - 2.0) + 1.0 + xr / 2.0;
            double yp = rand.nextDouble() * (8.0 - yr - 4.0) + 2.0 + yr / 2.0;
            double zp = rand.nextDouble() * (16.0 - zr - 2.0) + 1.0 + zr / 2.0;
            for (int xx = 1; xx < 15; xx++) {
                for (int zz = 1; zz < 15; zz++) {
                    for (int yy = 1; yy < 7; yy++) {
                        double xd = (xx - xp) / (xr / 2.0);
                        double yd = (yy - yp) / (yr / 2.0);
                        double zd = (zz - zp) / (zr / 2.0);
                        if (xd * xd + yd * yd + zd * zd < 1.0) {
                            grid[(xx * 16 + zz) * 8 + yy] = true;
                        }
                    }
                }
            }
        }
        IBlockState fluid = Blocks.WATER.getDefaultState();
        for (int xx = 0; xx < 16; xx++) {
            for (int zz = 0; zz < 16; zz++) {
                for (int yy = 0; yy < 8; yy++) {
                    if (edge(grid, xx, zz, yy)) {
                        BlockPos q = origin.add(xx, yy, zz);
                        IBlockState s = get(q);
                        if (yy >= 4 && s.getMaterial().isLiquid()) {
                            return false;
                        }
                        if (yy < 4 && !s.getMaterial().isSolid() && s != fluid) {
                            return false;
                        }
                        if (s.getBlock() == spike) {
                            return false;
                        }
                    }
                }
            }
        }
        for (int xx = 0; xx < 16; xx++) {
            for (int zz = 0; zz < 16; zz++) {
                for (int yy = 0; yy < 8; yy++) {
                    if (grid[(xx * 16 + zz) * 8 + yy]) {
                        BlockPos q = origin.add(xx, yy, zz);
                        if (!cannotReplace(get(q))) {
                            set(q, yy >= 4 ? Blocks.AIR.getDefaultState() : fluid);
                        }
                    }
                }
            }
        }
        IBlockState barrier = sulfur.getDefaultState();
        for (int xx = 0; xx < 16; xx++) {
            for (int zz = 0; zz < 16; zz++) {
                for (int yy = 0; yy < 8; yy++) {
                    if (edge(grid, xx, zz, yy) && (yy < 4 || rand.nextInt(2) != 0)) {
                        BlockPos q = origin.add(xx, yy, zz);
                        IBlockState s = get(q);
                        if (s.getMaterial().isSolid() && !cannotReplace(s) && !s.getBlock().isLeaves(s, world, q)
                                && !s.getBlock().isWood(world, q)) {
                            set(q, barrier);
                        }
                    }
                }
            }
        }
        return true;
    }

    private static boolean edge(boolean[] g, int xx, int zz, int yy) {
        return !g[(xx * 16 + zz) * 8 + yy]
                && (xx < 15 && g[((xx + 1) * 16 + zz) * 8 + yy] || xx > 0 && g[((xx - 1) * 16 + zz) * 8 + yy]
                || zz < 15 && g[(xx * 16 + zz + 1) * 8 + yy] || zz > 0 && g[(xx * 16 + (zz - 1)) * 8 + yy]
                || yy < 7 && g[(xx * 16 + zz) * 8 + yy + 1] || yy > 0 && g[(xx * 16 + zz) * 8 + (yy - 1)]);
    }

    /** {@code #features_cannot_replace}: bedrock, spawners, chests, end portal frames, reinforced deepslate, ... */
    static boolean cannotReplace(IBlockState s) {
        Block b = s.getBlock();
        if (b == Blocks.BEDROCK || b == Blocks.MOB_SPAWNER || b == Blocks.CHEST || b == Blocks.END_PORTAL_FRAME) {
            return true;
        }
        String path = b.getRegistryName() == null ? "" : b.getRegistryName().getPath();
        return path.equals("reinforced_deepslate") || path.equals("trial_spawner") || path.equals("vault");
    }

    // ------------------------------------------------------------------------------------------------ spikes

    /** Count 48-96, in square, height uniform, in the biome: {@code speleothem_cluster}. */
    void spikeClusters(int x0, int z0) {
        int tries = between(48, 96);
        for (int t = 0; t < tries; t++) {
            BlockPos p = new BlockPos(x0 + rand.nextInt(16), height(), z0 + rand.nextInt(16));
            if (inSulfurCaves(p)) {
                new Speleothems(this).cluster(p);
            }
        }
    }

    /**
     * Count 192-256, in square, height uniform, then count 1-5 each, offset by clamped normals (3, 0.6, 3), in the
     * biome; then one of two {@code speleothem}s: scanning down to a floor (one up) or up to a ceiling (one down) through
     * air or water, at most 12.
     */
    void spikes(int x0, int z0) {
        int tries = between(192, 256);
        for (int t = 0; t < tries; t++) {
            BlockPos base = new BlockPos(x0 + rand.nextInt(16), height(), z0 + rand.nextInt(16));
            int n = between(1, 5);
            for (int i = 0; i < n; i++) {
                BlockPos p = base.add(clampedNormal(3.0F, 10), clampedNormal(0.6F, 2), clampedNormal(3.0F, 10));
                if (!inSulfurCaves(p)) {
                    continue;
                }
                boolean down = rand.nextInt(2) == 0;
                BlockPos q = scan(p, down ? EnumFacing.DOWN : EnumFacing.UP, 12, Scan.EMPTY_OR_WATER, Scan.SOLID);
                if (q != null) {
                    new Speleothems(this).single(down ? q.up() : q.down());
                }
            }
        }
    }

    int clampedNormal(float deviation, int bound) {
        return (int) MathHelper.clamp(0.0F + (float) rand.nextGaussian() * deviation, -bound, bound);
    }

    // ------------------------------------------------------------------------------------------------ helpers

    enum Scan {
        ANY, AIR, SOLID, EMPTY_OR_WATER, SOLID_UNDER_WATER
    }

    boolean test(Scan s, BlockPos p) {
        switch (s) {
            case ANY:
                return true;
            case AIR:
                return air(p);
            case SOLID:
                return solid(p);
            case EMPTY_OR_WATER:
                return emptyOrWater(p);
            case SOLID_UNDER_WATER:
                return solid(p) && water(get(p.up()));
            default:
                return false;
        }
    }

    /** 26.x's {@code EnvironmentScanPlacement}. */
    @Nullable
    BlockPos scan(BlockPos start, EnumFacing dir, int maxSteps, Scan allowed, Scan target) {
        if (!in(start) || !test(allowed, start)) {
            return null;
        }
        BlockPos p = start;
        for (int i = 0; i < maxSteps; i++) {
            if (test(target, p)) {
                return p;
            }
            p = p.offset(dir);
            if (p.getY() < minY || p.getY() >= maxY || !in(p)) {
                return null;
            }
            if (!test(allowed, p)) {
                break;
            }
        }
        return test(target, p) ? p : null;
    }

    /** A state with a property set by its names, or the state as it was. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static IBlockState with(IBlockState state, String property, String value) {
        for (IProperty p : state.getPropertyKeys()) {
            if (p.getName().equals(property)) {
                com.google.common.base.Optional v = p.parseValue(value);
                if (v.isPresent()) {
                    return state.withProperty(p, (Comparable) v.get());
                }
            }
        }
        return state;
    }

    boolean sturdyBelow(BlockPos p) {
        IBlockState s = get(p);
        return in(p) && s.getBlockFaceShape(world, p, EnumFacing.DOWN) == BlockFaceShape.SOLID;
    }

    /** Places a spike state, keeping water that is there (Core's waterlogging). */
    void placeSpike(BlockPos p, IBlockState state) {
        if (!in(p)) {
            return;
        }
        if (Waterlogging.hasWater(world, p)) {
            Waterlogging.placeWithWater(world, p, state, 2 | 16);
        } else {
            Waterlogging.placeDry(world, p, state, 2 | 16);
        }
    }
}
