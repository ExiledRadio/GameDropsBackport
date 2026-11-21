package com.exiledradio.gamedropsbackport.softblocks;

import com.exiledradio.gamedropsbackport.GameDropsBackport;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The item of a stairs or half slab block: one metadata per color (the dye's own metadata), each with 26.3's name
 * ({@code tile.gamedropsbackport.<color>_<shape>.name}). A slab item also stacks two halves into the double slab,
 * as vanilla's {@code ItemSlab}, which cannot be used here because the double has a different color property.
 */
public class ItemSoftBlock extends ItemBlock {

    /** {@code wool_stairs}, {@code concrete_slab}, ... */
    private final String shape;
    private final int burnTime;

    ItemSoftBlock(Block block, String shape, int burnTime) {
        super(block);
        this.shape = shape;
        this.burnTime = burnTime;
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        return "tile." + GameDropsBackport.MOD_ID + "." + SoftColors.modern(EnumDyeColor.byMetadata(stack.getMetadata()))
                + "_" + this.shape;
    }

    @Override
    public int getItemBurnTime(ItemStack stack) {
        return this.burnTime;
    }

    // --- slabs --------------------------------------------------------------------------------

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing,
                                      float hitX, float hitY, float hitZ) {
        if (!(this.block instanceof BlockSoftSlab.Half)) {
            return super.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
        }
        ItemStack stack = player.getHeldItem(hand);
        if (stack.isEmpty() || !player.canPlayerEdit(pos.offset(facing), facing, stack)) {
            return EnumActionResult.FAIL;
        }
        BlockSoftSlab.Half half = (BlockSoftSlab.Half) this.block;
        EnumDyeColor color = EnumDyeColor.byMetadata(stack.getMetadata());
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() == half && half.colorOf(state) == color) {
            BlockSlab.EnumBlockHalf h = state.getValue(BlockSlab.HALF);
            if (facing == EnumFacing.UP && h == BlockSlab.EnumBlockHalf.BOTTOM
                    || facing == EnumFacing.DOWN && h == BlockSlab.EnumBlockHalf.TOP) {
                join(player, stack, world, pos, half, color);
                return EnumActionResult.SUCCESS;
            }
        }
        BlockPos other = pos.offset(facing);
        IBlockState there = world.getBlockState(other);
        if (there.getBlock() == half && half.colorOf(there) == color) {
            join(player, stack, world, other, half, color);
            return EnumActionResult.SUCCESS;
        }
        return super.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
    }

    private static void join(EntityPlayer player, ItemStack stack, World world, BlockPos pos, BlockSoftSlab.Half half,
                             EnumDyeColor color) {
        IBlockState full = half.full.withColor(color);
        AxisAlignedBB box = full.getCollisionBoundingBox(world, pos);
        if (box != Block.NULL_AABB && world.checkNoEntityCollision(box.offset(pos)) && world.setBlockState(pos, full, 11)) {
            SoundType sound = half.full.getSoundType(full, world, pos, player);
            world.playSound(player, pos, sound.getPlaceSound(), SoundCategory.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F,
                    sound.getPitch() * 0.8F);
            if (player instanceof EntityPlayerMP) {
                CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP) player, pos, stack);
            }
            stack.shrink(1);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side, EntityPlayer player, ItemStack stack) {
        if (this.block instanceof BlockSoftSlab.Half) {
            BlockSoftSlab.Half half = (BlockSoftSlab.Half) this.block;
            EnumDyeColor color = EnumDyeColor.byMetadata(stack.getMetadata());
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock() == half && half.colorOf(state) == color) {
                BlockSlab.EnumBlockHalf h = state.getValue(BlockSlab.HALF);
                if (side == EnumFacing.UP && h == BlockSlab.EnumBlockHalf.BOTTOM
                        || side == EnumFacing.DOWN && h == BlockSlab.EnumBlockHalf.TOP) {
                    return true;
                }
            }
            IBlockState there = world.getBlockState(pos.offset(side));
            if (there.getBlock() == half && half.colorOf(there) == color) {
                return true;
            }
        }
        return super.canPlaceBlockOnSide(world, pos, side, player, stack);
    }
}
