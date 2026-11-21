package com.exiledradio.gamedropsbackport.sulfur;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;

/**
 * Sulfur, cinnabar and every full block cut from them: rock, needs a pickaxe, hardness 1.5 and blast
 * resistance 6 (26.3's numbers for all of them), yellow (sulfur) or red (cinnabar) on maps.
 */
public class BlockSulfurStone extends Block {

    public BlockSulfurStone(MapColor color, SoundType sound) {
        super(Material.ROCK, color);
        setHardness(1.5F);
        // 1.12.2 stores resistance tripled and reports it fifthed: 10 here is a modern 6.
        setResistance(10.0F);
        setSoundType(sound);
        setHarvestLevel("pickaxe", 0);
    }
}
