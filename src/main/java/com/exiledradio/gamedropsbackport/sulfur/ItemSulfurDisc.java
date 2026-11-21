package com.exiledradio.gamedropsbackport.sulfur;

import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemRecord;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundEvent;

/**
 * Music disc Bounce on 1.12.2's record item, so the jukebox, "Now Playing", the tooltip line
 * ({@code item.record.bounce.desc}) and the record stat are vanilla's. Uncommon, as 26.3's.
 *
 * <p>It carries 26.3's comparator signal (8) under the name Backport Core's jukebox patch reads
 * ({@code comparatorSignal()}, patch P41 since Core 1.4.4), so a comparator next to a jukebox playing it gives 8.
 */
public final class ItemSulfurDisc extends ItemRecord {

    public static final int SIGNAL = 8;

    private final SoundEvent music;

    ItemSulfurDisc(String name, SoundEvent sound) {
        super(name, sound);
        this.music = sound;
    }

    /** @return the comparator signal from a jukebox playing this disc (26.3's {@code comparator_output}) */
    public int comparatorSignal() {
        return SIGNAL;
    }

    /** @return the disc's sound event (vanilla's getter is client-only) */
    public SoundEvent music() {
        return this.music;
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.UNCOMMON;
    }
}
