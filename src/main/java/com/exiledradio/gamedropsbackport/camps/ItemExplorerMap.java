package com.exiledradio.gamedropsbackport.camps;

import net.minecraft.item.ItemMap;

/**
 * One of 26.3's explorer map items: a filled map ({@code MapItem}) under its own name and texture. 1.12.2's map
 * code reads a map's data by the stack's damage ({@code map_<n>}), whichever map item holds it, and draws any
 * {@link ItemMap} in hand and in item frames.
 */
public class ItemExplorerMap extends ItemMap {

    public ItemExplorerMap() {
        super();
        setCreativeTab(null);
    }
}
