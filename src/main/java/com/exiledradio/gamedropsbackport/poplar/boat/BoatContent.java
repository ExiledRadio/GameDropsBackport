package com.exiledradio.gamedropsbackport.poplar.boat;

import com.exiledradio.backportcore.boat.Boats;
import com.exiledradio.backportcore.boat.ItemWoodBoat;
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
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * The poplar boat and, with Wild Update (the owner of chest boats, Rule 2), the poplar chest boat.
 *
 * <p>The boats are Backport Core's ({@code backportcore:boat} and {@code chest_boat}, Core's {@code boat/}, our
 * {@code docs/CORE_REQUESTS.md} 5): this registers poplar with Core, the items under our names, and the recipes, 26.3's:
 * five planks in a U make a boat; a boat and a chest, shapeless, make the chest boat. Both burn for 1200 ticks. Worlds
 * saved before (our own {@code gamedropsbackport:boat} and {@code chest_boat}, ids 6 and 7) load as Core's, wood and
 * chest kept ({@link #preInit}).
 */
@Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID)
public final class BoatContent {

    private static final Map<String, Item> ITEMS = new HashMap<String, Item>();

    private BoatContent() {
    }

    public static boolean enabled() {
        return ModConfig.poplar.enabled;
    }

    /** Our boats saved before Core 1.4.4 load as Core's, with or without poplar on. */
    public static void preInit() {
        Boats.renamed(Content.id("boat"), false);
        Boats.renamed(Content.id("chest_boat"), true);
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
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        for (final BoatWood w : BoatWood.values()) {
            if (!w.present()) {
                continue;
            }
            com.exiledradio.backportcore.boat.BoatWood wood = com.exiledradio.backportcore.boat.BoatWood.register(Content.id(w.name),
                    () -> w.planks(1), w.raft)
                    .setTextures(Content.id("textures/entity/boat/" + w.name + ".png"), Content.id("textures/entity/chest_boat/" + w.name + ".png"));
            for (boolean chest : chestBoats() ? new boolean[] {false, true} : new boolean[] {false}) {
                String name = w.itemName(chest);
                Item item = Content.item(event.getRegistry(), name, new ItemWoodBoat(wood, chest));
                ITEMS.put(name, item);
                if (chest) {
                    wood.setChestBoatItem(item);
                } else {
                    wood.setBoatItem(item);
                }
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
