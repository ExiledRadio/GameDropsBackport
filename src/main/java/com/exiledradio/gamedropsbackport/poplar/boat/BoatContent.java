package com.exiledradio.gamedropsbackport.poplar.boat;

import com.exiledradio.gamedropsbackport.ModConfig;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.poplar.Content;
import com.exiledradio.gamedropsbackport.poplar.Owners;

import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * The poplar boat and, with Wild Update (the owner of chest boats, Rule 2), the poplar chest boat.
 *
 * <p>Two entities, ids 6 and 7 of this stream's block 6-10: {@code boat} and {@code chest_boat}, the wood in their
 * data (Trails &amp; Tales' boat code, the family's own). Recipes are 26.3's: five planks in a U make a boat; a boat
 * and a chest, shapeless, make the chest boat. Both burn for 1200 ticks.
 */
@Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID)
public final class BoatContent {

    public static final int BOAT_ID = 6;
    public static final int CHEST_BOAT_ID = 7;

    private static final Map<String, Item> ITEMS = new HashMap<String, Item>();

    private BoatContent() {
    }

    public static boolean enabled() {
        return ModConfig.poplar.enabled;
    }

    /** @return true if chest boats exist in this game: Wild Update owns them (1.19) */
    public static boolean chestBoats() {
        return Owners.present(Owners.WILD_UPDATE);
    }

    /** @return the item of a wood's boat, or null if that boat is not in this game */
    @Nullable
    public static Item item(BoatWood wood, boolean chest) {
        return ITEMS.get(wood.itemName(chest));
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!enabled()) {
            return;
        }
        event.getRegistry().register(EntityEntryBuilder.create().entity(EntityModBoat.class).id(Content.id("boat"), BOAT_ID)
                .name(GameDropsBackport.MOD_ID + ".boat").tracker(80, 3, true).build());
        if (!chestBoats()) {
            return;
        }
        event.getRegistry().register(EntityEntryBuilder.create().entity(EntityModChestBoat.class).id(Content.id("chest_boat"), CHEST_BOAT_ID)
                .name(GameDropsBackport.MOD_ID + ".chest_boat").tracker(80, 3, true).build());
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        for (BoatWood w : BoatWood.values()) {
            if (!w.present()) {
                continue;
            }
            for (boolean chest : chestBoats() ? new boolean[] {false, true} : new boolean[] {false}) {
                String name = w.itemName(chest);
                ITEMS.put(name, Content.item(event.getRegistry(), name, new ItemModBoat(w, chest)));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        for (BoatWood w : BoatWood.values()) {
            Item boat = item(w, false);
            Item chestBoat = item(w, true);
            ItemStack planks = w.planks(1);
            if (boat == null || planks == null) {
                continue;
            }
            ResourceLocation id = Content.id(w.itemName(false));
            event.getRegistry().register(new ShapedOreRecipe(id, new ItemStack(boat), "# #", "###", '#', planks).setRegistryName(id));
            if (chestBoat == null) {
                continue;
            }
            ResourceLocation cid = Content.id(w.itemName(true));
            event.getRegistry().register(new ShapelessOreRecipe(cid, new ItemStack(chestBoat), Blocks.CHEST, boat).setRegistryName(cid));
        }
    }
}
