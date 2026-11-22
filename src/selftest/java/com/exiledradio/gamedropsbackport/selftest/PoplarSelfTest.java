package com.exiledradio.gamedropsbackport.selftest;

import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.backportcore.trade.TradeRegistry;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.poplar.BiomeDappledForest;
import com.exiledradio.gamedropsbackport.poplar.BlockPoplarLeaves;
import com.exiledradio.gamedropsbackport.poplar.BlockPoplarSapling;
import com.exiledradio.gamedropsbackport.poplar.BlockShelfMushroom;
import com.exiledradio.gamedropsbackport.poplar.Content;
import com.exiledradio.gamedropsbackport.poplar.PoplarContent;
import com.exiledradio.gamedropsbackport.poplar.PoplarFeatures;
import com.exiledradio.gamedropsbackport.poplar.boat.BoatContent;
import com.exiledradio.gamedropsbackport.poplar.boat.BoatWood;
import com.exiledradio.gamedropsbackport.poplar.wood.AxeStripping;
import com.exiledradio.gamedropsbackport.poplar.wood.WoodSet;

import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * Stream 5's checks: registration, numbers, recipes, the poplar and fallen poplar features, the red shrub, the shelf
 * mushroom, the dappled forest's placement and decoration. Also writes {@code selftest-states.txt} and
 * {@code selftest-items.txt} for {@code tools/check_assets.py}.
 */
final class PoplarSelfTest implements SelfTest {

    private static final String[] BLOCKS = {"poplar_log", "poplar_wood", "stripped_poplar_log", "stripped_poplar_wood",
            "poplar_planks", "poplar_stairs", "poplar_slab", "poplar_double_slab", "poplar_fence", "poplar_fence_gate",
            "poplar_door", "poplar_trapdoor", "poplar_pressure_plate", "poplar_button", "poplar_sign", "poplar_wall_sign",
            "red_poplar_leaves", "orange_poplar_leaves", "yellow_poplar_leaves", "poplar_sapling", "potted_poplar_sapling",
            "red_shrub", "shelf_mushroom"};

    private WorldServer world;
    private Check check;

    @Override
    public String name() {
        return "poplar";
    }

    @Override
    public void run(Check check) throws Exception {
        this.check = check;
        this.world = check.world;
        dump();
        if (!check.ok(PoplarContent.enabled() && PoplarContent.set != null, "stream on and registered")) {
            return;
        }
        registration();
        numbers();
        recipes();
        stripping();
        sapling();
        trees();
        fallen();
        shrub();
        mushroom();
        bounce();
        biome();
    }

    // ---------------------------------------------------------------------------------------------

    private void registration() {
        for (String name : BLOCKS) {
            Block b = ForgeRegistries.BLOCKS.getValue(Content.id(name));
            check.ok(b != null && b != Blocks.AIR && Content.id(name).equals(b.getRegistryName()), "block " + name);
            check.eq(GameDropsBackport.MOD_ID, BlockOwnership.owner(name), "owner of " + name);
        }
        for (String name : new String[] {"poplar_door", "poplar_sign", "poplar_boat", "red_poplar_leaves", "poplar_sapling",
                "red_shrub", "shelf_mushroom", "poplar_slab"}) {
            check.ok(ForgeRegistries.ITEMS.containsKey(Content.id(name)), "item " + name);
        }
        check.ok(!ForgeRegistries.ITEMS.containsKey(Content.id("poplar_wall_sign")), "no wall sign item");
        check.ok(!ForgeRegistries.ITEMS.containsKey(Content.id("poplar_hanging_sign")), "no hanging sign of ours (Trails & Tales')");
        check.ok(!ForgeRegistries.BLOCKS.containsKey(Content.id("poplar_shelf")), "no shelf of ours (Tricky Trials')");
        boolean wild = Loader.isModLoaded("wildupdatebackport");
        check.eq(wild, ForgeRegistries.ITEMS.containsKey(Content.id("poplar_chest_boat")), "chest boat only with Wild Update");
        check.ok(!ForgeRegistries.ENTITIES.containsKey(Content.id("boat")) && !ForgeRegistries.ENTITIES.containsKey(Content.id("chest_boat")),
                "no boat entities of our own: the boats are Backport Core's");
        com.exiledradio.backportcore.boat.BoatWood poplar = BoatWood.POPLAR.core();
        check.ok(poplar != null && poplar.boatItem() == BoatContent.item(BoatWood.POPLAR, false), "poplar is Core's wood, with our boat");
        check.ok(poplar != null && (poplar.chestBoatItem() != null) == wild
                && poplar.chestBoatItem() == BoatContent.item(BoatWood.POPLAR, true), "and our chest boat with Wild Update");
        check.eq(com.exiledradio.backportcore.boat.Boats.BOAT, com.exiledradio.backportcore.boat.Boats.renamedTo(Content.id("boat")),
                "our old boats load as Core's");
        check.eq(com.exiledradio.backportcore.boat.Boats.CHEST_BOAT, com.exiledradio.backportcore.boat.Boats.renamedTo(Content.id("chest_boat")),
                "our old chest boats load as Core's");
        check.ok(PoplarContent.biome != null && ForgeRegistries.BIOMES.containsKey(Content.id("dappled_forest")), "biome registered");
        check.ok(TradeRegistry.traderTrades(TradeRegistry.TraderPool.ORDINARY).size() >= 2
                && TradeRegistry.traderTrades(TradeRegistry.TraderPool.SPECIAL).size() >= 1, "trader trades added");
    }

    private void numbers() {
        WoodSet set = PoplarContent.set;
        check.eq(300, burn(new ItemStack(set.planks)), "planks burn");
        check.eq(150, burn(new ItemStack(set.slab)), "slab burns");
        check.eq(100, burn(new ItemStack(PoplarContent.sapling)), "sapling burns");
        check.eq(1200, burn(new ItemStack(BoatContent.item(BoatWood.POPLAR, false))), "boat burns");
        check.eq(30, Blocks.FIRE.getEncouragement(PoplarContent.redLeaves), "leaves catch fire");
        check.eq(60, Blocks.FIRE.getFlammability(PoplarContent.yellowLeaves), "leaves burn");
        check.eq(60, Blocks.FIRE.getEncouragement(PoplarContent.redShrub), "red shrub catches fire");
        check.eq(100, Blocks.FIRE.getFlammability(PoplarContent.redShrub), "red shrub burns");
        check.eq(5, Blocks.FIRE.getFlammability(set.log), "log burns");
        check.eq(2.0F, set.planks.getDefaultState().getBlockHardness(world, BlockPos.ORIGIN), "planks hardness");
        check.eq(0.0F, PoplarContent.shelfMushroom.getDefaultState().getBlockHardness(world, BlockPos.ORIGIN), "mushroom breaks at once");
        check.eq(0.0F, PoplarContent.redShrub.getDefaultState().getBlockHardness(world, BlockPos.ORIGIN), "shrub breaks at once");
        for (Block b : new Block[] {PoplarContent.redLeaves, PoplarContent.sapling, PoplarContent.shelfMushroom, set.log, set.door,
                set.trapdoor, set.button}) {
            for (IBlockState s : b.getBlockState().getValidStates()) {
                IBlockState back = b.getStateFromMeta(b.getMetaFromState(s));
                if (!keyEquals(s, back, b)) {
                    check.fail("metadata round trip " + s);
                    break;
                }
            }
        }
        check.ok(true, "metadata round trips");
        check.ok(Arrays.asList(ids("plankWood")).contains(Item.getItemFromBlock(set.planks)), "plankWood ore name");
        check.ok(Arrays.asList(ids("logWood")).contains(Item.getItemFromBlock(set.log)), "logWood ore name");
        check.ok(Arrays.asList(ids("treeLeaves")).contains(Item.getItemFromBlock(PoplarContent.orangeLeaves)), "treeLeaves ore name");
    }

    /** Door and button states are partly worked out from neighbors; compare what metadata keeps. */
    private static boolean keyEquals(IBlockState a, IBlockState b, Block block) {
        return block.getMetaFromState(a) == block.getMetaFromState(b);
    }

    private static Item[] ids(String ore) {
        List<Item> out = new ArrayList<Item>();
        for (ItemStack s : net.minecraftforge.oredict.OreDictionary.getOres(ore)) {
            out.add(s.getItem());
        }
        return out.toArray(new Item[0]);
    }

    private static int burn(ItemStack stack) {
        return net.minecraft.tileentity.TileEntityFurnace.getItemBurnTime(stack);
    }

    private void recipes() {
        WoodSet set = PoplarContent.set;
        check.eq(4, craft(new ItemStack(set.log)).getCount(), "planks from a log");
        check.ok(craft(new ItemStack(set.log)).getItem() == Item.getItemFromBlock(set.planks), "log makes poplar planks");
        check.ok(craft(new ItemStack(set.strippedWood)).getItem() == Item.getItemFromBlock(set.planks), "stripped wood makes planks");
        ItemStack p = new ItemStack(set.planks);
        check.ok(craft(p, ItemStack.EMPTY, p, p, p, p, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY).getItem()
                == BoatContent.item(BoatWood.POPLAR, false), "boat from five planks");
        check.ok(craft(new ItemStack(Items.BAKED_POTATO), new ItemStack(Items.COOKED_RABBIT), new ItemStack(Items.BOWL),
                new ItemStack(Items.CARROT), new ItemStack(PoplarContent.shelfMushroom)).getItem() == Items.RABBIT_STEW,
                "rabbit stew from a shelf mushroom");
        check.ok(craft(p, p, p, p, p, p, ItemStack.EMPTY, new ItemStack(Items.STICK), ItemStack.EMPTY).getItem() == set.signItem,
                "sign");
        check.eq(Item.getItemFromBlock(Blocks.PLANKS), craft(new ItemStack(Blocks.LOG)).getItem(), "vanilla planks still vanilla");
    }

    private ItemStack craft(ItemStack... grid) {
        InventoryCrafting inv = new InventoryCrafting(new net.minecraft.inventory.Container() {
            @Override
            public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer player) {
                return false;
            }
        }, 3, 3);
        for (int i = 0; i < grid.length; i++) {
            inv.setInventorySlotContents(i, grid[i]);
        }
        IRecipe r = CraftingManager.findMatchingRecipe(inv, world);
        return r == null ? ItemStack.EMPTY : r.getCraftingResult(inv);
    }

    private void stripping() {
        WoodSet set = PoplarContent.set;
        IBlockState log = set.log.getDefaultState().withProperty(BlockRotatedPillar.AXIS, EnumFacing.Axis.X);
        IBlockState out = AxeStripping.stripped(log);
        check.ok(out != null && out.getBlock() == set.strippedLog && out.getValue(BlockRotatedPillar.AXIS) == EnumFacing.Axis.X,
                "axe strips a poplar log, keeping the axis");
        IBlockState wood = AxeStripping.stripped(set.wood.getDefaultState());
        check.ok(wood != null && wood.getBlock() == set.strippedWood, "axe strips poplar wood");
    }

    // ---------------------------------------------------------------------------------------------
    // Scratch area: high in the sky over the spawn, cleared to air with a grass floor.

    private static boolean surrounded(Biome[] map, int side, int x, int z, int r, Biome biome) {
        for (int dz = -r; dz <= r; dz++) {
            for (int dx = -r; dx <= r; dx++) {
                int xx = x + dx;
                int zz = z + dz;
                if (xx < 0 || zz < 0 || xx >= side || zz >= side || map[xx + zz * side] != biome) {
                    return false;
                }
            }
        }
        return true;
    }

        private BlockPos area(int index) {
        BlockPos spawn = world.getSpawnPoint();
        return new BlockPos(spawn.getX() + 64 + index * 48, 150, spawn.getZ() + 64);
    }

    private void floor(BlockPos center, int r, int height) {
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                world.setBlockState(center.add(x, -1, z), Blocks.GRASS.getDefaultState(), 2);
                for (int y = 0; y < height; y++) {
                    world.setBlockState(center.add(x, y, z), Blocks.AIR.getDefaultState(), 2);
                }
                world.setBlockState(center.add(x, -2, z), Blocks.DIRT.getDefaultState(), 2);
            }
        }
    }

    private int count(BlockPos center, int r, int height, Block... blocks) {
        List<Block> want = Arrays.asList(blocks);
        int n = 0;
        for (BlockPos p : BlockPos.getAllInBoxMutable(center.add(-r, -1, -r), center.add(r, height, r))) {
            if (want.contains(world.getBlockState(p).getBlock())) {
                n++;
            }
        }
        return n;
    }

    private void sapling() {
        BlockPos at = area(0);
        floor(at, 12, 24);
        BlockPoplarSapling sapling = PoplarContent.sapling;
        world.setBlockState(at, sapling.getDefaultState(), 2);
        sapling.grow(world, new Random(1), at, sapling.getDefaultState());
        check.eq(1, world.getBlockState(at).getValue(BlockPoplarSapling.STAGE), "first growth: stage 1");
        boolean grown = false;
        for (int i = 0; i < 4 && !grown; i++) {
            sapling.grow(world, new Random(10 + i), at, world.getBlockState(at));
            grown = world.getBlockState(at).getBlock() == PoplarContent.set.log;
        }
        check.ok(grown, "a stage 1 sapling grows a poplar");
        check.ok(world.getBlockState(at.down()).getBlock() == Blocks.DIRT, "grass under the trunk became dirt");
        int logs = count(at, 10, 22, PoplarContent.set.log);
        check.ok(logs >= 8 && logs <= 40, "poplar logs: " + logs);
        int leaves = count(at, 10, 22, PoplarContent.redLeaves, PoplarContent.orangeLeaves, PoplarContent.yellowLeaves);
        check.ok(leaves > 60, "poplar leaves: " + leaves);
        check.ok(!sapling.canGrow(world, new BlockPos(0, 250, 0), sapling.getDefaultState(), false), "no bone meal near the top");
        // A sapling under a ceiling does not grow and stays.
        BlockPos low = at.add(20, 0, 0);
        floor(low, 3, 4);
        world.setBlockState(low.up(3), Blocks.STONE.getDefaultState(), 2);
        IBlockState stage1 = sapling.getDefaultState().withProperty(BlockPoplarSapling.STAGE, 1);
        world.setBlockState(low, stage1, 2);
        sapling.grow(world, new Random(3), low, stage1);
        check.ok(world.getBlockState(low).getBlock() == sapling, "a sapling with no room stays a sapling");
    }

    private void trees() {
        int shelves = 0;
        int branchesOut = 0;
        int min = 99;
        int max = 0;
        for (int i = 0; i < 24; i++) {
            BlockPos at = area(1 + i % 6).add(0, 0, (i / 6) * 40);
            floor(at, 12, 26);
            BlockPoplarLeaves.Color c = BlockPoplarLeaves.Color.values()[i % 3];
            boolean placed = PoplarFeatures.tree(world, new Random(100 + i), at, c, i % 2 == 0);
            if (!check.ok(placed, "tree " + i + " placed")) {
                continue;
            }
            int h = 0;
            while (world.getBlockState(at.up(h)).getBlock() == PoplarContent.set.log) {
                h++;
            }
            min = Math.min(min, h);
            max = Math.max(max, h);
            shelves += count(at, 3, 8, PoplarContent.shelfMushroom);
            for (BlockPos p : BlockPos.getAllInBoxMutable(at.add(-1, 0, -1), at.add(1, 12, 1))) {
                IBlockState s = world.getBlockState(p);
                if (s.getBlock() == PoplarContent.set.log && s.getValue(BlockRotatedPillar.AXIS) != EnumFacing.Axis.Y) {
                    branchesOut++;
                }
            }
            Block leaves = c == BlockPoplarLeaves.Color.RED ? PoplarContent.redLeaves
                    : c == BlockPoplarLeaves.Color.ORANGE ? PoplarContent.orangeLeaves : PoplarContent.yellowLeaves;
            check.ok(count(at, 10, 24, leaves) > 40, "tree " + i + " has its color's leaves");
            IBlockState anyLeaf = null;
            for (BlockPos p : BlockPos.getAllInBoxMutable(at.add(-1, 4, -1), at.add(1, 20, 1))) {
                if (world.getBlockState(p).getBlock() == leaves) {
                    anyLeaf = world.getBlockState(p);
                    break;
                }
            }
            check.ok(anyLeaf != null && anyLeaf.getValue(BlockPoplarLeaves.DECAYABLE), "tree " + i + " leaves decay");
        }
        check.ok(min >= 7 && max <= 11, "trunk heights 7-11: " + min + "-" + max);
        check.ok(branchesOut > 0, "sideways branch logs: " + branchesOut);
        check.ok(shelves > 0, "shelf mushrooms on trunks: " + shelves);
    }

    private void fallen() {
        int logsLying = 0;
        int stumps = 0;
        for (int i = 0; i < 12; i++) {
            // Generated and populated first: a lake or tree decorated afterwards would land on the logs.
            BlockPos c = area(8 + i % 6).add(0, 0, 200 + (i / 6) * 30);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    world.getChunkProvider().provideChunk((c.getX() >> 4) + dx, (c.getZ() >> 4) + dz);
                }
            }
        }
        for (int i = 0; i < 12; i++) {
            BlockPos at = area(8 + i % 6).add(0, 0, 200 + (i / 6) * 30);
            floor(at, 10, 4);
            PoplarFeatures.fallenTree(world, new Random(500 + i), at);
            IBlockState stump = world.getBlockState(at);
            if (stump.getBlock() == PoplarContent.set.log && stump.getValue(BlockRotatedPillar.AXIS) == EnumFacing.Axis.Y) {
                stumps++;
            }
            for (BlockPos p : BlockPos.getAllInBoxMutable(at.add(-8, 0, -8), at.add(8, 1, 8))) {
                IBlockState s = world.getBlockState(p);
                if (s.getBlock() == PoplarContent.set.log && s.getValue(BlockRotatedPillar.AXIS) != EnumFacing.Axis.Y) {
                    logsLying++;
                }
            }
        }
        check.eq(12, stumps, "every fallen poplar has its stump");
        check.ok(logsLying >= 12 * 2, "fallen logs lying: " + logsLying);
    }

    private void shrub() {
        BlockPos at = area(14).add(0, 0, 60);
        floor(at, 3, 3);
        world.setBlockState(at, PoplarContent.redShrub.getDefaultState(), 2);
        check.ok(PoplarContent.redShrub.canGrow(world, at, world.getBlockState(at), false), "bone meal can spread it");
        PoplarContent.redShrub.grow(world, new Random(1), at, world.getBlockState(at));
        check.eq(2, count(at, 1, 0, PoplarContent.redShrub), "one bone meal: one more shrub beside");
        for (int i = 0; i < 6; i++) {
            BlockPos p = at.offset(EnumFacing.byHorizontalIndex(i % 4));
            world.setBlockState(p, PoplarContent.redShrub.getDefaultState(), 2);
        }
        check.ok(!PoplarContent.redShrub.canGrow(world, at, world.getBlockState(at), false), "surrounded: no spread");
        check.ok(PoplarContent.redShrub.isReplaceable(world, at), "replaceable");
        check.ok(PoplarContent.redShrub.getDefaultState().getCollisionBoundingBox(world, at) == null, "no collision");
    }

    private void mushroom() {
        BlockShelfMushroom m = PoplarContent.shelfMushroom;
        BlockPos at = area(15).add(0, 0, 60);
        floor(at, 3, 4);
        BlockPos wall = at.south();
        world.setBlockState(wall, PoplarContent.set.log.getDefaultState(), 2);
        check.ok(BlockShelfMushroom.canStay(world, at, EnumFacing.NORTH), "stands on a log behind it");
        check.ok(!BlockShelfMushroom.canStay(world, at, EnumFacing.SOUTH), "not facing into the log");
        IBlockState s = m.getDefaultState().withProperty(BlockHorizontal.FACING, EnumFacing.NORTH);
        world.setBlockState(at, s, 3);
        check.ok(m.canGrow(world, at, s, false), "a small one takes bone meal");
        m.grow(world, new Random(1), at, s);
        check.eq(1, world.getBlockState(at).getValue(BlockShelfMushroom.AGE), "bone meal: big");
        check.ok(!m.canGrow(world, at, world.getBlockState(at), false), "a big one takes no more");
        NonNullList<ItemStack> drops = NonNullList.create();
        m.getDrops(drops, world, at, world.getBlockState(at), 0);
        int dropped = 0;
        for (ItemStack d : drops) {
            dropped += d.getCount();
        }
        check.eq(2, dropped, "a big one drops two");
        world.setBlockToAir(wall);
        check.ok(world.getBlockState(at).getBlock() != m, "pops off without its support");
        check.ok(m.getDefaultState().getCollisionBoundingBox(world, at) != null, "it has collision");
    }

    private void bounce() {
        BlockShelfMushroom m = PoplarContent.shelfMushroom;
        EntityPig pig = new EntityPig(world);
        pig.motionY = -0.8D;
        m.onLanded(world, pig);
        check.ok(Math.abs(pig.motionY - 0.6D) < 1e-6, "bounced back at 0.75: " + pig.motionY);
        pig.motionY = -0.05D;
        m.onLanded(world, pig);
        check.ok(pig.motionY <= 0.0D, "a slow landing does not bounce");
        net.minecraft.entity.item.EntityItem item = new net.minecraft.entity.item.EntityItem(world, 0, 100, 0, new ItemStack(Items.STICK));
        item.motionY = -1.0D;
        m.onLanded(world, item);
        check.ok(Math.abs(item.motionY - 0.6D) < 1e-6, "not alive: 0.8 of that: " + item.motionY);
    }

    // ---------------------------------------------------------------------------------------------

    private void biome() {
        BiomeDappledForest dappled = PoplarContent.biome;
        if (!check.ok(dappled != null, "biome on")) {
            return;
        }
        check.eq(0.6F, dappled.getDefaultTemperature(), "temperature");
        check.eq(0.6F, dappled.getRainfall(), "downfall");
        check.ok(dappled.getSpawnableList(net.minecraft.entity.EnumCreatureType.CREATURE).size() >= 5, "animals spawn");
        if (com.exiledradio.backportcore.terrain.ModernGeneration.biomeId(world, BlockPos.ORIGIN) != null) {
            // Caves & Cliffs' 1.18 biome source does not use 1.12.2's layers (CORE_REQUESTS.md 4).
            GameDropsBackport.LOGGER.info("SELFTEST poplar biome map: skipped on Caves & Cliffs' 1.18 terrain");
            return;
        }
        // The 1:4 biome map, 6144 blocks around the spawn.
        int half = 768;
        BlockPos spawn = world.getSpawnPoint();
        Biome[] map = world.getBiomeProvider().getBiomesForGeneration(null, (spawn.getX() >> 2) - half, (spawn.getZ() >> 2) - half,
                half * 2, half * 2);
        int dap = 0;
        int plains = 0;
        int coolNear = 0;
        int warmNear = 0;
        int side = half * 2;
        BlockPos found = null;
        for (int z = 1; z < side - 1; z++) {
            for (int x = 1; x < side - 1; x++) {
                Biome b = map[x + z * side];
                if (b == Biomes.PLAINS) {
                    plains++;
                }
                if (b != dappled) {
                    continue;
                }
                dap++;
                // A cell deep in the forest (9 by 9 cells of it, 36 blocks), so the patch is decorated as dappled forest
                // whatever the biome layout around it (1.12.2's layers, or the modern ones once a 1.13+ version is installed).
                if (found == null && x > 64 && z > 64 && x < side - 64 && z < side - 64 && surrounded(map, side, x, z, 4, dappled)) {
                    found = new BlockPos(((spawn.getX() >> 2) - half + x) << 2, 0, ((spawn.getZ() >> 2) - half + z) << 2);
                }
                for (Biome n : new Biome[] {map[x - 1 + z * side], map[x + 1 + z * side], map[x + (z - 1) * side], map[x + (z + 1) * side]}) {
                    if (n == Biomes.TAIGA || n == Biomes.EXTREME_HILLS || n == Biomes.FOREST || n == Biomes.REDWOOD_TAIGA
                            || n == Biomes.ICE_PLAINS || n == Biomes.COLD_TAIGA || n == Biomes.TAIGA_HILLS) {
                        coolNear++;
                    }
                    if (n == Biomes.DESERT || n == Biomes.SAVANNA || n == Biomes.JUNGLE || n == Biomes.SWAMPLAND
                            || n == Biomes.ROOFED_FOREST || n == Biomes.MESA) {
                        warmNear++;
                    }
                }
            }
        }
        GameDropsBackport.LOGGER.info("SELFTEST poplar biome map: dappled {} plains {} cool neighbors {} warm neighbors {}",
                dap, plains, coolNear, warmNear);
        check.ok(dap > 0, "dappled forest on the biome map: " + dap + " cells");
        check.ok(plains > 0, "plains stay: " + plains + " cells");
        check.ok(coolNear >= warmNear, "beside cool biomes more than warm ones: " + coolNear + " vs " + warmNear);
        if (found == null) {
            return;
        }
        // Generate and decorate a 6x6 chunk patch there: poplars, red shrubs, the surface.
        int cx = found.getX() >> 4;
        int cz = found.getZ() >> 4;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                world.getChunkProvider().provideChunk(cx + dx, cz + dz);
            }
        }
        int logs = 0;
        int leaves = 0;
        int shrubs = 0;
        int mushrooms = 0;
        int inBiome = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        int bx = ((cx + dx) << 4) + x;
                        int bz = ((cz + dz) << 4) + z;
                        if (world.getBiome(new BlockPos(bx, 64, bz)) == dappled) {
                            inBiome++;
                        }
                        int top = world.getHeight(bx, bz);
                        for (int y = Math.max(1, top - 24); y <= top + 2; y++) {
                            Block b = world.getBlockState(new BlockPos(bx, y, bz)).getBlock();
                            if (b == PoplarContent.set.log) {
                                logs++;
                            } else if (b instanceof BlockPoplarLeaves) {
                                leaves++;
                            } else if (b == PoplarContent.redShrub) {
                                shrubs++;
                            } else if (b == PoplarContent.shelfMushroom) {
                                mushrooms++;
                            }
                        }
                    }
                }
            }
        }
        GameDropsBackport.LOGGER.info("SELFTEST poplar decoration near {}: columns in biome {} logs {} leaves {} shrubs {} shelf mushrooms {}",
                found, inBiome, logs, leaves, shrubs, mushrooms);
        check.ok(inBiome > 0, "the decorated patch is dappled forest");
        check.ok(logs > 20 && leaves > 200, "poplars grow there: " + logs + " logs, " + leaves + " leaves");
        check.ok(shrubs > 0, "red shrubs grow there: " + shrubs);
    }

    // ---------------------------------------------------------------------------------------------
    // For tools/check_assets.py

    private void dump() throws Exception {
        net.minecraft.server.MinecraftServer server = world.getMinecraftServer();
        PrintWriter out = SelfTestRunner.statesOut();
        for (Block block : Content.blocks()) {
            String kind = Content.multipartFiles().contains(block) ? "multipart" : "variants";
            for (IBlockState state : block.getBlockState().getValidStates()) {
                out.println(block.getRegistryName() + "\t" + variant(state) + "\t" + kind);
            }
        }
        out.close();
        out = SelfTestRunner.itemsOut();
        List<Item> seen = new ArrayList<Item>();
        for (Map.Entry<Item, Map<Integer, String>> e : Content.itemModels().entrySet()) {
            seen.add(e.getKey());
            for (Map.Entry<Integer, String> m : e.getValue().entrySet()) {
                ItemStack stack = new ItemStack(e.getKey(), 1, m.getKey());
                out.println(e.getKey().getRegistryName() + "\t" + m.getKey() + "\t" + m.getValue() + "\t"
                        + e.getKey().getTranslationKey(stack) + ".name");
            }
        }
        out.close();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String variant(IBlockState state) {
        Map<String, String> sorted = new TreeMap<String, String>();
        IProperty<?>[] ignored = Content.ignoredProperties().get(state.getBlock());
        List<IProperty<?>> skip = ignored == null ? java.util.Collections.<IProperty<?>>emptyList() : Arrays.asList(ignored);
        for (Map.Entry<IProperty<?>, Comparable<?>> e : state.getProperties().entrySet()) {
            if (skip.contains(e.getKey())) {
                continue;
            }
            IProperty p = e.getKey();
            sorted.put(p.getName(), p.getName((Comparable) e.getValue()));
        }
        if (sorted.isEmpty()) {
            return "normal";
        }
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            if (b.length() > 0) {
                b.append(',');
            }
            b.append(e.getKey()).append('=').append(e.getValue());
        }
        return b.toString();
    }
}
