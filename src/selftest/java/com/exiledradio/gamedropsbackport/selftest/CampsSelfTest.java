package com.exiledradio.gamedropsbackport.selftest;

import com.exiledradio.backportcore.locate.LocateChecks;
import com.exiledradio.backportcore.locate.StructureLocate;
import com.exiledradio.backportcore.structure.JigsawPools;
import com.exiledradio.backportcore.structure.ScatteredStructure;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.camps.AbandonedCampStructure;
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
        ItemStack map = ExplorerMaps.create(world, from, mineshaftMap, "#minecraft:on_mineshaft_maps", "minecraft:mineshaft");
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
        ItemStack toCamp = ExplorerMaps.create(world, from, campMap, first.getKey(), "minecraft:abandoned_camp");
        check.ok(toCamp != null, "a camp map to " + first.getKey() + " (" + tag + ")");
        // A structure no installed mod has: no map.
        check.ok(ExplorerMaps.create(world, from, campMap, "minecraft:no_such_structure", "minecraft:abandoned_camp") == null,
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
