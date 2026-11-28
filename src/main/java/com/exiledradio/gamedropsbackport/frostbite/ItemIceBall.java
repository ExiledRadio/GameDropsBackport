package com.exiledradio.gamedropsbackport.frostbite;

import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.BehaviorProjectileDispense;
import net.minecraft.dispenser.IPosition;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;

/**
 * 26.4's ice ball item ({@code IceBallItem}): stacks to 16, no cooldown; thrown at 1.5 with inaccuracy 1.0 and
 * {@code entity.ice_ball.throw} (players, volume 0.5, pitch {@code 0.4 / (r x 0.4 + 0.8)}); a dispenser shoots it.
 */
public class ItemIceBall extends Item {

    public static final float PROJECTILE_SHOOT_POWER = 1.5F;

    public ItemIceBall() {
        setMaxStackSize(16);
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(this, new BehaviorProjectileDispense() {
            @Override
            protected IProjectile getProjectileEntity(World world, IPosition position, ItemStack stack) {
                return new EntityIceBall(world, position.getX(), position.getY(), position.getZ());
            }
        });
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        world.playSound(null, player.posX, player.posY, player.posZ, FrostbiteContent.iceBallThrow, SoundCategory.PLAYERS, 0.5F,
                0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));
        if (!world.isRemote) {
            EntityIceBall ball = new EntityIceBall(world, player);
            ball.shoot(player, player.rotationPitch, player.rotationYaw, 0.0F, PROJECTILE_SHOOT_POWER, 1.0F);
            world.spawnEntity(ball);
        }
        if (!player.capabilities.isCreativeMode) {
            stack.shrink(1);
        }
        player.addStat(StatList.getObjectUseStats(this));
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
    }
}
