package com.exiledradio.gamedropsbackport.sulfur;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The spike's item: places only where the chosen direction can hold on (26.3's {@code getStateForPlacement} returns
 * nothing otherwise). The check is here, not in the block's {@code canPlaceBlockAt}, because 1.12.2's falling block
 * asks the block too and must be able to set a fallen segment down so it can break.
 */
public class ItemSulfurSpike extends ItemBlock {

    public ItemSulfurSpike(BlockSulfurSpike block) {
        super(block);
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX,
                                float hitY, float hitZ, IBlockState newState) {
        if (!((BlockSulfurSpike) this.block).canSurvive(world, pos, newState)) {
            return false;
        }
        return super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState);
    }
}
