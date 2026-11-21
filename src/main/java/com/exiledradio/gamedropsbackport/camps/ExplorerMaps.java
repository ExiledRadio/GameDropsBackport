package com.exiledradio.gamedropsbackport.camps;

import com.exiledradio.backportcore.locate.StructureLocate;
import com.exiledradio.backportcore.map.MapIcons;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import net.minecraft.entity.IMerchant;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerMerchant;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapData;
import net.minecraft.world.storage.MapDecoration;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 26.3's explorer maps: sixteen map items of their own ({@code Items}: {@code registerItem(..., MapItem::new)}), each
 * its own texture and name, made by loot and trades as filled maps were.
 *
 * <ul>
 *   <li>A map is made where its structure exists: today's {@code exploration_map} loot function asks Backport Core's
 *       {@code /locate} for the destination tag's nearest structure; none (no backport has the structure, or none is
 *       in range) and the map is not made, as 26.3's {@code filtered} modifier discards a map without an id. A zoom-2
 *       map centered on it, with the biome preview and a {@code "+"} decoration of today's type (Backport Core's
 *       {@link MapIcons} for the types 1.12.2 lacks).</li>
 *   <li>26.3's {@code ExplorerMapItemFix}, live: a filled map whose {@code "+"} decoration is one of the explorer
 *       types (the monument and mansion maps 1.12.2's cartographer sells, the village, swamp, jungle, trial chambers and
 *       buried treasure maps of the other backports) becomes that type's map item when it reaches a player's inventory,
 *       an open container or a villager's offers, and loses its old name where it was the type's old one.</li>
 * </ul>
 */
public final class ExplorerMaps {

    /** One map item: its decoration (Backport Core's sprite name, or a 1.12.2 type) and 1.21's old name key. */
    static final class Kind {
        final String item;
        final String icon;
        final int color;
        @Nullable
        final String legacyName;
        MapDecoration.Type type;
        Item registered;

        Kind(String item, String icon, int color, @Nullable String legacyName) {
            this.item = item;
            this.icon = icon;
            this.color = color;
            this.legacyName = legacyName;
        }
    }

    static final Map<String, Kind> KINDS = new LinkedHashMap<String, Kind>();

    static {
        add("abandoned_camp_map", "abandoned_camp", -1, null);
        add("buried_ancient_city_map", "ancient_city", -1, null);
        add("buried_mineshaft_map", "mineshaft", -1, null);
        add("buried_treasure_map", "red_x", -1, "filled_map.buried_treasure");
        add("buried_trial_chambers_map", "trial_chambers", MapIcons.TRIAL_CHAMBERS_COLOR, "filled_map.trial_chambers");
        add("desert_pyramid_map", "desert_pyramid", -1, null);
        add("desert_village_map", "desert_village", MapIcons.LIGHT_GRAY, "filled_map.village_desert");
        add("jungle_pyramid_map", "jungle_temple", MapIcons.LIGHT_GRAY, "filled_map.explorer_jungle");
        add("ocean_monument_map", "MONUMENT", 0, "filled_map.monument");
        add("plains_village_map", "plains_village", MapIcons.LIGHT_GRAY, "filled_map.village_plains");
        add("savanna_village_map", "savanna_village", MapIcons.LIGHT_GRAY, "filled_map.village_savanna");
        add("snowy_village_map", "snowy_village", MapIcons.LIGHT_GRAY, "filled_map.village_snowy");
        add("swamp_hut_map", "swamp_hut", MapIcons.LIGHT_GRAY, "filled_map.explorer_swamp");
        add("taiga_village_map", "taiga_village", MapIcons.LIGHT_GRAY, "filled_map.village_taiga");
        add("warm_ocean_ruins_map", "warm_ocean_ruins", -1, null);
        add("woodland_mansion_map", "MANSION", 0, "filled_map.mansion");
    }

    private static void add(String item, String icon, int color, @Nullable String legacyName) {
        KINDS.put(item, new Kind(item, icon, color, legacyName));
    }

    private ExplorerMaps() {
    }

    /** Pre-init, both sides: the decoration types are numbered the same on servers and clients. */
    static void preInit() {
        for (Kind k : KINDS.values()) {
            k.type = k.icon.equals(k.icon.toUpperCase()) ? MapDecoration.Type.valueOf(k.icon) : MapIcons.get(k.icon, k.color);
        }
    }

    /** @return the items, registry path to item, made once */
    static List<Item> items() {
        List<Item> out = new ArrayList<Item>();
        for (Kind k : KINDS.values()) {
            if (k.registered == null) {
                k.registered = new ItemExplorerMap().setRegistryName(GameDropsBackport.MOD_ID, k.item)
                        .setTranslationKey(GameDropsBackport.MOD_ID + "." + k.item);
            }
            out.add(k.registered);
        }
        return out;
    }

    @Nullable
    static Kind kind(String item) {
        return KINDS.get(item.substring(item.indexOf(':') + 1));
    }

    /** @return the decoration type of a map item's name ({@code minecraft:abandoned_camp_map}), or null */
    @Nullable
    public static MapDecoration.Type type(String item) {
        Kind k = kind(item);
        return k == null ? null : k.type;
    }

    /**
     * Today's {@code exploration_map} (zoom 2) on a stack of one of our map items.
     *
     * @param destination a structure id or tag, {@code #minecraft:on_mineshaft_maps}
     * @param decoration  today's decoration id, {@code minecraft:mineshaft}
     * @return the map, or null where no such structure can be found
     */
    @Nullable
    public static ItemStack create(World world, BlockPos from, Item item, String destination, String decoration) {
        if (!(world instanceof WorldServer)) {
            return null;
        }
        BlockPos target = null;
        for (String id : CampSettings.structureTag(destination)) {
            BlockPos found = locate((WorldServer) world, from, id);
            if (found != null && (target == null || from.distanceSq(found) < from.distanceSq(target))) {
                target = found;
            }
        }
        if (target == null) {
            return null;
        }
        Kind kind = KINDS.get(item.getRegistryName().getPath());
        MapDecoration.Type type = byDecoration(decoration, kind == null ? null : kind.type);
        if (type == null) {
            return null;
        }
        ItemStack map = setupNewMap(world, item, target.getX(), target.getZ(), (byte) 2);
        ItemMap.renderBiomePreviewMap(world, map);
        MapData.addTargetDecoration(map, target, "+", type);
        return map;
    }

    @Nullable
    private static BlockPos locate(WorldServer world, BlockPos from, String id) {
        CampSettings.Variant camp = CampSettings.get().variant(id);
        if (!StructureLocate.known(id) || camp != null && !AbandonedCampStructure.possible(camp)) {
            return null;
        }
        try {
            StructureLocate.Found found = StructureLocate.nearest(world, from, id);
            return found == null ? null : found.pos;
        } catch (StructureLocate.Unavailable e) {
            return null;
        } catch (RuntimeException e) {
            GameDropsBackport.LOGGER.warn("Explorer map: locating {} failed.", id, e);
            return null;
        }
    }

    /** Today's decoration id to its 1.12.2 type, or the item's own type. */
    @Nullable
    private static MapDecoration.Type byDecoration(String decoration, @Nullable MapDecoration.Type fallback) {
        String d = decoration.substring(decoration.indexOf(':') + 1);
        switch (d) {
            case "mansion":
                return MapDecoration.Type.MANSION;
            case "monument":
                return MapDecoration.Type.MONUMENT;
            case "ocean_ruin_warm":
                return MapIcons.find("warm_ocean_ruins");
            default:
                MapDecoration.Type t = MapIcons.find(d);
                return t != null ? t : fallback;
        }
    }

    /** 1.12.2's {@code ItemMap.setupNewMap}, on our item. */
    static ItemStack setupNewMap(World world, Item item, int x, int z, byte scale) {
        ItemStack stack = new ItemStack(item, 1, world.getUniqueDataId("map"));
        String name = "map_" + stack.getMetadata();
        MapData data = new MapData(name);
        world.setData(name, data);
        data.scale = scale;
        data.calculateMapCenter(x, z, data.scale);
        data.dimension = world.provider.getDimension();
        data.trackingPosition = true;
        data.unlimitedTracking = true;
        data.markDirty();
        return stack;
    }

    // ---- 26.3's ExplorerMapItemFix, live

    /** @return our map item for an old filled explorer map, or the stack itself (unchanged) */
    static ItemStack upgrade(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != Items.FILLED_MAP || !stack.hasTagCompound()
                || !stack.getTagCompound().hasKey("Decorations", 9)) {
            return stack;
        }
        NBTTagList list = stack.getTagCompound().getTagList("Decorations", 10);
        Kind kind = null;
        for (int i = 0; i < list.tagCount() && kind == null; i++) {
            NBTTagCompound d = list.getCompoundTagAt(i);
            if (!"+".equals(d.getString("id"))) {
                continue;
            }
            int ordinal = d.getByte("type") & 0xFF;
            MapDecoration.Type[] types = MapDecoration.Type.values();
            if (ordinal >= types.length) {
                continue;
            }
            for (Kind k : KINDS.values()) {
                if (k.legacyName != null && k.type == types[ordinal] && k.registered != null) {
                    kind = k;
                    break;
                }
            }
        }
        if (kind == null) {
            return stack;
        }
        ItemStack out = new ItemStack(kind.registered, stack.getCount(), stack.getMetadata());
        NBTTagCompound tag = stack.getTagCompound().copy();
        if (tag.hasKey("display", 10)) {
            NBTTagCompound display = tag.getCompoundTag("display");
            if (kind.legacyName.equals(display.getString("LocName"))) {
                display.removeTag("LocName");
            }
            if (display.isEmpty()) {
                tag.removeTag("display");
            }
        }
        out.setTagCompound(tag);
        return out;
    }

    /** @return true if a villager's offer was changed */
    static boolean upgrade(MerchantRecipeList recipes) {
        boolean changed = false;
        for (int i = 0; i < recipes.size(); i++) {
            MerchantRecipe r = recipes.get(i);
            ItemStack sold = r.getItemToSell();
            ItemStack upgraded = upgrade(sold);
            if (upgraded != sold) {
                NBTTagCompound tag = r.writeToTags();
                tag.setTag("sell", upgraded.writeToNBT(new NBTTagCompound()));
                recipes.set(i, new MerchantRecipe(tag));
                changed = true;
            }
        }
        return changed;
    }

    public static final class Events {
        @SubscribeEvent
        public void playerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || !(event.player instanceof EntityPlayerMP) || event.player.ticksExisted % 20 != 0) {
                return;
            }
            EntityPlayer p = event.player;
            for (int i = 0; i < p.inventory.getSizeInventory(); i++) {
                ItemStack s = p.inventory.getStackInSlot(i);
                ItemStack u = upgrade(s);
                if (u != s) {
                    p.inventory.setInventorySlotContents(i, u);
                }
            }
        }

        @SubscribeEvent
        public void open(PlayerContainerEvent.Open event) {
            Container c = event.getContainer();
            if (!(event.getEntityPlayer() instanceof EntityPlayerMP)) {
                return;
            }
            if (c instanceof ContainerMerchant) {
                IMerchant merchant = merchant((ContainerMerchant) c);
                MerchantRecipeList recipes = merchant == null ? null : merchant.getRecipes(event.getEntityPlayer());
                if (recipes != null && upgrade(recipes)) {
                    merchant.setRecipes(recipes);
                }
                return;
            }
            for (Slot slot : c.inventorySlots) {
                ItemStack s = slot.getStack();
                ItemStack u = upgrade(s);
                if (u != s) {
                    slot.putStack(u);
                }
            }
            c.detectAndSendChanges();
        }
    }

    private static java.lang.reflect.Field merchantField;

    /** ContainerMerchant's merchant, found by its type (one field holds an IMerchant). */
    @Nullable
    static synchronized IMerchant merchant(ContainerMerchant c) {
        try {
            if (merchantField == null) {
                for (java.lang.reflect.Field f : ContainerMerchant.class.getDeclaredFields()) {
                    if (f.getType() == IMerchant.class) {
                        f.setAccessible(true);
                        merchantField = f;
                    }
                }
            }
            return merchantField == null ? null : (IMerchant) merchantField.get(c);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** @return every map item's registry path, in 26.3's order */
    public static List<String> names() {
        return Collections.unmodifiableList(new ArrayList<String>(KINDS.keySet()));
    }
}
