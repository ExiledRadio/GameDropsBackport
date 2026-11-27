package com.exiledradio.gamedropsbackport.frostbite;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.item.Item;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 26.4's ice ball ({@code IceBall}, a {@code ThrowableBallProjectile}): 0.25 x 0.25, gravity 0.03 (1.12.2's throwable
 * default). On an entity it deals {@code ceil(speed x 1.3)} as thrown damage ({@code BASE_DAMAGE = 1.3}); it does not
 * freeze. On any hit it breaks with {@code entity.ice_ball.break} (volume 0.5, pitch {@code 0.4 / (r x 0.4 + 0.8)})
 * and 8 item particles of the ice ball.
 */
public class EntityIceBall extends EntityThrowable {

    public static final float BASE_DAMAGE = 1.3F;

    public EntityIceBall(World world) {
        super(world);
    }

    public EntityIceBall(World world, EntityLivingBase thrower) {
        super(world, thrower);
    }

    public EntityIceBall(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    /** {@code getDamage}: from the speed at impact. */
    public int damage() {
        double speed = Math.sqrt(this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ);
        return MathHelper.ceil(MathHelper.clamp(speed * BASE_DAMAGE, 0.0D, Integer.MAX_VALUE));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleStatusUpdate(byte id) {
        if (id == 3) {
            Item item = FrostbiteContent.iceBall;
            for (int i = 0; i < 8; i++) {
                this.world.spawnParticle(EnumParticleTypes.ITEM_CRACK, this.posX, this.posY, this.posZ, 0.0D, 0.0D, 0.0D,
                        Item.getIdFromItem(item));
            }
        }
    }

    @Override
    protected void onImpact(RayTraceResult result) {
        if (result.entityHit != null) {
            result.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(this, getThrower()), damage());
        }
        if (!this.world.isRemote) {
            this.world.setEntityState(this, (byte) 3);
            if (!this.isSilent()) {
                this.world.playSound(null, result.hitVec.x, result.hitVec.y, result.hitVec.z, FrostbiteContent.iceBallBreak,
                        SoundCategory.NEUTRAL, 0.5F, 0.4F / (this.rand.nextFloat() * 0.4F + 0.8F));
            }
            setDead();
        }
    }
}
