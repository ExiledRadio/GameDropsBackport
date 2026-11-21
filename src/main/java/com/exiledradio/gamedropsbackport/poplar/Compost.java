package com.exiledradio.gamedropsbackport.poplar;

import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.poplar.Owners;

import net.minecraft.item.Item;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The composter is Village &amp; Pillage's 1.14 block (Rule 2). With it installed, the poplar items
 * compost at 26.3's chances: leaves, sapling and red shrub 30%, the shelf mushroom 65%.
 * Reached by name at init; without it, nothing composts.
 */
public final class Compost {

    private static final Map<Item, Float> CHANCES = new LinkedHashMap<Item, Float>();

    private Compost() {
    }

    static void add(float chance, Item... items) {
        for (Item item : items) {
            if (item != null) {
                CHANCES.put(item, chance);
            }
        }
    }

    /** Init: hands the chances to Village &amp; Pillage's composter, if it is installed. */
    public static void init() {
        if (!Loader.isModLoaded(Owners.VILLAGE_AND_PILLAGE) || CHANCES.isEmpty()) {
            return;
        }
        try {
            Method register = Class.forName("com.exiledradio.villagerbackport.block.Compostables")
                    .getMethod("register", float.class, Item.class);
            for (Map.Entry<Item, Float> e : CHANCES.entrySet()) {
                register.invoke(null, e.getValue(), e.getKey());
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            GameDropsBackport.LOGGER.warn("Could not add the poplar items to the composter; they stay uncompostable.", e);
        }
    }
}
