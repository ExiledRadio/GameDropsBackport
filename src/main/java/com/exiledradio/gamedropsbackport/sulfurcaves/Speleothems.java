package com.exiledradio.gamedropsbackport.sulfurcaves;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/**
 * 26.3's {@code speleothem} and {@code speleothem_cluster} features (1.17's pointed dripstone and dripstone cluster,
 * generalized) with sulfur as the base block, sulfur spikes as the pointed block and {@code
 * #sulfur_spike_replaceable_blocks} (sulfur, cinnabar) as what they may replace.
 */
final class Speleothems {

    private final Features f;

    Speleothems(Features f) {
        this.f = f;
    }

    // --------------------------------------------------------------------------- speleothem (sulfur_spike)

    /** Defaults: taller 0.2, directional spread 0.7, spread radius 2 and 3 both 0.5. */
    void single(BlockPos origin) {
        boolean above = isBase(f.get(origin.up())), below = isBase(f.get(origin.down()));
        EnumFacing tip;
        if (above && below) {
            tip = f.rand.nextBoolean() ? EnumFacing.DOWN : EnumFacing.UP;
        } else if (above) {
            tip = EnumFacing.DOWN;
        } else if (below) {
            tip = EnumFacing.UP;
        } else {
            return;
        }
        BlockPos root = origin.offset(tip.getOpposite());
        patch(root);
        int height = f.rand.nextFloat() < 0.2F && f.emptyOrWater(origin.offset(tip)) ? 2 : 1;
        grow(origin, tip, height, false);
    }

    private void patch(BlockPos pos) {
        placeBase(pos);
        for (EnumFacing d : EnumFacing.Plane.HORIZONTAL) {
            if (f.rand.nextFloat() > 0.7F) {
                continue;
            }
            BlockPos p1 = pos.offset(d);
            placeBase(p1);
            if (f.rand.nextFloat() > 0.5F) {
                continue;
            }
            BlockPos p2 = p1.offset(EnumFacing.random(f.rand));
            placeBase(p2);
            if (f.rand.nextFloat() > 0.5F) {
                continue;
            }
            placeBase(p2.offset(EnumFacing.random(f.rand)));
        }
    }

    // --------------------------------------------------------------------------- speleothem_cluster

    /**
     * 26.3's sulfur_spike_cluster: search 12, height 1-4, wetness 0, density 0.3-0.7, radius 2-8 each way, layer
     * thickness 2-4, height deviation 3, stalagmite and stalactite within 1 of each other, 0.1 at the edge rising over
     * 3 blocks, height bias over 8.
     */
    void cluster(BlockPos origin) {
        if (!f.emptyOrWater(origin)) {
            return;
        }
        int height = f.between(1, 4);
        float wetness = 0.0F;
        float density = 0.3F + f.rand.nextFloat() * 0.4F;
        int xr = f.between(2, 8), zr = f.between(2, 8);
        for (int dx = -xr; dx <= xr; dx++) {
            for (int dz = -zr; dz <= zr; dz++) {
                double chance = chance(xr, zr, dx, dz);
                column(origin.add(dx, 0, dz), dx, dz, wetness, chance, height, density);
            }
        }
    }

    private void column(BlockPos pos, int dx, int dz, float wetness, double chance, int clusterHeight, float density) {
        if (!f.emptyOrWater(pos)) {
            return;
        }
        Integer ceiling = edge(pos, EnumFacing.UP), floor = edge(pos, EnumFacing.DOWN);
        if (ceiling == null && floor == null) {
            return;
        }
        // A pool where the floor allows one, with the chance of the wetness: 0 for sulfur, so never (the draw stays).
        f.rand.nextFloat();
        boolean wantStalactite = f.rand.nextDouble() < chance;
        int stalactite = 0;
        if (ceiling != null && wantStalactite && !lava(pos, ceiling)) {
            int thickness = f.between(2, 4);
            replaceWithBase(at(pos, ceiling), thickness, EnumFacing.UP);
            int max = floor != null ? Math.min(clusterHeight, ceiling - floor) : clusterHeight;
            stalactite = height(dx, dz, density, max);
        }
        boolean wantStalagmite = f.rand.nextDouble() < chance;
        int stalagmite = 0;
        if (floor != null && wantStalagmite && !lava(pos, floor)) {
            int thickness = f.between(2, 4);
            replaceWithBase(at(pos, floor), thickness, EnumFacing.DOWN);
            stalagmite = ceiling != null ? Math.max(0, stalactite + f.between(-1, 1)) : height(dx, dz, density, clusterHeight);
        }
        int actualStalactite = stalactite, actualStalagmite = stalagmite;
        if (ceiling != null && floor != null && ceiling - stalactite <= floor + stalagmite) {
            int lowest = Math.max(ceiling - stalactite, floor + 1);
            int highest = Math.min(floor + stalagmite, ceiling - 1);
            int bottom = f.between(lowest, highest + 1);
            actualStalactite = ceiling - bottom;
            actualStalagmite = bottom - 1 - floor;
        }
        boolean merge = f.rand.nextBoolean() && actualStalactite > 0 && actualStalagmite > 0 && ceiling != null
                && floor != null && actualStalactite + actualStalagmite == ceiling - floor - 1;
        if (ceiling != null) {
            grow(at(pos, ceiling - 1), EnumFacing.DOWN, actualStalactite, merge);
        }
        if (floor != null) {
            grow(at(pos, floor + 1), EnumFacing.UP, actualStalagmite, merge);
        }
    }

    /** 26.x's {@code Column.scan} one way: through air or water, at most 11 steps, to a block that is neither. */
    private Integer edge(BlockPos pos, EnumFacing dir) {
        BlockPos p = pos;
        for (int i = 1; i < 12 && f.emptyOrWater(p); i++) {
            p = p.offset(dir);
        }
        return f.in(p) && !f.emptyOrWater(p) ? Integer.valueOf(p.getY()) : null;
    }

    private static BlockPos at(BlockPos p, int y) {
        return new BlockPos(p.getX(), y, p.getZ());
    }

    private boolean lava(BlockPos pos, int y) {
        IBlockState s = f.get(at(pos, y));
        return s.getBlock() == Blocks.LAVA || s.getBlock() == Blocks.FLOWING_LAVA;
    }

    private int height(int dx, int dz, float density, int max) {
        if (f.rand.nextFloat() > density) {
            return 0;
        }
        int dist = Math.abs(dx) + Math.abs(dz);
        float mean = (float) clampedMap(dist, 0.0, 8.0, max / 2.0, 0.0);
        float v = MathHelper.clamp(mean + (float) f.rand.nextGaussian() * 3.0F, 0.0F, (float) max);
        return (int) v;
    }

    private static double chance(int xr, int zr, int dx, int dz) {
        int edge = Math.min(xr - Math.abs(dx), zr - Math.abs(dz));
        return (float) clampedMap(edge, 0.0, 3.0, 0.1F, 1.0F);
    }

    private static double clampedMap(double v, double a, double b, double c, double d) {
        double t = MathHelper.clamp((v - a) / (b - a), 0.0, 1.0);
        return c + t * (d - c);
    }

    private void replaceWithBase(BlockPos first, int count, EnumFacing dir) {
        BlockPos p = first;
        for (int i = 0; i < count; i++) {
            if (!placeBase(p)) {
                return;
            }
            p = p.offset(dir);
        }
    }

    // --------------------------------------------------------------------------- SpeleothemUtils

    private boolean isBase(IBlockState s) {
        return f.in(s.getBlock());
    }

    private boolean placeBase(BlockPos p) {
        if (f.in(p) && f.in(f.get(p).getBlock())) {
            f.set(p, f.sulfur.getDefaultState());
            return true;
        }
        return false;
    }

    /** {@code growSpeleothem}: base, middles, frustum, tip from the root along the tip direction, if rooted in base. */
    private void grow(BlockPos start, EnumFacing tip, int length, boolean merged) {
        if (length <= 0 || !isBase(f.get(start.offset(tip.getOpposite())))) {
            return;
        }
        BlockPos p = start;
        String dir = tip == EnumFacing.UP ? "up" : "down";
        String[] parts = new String[length];
        int n = 0;
        if (length >= 3) {
            parts[n++] = "base";
            for (int i = 0; i < length - 3; i++) {
                parts[n++] = "middle";
            }
        }
        if (length >= 2) {
            parts[n++] = "frustum";
        }
        parts[n++] = merged ? "tip_merge" : "tip";
        for (int i = 0; i < n; i++) {
            IBlockState s = Features.with(Features.with(f.spike.getDefaultState(), "vertical_direction", dir), "thickness", parts[i]);
            f.placeSpike(p, s);
            p = p.offset(tip);
        }
    }
}
