package com.exiledradio.gamedropsbackport.softblocks;

import com.exiledradio.gamedropsbackport.GameDropsBackport;

import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The cushion item, one metadata per color (the dye's). Used on the top of a block, it places a cushion there, at the
 * height of the face clicked, turned the way the player faces (26.3's {@code CushionItem}). Stacks to 16 and burns for
 * 50 ticks, as a wool slab.
 */
public class ItemCushion extends Item {

    ItemCushion() {
        setHasSubtypes(true);
        setMaxDamage(0);
        setMaxStackSize(16);
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        return "item." + GameDropsBackport.MOD_ID + "." + SoftColors.modern(EnumDyeColor.byMetadata(stack.getMetadata()))
                + "_cushion";
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (isInCreativeTab(tab)) {
            for (EnumDyeColor c : EnumDyeColor.values()) {
                items.add(new ItemStack(this, 1, c.getMetadata()));
            }
        }
    }

    @Override
    public int getItemBurnTime(ItemStack stack) {
        return 50;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing,
                                      float hitX, float hitY, float hitZ) {
        if (facing != EnumFacing.UP) {
            return EnumActionResult.FAIL;
        }
        IBlockState clicked = world.getBlockState(pos);
        BlockPos at = clicked.getBlock().isReplaceable(world, pos) ? pos : pos.up();
        double x = at.getX() + 0.5D;
        double y = pos.getY() + hitY;
        double z = at.getZ() + 0.5D;
        AxisAlignedBB box = new AxisAlignedBB(x - 0.5D, y, z - 0.5D, x + 0.5D, y + 0.25D, z + 0.5D);
        if (!EntityCushion.canBePlacedAt(world, box)) {
            return EnumActionResult.FAIL;
        }
        if (!world.isRemote) {
            if (!world.getEntitiesWithinAABB(EntityCushion.class, box).isEmpty()) {
                return EnumActionResult.FAIL;
            }
            ItemStack stack = player.getHeldItem(hand);
            EntityCushion cushion = new EntityCushion(world);
            cushion.setColor(EnumDyeColor.byMetadata(stack.getMetadata()));
            if (stack.hasDisplayName()) {
                cushion.setCustomNameTag(stack.getDisplayName());
            }
            cushion.setLocationAndAngles(x, y, z, player.getHorizontalFacing().getHorizontalAngle(), 0.0F);
            world.spawnEntity(cushion);
            cushion.breakIfBurning();
            if (!cushion.isSilent()) {
                world.playSound(null, cushion.posX, cushion.posY, cushion.posZ, SoftBlocks.cushionPlace, SoundCategory.BLOCKS,
                        0.75F, 0.8F);
            }
            if (!player.capabilities.isCreativeMode) {
                stack.shrink(1);
            }
        }
        return EnumActionResult.SUCCESS;
    }
}
