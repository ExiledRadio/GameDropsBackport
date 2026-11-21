package com.exiledradio.gamedropsbackport.softblocks;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * Places both halves of a straw bed, foot where the player clicks and head beyond it, as 26.3's bed items do: on
 * any face, with nothing needed underneath (a 26.x bed floats). Stacks to 16.
 */
public class ItemStrawBed extends ItemBlock {

    ItemStrawBed(BlockStrawBed block) {
        super(block);
        setMaxStackSize(16);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing,
                                      float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }
        if (!world.getBlockState(pos).getBlock().isReplaceable(world, pos)) {
            pos = pos.offset(facing);
        }
        EnumFacing dir = EnumFacing.byHorizontalIndex(MathHelper.floor(player.rotationYaw * 4.0F / 360.0F + 0.5D) & 3);
        BlockPos head = pos.offset(dir);
        ItemStack stack = player.getHeldItem(hand);
        if (!player.canPlayerEdit(pos, facing, stack) || !player.canPlayerEdit(head, facing, stack)
                || !world.getBlockState(pos).getBlock().isReplaceable(world, pos)
                || !world.getBlockState(head).getBlock().isReplaceable(world, head)
                || !world.getWorldBorder().contains(head)
                || !world.mayPlace(this.block, pos, false, facing, player)) {
            return EnumActionResult.FAIL;
        }
        IBlockState foot = this.block.getDefaultState().withProperty(BlockHorizontal.FACING, dir)
                .withProperty(BlockBed.PART, BlockBed.EnumPartType.FOOT);
        world.setBlockState(pos, foot, 10);
        world.setBlockState(head, foot.withProperty(BlockBed.PART, BlockBed.EnumPartType.HEAD), 10);
        world.notifyNeighborsRespectDebug(pos, this.block, false);
        world.notifyNeighborsRespectDebug(head, this.block, false);
        SoundType sound = this.block.getSoundType(foot, world, pos, player);
        world.playSound(null, pos, sound.getPlaceSound(), SoundCategory.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F,
                sound.getPitch() * 0.8F);
        if (player instanceof EntityPlayerMP) {
            CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP) player, pos, stack);
        }
        stack.shrink(1);
        return EnumActionResult.SUCCESS;
    }
}
