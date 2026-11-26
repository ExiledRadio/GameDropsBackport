package com.exiledradio.gamedropsbackport.icecaves;

import com.exiledradio.backportcore.speleothem.BlockSpeleothem;
import com.exiledradio.backportcore.speleothem.Thickness;
import com.exiledradio.backportcore.worldgen.FlatWorlds;
import com.exiledradio.backportcore.worldgen.UndergroundBiomes;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStone;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The ice caves in newly generated chunks, over a chunk's population window, in 26.4's order (Pre-Release 1,
 * {@code worldgen/biome/ice_caves.json}):
 * <ol>
 *   <li><b>Bands</b> ({@code overworld/ice_cave_bands}, before deepslate): by the {@code ice_cave_gradient} noise,
 *       calcite from -0.6 to -0.5, packed ice to 0.2, calcite to 0.3, packed ice to 0.5, calcite to 0.7, stone or
 *       deepslate outside. Calcite is Caves &amp; Cliffs'; without it, packed ice. They come after 1.12's ores and take
 *       them, as 26.4's bands come before its ores.</li>
 *   <li>Local modifications: {@code large_icicle} (48-64 tries), packed ice spikes as large dripstone.</li>
 *   <li>Underground ores: the 21 {@code ice_cave_ore_*} placements, each ore in a size-20 stone (or deepslate) blob.</li>
 *   <li>Underground decoration: {@code snow_layers}, {@code ice_crystals}, {@code icicle_cluster}, {@code ice_crevasse}.</li>
 * </ol>
 * Every feature keeps its 26.4 counts, sizes and heights (proportional in a 0-255 world, as the sulfur caves). Its
 * placements check the biome at their origin, as 26.4's {@code biome} modifier. Writes stay inside the four chunks
 * 1.12.2 guarantees around the window; a feature reaching past them is cut there (26.4's region is 3x3 chunks).
 */
public final class IceCavesGen implements IWorldGenerator {

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator generator, IChunkProvider provider) {
        if (!IceCaves.generates(world) || IceCavesContent.brittleIce == null) {
            return;
        }
        if (FlatWorlds.isFlat(world) && !FlatWorlds.decoration(world)) {
            return;
        }
        populate(world, chunkX, chunkZ);
    }

    /** @return how many blocks the bands changed */
    public static int populate(World world, int chunkX, int chunkZ) {
        int x0 = chunkX * 16 + 8, z0 = chunkZ * 16 + 8;
        boolean any = false;
        for (int x = x0; x < x0 + 16 && !any; x += 4) {
            for (int z = z0; z < z0 + 16 && !any; z += 4) {
                any = IceCaves.mask(world, x, z) != null || IceCaves.mask(world, x + 3, z + 3) != null;
            }
        }
        if (!any) {
            return 0;
        }
        int changed = bands(world, x0, z0);
        if (!FlatWorlds.isFlat(world)) {
            registerBiome(world, x0, z0);
        }
        Random rand = new Random(IceCaves.mix(world.getSeed() ^ 0x26_04_1CEL, chunkX, chunkZ));
        Gen g = new Gen(world, chunkX * 16, chunkZ * 16, rand);
        g.largeIcicles(x0, z0);
        g.ores(x0, z0);
        g.snowLayers(x0, z0);
        g.crystals(x0, z0);
        g.icicleClusters(x0, z0);
        g.crevasses(x0, z0);
        return changed;
    }

    // ------------------------------------------------------------------------------------------------- bands

    static int bands(World world, int x0, int z0) {
        long seed = world.getSeed();
        int min = IceCaves.minY(world);
        IBlockState packed = Blocks.PACKED_ICE.getDefaultState(), calcite = calcite();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int changed = 0;
        for (int x = x0; x < x0 + 16; x++) {
            for (int z = z0; z < z0 + 16; z++) {
                boolean[] mask = IceCaves.mask(world, x, z);
                if (mask == null) {
                    continue;
                }
                for (int i = 0; i < mask.length; i++) {
                    if (!mask[i]) {
                        continue;
                    }
                    int y = min + i;
                    IBlockState band = band(IceNoise.gradient(seed, x, y, z), packed, calcite);
                    if (band == null || !replaceable(world.getBlockState(m.setPos(x, y, z)))) {
                        continue;
                    }
                    world.setBlockState(m, band, 2 | 16);
                    changed++;
                }
            }
        }
        return changed;
    }

    /** 26.4's {@code ice_cave_bands}: null keeps the stone. */
    @Nullable
    static IBlockState band(double v, IBlockState packed, IBlockState calcite) {
        if (v >= -0.6000000238418579D && v <= -0.5D) {
            return calcite;
        }
        if (v >= -0.5D && v <= 0.20000000298023224D) {
            return packed;
        }
        if (v >= 0.20000000298023224D && v <= 0.30000001192092896D) {
            return calcite;
        }
        if (v >= 0.30000001192092896D && v <= 0.5D) {
            return packed;
        }
        return v >= 0.5D && v <= 0.699999988079071D ? calcite : null;
    }

    /** What 26.4's material rule and its stone features would have made: stone, deepslate, tuff, dirt, gravel, ores. */
    static boolean replaceable(IBlockState state) {
        Block b = state.getBlock();
        if (b == Blocks.STONE || b == Blocks.DIRT || b == Blocks.GRAVEL || b instanceof net.minecraft.block.BlockOre
                || b instanceof net.minecraft.block.BlockRedstoneOre) {
            return true;
        }
        String path = b.getRegistryName() == null ? "" : b.getRegistryName().getPath();
        return path.equals("deepslate") || path.equals("tuff") || path.endsWith("_ore") && !path.contains("nether");
    }

    static void registerBiome(World world, int x0, int z0) {
        if (IceCavesContent.biome == null) {
            return;
        }
        int min = IceCaves.minY(world);
        for (int x = x0; x < x0 + 16; x++) {
            for (int z = z0; z < z0 + 16; z++) {
                boolean[] mask = IceCaves.mask(world, x, z);
                if (mask != null) {
                    UndergroundBiomes.setColumn(world, x, z, min, mask, IceCavesContent.biome);
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------- blocks

    @Nullable
    static Block named(String id) {
        ResourceLocation rl = new ResourceLocation(id);
        Block b = ForgeRegistries.BLOCKS.containsKey(rl) ? ForgeRegistries.BLOCKS.getValue(rl) : null;
        return b == Blocks.AIR ? null : b;
    }

    static IBlockState calcite() {
        Block b = named("cavesandcliffsbackport:calcite");
        return b != null ? b.getDefaultState() : Blocks.PACKED_ICE.getDefaultState();
    }

    @Nullable
    static IBlockState deepslate() {
        Block b = named("cavesandcliffsbackport:deepslate");
        return b == null ? null : b.getDefaultState();
    }

    static boolean is(IBlockState s, String path) {
        return s.getBlock().getRegistryName() != null && path.equals(s.getBlock().getRegistryName().getPath());
    }

    /** {@code #stone_ore_replaceables}: stone, granite, diorite, andesite (1.12.2's stone and its variants). */
    static boolean stoneLike(IBlockState s) {
        if (s.getBlock() != Blocks.STONE) {
            return false;
        }
        BlockStone.EnumType t = s.getValue(BlockStone.VARIANT);
        return t == BlockStone.EnumType.STONE || t == BlockStone.EnumType.GRANITE || t == BlockStone.EnumType.DIORITE
                || t == BlockStone.EnumType.ANDESITE;
    }

    /** {@code #base_stone_overworld}. */
    static boolean baseStone(IBlockState s) {
        return stoneLike(s) || is(s, "deepslate") || is(s, "tuff");
    }

    static boolean iceOrCalcite(IBlockState s) {
        return s.getBlock() == Blocks.PACKED_ICE || is(s, "calcite");
    }

    // ------------------------------------------------------------------------------------------------- the features

    /** One population's features, writing only inside the four guaranteed chunks. */
    static final class Gen {
        final World world;
        final int minX, minZ;
        final Random rand;
        final int bottom, top;

        Gen(World world, int minX, int minZ, Random rand) {
            this.world = world;
            this.minX = minX;
            this.minZ = minZ;
            this.rand = rand;
            this.bottom = IceCaves.minY(world);
            this.top = IceCaves.maxYExclusive(world) - 1;
        }

        boolean inside(BlockPos p) {
            return p.getX() >= this.minX && p.getX() < this.minX + 32 && p.getZ() >= this.minZ && p.getZ() < this.minZ + 32
                    && p.getY() >= this.bottom && p.getY() <= this.top;
        }

        /** Outside the area: stone, which every feature treats as solid and none replaces. */
        IBlockState get(BlockPos p) {
            return inside(p) ? this.world.getBlockState(p) : Blocks.BEDROCK.getDefaultState();
        }

        void set(BlockPos p, IBlockState s) {
            if (inside(p)) {
                this.world.setBlockState(p, s, 2 | 16);
            }
        }

        boolean air(BlockPos p) {
            return get(p).getMaterial() == Material.AIR;
        }

        boolean water(IBlockState s) {
            return s.getMaterial() == Material.WATER;
        }

        boolean emptyOrWater(IBlockState s) {
            return s.getMaterial() == Material.AIR || water(s);
        }

        boolean emptyOrWaterOrLava(IBlockState s) {
            return emptyOrWater(s) || s.getMaterial() == Material.LAVA;
        }

        int y(int y264) {
            return IceCaves.worldY(this.world, y264);
        }

        int uniform(int min, int max) {
            return min >= max ? min : min + this.rand.nextInt(max - min + 1);
        }

        /** {@code TrapezoidHeight} with plateau 0. */
        int trapezoid(int min, int max) {
            int range = max - min;
            if (range <= 0) {
                return min;
            }
            int side = range / 2, topWidth = range - side;
            return min + this.rand.nextInt(topWidth + 1) + this.rand.nextInt(side + 1);
        }

        /** {@code TrapezoidInt} with plateau 0 as {@code offset}'s x and z: -n to n. */
        int offsetTri(int n) {
            return trapezoid(-n, n);
        }

        boolean inBiome(BlockPos p) {
            return IceCaves.at(this.world, p);
        }

        BlockPos square(int x0, int z0, int y) {
            return new BlockPos(x0 + this.rand.nextInt(16), y, z0 + this.rand.nextInt(16));
        }

        /** {@code environment_scan} down through air, up to {@code steps}: the target's position, or null. */
        @Nullable
        BlockPos scanDown(BlockPos from, int steps, java.util.function.Predicate<IBlockState> target) {
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(from);
            for (int i = 0; i < steps; i++) {
                IBlockState s = get(m);
                if (target.test(s)) {
                    return m.toImmutable();
                }
                if (s.getMaterial() != Material.AIR) {
                    return null;
                }
                m.move(EnumFacing.DOWN);
            }
            return target.test(get(m)) ? m.toImmutable() : null;
        }

        // ---- snow layers and crystals --------------------------------------------------------------------------

        /** {@code snow_layers}: 16-24 anchors x 32, offset -4..4, down to a solid block through air, one up. */
        void snowLayers(int x0, int z0) {
            int anchors = uniform(16, 24);
            IBlockState snow = Blocks.SNOW_LAYER.getDefaultState();
            for (int a = 0; a < anchors; a++) {
                BlockPos anchor = square(x0, z0, uniform(y(-64), y(256)));
                for (int i = 0; i < 32; i++) {
                    BlockPos p = anchor.add(offsetTri(4), 0, offsetTri(4));
                    BlockPos hit = scanDown(p, 32, s -> s.getMaterial().blocksMovement());
                    if (hit == null) {
                        continue;
                    }
                    BlockPos at = hit.up();
                    if (inBiome(at) && air(at) && inside(at) && Blocks.SNOW_LAYER.canPlaceBlockAt(this.world, at)) {
                        set(at, snow);
                    }
                }
            }
        }

        /** {@code ice_crystals}: 48-64 anchors x 2, offset -2..2, onto packed ice, facing up. */
        void crystals(int x0, int z0) {
            int anchors = uniform(48, 64);
            IBlockState crystal = IceCavesContent.iceCrystal.getDefaultState().withProperty(BlockIceCrystal.FACING, EnumFacing.UP);
            for (int a = 0; a < anchors; a++) {
                BlockPos anchor = square(x0, z0, uniform(y(-64), y(256)));
                for (int i = 0; i < 2; i++) {
                    BlockPos p = anchor.add(offsetTri(2), 0, offsetTri(2));
                    BlockPos hit = scanDown(p, 32, s -> s.getBlock() == Blocks.PACKED_ICE);
                    if (hit == null) {
                        continue;
                    }
                    BlockPos at = hit.up();
                    if (inBiome(at) && air(at) && inside(at)) {
                        set(at, crystal);
                    }
                }
            }
        }

        // ---- icicle clusters -----------------------------------------------------------------------------------

        /**
         * {@code icicle_cluster}: 24-64 tries; {@code speleothem_cluster} on ceilings only, radius 6, height 3-5,
         * density 0.3-0.7, search 32, columns only under packed ice (no replaceables), bases attached, no water.
         */
        void icicleClusters(int x0, int z0) {
            int tries = uniform(24, 64);
            for (int t = 0; t < tries; t++) {
                BlockPos origin = square(x0, z0, uniform(y(-64), y(256)));
                if (inBiome(origin)) {
                    cluster(origin);
                }
            }
        }

        void cluster(BlockPos origin) {
            if (!emptyOrWater(get(origin))) {
                return;
            }
            int height = uniform(3, 5);
            float density = 0.3F + this.rand.nextFloat() * 0.4F;
            int xRadius = 6, zRadius = 6;
            for (int dx = -xRadius; dx <= xRadius; dx++) {
                for (int dz = -zRadius; dz <= zRadius; dz++) {
                    int fromEdge = Math.min(xRadius - Math.abs(dx), zRadius - Math.abs(dz));
                    double chance = clampedMap(fromEdge, 0, 3, 0.1F, 1.0F);
                    column(origin.add(dx, 0, dz), dx, dz, chance, height, density);
                }
            }
        }

        private void column(BlockPos pos, int dx, int dz, double chance, int clusterHeight, float density) {
            // Column.scan with canPlace = air (allow_water_placement false) and the edge any non-air non-water block.
            if (!air(pos)) {
                return;
            }
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(pos);
            int i = 1;
            while (i < 32 && air(m)) {
                m.move(EnumFacing.UP);
                i++;
            }
            IBlockState edge = get(m);
            if (emptyOrWater(edge)) {
                return;
            }
            int ceiling = m.getY();
            m.setPos(pos);
            i = 1;
            while (i < 32 && air(m)) {
                m.move(EnumFacing.DOWN);
                i++;
            }
            Integer floor = emptyOrWater(get(m)) ? null : m.getY();
            Integer scannedHeight = floor == null ? null : ceiling - floor - 1;
            // Wetness 0: no pool. Ceiling only: no stalagmites.
            this.rand.nextFloat();
            boolean want = this.rand.nextDouble() < chance;
            int stalactite = 0;
            if (want && get(new BlockPos(pos.getX(), ceiling, pos.getZ())).getMaterial() != Material.LAVA) {
                uniform(2, 3); // speleothem_block_layer_thickness: nothing replaceable to thicken.
                int max = scannedHeight == null ? clusterHeight : Math.min(clusterHeight, scannedHeight);
                stalactite = speleothemHeight(dx, dz, density, max);
            }
            this.rand.nextDouble();
            // getAvailableSpeleothemHeight: down through air.
            BlockPos start = new BlockPos(pos.getX(), ceiling - 1, pos.getZ());
            int available = 0;
            BlockPos.MutableBlockPos a = new BlockPos.MutableBlockPos(start);
            while (available < stalactite && air(a)) {
                available++;
                a.move(EnumFacing.DOWN);
            }
            this.rand.nextBoolean();
            grow(start, available);
        }

        /** {@code growSpeleothem} with {@code set_attached}: only under packed ice (the base block). */
        void grow(BlockPos start, int length) {
            if (length <= 0 || get(start.up()).getBlock() != Blocks.PACKED_ICE) {
                return;
            }
            BlockIcicle icicle = IceCavesContent.icicle;
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(start);
            for (int remaining = length; remaining > 0; remaining--) {
                Thickness t = remaining == 2 ? Thickness.FRUSTUM : remaining == 1 ? Thickness.TIP
                        : remaining == length ? Thickness.BASE : Thickness.MIDDLE;
                set(p, icicle.state(EnumFacing.DOWN, t));
                p.move(EnumFacing.DOWN);
            }
        }

        private int speleothemHeight(int dx, int dz, float density, int maxHeight) {
            if (this.rand.nextFloat() > density) {
                return 0;
            }
            int distance = Math.abs(dx) + Math.abs(dz);
            float mean = (float) clampedMap(distance, 0, 8, maxHeight / 2.0, 0.0);
            double v = mean + this.rand.nextGaussian() * 3;
            return (int) MathHelper.clamp(v, 0.0F, maxHeight);
        }

        // ---- large icicles -------------------------------------------------------------------------------------

        /** {@code large_icicle}: 48-64 tries of {@code large_speleothem} in packed ice, large dripstone's numbers. */
        void largeIcicles(int x0, int z0) {
            int tries = uniform(48, 64);
            for (int t = 0; t < tries; t++) {
                BlockPos origin = square(x0, z0, uniform(y(-64), y(256)));
                if (inBiome(origin)) {
                    largeIcicle(origin);
                }
            }
        }

        boolean baseOrLava(IBlockState s) {
            Block b = s.getBlock();
            return b == Blocks.PACKED_ICE || is(s, "calcite") || stoneLike(s) && s.getValue(BlockStone.VARIANT) == BlockStone.EnumType.STONE
                    || is(s, "deepslate") || s.getMaterial() == Material.LAVA;
        }

        void largeIcicle(BlockPos origin) {
            if (!emptyOrWater(get(origin))) {
                return;
            }
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(origin);
            int i = 1;
            while (i < 30 && emptyOrWater(get(m))) {
                m.move(EnumFacing.UP);
                i++;
            }
            if (!baseOrLava(get(m))) {
                return;
            }
            int ceiling = m.getY();
            m.setPos(origin);
            i = 1;
            while (i < 30 && emptyOrWater(get(m))) {
                m.move(EnumFacing.DOWN);
                i++;
            }
            if (!baseOrLava(get(m))) {
                return;
            }
            int floor = m.getY();
            int caveHeight = ceiling - floor - 1;
            if (caveHeight < 4) {
                return;
            }
            int maxRadius = MathHelper.clamp((int) (caveHeight * 0.33F), 3, 16);
            int radius = uniform(3, maxRadius);
            Spike stalactite = new Spike(new BlockPos(origin.getX(), ceiling - 1, origin.getZ()), false, radius,
                    0.3F + this.rand.nextFloat() * 0.6F, 0.4F + this.rand.nextFloat() * 1.6F);
            Spike stalagmite = new Spike(new BlockPos(origin.getX(), floor + 1, origin.getZ()), true, radius,
                    0.4F + this.rand.nextFloat() * 0.6F, 0.4F + this.rand.nextFloat() * 1.6F);
            Wind wind = stalactite.radius >= 4 && stalactite.bluntness >= 0.6 && stalagmite.radius >= 4 && stalagmite.bluntness >= 0.6
                    ? new Wind(origin.getY(), this.rand, 16 - radius) : null;
            boolean a = stalactite.embed(wind), b = stalagmite.embed(wind);
            if (a) {
                stalactite.place(wind);
            }
            if (b) {
                stalagmite.place(wind);
            }
        }

        final class Wind {
            final int originY, maxOffset;
            final double wx, wz;

            Wind(int originY, Random r, int maxOffset) {
                this.originY = originY;
                this.maxOffset = maxOffset;
                float speed = r.nextFloat() * 0.3F;
                float direction = r.nextFloat() * (float) Math.PI;
                this.wx = MathHelper.cos(direction) * speed;
                this.wz = MathHelper.sin(direction) * speed;
            }

            BlockPos offset(BlockPos p) {
                int dy = this.originY - p.getY();
                int dx = MathHelper.clamp(MathHelper.floor(this.wx * dy), -this.maxOffset, this.maxOffset);
                int dz = MathHelper.clamp(MathHelper.floor(this.wz * dy), -this.maxOffset, this.maxOffset);
                return p.add(dx, 0, dz);
            }
        }

        final class Spike {
            BlockPos root;
            final boolean up;
            int radius;
            final double bluntness, scale;

            Spike(BlockPos root, boolean up, int radius, double bluntness, double scale) {
                this.root = root;
                this.up = up;
                this.radius = radius;
                this.bluntness = bluntness;
                this.scale = scale;
            }

            int heightAt(float r) {
                return (int) spikeHeight(r, this.radius, this.scale, this.bluntness);
            }

            BlockPos wind(@Nullable Wind w, BlockPos p) {
                return w == null ? p : w.offset(p);
            }

            boolean embed(@Nullable Wind w) {
                while (this.radius > 1) {
                    BlockPos.MutableBlockPos r = new BlockPos.MutableBlockPos(this.root);
                    int tries = Math.min(10, heightAt(0.0F));
                    for (int i = 0; i < tries; i++) {
                        if (get(r).getMaterial() == Material.LAVA) {
                            return false;
                        }
                        if (embedded(wind(w, r), this.radius)) {
                            this.root = r.toImmutable();
                            return true;
                        }
                        r.move(this.up ? EnumFacing.DOWN : EnumFacing.UP);
                    }
                    this.radius /= 2;
                }
                return false;
            }

            boolean embedded(BlockPos center, int r) {
                if (emptyOrWaterOrLava(get(center))) {
                    return false;
                }
                float increment = 6.0F / r;
                for (float angle = 0.0F; angle < (float) (Math.PI * 2); angle += increment) {
                    int dx = (int) (MathHelper.cos(angle) * r), dz = (int) (MathHelper.sin(angle) * r);
                    if (emptyOrWaterOrLava(get(center.add(dx, 0, dz)))) {
                        return false;
                    }
                }
                return true;
            }

            void place(@Nullable Wind w) {
                IBlockState packed = Blocks.PACKED_ICE.getDefaultState();
                for (int dx = -this.radius; dx <= this.radius; dx++) {
                    for (int dz = -this.radius; dz <= this.radius; dz++) {
                        float current = MathHelper.sqrt(dx * dx + dz * dz);
                        if (current > this.radius) {
                            continue;
                        }
                        int h = heightAt(current);
                        if (h <= 0) {
                            continue;
                        }
                        if (rand.nextFloat() < 0.2F) {
                            h = (int) (h * (0.8F + rand.nextFloat() * 0.2F));
                        }
                        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(this.root.add(dx, 0, dz));
                        boolean out = false;
                        int maxY = this.up ? world.getHeight(p.getX(), p.getZ()) : Integer.MAX_VALUE;
                        for (int i = 0; i < h && p.getY() < maxY; i++) {
                            BlockPos at = wind(w, p);
                            IBlockState s = get(at);
                            if (emptyOrWaterOrLava(s)) {
                                out = true;
                                set(at, packed);
                            } else if (out && baseStone(s)) {
                                break;
                            }
                            p.move(this.up ? EnumFacing.UP : EnumFacing.DOWN);
                        }
                    }
                }
            }
        }

        // ---- ores ----------------------------------------------------------------------------------------------

        /** The 21 {@code ice_cave_ore_*} placements, in 26.4's order (counts, rarities and heights as vanilla's ores). */
        void ores(int x0, int z0) {
            String coal = "minecraft:coal_ore", iron = "minecraft:iron_ore", gold = "minecraft:gold_ore", red = "minecraft:redstone_ore";
            String diamond = "minecraft:diamond_ore", lapis = "minecraft:lapis_ore", copper = "cavesandcliffsbackport:copper_ore";
            hosted(x0, z0, 30, false, coal, 17, 0.0F, U, 136, TOP);
            hosted(x0, z0, 20, false, coal, 17, 0.5F, T, 0, 192);
            hosted(x0, z0, 90, false, iron, 9, 0.0F, T, 80, 384);
            hosted(x0, z0, 10, false, iron, 9, 0.0F, T, -24, 56);
            hosted(x0, z0, 10, false, iron, 4, 0.0F, U, BOTTOM, 72);
            hosted(x0, z0, 4, false, gold, 9, 0.5F, T, -64, 32);
            hosted(x0, z0, -1, false, gold, 9, 0.5F, U, -64, -48);
            hosted(x0, z0, 4, false, red, 8, 0.0F, U, BOTTOM, 15);
            hosted(x0, z0, 8, false, red, 8, 0.0F, T, BOTTOM - 32, BOTTOM + 32);
            hosted(x0, z0, 7, false, diamond, 4, 0.5F, T, BOTTOM - 80, BOTTOM + 80);
            hosted(x0, z0, 2, false, diamond, 8, 0.5F, U, -64, -4);
            hosted(x0, z0, 9, true, diamond, 12, 0.7F, T, BOTTOM - 80, BOTTOM + 80);
            hosted(x0, z0, 4, false, diamond, 8, 1.0F, T, BOTTOM - 80, BOTTOM + 80);
            hosted(x0, z0, 2, false, lapis, 7, 0.0F, T, -32, 32);
            hosted(x0, z0, 4, false, lapis, 7, 1.0F, U, BOTTOM, 64);
            hosted(x0, z0, 16, false, copper, 10, 0.0F, T, -16, 112);
            plain(x0, z0, 14, false, Blocks.GRAVEL.getDefaultState(), 33, U, BOTTOM, TOP);
            IBlockState diorite = Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.DIORITE);
            IBlockState andesite = Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.ANDESITE);
            plain(x0, z0, 6, true, diorite, 64, U, 64, 128);
            plain(x0, z0, 2, false, diorite, 64, U, 0, 60);
            plain(x0, z0, 6, true, andesite, 64, U, 64, 128);
            plain(x0, z0, 2, false, andesite, 64, U, 0, 60);
        }

        static final int U = 0, T = 1;
        static final int BOTTOM = -1000, TOP = 1000;

        /** A 26.4 height anchor: {@code above_bottom} offsets from BOTTOM, {@code below_top} from TOP, else absolute. */
        int anchor(int v) {
            if (v >= BOTTOM - 200 && v <= BOTTOM + 200) {
                return y(-64 + (v - BOTTOM));
            }
            if (v >= TOP - 200 && v <= TOP + 200) {
                return Math.min(this.top, y(319 - (TOP - v)));
            }
            return y(v);
        }

        int height(int kind, int min, int max) {
            int a = anchor(min), b = anchor(max);
            return kind == T ? trapezoid(a, b) : uniform(a, b);
        }

        /** count (or rarity 1/n when {@code rarity}, or 0-1 when -1), in_square, height, biome. */
        int tries(int count, boolean rarity) {
            if (rarity) {
                return this.rand.nextFloat() < 1.0F / count ? 1 : 0;
            }
            return count < 0 ? this.rand.nextInt(2) : count;
        }

        void hosted(int x0, int z0, int count, boolean rarity, String oreId, int size, float discard, int kind, int min, int max) {
            Block ore = named(oreId);
            int n = tries(count, rarity);
            for (int i = 0; i < n; i++) {
                BlockPos p = new BlockPos(x0 + this.rand.nextInt(16), height(kind, min, max), z0 + this.rand.nextInt(16));
                if (ore == null || !inBiome(p)) {
                    continue;
                }
                // The sequence: the host blob first, then the ore; a failed host stops it.
                if (!blob(p, 20, 0.0F, this::hostTarget)) {
                    continue;
                }
                final IBlockState stoneOre = ore.getDefaultState();
                final Block deep = named(oreId.replace("minecraft:", "cavesandcliffsbackport:").replace(":", ":deepslate_"));
                blob(p, size, discard, (s, y) -> oreTarget(s, y, stoneOre, deep == null ? stoneOre : deep.getDefaultState()));
            }
        }

        void plain(int x0, int z0, int count, boolean rarity, IBlockState state, int size, int kind, int min, int max) {
            int n = tries(count, rarity);
            for (int i = 0; i < n; i++) {
                BlockPos p = new BlockPos(x0 + this.rand.nextInt(16), height(kind, min, max), z0 + this.rand.nextInt(16));
                if (inBiome(p)) {
                    blob(p, size, 0.0F, (s, y) -> baseStone(s) || iceOrCalcite(s) ? state : null);
                }
            }
        }

        /** The host: stone where stone was, deepslate where deepslate, and ice or calcite to stone (deepslate under 4). */
        @Nullable
        IBlockState hostTarget(IBlockState s, int y) {
            IBlockState deep = deepslate();
            if (stoneLike(s)) {
                return Blocks.STONE.getDefaultState();
            }
            if (is(s, "deepslate")) {
                return deep;
            }
            if (is(s, "tuff") || iceOrCalcite(s)) {
                return y >= IceCaves.worldY(this.world, 4) || deep == null ? Blocks.STONE.getDefaultState() : deep;
            }
            return null;
        }

        @Nullable
        IBlockState oreTarget(IBlockState s, int y, IBlockState stoneOre, IBlockState deepOre) {
            if (stoneLike(s)) {
                return stoneOre;
            }
            if (is(s, "deepslate")) {
                return deepOre;
            }
            if (is(s, "tuff")) {
                return y >= IceCaves.worldY(this.world, 4) ? stoneOre : deepOre;
            }
            return null;
        }

        interface Target {
            @Nullable
            IBlockState replace(IBlockState state, int y);
        }

        /** {@code OreFeature}: 1.12.2's and 26.x's blob shape, with the air-exposure discard. @return whether it placed any */
        boolean blob(BlockPos pos, int size, float discard, Target target) {
            float f = this.rand.nextFloat() * (float) Math.PI;
            double x1 = pos.getX() + 8 + MathHelper.sin(f) * size / 8.0F, x2 = pos.getX() + 8 - MathHelper.sin(f) * size / 8.0F;
            double z1 = pos.getZ() + 8 + MathHelper.cos(f) * size / 8.0F, z2 = pos.getZ() + 8 - MathHelper.cos(f) * size / 8.0F;
            double y1 = pos.getY() + this.rand.nextInt(3) - 2, y2 = pos.getY() + this.rand.nextInt(3) - 2;
            boolean any = false;
            for (int i = 0; i < size; i++) {
                float t = (float) i / size;
                double cx = x1 + (x2 - x1) * t, cy = y1 + (y2 - y1) * t, cz = z1 + (z2 - z1) * t;
                double d = this.rand.nextDouble() * size / 16.0D;
                double r = (MathHelper.sin((float) Math.PI * t) + 1.0F) * d + 1.0D;
                int ax = MathHelper.floor(cx - r / 2.0D) - 8, ay = MathHelper.floor(cy - r / 2.0D), az = MathHelper.floor(cz - r / 2.0D) - 8;
                int bx = MathHelper.floor(cx + r / 2.0D) - 8, by = MathHelper.floor(cy + r / 2.0D), bz = MathHelper.floor(cz + r / 2.0D) - 8;
                for (int x = ax; x <= bx; x++) {
                    double dx = (x + 8 + 0.5D - cx) / (r / 2.0D);
                    if (dx * dx >= 1.0D) {
                        continue;
                    }
                    for (int y = ay; y <= by; y++) {
                        double dy = (y + 0.5D - cy) / (r / 2.0D);
                        if (dx * dx + dy * dy >= 1.0D) {
                            continue;
                        }
                        for (int z = az; z <= bz; z++) {
                            double dz = (z + 8 + 0.5D - cz) / (r / 2.0D);
                            if (dx * dx + dy * dy + dz * dz >= 1.0D) {
                                continue;
                            }
                            BlockPos p = new BlockPos(x, y, z);
                            if (!inside(p)) {
                                continue;
                            }
                            IBlockState s = get(p);
                            IBlockState to = target.replace(s, y);
                            if (to == null || discard > 0.0F && exposed(p) && this.rand.nextFloat() < discard) {
                                continue;
                            }
                            set(p, to);
                            any = true;
                        }
                    }
                }
            }
            return any;
        }

        boolean exposed(BlockPos p) {
            for (EnumFacing f : EnumFacing.values()) {
                if (air(p.offset(f))) {
                    return true;
                }
            }
            return false;
        }

        // ---- crevasses -----------------------------------------------------------------------------------------

        /** {@code ice_crevasse}: 16 tries, 24 above the bottom to 64 below the top, down through air (8) onto carvable. */
        void crevasses(int x0, int z0) {
            for (int t = 0; t < 16; t++) {
                BlockPos p = square(x0, z0, uniform(this.bottom + 24, this.top - 64));
                BlockPos hit = scanDown(p, 8, IceCavesGen::carvable);
                if (hit != null && inBiome(hit)) {
                    crevasse(hit);
                }
            }
        }

        final class CarveColumn implements Comparable<CarveColumn> {
            final BlockPos surface;
            final IBlockState original;
            final int depth;

            CarveColumn(BlockPos surface, IBlockState original, int depth) {
                this.surface = surface;
                this.original = original;
                this.depth = depth;
            }

            int bottomY() {
                return this.surface.getY() - this.depth;
            }

            boolean containsY(int y) {
                return y >= bottomY() && y <= this.surface.getY();
            }

            @Override
            public int compareTo(CarveColumn o) {
                int dx = this.surface.getX() - o.surface.getX();
                return dx == 0 ? this.surface.getZ() - o.surface.getZ() : dx;
            }
        }

        long key(int x, int z) {
            return (long) x << 32 | (z & 0xFFFFFFFFL);
        }

        /** {@code findCarvableSurface}: from y + range/2 down to y - range/2... the first carvable block under air. */
        @Nullable
        Object[] surface(BlockPos center, int range) {
            BlockPos start = center.up(range / 2);
            boolean aboveEmpty = air(start.up());
            int minY = Math.max(this.bottom, start.getY() - range);
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(start);
            while (m.getY() >= minY) {
                IBlockState s = get(m);
                if (carvable(s) && aboveEmpty) {
                    return new Object[] {m.toImmutable(), s};
                }
                aboveEmpty = s.getMaterial() == Material.AIR;
                m.move(EnumFacing.DOWN);
            }
            return null;
        }

        void crevasse(BlockPos pos) {
            int carveDepth = uniform(6, 10);
            if (pos.getY() <= this.bottom + carveDepth) {
                return;
            }
            int branchLength = 16 / 2;
            float yaw = this.rand.nextFloat() * (float) (Math.PI * 2);
            int fillDepth = uniform(4, 6);
            Map<Long, CarveColumn> columns = new HashMap<Long, CarveColumn>();
            for (int d = 0; d < 2; d++) {
                branch(columns, pos, branchLength, carveDepth, yaw + d * (float) Math.PI);
            }
            connected(columns, pos);
            if (columns.size() < 10) {
                return;
            }
            int lowest = Integer.MAX_VALUE;
            for (CarveColumn c : columns.values()) {
                lowest = Math.min(lowest, c.surface.getY());
            }
            int fillLevel = Math.min(pos.getY() - fillDepth, lowest - 1);
            List<CarveColumn> sorted = new ArrayList<CarveColumn>(columns.values());
            java.util.Collections.sort(sorted);
            for (CarveColumn c : sorted) {
                carve(columns, c, fillLevel);
            }
        }

        void branch(Map<Long, CarveColumn> columns, BlockPos origin, int length, int carveDepth, float yaw) {
            double cx = origin.getX(), cz = origin.getZ();
            int prevY = origin.getY();
            float accel = 0.0F;
            for (int step = 0; step < length; step++) {
                cx += Math.cos(yaw);
                cz += Math.sin(yaw);
                float dir = (float) this.rand.nextGaussian();
                float mag = this.rand.nextFloat();
                accel = accel * 0.5F + dir * mag * 4.0F;
                yaw += accel * 0.05F;
                int bx = MathHelper.floor(cx), bz = MathHelper.floor(cz);
                Object[] s = surface(new BlockPos(bx, prevY, bz), 8);
                if (s == null) {
                    continue;
                }
                BlockPos sp = (BlockPos) s[0];
                float progress = (float) step / length;
                float taper = 1.0F - progress * 0.6F;
                float radius = Math.max(4 * 0.5F * taper, 0.5F);
                int maxDepth = Math.max((int) (carveDepth * taper), 1);
                int expanded = MathHelper.ceil(radius);
                for (int x = bx - expanded; x <= bx + expanded; x++) {
                    for (int z = bz - expanded; z <= bz + expanded; z++) {
                        int dx = x - bx, dz = z - bz;
                        if (dx * dx + dz * dz > radius * radius || columns.containsKey(key(x, z))) {
                            continue;
                        }
                        Object[] cs = surface(new BlockPos(x, sp.getY(), z), 8);
                        if (cs == null) {
                            continue;
                        }
                        BlockPos csp = (BlockPos) cs[0];
                        int depth = 1;
                        BlockPos.MutableBlockPos t = new BlockPos.MutableBlockPos(csp.down());
                        while (depth < maxDepth && carvable(get(t))) {
                            t.move(EnumFacing.DOWN);
                            depth++;
                        }
                        columns.put(key(x, z), new CarveColumn(csp, (IBlockState) cs[1], depth - 1));
                    }
                }
                prevY = sp.getY();
            }
        }

        boolean steep(Map<Long, CarveColumn> columns, CarveColumn c) {
            for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) {
                BlockPos n = c.surface.offset(f);
                CarveColumn o = columns.get(key(n.getX(), n.getZ()));
                if (o != null && Math.abs(c.surface.getY() - o.surface.getY()) > 1) {
                    return true;
                }
            }
            return false;
        }

        void connected(Map<Long, CarveColumn> columns, BlockPos origin) {
            CarveColumn start = columns.get(key(origin.getX(), origin.getZ()));
            if (start == null || steep(columns, start)) {
                columns.clear();
                return;
            }
            List<CarveColumn> keep = new ArrayList<CarveColumn>();
            java.util.Set<Long> seen = new java.util.HashSet<Long>();
            keep.add(start);
            seen.add(key(origin.getX(), origin.getZ()));
            for (int i = 0; i < keep.size(); i++) {
                CarveColumn c = keep.get(i);
                for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) {
                    BlockPos n = c.surface.offset(f);
                    long k = key(n.getX(), n.getZ());
                    CarveColumn o = columns.get(k);
                    if (o != null && !seen.contains(k) && !steep(columns, o)) {
                        keep.add(o);
                        seen.add(k);
                    }
                }
            }
            columns.clear();
            for (CarveColumn c : keep) {
                columns.put(key(c.surface.getX(), c.surface.getZ()), c);
            }
        }

        /** {@code #blocks_fluid_flow}: blocks movement, or a sign. */
        boolean barrier(BlockPos p) {
            IBlockState s = get(p);
            return s.getMaterial().blocksMovement() || s.getBlock() instanceof net.minecraft.block.BlockSign;
        }

        void carve(Map<Long, CarveColumn> columns, CarveColumn c, int fillLevel) {
            IBlockState air = Blocks.AIR.getDefaultState(), water = Blocks.WATER.getDefaultState();
            int minY = c.bottomY();
            boolean carved = false;
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(c.surface);
            while (p.getY() >= minY) {
                boolean fill = p.getY() <= fillLevel;
                if (!fill && sideLiquid(p)) {
                    break;
                }
                Block placement = fill ? Blocks.WATER : Blocks.AIR;
                for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) {
                    BlockPos n = p.offset(f);
                    if (barrier(n)) {
                        continue;
                    }
                    CarveColumn o = columns.get(key(n.getX(), n.getZ()));
                    if (o == null || !o.containsY(p.getY())) {
                        if (get(n).getBlock() != placement) {
                            set(n, c.original);
                        }
                    }
                }
                set(p.toImmutable(), fill ? water : air);
                if (fill && p.getY() == minY) {
                    BlockPos below = p.down();
                    if (!barrier(below) && get(below).getBlock() != Blocks.WATER) {
                        set(below, c.original);
                    }
                }
                carved = true;
                p.move(EnumFacing.DOWN);
            }
            if (carved) {
                set(c.surface, IceCavesContent.brittleIce.getDefaultState());
            }
        }

        boolean sideLiquid(BlockPos p) {
            for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) {
                if (get(p.offset(f)).getMaterial().isLiquid()) {
                    return true;
                }
            }
            return false;
        }
    }

    /** {@code #ice_crevasse_replaceables}: stone and its variants, deepslate, tuff, calcite, packed ice. */
    static boolean carvable(IBlockState s) {
        return stoneLike(s) || is(s, "deepslate") || is(s, "tuff") || iceOrCalcite(s);
    }

    static double clampedMap(double v, double fromMin, double fromMax, double toMin, double toMax) {
        double t = MathHelper.clamp((v - fromMin) / (fromMax - fromMin), 0.0D, 1.0D);
        return toMin + t * (toMax - toMin);
    }

    /** {@code SpeleothemUtils.getSpeleothemHeight}. */
    static double spikeHeight(double distance, double radius, double scale, double bluntness) {
        if (distance < bluntness) {
            distance = bluntness;
        }
        double r = distance / radius * 0.384D;
        double part1 = 0.75D * Math.pow(r, 1.3333333333333333D);
        double part2 = Math.pow(r, 0.6666666666666666D);
        double part3 = 0.3333333333333333D * Math.log(r);
        double h = Math.max(scale * (part1 - part2 - part3), 0.0D);
        return h / 0.384D * radius;
    }
}
