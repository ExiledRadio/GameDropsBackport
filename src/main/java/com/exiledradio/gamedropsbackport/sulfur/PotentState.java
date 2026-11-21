package com.exiledradio.gamedropsbackport.sulfur;

import net.minecraft.util.IStringSerializable;

/** 26.2's {@code potent_sulfur_state}: what the block does, from the water above and the block below. */
public enum PotentState implements IStringSerializable {
    /** No water source right above. */
    DRY,
    /** Water above: bubbles and nausea gas. */
    WET,
    /** Water above, magma below, between eruptions. */
    DORMANT,
    /** Water above, magma below, erupting. */
    ERUPTING,
    /** Water above, a lava source below: erupting all the time. */
    CONTINUOUS;

    @Override
    public String getName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public boolean erupts() {
        return this == ERUPTING || this == CONTINUOUS;
    }
}
