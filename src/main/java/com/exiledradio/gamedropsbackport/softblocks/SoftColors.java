package com.exiledradio.gamedropsbackport.softblocks;

import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.item.EnumDyeColor;

import java.util.Arrays;

/**
 * How the sixteen colors share blocks, to save 1.12.2's block ids: 26.3 has a block per color and shape (64, plus a
 * double slab state each), this stream has 22.
 *
 * <ul>
 *   <li>Stairs use 3 of the 4 metadata bits (facing, half), so a stairs block holds 2 colors: 8 per material.</li>
 *   <li>A half slab uses 1 bit (half), so it holds 8 colors: 2 per material.</li>
 *   <li>A double slab holds all 16: 1 per material.</li>
 * </ul>
 * A block is named after its first color ({@code white_wool_stairs} holds white and orange). Each color keeps its
 * own item metadata, name and model, so in game nothing shows the grouping.
 */
public final class SoftColors {

    private SoftColors() {
    }

    /** 26.3's name of a color ({@code light_gray}; 1.12.2 calls it {@code silver}). */
    public static String modern(EnumDyeColor color) {
        return color == EnumDyeColor.SILVER ? "light_gray" : color.getName();
    }

    /** The colors from {@code first}, {@code count} of them, in dye order. */
    static EnumDyeColor[] range(int first, int count) {
        return Arrays.copyOfRange(EnumDyeColor.values(), first, first + count);
    }

    /** A color property limited to these colors; its values sort in dye order. */
    static PropertyEnum<EnumDyeColor> property(EnumDyeColor[] colors) {
        return PropertyEnum.create("color", EnumDyeColor.class, Arrays.asList(colors));
    }

    /** @return the index of {@code color} in {@code colors}, or -1 */
    static int indexOf(EnumDyeColor[] colors, EnumDyeColor color) {
        for (int i = 0; i < colors.length; i++) {
            if (colors[i] == color) {
                return i;
            }
        }
        return -1;
    }
}
