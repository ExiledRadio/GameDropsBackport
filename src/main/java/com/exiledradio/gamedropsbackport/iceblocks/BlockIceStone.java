package com.exiledradio.gamedropsbackport.iceblocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A full block of this stream with 26.4's numbers. Hardness and blast resistance are given as 26.4 states them; 1.12.2
 * stores resistance tripled and reports it fifthed, so it is set as {@code resistance x 5 / 3}.
 *
 * <p>{@code requiresCorrectToolForDrops} on a material 1.12.2 lets anything harvest (packed ice, kept so a note block
 * on it plays the chime as 26.4's {@code CHIME} instrument): {@link #toolRequired} checks the tool itself.
 */
public class BlockIceStone extends Block {

    private final boolean toolRequired;

    public BlockIceStone(Material material, MapColor color, SoundType sound, float hardness, float resistance, boolean toolRequired) {
        super(material, color);
        setHardness(hardness);
        setResistance(resistance * 5.0F / 3.0F);
        setSoundType(sound);
        this.toolRequired = toolRequired;
    }

    @Override
    public boolean canHarvestBlock(IBlockAccess world, BlockPos pos, EntityPlayer player) {
        return toolRequired(this, this.toolRequired, world, pos, player);
    }

    /**
     * {@code requiresCorrectToolForDrops}: the held item must be the block's tool, at its level or above. A block whose
     * material already needs a tool is left to Forge.
     */
    static boolean toolRequired(Block block, boolean required, IBlockAccess world, BlockPos pos, EntityPlayer player) {
        IBlockState state = world.getBlockState(pos);
        if (!required || !state.getMaterial().isToolNotRequired()) {
            return net.minecraftforge.common.ForgeHooks.canHarvestBlock(block, player, world, pos);
        }
        String tool = block.getHarvestTool(state);
        ItemStack stack = player.getHeldItemMainhand();
        if (tool == null) {
            return true;
        }
        int level = stack.isEmpty() ? -1 : stack.getItem().getHarvestLevel(stack, tool, player, state);
        return level >= block.getHarvestLevel(state);
    }

    /** {@code calcite_pillar}: a {@code RotatedPillarBlock} copying calcite. */
    public static class Pillar extends BlockRotatedPillar {
        public Pillar(Material material, MapColor color, SoundType sound, float hardness, float resistance) {
            super(material, color);
            setHardness(hardness);
            setResistance(resistance * 5.0F / 3.0F);
            setSoundType(sound);
        }
    }
}
