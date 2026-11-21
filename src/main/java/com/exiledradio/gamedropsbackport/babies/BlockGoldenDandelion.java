package com.exiledradio.gamedropsbackport.babies;

import net.minecraft.block.BlockBush;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * 26.1's golden dandelion: a small flower like the dandelion (hardness 0, random offset, a 6-wide, 10-high
 * outline, plant sounds). Used on a baby mob it is {@link AgeLock}'s item.
 */
public class BlockGoldenDandelion extends BlockBush {

    /** 1.12.2's flower outline, the same as 26.3's {@code Block.column(6, 0, 10)}. */
    private static final AxisAlignedBB AABB = new AxisAlignedBB(0.3125D, 0.0D, 0.3125D, 0.6875D, 0.625D, 0.6875D);

    BlockGoldenDandelion() {
        setHardness(0.0F);
        setSoundType(SoundType.PLANT);
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return AABB.offset(state.getOffset(source, pos));
    }

    @Override
    public EnumOffsetType getOffsetType() {
        return EnumOffsetType.XZ;
    }
}
