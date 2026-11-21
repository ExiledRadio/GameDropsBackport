package com.exiledradio.gamedropsbackport.sulfurcube;

import com.exiledradio.gamedropsbackport.GameDropsBackport;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

import javax.annotation.Nullable;

/**
 * The bucket of sulfur cube (26.2): an adult cube scooped up with an empty bucket, placed again on a block. It keeps
 * what 26.3's {@code Bucketable} keeps (name, health, flags), its age and the block it holds, which the tooltip names.
 */
public class ItemSulfurCubeBucket extends Item {

    private static final String TAG = "sulfur_cube";

    public ItemSulfurCubeBucket() {
        this.setMaxStackSize(1);
        this.setTranslationKey(GameDropsBackport.MOD_ID + ".sulfur_cube_bucket");
        this.setCreativeTab(net.minecraft.creativetab.CreativeTabs.MISC);
    }

    /** @return a bucket holding this cube */
    public static ItemStack of(EntitySulfurCube cube) {
        ItemStack stack = new ItemStack(SulfurCubes.bucket);
        NBTTagCompound tag = new NBTTagCompound();
        if (cube.hasCustomName()) {
            tag.setString("CustomName", cube.getCustomNameTag());
        }
        tag.setBoolean("NoAI", cube.isAIDisabled());
        tag.setBoolean("Silent", cube.isSilent());
        tag.setBoolean("NoGravity", cube.hasNoGravity());
        tag.setBoolean("Glowing", cube.isGlowing());
        tag.setBoolean("Invulnerable", cube.getIsInvulnerable());
        tag.setFloat("Health", cube.getHealth());
        tag.setInteger("age", cube.getGrowingAge());
        if (cube.hasBodyItem()) {
            tag.setTag("body_item", cube.getBodyItem().writeToNBT(new NBTTagCompound()));
        }
        stack.setTagInfo(TAG, tag);
        if (cube.hasCustomName()) {
            stack.setStackDisplayName(cube.getCustomNameTag());
        }
        return stack;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing,
                                      float hitX, float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        BlockPos at = world.getBlockState(pos).getBlock().isReplaceable(world, pos) ? pos : pos.offset(facing);
        if (!player.canPlayerEdit(at, facing, stack)) {
            return EnumActionResult.FAIL;
        }
        if (!world.isRemote) {
            EntitySulfurCube cube = new EntitySulfurCube(world);
            cube.setLocationAndAngles(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, world.rand.nextFloat() * 360.0F, 0.0F);
            cube.rotationYawHead = cube.rotationYaw;
            cube.renderYawOffset = cube.rotationYaw;
            cube.setCubeSize(2, true);
            load(cube, stack.getSubCompound(TAG));
            cube.setFromBucket(true);
            cube.enablePersistence();
            world.spawnEntity(cube);
            cube.vibration(com.exiledradio.backportcore.gameevent.GameEvent.ENTITY_PLACE, player);
            world.playSound(null, at, SulfurCubes.bucketEmpty, SoundCategory.NEUTRAL, 1.0F, 1.0F);
            if (!player.capabilities.isCreativeMode) {
                player.setHeldItem(hand, new ItemStack(Items.BUCKET));
            }
        }
        return EnumActionResult.SUCCESS;
    }

    private static void load(EntitySulfurCube cube, @Nullable NBTTagCompound tag) {
        if (tag == null) {
            return;
        }
        if (tag.hasKey("CustomName")) {
            cube.setCustomNameTag(tag.getString("CustomName"));
        }
        cube.setNoAI(tag.getBoolean("NoAI"));
        cube.setSilent(tag.getBoolean("Silent"));
        cube.setNoGravity(tag.getBoolean("NoGravity"));
        cube.setGlowing(tag.getBoolean("Glowing"));
        cube.setEntityInvulnerable(tag.getBoolean("Invulnerable"));
        cube.setGrowingAge(tag.getInteger("age"));
        if (tag.hasKey("Health")) {
            cube.setHealth(tag.getFloat("Health"));
        }
        if (tag.hasKey("body_item")) {
            cube.setBodyItem(new ItemStack(tag.getCompoundTag("body_item")));
        }
    }

    /** @return the block a bucketed cube holds, or empty */
    public static ItemStack content(ItemStack bucket) {
        NBTTagCompound tag = bucket.getSubCompound(TAG);
        return tag != null && tag.hasKey("body_item") ? new ItemStack(tag.getCompoundTag("body_item")) : ItemStack.EMPTY;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        ItemStack held = content(stack);
        if (!held.isEmpty()) {
            tooltip.add(net.minecraft.util.text.TextFormatting.GRAY
                    + I18n.translateToLocalFormatted("entity." + GameDropsBackport.MOD_ID + ".sulfur_cube.content", held.getDisplayName()));
        }
    }
}
