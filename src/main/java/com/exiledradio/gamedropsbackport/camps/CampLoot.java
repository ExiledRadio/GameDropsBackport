package com.exiledradio.gamedropsbackport.camps;

import com.exiledradio.backportcore.ownership.ItemOwnership;
import com.exiledradio.backportcore.structure.ModernPalette;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.entity.Entity;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionType;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The camps' chests, barrels and copper chests name 26.3's loot tables ({@code LootTable} in their block entity
 * data). Backport Core's palette renames each to ours, {@code gamedropsbackport:camps/<path>}, an empty 1.12.2 table
 * that this fills at load with one entry: a roll of 26.3's own file ({@code camps/loot_table/<path>.json}), every
 * item looked up when the container is opened, so an item no installed mod has (a copper spear without Tricky Trials)
 * takes no part in its pool.
 *
 * <p>What the camp tables use: item entries, {@code set_count}, {@code set_name} (the camp maps' names),
 * {@code set_potion}, {@code exploration_map} with {@code filtered} (no structure: no map), and the
 * {@code inverted location_check} on the biome. 1.12.2's container loot has no position; the player opening it
 * stands within reach of it, so the player's block is the loot's origin.
 */
public final class CampLoot {

    private static final LootCondition[] NONE = new LootCondition[0];
    static final String[] TABLES = {"barrels/abandoned_camp_barrel", "chests/abandoned_camp_common_chest",
            "chests/abandoned_camp_secret_chest"};
    /** Our 1.12.2 table names to 26.3's paths. */
    static final Map<ResourceLocation, String> OURS = new HashMap<ResourceLocation, String>();
    /** 1.13's item renames of the items these tables name. */
    static final Map<String, String> RENAMED = new HashMap<String, String>();

    static {
        RENAMED.put("cobweb", "web");
    }

    CampLoot() {
    }

    static void preInit() {
        for (String path : TABLES) {
            ResourceLocation ours = new ResourceLocation(GameDropsBackport.MOD_ID, "camps/" + path);
            OURS.put(ours, path);
            ModernPalette.lootTable("minecraft:" + path, ours);
        }
    }

    @SubscribeEvent
    public void load(LootTableLoadEvent event) {
        String path = OURS.get(event.getName());
        if (path != null) {
            event.getTable().addPool(new LootPool(new LootEntry[] {new Modern(path)}, NONE, new RandomValueRange(1), new RandomValueRange(0),
                    GameDropsBackport.MOD_ID + ":camps"));
        }
    }

    /** One entry that adds a whole roll of a 26.3 table. */
    static final class Modern extends LootEntry {
        final String table;

        Modern(String table) {
            super(1, 0, NONE, table);
            this.table = table;
        }

        @Override
        public void addLoot(Collection<ItemStack> stacks, Random rand, LootContext context) {
            Entity player = context.getKillerPlayer();
            BlockPos origin = player == null ? null : new BlockPos(player);
            stacks.addAll(roll(context.getWorld(), origin, this.table, rand));
        }

        @Override
        protected void serialize(JsonObject json, JsonSerializationContext context) {
            json.addProperty("table", this.table);
        }
    }

    /** @return a roll of the 26.3 table at {@code path} ({@code chests/abandoned_camp_common_chest}) */
    public static List<ItemStack> roll(World world, @Nullable BlockPos origin, String path, Random random) {
        List<ItemStack> out = new ArrayList<ItemStack>();
        JsonObject table = CampSettings.read("camps/loot_table/" + path + ".json");
        if (table == null) {
            GameDropsBackport.LOGGER.warn("No camp loot table {}", path);
            return out;
        }
        for (JsonElement p : table.getAsJsonArray("pools")) {
            JsonObject pool = p.getAsJsonObject();
            int rolls = intOf(pool.get("rolls"), random, 1);
            List<JsonObject> entries = new ArrayList<JsonObject>();
            for (JsonElement e : pool.getAsJsonArray("entries")) {
                if (item(string(e.getAsJsonObject(), "name", "minecraft:air")) != null) {
                    entries.add(e.getAsJsonObject());
                }
            }
            for (int r = 0; r < rolls; r++) {
                List<JsonObject> live = new ArrayList<JsonObject>();
                int total = 0;
                for (JsonObject e : entries) {
                    if (condition(e.get("condition"), world, origin)) {
                        live.add(e);
                        total += Math.max(intOf(e.get("weight"), random, 1), 0);
                    }
                }
                if (live.isEmpty() || total <= 0) {
                    continue;
                }
                JsonObject chosen = live.get(0);
                if (live.size() > 1) {
                    int pick = random.nextInt(total);
                    for (JsonObject e : live) {
                        pick -= Math.max(intOf(e.get("weight"), random, 1), 0);
                        if (pick < 0) {
                            chosen = e;
                            break;
                        }
                    }
                }
                ItemStack stack = expand(chosen, world, origin, random);
                if (stack != null && !stack.isEmpty()) {
                    while (stack.getCount() > stack.getMaxStackSize()) {
                        out.add(stack.splitStack(stack.getMaxStackSize()));
                    }
                    out.add(stack);
                }
            }
        }
        return out;
    }

    @Nullable
    private static ItemStack expand(JsonObject entry, World world, @Nullable BlockPos origin, Random random) {
        String name = string(entry, "name", "minecraft:air");
        Item item = item(name);
        if (item == null) {
            return null;
        }
        ItemStack stack = new ItemStack(item);
        JsonElement m = entry.get("modifier");
        List<JsonObject> modifiers = new ArrayList<JsonObject>();
        if (m != null && m.isJsonObject()) {
            modifiers.add(m.getAsJsonObject());
        } else if (m != null && m.isJsonArray()) {
            for (JsonElement e : m.getAsJsonArray()) {
                modifiers.add(e.getAsJsonObject());
            }
        }
        boolean hasMapId = false;
        for (JsonObject fn : modifiers) {
            String type = string(fn, "type", "");
            type = type.substring(type.indexOf(':') + 1);
            switch (type) {
                case "set_count":
                    stack.setCount(intOf(fn.get("count"), random, 1));
                    break;
                case "set_name":
                    JsonElement n = fn.get("name");
                    if (n != null && n.isJsonObject() && n.getAsJsonObject().has("translate")) {
                        stack.setTranslatableName(n.getAsJsonObject().get("translate").getAsString());
                    }
                    break;
                case "set_potion":
                    PotionType potion = PotionType.REGISTRY.getObject(new ResourceLocation(string(fn, "id", "")));
                    if (potion == null || "empty".equals(potion.getRegistryName().getPath())) {
                        return null;
                    }
                    PotionUtils.addPotionToItemStack(stack, potion);
                    break;
                case "exploration_map":
                    if (origin == null) {
                        break;
                    }
                    ItemStack map = ExplorerMaps.create(world, origin, stack.getItem(), string(fn, "destination", ""),
                            string(fn, "decoration", "minecraft:mansion"));
                    if (map != null) {
                        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("display", 10)) {
                            map.setTagInfo("display", stack.getTagCompound().getCompoundTag("display").copy());
                        }
                        stack = map;
                        hasMapId = true;
                    }
                    break;
                case "filtered":
                    // The camp tables' only filter: a map without an id is discarded.
                    if (!hasMapId) {
                        return null;
                    }
                    break;
                default:
                    GameDropsBackport.LOGGER.warn("Camp loot: modifier {} is not supported; skipped", type);
            }
        }
        return stack;
    }

    /** 26.3's {@code inverted} and {@code location_check} (biomes) conditions. */
    static boolean condition(@Nullable JsonElement c, World world, @Nullable BlockPos origin) {
        if (c == null) {
            return true;
        }
        if (c.isJsonArray()) {
            for (JsonElement e : c.getAsJsonArray()) {
                if (!condition(e, world, origin)) {
                    return false;
                }
            }
            return true;
        }
        JsonObject o = c.getAsJsonObject();
        String type = string(o, "type", "");
        type = type.substring(type.indexOf(':') + 1);
        switch (type) {
            case "inverted":
                return !condition(o.get("term"), world, origin);
            case "location_check": {
                if (origin == null) {
                    return false;
                }
                JsonObject predicate = o.getAsJsonObject("predicate");
                JsonElement biomes = predicate == null ? null : predicate.get("biomes");
                if (biomes == null) {
                    return true;
                }
                String here = CampBiomes.at(world, origin);
                if (biomes.isJsonArray()) {
                    for (JsonElement b : biomes.getAsJsonArray()) {
                        if (b.getAsString().equals(here)) {
                            return true;
                        }
                    }
                    return false;
                }
                return biomes.getAsString().equals(here);
            }
            default:
                GameDropsBackport.LOGGER.warn("Camp loot: condition {} is not supported; treated as failed", type);
                return false;
        }
    }

    /**
     * Today's item name to the item: our own (the explorer maps, the soft blocks stream's cushions and straw beds),
     * the family owner's (Backport Core's item ownership), then 1.12.2's under its 1.13 rename.
     */
    @Nullable
    static Item item(String name) {
        String path = name.substring(name.indexOf(':') + 1);
        Item owned = ItemOwnership.item(path, GameDropsBackport.MOD_ID);
        if (owned != null) {
            return owned;
        }
        String vanilla = RENAMED.containsKey(path) ? RENAMED.get(path) : path;
        ResourceLocation id = new ResourceLocation("minecraft", vanilla);
        Item item = ForgeRegistries.ITEMS.getValue(id);
        return item == null || item == Items.AIR || !id.equals(item.getRegistryName()) ? null : item;
    }

    static int intOf(@Nullable JsonElement e, Random random, int fallback) {
        if (e == null) {
            return fallback;
        }
        if (e.isJsonPrimitive()) {
            return MathHelper.floor(e.getAsFloat());
        }
        JsonObject o = e.getAsJsonObject();
        String type = string(o, "type", "minecraft:uniform");
        if (type.endsWith("constant")) {
            return MathHelper.floor(o.get("value").getAsFloat());
        }
        // 26.3's UniformGenerator.getInt: Mth.nextInt(min, max), both ends included.
        return MathHelper.getInt(random, intOf(o.get("min"), random, 0), intOf(o.get("max"), random, 0));
    }

    static String string(JsonObject o, String key, String fallback) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : fallback;
    }
}
