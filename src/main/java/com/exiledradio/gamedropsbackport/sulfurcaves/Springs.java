package com.exiledradio.gamedropsbackport.sulfurcaves;

import com.exiledradio.gamedropsbackport.GameDropsBackport;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStone;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sulfur springs: 26.3's {@code rooted_sulfur_spring}, a {@code root_system} whose tree is {@code sulfur_spring}.
 *
 * <p>From a sulfur cave ceiling the column is followed up, at most 184 blocks and never above the surface, to the
 * first air with 5 more air above, whose ground is solid (not lava) and level (8 blocks out each way, the ground is
 * within 2 of it). There a spring is placed (by weight: one of four small 200, three medium 90, two large 20, the extra
 * large 5): first tuff on solid ground around it (64, 80, 96 or 128 tries within 7, 8, 9 or 10 blocks, triangular),
 * then the spring's template, centered, 7 blocks down, at a random rotation. Then sulfur roots run from the ceiling up
 * to it (20 tries a layer within 2 blocks, replacing stone, dirt, sand, gravel, clay, terracotta and snow) and one
 * hanging sulfur under the ceiling.
 */
final class Springs {

    static final String[] SMALL = {"small_1", "small_2", "small_3", "small_4"};
    static final String[] MEDIUM = {"medium_1", "medium_2", "medium_3"};
    static final String[] LARGE = {"large_1", "large_2"};
    static final String[] EXTRA_LARGE = {"extra_large_1"};

    private static final Map<String, Template> TEMPLATES = new HashMap<String, Template>();

    private final Features f;

    Springs(Features f) {
        this.f = f;
    }

    /** 26.3's {@code RootSystemFeature.place}. */
    boolean rootSystem(BlockPos origin) {
        if (!f.air(origin)) {
            return false;
        }
        BlockPos.MutableBlockPos w = new BlockPos.MutableBlockPos(origin);
        for (int y = 0; y < 184; y++) {
            w.move(EnumFacing.UP);
            if (!f.in(w) || surface(w) < w.getY()) {
                return false;
            }
            if (f.air(w) && spaceForTree(w)) {
                BlockPos below = w.down();
                IBlockState b = f.get(below);
                if (b.getMaterial() == Material.LAVA || !b.getMaterial().isSolid()) {
                    return false;
                }
                if (spring(w.toImmutable())) {
                    roots(origin, origin.getY() + y);
                    hangingRoot(origin);
                    return true;
                }
            }
        }
        return false;
    }

    /** {@code WORLD_SURFACE}: one above the highest block that is not air. */
    private int surface(BlockPos p) {
        return f.world.getHeight(p.getX(), p.getZ());
    }

    private boolean spaceForTree(BlockPos pos) {
        BlockPos.MutableBlockPos up = new BlockPos.MutableBlockPos(pos);
        for (int i = 1; i <= 5; i++) {
            up.move(EnumFacing.UP);
            if (!f.air(up)) {
                return false;
            }
        }
        for (EnumFacing d : EnumFacing.Plane.HORIZONTAL) {
            BlockPos corner = pos.offset(d, 8);
            if (f.air(corner.down(2)) || !f.in(corner) || !f.air(corner.up(2))) {
                return false;
            }
        }
        return true;
    }

    private void roots(BlockPos origin, int target) {
        BlockPos.MutableBlockPos w = new BlockPos.MutableBlockPos();
        for (int y = origin.getY(); y < target; y++) {
            for (int i = 0; i < 20; i++) {
                w.setPos(origin.getX() + f.rand.nextInt(3) - f.rand.nextInt(3), y, origin.getZ() + f.rand.nextInt(3) - f.rand.nextInt(3));
                if (rootReplaceable(f.get(w))) {
                    f.set(w, f.sulfur.getDefaultState());
                }
            }
        }
    }

    private void hangingRoot(BlockPos origin) {
        // Radius 1 and span 1: nextInt(1) is always 0, so the one try is the origin itself.
        f.rand.nextInt(1);
        f.rand.nextInt(1);
        f.rand.nextInt(1);
        f.rand.nextInt(1);
        f.rand.nextInt(1);
        f.rand.nextInt(1);
        if (f.air(origin) && f.sturdyBelow(origin.up())) {
            f.set(origin, f.sulfur.getDefaultState());
        }
    }

    /** {@code #azalea_root_replaceable}: overworld stone, substrate (dirt, mud, moss, grass), terracotta, sands, ... */
    private static boolean rootReplaceable(IBlockState s) {
        Block b = s.getBlock();
        if (b == Blocks.STONE || b == Blocks.DIRT || b == Blocks.GRASS || b == Blocks.MYCELIUM || b == Blocks.HARDENED_CLAY
                || b == Blocks.STAINED_HARDENED_CLAY || b == Blocks.SAND || b == Blocks.CLAY || b == Blocks.GRAVEL
                || b == Blocks.SNOW || b == Blocks.FARMLAND || b == Blocks.GRASS_PATH) {
            return true;
        }
        String path = b.getRegistryName() == null ? "" : b.getRegistryName().getPath();
        return path.equals("deepslate") || path.equals("tuff") || path.equals("mud") || path.equals("moss_block")
                || path.equals("rooted_dirt") || path.equals("powder_snow") || path.endsWith("_terracotta") && !path.contains("glazed");
    }

    // ------------------------------------------------------------------------------------------------ sulfur_spring

    /** The weighted spring: tuff, then the template. */
    boolean spring(BlockPos at) {
        int roll = f.rand.nextInt(315);
        String[] names;
        int count, reach;
        if (roll < 200) {
            names = SMALL;
            count = 64;
            reach = 7;
        } else if (roll < 290) {
            names = MEDIUM;
            count = 80;
            reach = 8;
        } else if (roll < 310) {
            names = LARGE;
            count = 96;
            reach = 9;
        } else {
            names = EXTRA_LARGE;
            count = 128;
            reach = 10;
        }
        IBlockState tuff = SulfurCavesContent.tuff();
        for (int i = 0; i < count; i++) {
            BlockPos p = at.add(trapezoid(reach), trapezoid(3), trapezoid(reach));
            if (!f.inColumns(p, 0)) {
                // Past the area (reach 9 or 10 from a spring near its edge): placed once that chunk is ready. At least
                // 9 out, so never where the template (8 each way at most) goes.
                Deferred.tuff(f.world, p);
                continue;
            }
            p = f.scan(p, EnumFacing.DOWN, 4, Features.Scan.ANY, Features.Scan.SOLID);
            if (p != null && f.solid(p)) {
                f.set(p, tuff);
            }
        }
        Template t = template(names[f.rand.nextInt(names.length)]);
        if (t == null) {
            return false;
        }
        t.place(f, at.down(7), f.rand.nextInt(4));
        return true;
    }

    /** {@code trapezoid} from -r to r with no plateau: a triangle. */
    private int trapezoid(int r) {
        int range = 2 * r, start = range / 2, end = range - start;
        return -r + f.rand.nextInt(range - end + 1) + f.rand.nextInt(end + 1);
    }

    static synchronized Template template(String name) {
        if (!TEMPLATES.containsKey(name)) {
            TEMPLATES.put(name, Template.load("sulfur_spring_" + name));
        }
        return TEMPLATES.get(name);
    }

    /** A 26.3 structure file: its size, palette (by 26.3 name) and blocks. */
    static final class Template {
        final int sx, sy, sz;
        final List<int[]> blocks = new ArrayList<int[]>();
        final List<NBTTagCompound> palette = new ArrayList<NBTTagCompound>();

        Template(int sx, int sy, int sz) {
            this.sx = sx;
            this.sy = sy;
            this.sz = sz;
        }

        static Template load(String name) {
            String path = "/assets/" + GameDropsBackport.MOD_ID + "/structures/spring/" + name + ".nbt";
            try (InputStream in = Springs.class.getResourceAsStream(path)) {
                if (in == null) {
                    GameDropsBackport.LOGGER.error("Missing spring template {}", path);
                    return null;
                }
                NBTTagCompound tag = CompressedStreamTools.readCompressed(in);
                NBTTagList size = tag.getTagList("size", 3);
                Template t = new Template(size.getIntAt(0), size.getIntAt(1), size.getIntAt(2));
                NBTTagList pal = tag.getTagList("palette", 10);
                for (int i = 0; i < pal.tagCount(); i++) {
                    t.palette.add(pal.getCompoundTagAt(i));
                }
                NBTTagList bl = tag.getTagList("blocks", 10);
                for (int i = 0; i < bl.tagCount(); i++) {
                    NBTTagCompound b = bl.getCompoundTagAt(i);
                    NBTTagList pos = b.getTagList("pos", 3);
                    t.blocks.add(new int[] {pos.getIntAt(0), pos.getIntAt(1), pos.getIntAt(2), b.getInteger("state")});
                }
                return t;
            } catch (Exception e) {
                GameDropsBackport.LOGGER.error("Could not read spring template {}", path, e);
                return null;
            }
        }

        /**
         * {@code TemplateFeature.place}: rotation 0-3 clockwise quarter turns; the corner moves so the template's
         * middle is at {@code origin}.
         */
        void place(Features f, BlockPos origin, int rotation) {
            int ox, oz;
            switch (rotation) {
                case 1:
                    ox = sz / 2;
                    oz = -(sx / 2);
                    break;
                case 2:
                    ox = sx / 2;
                    oz = sz / 2;
                    break;
                case 3:
                    ox = -(sz / 2);
                    oz = sx / 2;
                    break;
                default:
                    ox = -(sx / 2);
                    oz = -(sz / 2);
            }
            IBlockState[] states = new IBlockState[palette.size()];
            boolean[] wet = new boolean[palette.size()];
            for (int i = 0; i < states.length; i++) {
                states[i] = state(f, palette.get(i));
                NBTTagCompound props = properties(palette.get(i));
                wet[i] = props != null && "true".equals(props.getString("waterlogged"));
            }
            List<BlockPos> potent = new ArrayList<BlockPos>();
            for (int[] b : blocks) {
                int x = b[0], z = b[2];
                int rx, rz;
                switch (rotation) {
                    case 1:
                        rx = -z;
                        rz = x;
                        break;
                    case 2:
                        rx = -x;
                        rz = -z;
                        break;
                    case 3:
                        rx = z;
                        rz = -x;
                        break;
                    default:
                        rx = x;
                        rz = z;
                }
                BlockPos p = origin.add(ox + rx, b[1], oz + rz);
                IBlockState s = states[b[3]];
                if (s == null) {
                    continue;
                }
                if (s.getBlock() == f.potent) {
                    potent.add(p);
                    continue;
                }
                if (s.getBlock() == f.spike) {
                    if (f.in(p)) {
                        if (wet[b[3]]) {
                            com.exiledradio.backportcore.waterlog.Waterlogging.placeWithWater(f.world, p, s, 2 | 16);
                        } else {
                            com.exiledradio.backportcore.waterlog.Waterlogging.placeDry(f.world, p, s, 2 | 16);
                        }
                    }
                    continue;
                }
                f.set(p, s);
            }
            // Last, so it sees its magma and water when it works out its state.
            for (BlockPos p : potent) {
                f.set(p, Features.with(f.potent.getDefaultState(), "potent_sulfur_state", "dormant"));
            }
        }

        private static NBTTagCompound properties(NBTTagCompound entry) {
            if (entry.hasKey("properties", 10)) {
                return entry.getCompoundTag("properties");
            }
            return entry.hasKey("Properties", 10) ? entry.getCompoundTag("Properties") : null;
        }

        /** 26.3's names in 1.12.2's blocks and ours. */
        private static IBlockState state(Features f, NBTTagCompound entry) {
            String id = entry.hasKey("id") ? entry.getString("id") : entry.getString("Name");
            NBTTagCompound props = properties(entry);
            switch (id) {
                case "minecraft:air":
                    return Blocks.AIR.getDefaultState();
                case "minecraft:stone":
                    return Blocks.STONE.getDefaultState();
                case "minecraft:granite":
                    return Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.GRANITE);
                case "minecraft:tuff":
                    return SulfurCavesContent.tuff();
                case "minecraft:magma_block":
                    return Blocks.MAGMA.getDefaultState();
                case "minecraft:water":
                    return Blocks.WATER.getDefaultState();
                case "minecraft:sulfur":
                    return f.sulfur.getDefaultState();
                case "minecraft:cinnabar":
                    return f.cinnabar.getDefaultState();
                case "minecraft:potent_sulfur":
                    return f.potent.getDefaultState();
                case "minecraft:sulfur_spike": {
                    IBlockState s = f.spike.getDefaultState();
                    if (props != null) {
                        s = Features.with(s, "thickness", props.getString("thickness"));
                        s = Features.with(s, "vertical_direction", props.getString("vertical_direction"));
                    }
                    return s;
                }
                default:
                    GameDropsBackport.LOGGER.warn("Spring template block {} has no 1.12.2 counterpart", id);
                    return null;
            }
        }
    }
}
