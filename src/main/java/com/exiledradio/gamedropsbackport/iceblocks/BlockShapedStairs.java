package com.exiledradio.gamedropsbackport.iceblocks;

import com.exiledradio.backportcore.waterlog.Waterloggable;

import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;

/**
 * Modern stairs. Vanilla's {@link BlockStairs} already does corners, upside-down placement and copying
 * its strength and sound from the block it is made of; it only hides its constructor.
 */
public class BlockShapedStairs extends BlockStairs implements Waterloggable {

    private final boolean toolRequired;

    public BlockShapedStairs(IBlockState base, boolean toolRequired) {
        super(base);
        this.toolRequired = toolRequired;
        this.useNeighborBrightness = true;
        String tool = base.getBlock().getHarvestTool(base);
        if (tool != null) {
            setHarvestLevel(tool, base.getBlock().getHarvestLevel(base));
        }
    }

    @Override
    public boolean canHarvestBlock(net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos,
                                   net.minecraft.entity.player.EntityPlayer player) {
        return BlockIceStone.toolRequired(this, this.toolRequired, world, pos, player);
    }
}
