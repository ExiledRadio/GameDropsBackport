package com.exiledradio.gamedropsbackport.sulfur;

import net.minecraft.util.IStringSerializable;

/** 26.3's {@code SpeleothemThickness}, in its order. */
public enum Thickness implements IStringSerializable {
    TIP_MERGE, TIP, FRUSTUM, MIDDLE, BASE;

    @Override
    public String getName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
