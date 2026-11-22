package com.exiledradio.gamedropsbackport.selftest;

import com.exiledradio.backportcore.locate.LocateChecks;
import com.exiledradio.backportcore.locate.StructureLocate;
import com.exiledradio.backportcore.structure.JigsawPools;
import com.exiledradio.backportcore.structure.ScatteredStructure;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.camps.AbandonedCampStructure;
import com.exiledradio.gamedropsbackport.camps.CampBees;
import com.exiledradio.gamedropsbackport.camps.CampLoot;
import com.exiledradio.gamedropsbackport.camps.CampPiece;
import com.exiledradio.gamedropsbackport.camps.CampSettings;
import com.exiledradio.gamedropsbackport.camps.CampsContent;
import com.exiledradio.gamedropsbackport.camps.ExplorerMaps;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraft.world.storage.MapData;
import net.minecraft.world.storage.MapDecoration;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * Stream 7: abandoned camps and explorer maps. The data is 26.3's; the layout is laid out and then generated for
 * real, the loot rolled, and maps made and upgraded on the running server.
 */
final class CampsSelfTest implements SelfTest {

    @Override
    public String name() {
        return "camps";
    }

    @Override
    public void run(Check check) throws Exception {
        WorldServer world = check.world;
        check.ok(CampsContent.enabled(), "camps enabled");
        oracle(check, world);

        // --- 26.3's files
        CampSettings s = CampSettings.get();
        check.eq(37, s.spacing, "structure set spacing");
        check.eq(8, s.separation, "structure set separation");
        check.eq(91231127, s.salt, "structure set salt");
        check.eq(18, s.variants.size(), "18 camp structures");
        for (CampSettings.Variant v : s.variants) {
            check.eq(2, v.maxDepth, v.id + " size");
            check.ok(v.expansionHack && v.projectToSurface, v.id + " expansion hack and surface projection");
            check.eq(Collections.singleton("minecraft:" + v.name), v.biomes, v.id + " biome tag");
            JigsawPools.Pool pool = JigsawPools.pool(v.startPool);
            check.ok(pool != null && pool.size() > 0, v.id + " start pool " + v.startPool);
            JigsawPools.Pool camp = JigsawPools.pool(new ResourceLocation("minecraft", "abandoned_camp/camp/" + v.name));
            check.ok(camp != null && camp.size() >= 45, v.id + " camp pool");
        }

        // --- what the installed mods leave out of the templates (informational: it depends on the family mods present)
        java.util.Set<String> missing = new java.util.TreeSet<String>();
        int templates = 0;
        for (CampSettings.Variant v : s.variants) {
            for (String kind : new String[] {"tent", "camp"}) {
                JigsawPools.Pool pool = JigsawPools.pool(new ResourceLocation("minecraft", "abandoned_camp/" + kind + "/" + v.name));
                for (com.exiledradio.backportcore.structure.JigsawElement e : pool.elements()) {
                    if (e.template() != null) {
                        templates++;
                        missing.addAll(com.exiledradio.backportcore.structure.ModernTemplate.load(JigsawPools.templateId(e.template())).unresolvedBlocks());
                    }
                }
            }
        }
        GameDropsBackport.LOGGER.info("SELFTEST camps: {} template uses; blocks no installed mod has: {}", templates, missing);
        check.ok(templates > 297, "every template loads");

        // --- the grid and the choice: candidates near spawn, by variant
        ScatteredStructure structure = ScatteredStructure.of(world, CampsContent.KIND);
        check.ok(structure instanceof AbandonedCampStructure, "the world has abandoned camps");
        Map<String, ChunkPos> found = new TreeMap<String, ChunkPos>();
        int candidates = 0;
        for (int gx = -8; gx <= 8; gx++) {
            for (int gz = -8; gz <= 8; gz++) {
                ChunkPos c = s.grid.candidate(world.getSeed(), gx * s.spacing, gz * s.spacing);
                candidates++;
                AbandonedCampStructure.Choice choice = AbandonedCampStructure.choose(world, c.x, c.z);
                if (choice != null && !found.containsKey(choice.variant.id)) {
                    found.put(choice.variant.id, c);
                }
            }
        }
        GameDropsBackport.LOGGER.info("SELFTEST camps: {} candidates, variants found {}", candidates, found);
        check.ok(found.size() >= 3, "camps of several variants within 8 regions of spawn: " + found.keySet());
        if (found.isEmpty()) {
            return;
        }

        // --- determinism: the same chunk lays out the same camp
        Map.Entry<String, ChunkPos> first = found.entrySet().iterator().next();
        ChunkPos at = first.getValue();
        List<CampPiece> a = AbandonedCampStructure.layout(world, at.x, at.z);
        List<CampPiece> b = AbandonedCampStructure.layout(world, at.x, at.z);
        check.eq(a.size(), b.size(), "layout repeats: piece count");
        check.ok(a.size() >= 2, "a tent and its camp: " + a.size() + " pieces");
        boolean same = a.size() == b.size();
        for (int i = 0; same && i < a.size(); i++) {
            same = a.get(i).getBoundingBox().toString().equals(b.get(i).getBoundingBox().toString())
                    && String.valueOf(a.get(i).element()).equals(String.valueOf(b.get(i).element()));
        }
        check.ok(same, "layout repeats piece for piece");
        check.ok(String.valueOf(a.get(0).element()).contains("abandoned_camp/tent/"), "the start is a tent: " + a.get(0).element());
        boolean campPiece = false;
        for (CampPiece p : a) {
            campPiece |= String.valueOf(p.element()).contains("abandoned_camp/camp/");
        }
        check.ok(campPiece, "a camp piece joins the tent");

        // --- locate: the nearest of that variant is a candidate chunk with that variant
        BlockPos from = new BlockPos((at.x << 4) + 8, 64, (at.z << 4) + 8);
        StructureLocate.Found loc = StructureLocate.nearest(world, from, first.getKey());
        check.ok(loc != null, "/locate finds " + first.getKey());
        if (loc != null) {
            check.eq(first.getKey(), loc.id, "/locate names the variant");
            String ring = LocateChecks.checkRingOrder(world, from, first.getKey(), loc);
            check.ok(ring == null, "/locate ring order: " + ring);
        }

        // --- generate it for real
        LocateChecks.generateAround(world, at.x, at.z, 4);
        StructureStart start = LocateChecks.savedStart(structure, at.x, at.z);
        check.ok(start != null && start.isSizeableStructure(), "the start was saved by the populate step");
        if (start != null && start.getBoundingBox() != null) {
            StructureBoundingBox box = start.getBoundingBox();
            int chests = 0, looted = 0, wool = 0, jigsaws = 0;
            for (BlockPos p : BlockPos.getAllInBoxMutable(new BlockPos(box.minX, box.minY, box.minZ), new BlockPos(box.maxX, box.maxY, box.maxZ))) {
                Block block = world.getBlockState(p).getBlock();
                ResourceLocation name = block.getRegistryName();
                if (block == Blocks.CHEST) {
                    chests++;
                    TileEntity te = world.getTileEntity(p);
                    if (te instanceof TileEntityChest && ((TileEntityChest) te).getLootTable() != null
                            && GameDropsBackport.MOD_ID.equals(((TileEntityChest) te).getLootTable().getNamespace())) {
                        looted++;
                    }
                }
                if (block == Blocks.WOOL || name.getPath().endsWith("wool_stairs")) {
                    wool++;
                }
                if (name.getPath().equals("structure_block") || name.getPath().equals("jigsaw")) {
                    jigsaws++;
                }
            }
            GameDropsBackport.LOGGER.info("SELFTEST camps: {} at {} box {}: chests {} ({} with our loot), wool {}", first.getKey(), at, box,
                    chests, looted, wool);
            check.ok(wool >= 20, "the tent's cloth was placed: " + wool);
            check.eq(chests, looted, "every chest names a camp loot table");
            check.eq(0, jigsaws, "no jigsaw or structure blocks left");
            List<EntityItem> drops = world.getEntitiesWithinAABB(EntityItem.class,
                    new AxisAlignedBB(box.minX - 2, box.minY - 2, box.minZ - 2, box.maxX + 3, box.maxY + 3, box.maxZ + 3));
            check.eq(0, drops.size(), "nothing dropped as an item while placing");
        }

        // --- loot
        Random random = new Random(1013);
        int common = 0, maps = 0;
        for (int i = 0; i < 20; i++) {
            List<ItemStack> roll = CampLoot.roll(world, from, "chests/abandoned_camp_common_chest", random);
            common += roll.size();
            for (ItemStack st : roll) {
                maps += st.getItem().getRegistryName().getPath().equals("abandoned_camp_map") ? 1 : 0;
            }
        }
        check.ok(common >= 20 * 6, "common chest rolls 7-9 stacks: " + common + " in 20");
        GameDropsBackport.LOGGER.info("SELFTEST camps: 20 common chests, {} stacks, {} camp maps", common, maps);
        int barrel = CampLoot.roll(world, from, "barrels/abandoned_camp_barrel", random).size();
        check.ok(barrel >= 4, "barrel rolls 5-9 stacks: " + barrel);
        check.ok(CampLoot.roll(world, from, "chests/abandoned_camp_secret_chest", random).size() >= 5, "secret chest rolls");

        // --- explorer maps
        check.eq(16, ExplorerMaps.names().size(), "16 explorer map items");
        for (String n : ExplorerMaps.names()) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(GameDropsBackport.MOD_ID, n));
            check.ok(item instanceof ItemMap, n + " is a map item");
            check.ok(ExplorerMaps.type(n) != null, n + " has a decoration type");
        }
        Item mineshaftMap = ForgeRegistries.ITEMS.getValue(new ResourceLocation(GameDropsBackport.MOD_ID, "buried_mineshaft_map"));
        ItemStack map = ExplorerMaps.create(world, from, mineshaftMap, "#minecraft:on_mineshaft_maps", "minecraft:mineshaft", 50, true);
        if (check.ok(map != null, "a buried mineshaft map is made (mineshafts exist)")) {
            check.ok(map.getItem() == mineshaftMap, "made as the buried mineshaft map item");
            MapData data = ((ItemMap) mineshaftMap).getMapData(map, world);
            check.ok(data != null && data.scale == 2, "zoom 2 map data");
            NBTTagList deco = map.getTagCompound().getTagList("Decorations", 10);
            check.ok(deco.tagCount() == 1 && "+".equals(deco.getCompoundTagAt(0).getString("id"))
                    && deco.getCompoundTagAt(0).getByte("type") == (byte) ExplorerMaps.type("buried_mineshaft_map").ordinal(), "the '+' decoration");
        }
        Item campMap = ForgeRegistries.ITEMS.getValue(new ResourceLocation(GameDropsBackport.MOD_ID, "abandoned_camp_map"));
        String tag = "#minecraft:on_abandoned_camp_" + first.getKey().substring("minecraft:abandoned_camp_".length());
        ItemStack toCamp = ExplorerMaps.create(world, from, campMap, first.getKey(), "minecraft:abandoned_camp", 50, true);
        check.ok(toCamp != null, "a camp map to " + first.getKey() + " (" + tag + ")");
        // 26.3's skip_existing_chunks: the next map leads to another camp (Backport Core's structure claims).
        ItemStack next = ExplorerMaps.create(world, from, campMap, first.getKey(), "minecraft:abandoned_camp", 50, true);
        if (check.ok(toCamp != null && next != null, "a second camp map")) {
            NBTTagCompound da = toCamp.getTagCompound().getTagList("Decorations", 10).getCompoundTagAt(0);
            NBTTagCompound db = next.getTagCompound().getTagList("Decorations", 10).getCompoundTagAt(0);
            check.ok(da.getDouble("x") != db.getDouble("x") || da.getDouble("z") != db.getDouble("z"),
                    "the second map leads to another camp (skip_existing_chunks)");
        }
        // Today's item names through Backport Core's ModernItems: cobweb is 1.12.2's web, red_dye is dye:1.
        check.ok(CampLoot.stack("minecraft:cobweb").getItem() == Item.getItemFromBlock(net.minecraft.init.Blocks.WEB), "cobweb is web");
        check.ok(CampLoot.stack("minecraft:red_dye").getMetadata() == 1, "red_dye is dye:1");
        // A structure no installed mod has: no map.
        check.ok(ExplorerMaps.create(world, from, campMap, "minecraft:no_such_structure", "minecraft:abandoned_camp", 50, true) == null,
                "no map where the structure does not exist");

        // --- 26.3's ExplorerMapItemFix: a 1.12.2 cartographer's ocean explorer map
        ItemStack old = ItemMap.setupNewMap(world, 0, 0, (byte) 2, true, true);
        MapData.addTargetDecoration(old, new BlockPos(100, 64, 100), "+", MapDecoration.Type.MONUMENT);
        old.setTranslatableName("filled_map.monument");
        int id = old.getMetadata();
        ItemStack upgraded = upgrade(old);
        check.eq("ocean_monument_map", upgraded.getItem().getRegistryName().getPath(), "a monument explorer map becomes the ocean monument map");
        check.eq(id, upgraded.getMetadata(), "the upgraded map keeps its id");
        check.ok(!upgraded.getTagCompound().getCompoundTag("display").hasKey("LocName"), "and loses its old name");
        ItemStack plain = ItemMap.setupNewMap(world, 0, 0, (byte) 0, true, false);
        check.ok(upgrade(plain).getItem() == Items.FILLED_MAP, "a plain map stays a filled map");

        gaps(check, world, from, toCamp != null ? toCamp : map);
    }

    /** What stream 7 first left out: the loot's origin (P6), map cloning, the {@code _bees} trees' nests. */
    private static void gaps(Check check, WorldServer world, BlockPos from, ItemStack explorerMap) throws Exception {
        // --- P6: container loot rolls at the container (26.3's ORIGIN), not at the player
        check.ok(com.exiledradio.gamedropsbackport.asm.LootOriginTransformer.appliedP6, "P6 applied (TileEntityLockableLoot.fillWithLoot)");
        ResourceLocation common = new ResourceLocation(GameDropsBackport.MOD_ID, "camps/chests/abandoned_camp_common_chest");
        BlockPos chestAt = world.getTopSolidOrLiquidBlock(from.add(40, 0, 40));
        world.setBlockState(chestAt, Blocks.CHEST.getDefaultState(), 2);
        TileEntityChest chest = (TileEntityChest) world.getTileEntity(chestAt);
        chest.setLootTable(common, 1013L);
        net.minecraftforge.common.util.FakePlayer player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(world);
        player.setPosition(from.getX() - 300, 120, from.getZ() - 300);
        CampLoot.lastOrigin = null;
        java.lang.reflect.Method fill = net.minecraft.tileentity.TileEntityLockableLoot.class.getDeclaredMethod(fillName(),
                net.minecraft.entity.player.EntityPlayer.class);
        fill.setAccessible(true);
        fill.invoke(chest, player);
        check.eq(chestAt, CampLoot.lastOrigin, "a chest opened by a player 300 blocks away rolls at the chest");
        check.ok(chest.getLootTable() == null && !chest.isEmpty(), "the chest was filled");
        world.setBlockState(chestAt.up(), Blocks.CHEST.getDefaultState(), 2);
        TileEntityChest pulled = (TileEntityChest) world.getTileEntity(chestAt.up());
        pulled.setLootTable(common, 7L);
        CampLoot.lastOrigin = null;
        pulled.getStackInSlot(0); // a hopper's or a comparator's read: fillWithLoot(null)
        check.eq(chestAt.up(), CampLoot.lastOrigin, "a chest filled with no player (a hopper) rolls at the chest");
        int campMaps = 0;
        for (int i = 0; i < 12; i++) {
            BlockPos p = chestAt.add(2 + i, 0, 0);
            world.setBlockState(p, Blocks.CHEST.getDefaultState(), 2);
            TileEntityChest c = (TileEntityChest) world.getTileEntity(p);
            c.setLootTable(common, 100L + i);
            for (int slot = 0; slot < c.getSizeInventory(); slot++) {
                ItemStack st = c.getStackInSlot(slot);
                campMaps += !st.isEmpty() && st.getItem().getRegistryName().getPath().equals("abandoned_camp_map") ? 1 : 0;
            }
        }
        GameDropsBackport.LOGGER.info("SELFTEST camps: 12 player-less common chests gave {} camp maps", campMaps);
        CampLoot.lastOrigin = null;
        check.ok(CampLoot.roll(world, from, "chests/abandoned_camp_common_chest", new Random(3)).size() > 0 && CampLoot.lastOrigin == null,
                "a roll outside a container leaves the origin alone");

        // --- 26.3's map_cloning for the explorer maps
        if (check.ok(explorerMap != null, "an explorer map to copy")) {
            net.minecraft.inventory.InventoryCrafting grid = new net.minecraft.inventory.InventoryCrafting(new net.minecraft.inventory.Container() {
                @Override
                public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer p) {
                    return true;
                }
            }, 3, 3);
            grid.setInventorySlotContents(4, explorerMap.copy());
            grid.setInventorySlotContents(0, new ItemStack(Items.MAP));
            grid.setInventorySlotContents(8, new ItemStack(Items.MAP));
            net.minecraft.item.crafting.IRecipe r = net.minecraft.item.crafting.CraftingManager.findMatchingRecipe(grid, world);
            check.ok(r instanceof com.exiledradio.gamedropsbackport.camps.ExplorerMapCloning, "explorer map + 2 empty maps: map cloning " + r);
            ItemStack out = r == null ? ItemStack.EMPTY : r.getCraftingResult(grid);
            check.ok(out.getItem() == explorerMap.getItem() && out.getCount() == 3 && out.getMetadata() == explorerMap.getMetadata()
                    && ItemStack.areItemStackTagsEqual(out, explorerMap), "three copies of the same map, data kept: " + out);
            for (int i = 0; i < 9; i++) {
                grid.setInventorySlotContents(i, i == 4 ? explorerMap.copy() : new ItemStack(Items.MAP));
            }
            r = net.minecraft.item.crafting.CraftingManager.findMatchingRecipe(grid, world);
            check.ok(r != null && r.getCraftingResult(grid).getCount() == 9, "8 empty maps make 9");
            grid.clear();
            grid.setInventorySlotContents(0, explorerMap.copy());
            check.ok(net.minecraft.item.crafting.CraftingManager.findMatchingRecipe(grid, world) == null, "no empty map: no recipe");
            grid.setInventorySlotContents(1, explorerMap.copy());
            grid.setInventorySlotContents(2, new ItemStack(Items.MAP));
            check.ok(net.minecraft.item.crafting.CraftingManager.findMatchingRecipe(grid, world) == null, "two explorer maps: no recipe");
            grid.clear();
            grid.setInventorySlotContents(0, explorerMap.copy());
            grid.setInventorySlotContents(1, new ItemStack(Items.PAPER));
            check.ok(net.minecraft.item.crafting.CraftingManager.findMatchingRecipe(grid, world) == null, "paper is not an empty map");
            grid.clear();
            grid.setInventorySlotContents(0, ItemMap.setupNewMap(world, 0, 0, (byte) 0, true, false));
            grid.setInventorySlotContents(1, new ItemStack(Items.MAP));
            r = net.minecraft.item.crafting.CraftingManager.findMatchingRecipe(grid, world);
            check.ok(r != null && !(r instanceof com.exiledradio.gamedropsbackport.camps.ExplorerMapCloning)
                    && r.getCraftingResult(grid).getItem() == Items.FILLED_MAP, "a filled map still copies by 1.12.2's recipe");
        }

        // --- 26.3's BeehiveDecorator on the _bees trees
        check.ok(Math.abs(CampBees.probability("minecraft:cherry_bees_005") - 0.05F) < 1e-6
                && Math.abs(CampBees.probability("minecraft:fancy_oak_bees_002") - 0.02F) < 1e-6
                && Math.abs(CampBees.probability("minecraft:super_birch_bees_0002") - 0.002F) < 1e-6
                && CampBees.probability("minecraft:oak_checked") == 0.0F, "bee probabilities from the feature names");
        for (String id : new String[] {"minecraft:cherry_bees_005", "minecraft:fancy_oak_bees_002", "minecraft:birch_bees_002",
                "minecraft:super_birch_bees_0002"}) {
            JigsawPools.FeaturePlacer placer = JigsawPools.placer(id);
            check.ok(placer != null && placer.getClass().getName().endsWith("CampBees$Placer"), id + " places its tree, then the nest: " + placer);
        }
        net.minecraft.block.state.IBlockState nest = CampBees.nest();
        boolean bees = net.minecraftforge.fml.common.Loader.isModLoaded("buzzybeesbackport");
        check.eq(bees, nest != null, "a bee nest exactly when Buzzy Bees is installed");
        BlockPos base = new BlockPos(from.getX() + 200, 150, from.getZ() + 200);
        List<BlockPos> logs = new java.util.ArrayList<BlockPos>();
        List<BlockPos> leaves = new java.util.ArrayList<BlockPos>();
        for (int y = 0; y < 6; y++) {
            world.setBlockState(base.up(y), Blocks.LOG.getDefaultState(), 2);
            logs.add(base.up(y));
        }
        for (int y = 3; y < 7; y++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > 1) {
                        BlockPos p = base.add(dx, y, dz);
                        world.setBlockState(p, Blocks.LEAVES.getDefaultState(), 2);
                        leaves.add(p);
                    }
                }
            }
        }
        Collections.reverse(logs);
        boolean placed = CampBees.decorate(world, new Random(5), 1.0F, logs, leaves);
        check.eq(bees, placed, "a nest with probability 1 (Buzzy Bees: " + bees + ")");
        if (bees && placed) {
            // max(lowest leaf 3 - 1, lowest log 0 + 1) = 2, beside the trunk to the east, south or west.
            BlockPos at = null;
            for (net.minecraft.util.EnumFacing f : net.minecraft.util.EnumFacing.HORIZONTALS) {
                if (world.getBlockState(base.up(2).offset(f)).getBlock() == nest.getBlock()) {
                    at = base.up(2).offset(f);
                    check.ok(f != net.minecraft.util.EnumFacing.NORTH, "not on the north side");
                }
            }
            if (check.ok(at != null, "the nest is on the trunk one below the lowest leaves")) {
                check.ok(String.valueOf(world.getBlockState(at)).contains("facing=south"), "facing south: " + world.getBlockState(at));
                NBTTagCompound te = world.getTileEntity(at).writeToNBT(new NBTTagCompound());
                int n = te.getTagList("Bees", 10).tagCount();
                check.ok(n >= 2 && n <= 3, "2-3 bees inside: " + n);
                NBTTagCompound bee = te.getTagList("Bees", 10).getCompoundTagAt(0);
                check.ok(bee.getInteger("TicksInHive") < 599 && bee.getInteger("MinOccupationTicks") == 600
                        && bee.getCompoundTag("EntityData").getString("id").endsWith(":bee"), "a resting bee: " + bee);
            }
            // Every side at the nest's height blocked: no nest, as 26.3.
            BlockPos base2 = base.add(20, 0, 0);
            List<BlockPos> logs2 = new java.util.ArrayList<BlockPos>();
            for (int y = 0; y < 4; y++) {
                world.setBlockState(base2.up(y), Blocks.LOG.getDefaultState(), 2);
                logs2.add(base2.up(y));
                for (net.minecraft.util.EnumFacing f : net.minecraft.util.EnumFacing.HORIZONTALS) {
                    if (y > 0) {
                        world.setBlockState(base2.up(y).offset(f), Blocks.STONE.getDefaultState(), 2);
                    }
                }
            }
            check.ok(!CampBees.decorate(world, new Random(6), 1.0F, logs2, Collections.<BlockPos>emptyList()), "a blocked trunk gets no nest");
            // A real tree: 1.12.2's oak (6-8 tall: a 4-tall one has leaves beside the nest's layer, no nest in 26.3
            // either) through the decorator, its logs and leaves found by looking.
            BlockPos treeAt = base.add(-40, 0, 0);
            for (BlockPos p : BlockPos.getAllInBoxMutable(treeAt.add(-4, -1, -4), treeAt.add(4, 12, 4))) {
                world.setBlockState(p, p.getY() == treeAt.getY() - 1 ? Blocks.GRASS.getDefaultState() : Blocks.AIR.getDefaultState(), 2);
            }
            CampBees.grow(world, treeAt, new Random(11), 1.0F,
                    () -> new net.minecraft.world.gen.feature.WorldGenTrees(false, 6, Blocks.LOG.getDefaultState(),
                            Blocks.LEAVES.getDefaultState().withProperty(net.minecraft.block.BlockLeaves.CHECK_DECAY, false), false)
                            .generate(world, new Random(11), treeAt));
            int nests = 0;
            for (BlockPos p : BlockPos.getAllInBoxMutable(treeAt.add(-3, 0, -3), treeAt.add(3, 10, 3))) {
                nests += world.getBlockState(p).getBlock() == nest.getBlock() ? 1 : 0;
            }
            check.eq(1, nests, "a grown oak through the decorator (probability 1) has one nest, trunk "
                    + world.getBlockState(treeAt) + " " + world.getBlockState(treeAt.up(5)));
        }
    }

    private static String fillName() {
        for (java.lang.reflect.Method m : net.minecraft.tileentity.TileEntityLockableLoot.class.getDeclaredMethods()) {
            if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == net.minecraft.entity.player.EntityPlayer.class
                    && m.getReturnType() == void.class && (m.getName().equals("fillWithLoot") || m.getName().equals("func_184281_d"))) {
                return m.getName();
            }
        }
        return "fillWithLoot";
    }

    /**
     * Vanilla 26.3's own starts for this seed ({@code tools/camps/oracle/oracle.py}, its JSON copied to
     * {@code eclipse/camps-oracle.json}): each laid out here for the same chunk and structure, piece for piece. The
     * start's height is the terrain's, so heights are compared relative to the start piece.
     */
    private static void oracle(Check check, WorldServer world) throws Exception {
        java.io.File file = world.getMinecraftServer().getFile("camps-oracle.json");
        if (!file.exists()) {
            return;
        }
        com.google.gson.JsonObject json = new com.google.gson.JsonParser().parse(
                new java.io.InputStreamReader(new java.io.FileInputStream(file), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        check.eq(world.getSeed(), json.get("seed").getAsLong(), "oracle seed");
        int starts = 0, pieces = 0, matched = 0;
        for (com.google.gson.JsonElement e : json.getAsJsonArray("starts")) {
            com.google.gson.JsonObject st = e.getAsJsonObject();
            if (!st.has("valid") || !st.get("valid").getAsBoolean()) {
                continue;
            }
            starts++;
            int cx = st.getAsJsonArray("chunk").get(0).getAsInt();
            int cz = st.getAsJsonArray("chunk").get(1).getAsInt();
            CampSettings.Variant v = CampSettings.get().variant(st.get("structure").getAsString());
            if (!check.ok(v != null, "oracle structure " + st.get("structure"))) {
                continue;
            }
            Random random = com.exiledradio.backportcore.structure.StructureRandoms.start(world.getSeed(), cx, cz);
            com.exiledradio.gamedropsbackport.camps.CampLayout.Stub stub =
                    com.exiledradio.gamedropsbackport.camps.CampLayout.stub(v, cx, cz, random, (x, z) -> 100, null);
            if (!check.ok(stub != null, "oracle start " + cx + "," + cz + " has a stub")) {
                continue;
            }
            List<CampPiece> ours = com.exiledradio.gamedropsbackport.camps.CampLayout.assemble(v, stub, random, -64, 319);
            com.google.gson.JsonArray theirs = st.getAsJsonArray("pieces");
            pieces += theirs.size();
            int dy = theirs.get(0).getAsJsonObject().getAsJsonArray("pos").get(1).getAsInt() - ours.get(0).position().getY();
            boolean all = check.eq(theirs.size(), ours.size(), "oracle " + v.id + " at " + cx + "," + cz + ": piece count");
            for (int i = 0; i < Math.min(theirs.size(), ours.size()); i++) {
                com.google.gson.JsonObject t = theirs.get(i).getAsJsonObject();
                CampPiece o = ours.get(i);
                com.google.gson.JsonArray pos = t.getAsJsonArray("pos");
                com.google.gson.JsonArray bb = t.getAsJsonArray("bb");
                StructureBoundingBox ob = o.getBoundingBox();
                String what = t.has("location") && !t.get("location").isJsonNull() ? t.get("location").getAsString() : "feature";
                String ourWhat = o.element().json().has("location") ? o.element().json().get("location").getAsString() : "feature";
                boolean same = what.equals(ourWhat) && t.get("rotation").getAsString().equals(o.rotation().name())
                        && pos.get(0).getAsInt() == o.position().getX() && pos.get(1).getAsInt() == o.position().getY() + dy
                        && pos.get(2).getAsInt() == o.position().getZ()
                        && bb.get(0).getAsInt() == ob.minX && bb.get(1).getAsInt() == ob.minY + dy && bb.get(2).getAsInt() == ob.minZ
                        && bb.get(3).getAsInt() == ob.maxX && bb.get(4).getAsInt() == ob.maxY + dy && bb.get(5).getAsInt() == ob.maxZ;
                if (same) {
                    matched++;
                } else {
                    all = false;
                    check.fail("oracle " + v.id + " at " + cx + "," + cz + " piece " + i + ": vanilla " + t + ", ours " + ourWhat + " "
                            + o.rotation() + " " + o.position().add(0, dy, 0) + " " + ob);
                }
            }
            check.ok(all, "oracle " + v.id + " at " + cx + "," + cz + " piece for piece");
        }
        GameDropsBackport.LOGGER.info("SELFTEST camps oracle: {} starts, {} of {} pieces match", starts, matched, pieces);
        check.ok(starts > 0, "the oracle has starts");
    }

    private static ItemStack upgrade(ItemStack stack) throws Exception {
        java.lang.reflect.Method m = ExplorerMaps.class.getDeclaredMethod("upgrade", ItemStack.class);
        m.setAccessible(true);
        return (ItemStack) m.invoke(null, stack);
    }
}
