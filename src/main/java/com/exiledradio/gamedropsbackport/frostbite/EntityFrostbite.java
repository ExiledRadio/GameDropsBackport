package com.exiledradio.gamedropsbackport.frostbite;

import com.exiledradio.backportcore.worldgen.UndergroundBiomes;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.block.material.Material;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import javax.annotation.Nullable;

/**
 * 26.4's frostbite ({@code Frostbite extends RangedZombie extends Zombie}), on 1.12.2's zombie.
 *
 * <h2>Numbers (26.4 Pre-Release 1)</h2>
 * <ul>
 *   <li>24 health, speed 0.21 (a baby's +50%), the zombie's 35 follow range, 3 attack damage and 2 armor; the
 *       zombie's size, baby chance, reinforcements (frostbites) and burning in daylight.</li>
 *   <li>25% spawn holding an ice ball (no sword or shovel). With one, it throws ice balls at a target within 16
 *       blocks ({@link EntityAIFrostbiteAttack}: ranged from 5 blocks out until 3); without one, melee only. A melee
 *       hit gives 3 s of Freezing.</li>
 *   <li>Immune to Freezing (undead; Backport Core's freeze ticks). In water 600 ticks with its eyes under, then 300
 *       more, it becomes a zombie (keeping its equipment), not a drowned.</li>
 *   <li>Drops 0-2 rotten flesh and 0-3 ice balls, each +0-1 per Looting level.</li>
 *   <li>Spawns as cold variants do ({@code checkColdVariantSpawnRules}): a monster's rules, and with the sky in view
 *       (through powder snow) except in ice caves.</li>
 * </ul>
 */
public class EntityFrostbite extends EntityZombie implements IRangedAttackMob {

    public static final ResourceLocation LOOT = new ResourceLocation("gamedropsbackport", "entities/frostbite");
    public static final ResourceLocation ICE_CAVES = new ResourceLocation("gamedropsbackport", "ice_caves");
    public static final int FREEZING_TICKS_ON_MELEE = 60;
    public static final int MELEE_ATTACK_TICKS = 30;
    public static final float MAX_RANGE = 16.0F;
    public static final int WATER_TICKS = 600;
    public static final int CONVERSION_TICKS = 300;

    private int inWaterTime;
    private int conversionTime = -1;

    public EntityFrostbite(World world) {
        super(world);
    }

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(1, new EntityAIFrostbiteAttack(this));
        this.tasks.addTask(2, new EntityAIFrostbiteAttack.Melee(this));
        this.tasks.addTask(3, new EntityAIWander(this, 1.0D));
        this.tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(8, new EntityAILookIdle(this));
        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, true, EntityPigZombie.class));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, true));
        this.targetTasks.addTask(3, new EntityAINearestAttackableTarget<EntityVillager>(this, EntityVillager.class, false));
        this.targetTasks.addTask(3, new EntityAINearestAttackableTarget<EntityIronGolem>(this, EntityIronGolem.class, true));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(24.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.21D);
    }

    /** {@code populateDefaultEquipmentSlots}: 25% an ice ball in the main hand, and nothing else. */
    @Override
    protected void setEquipmentBasedOnDifficulty(DifficultyInstance difficulty) {
        if (this.rand.nextFloat() > 0.75F && FrostbiteContent.iceBall != null) {
            setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(FrostbiteContent.iceBall));
        }
    }

    @Override
    protected boolean shouldBurnInDay() {
        return true;
    }

    @Override
    @Nullable
    protected ResourceLocation getLootTable() {
        return LOOT;
    }

    @Override
    protected ItemStack getSkullDrop() {
        return ItemStack.EMPTY;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return FrostbiteContent.ambient;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return FrostbiteContent.hurt;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return FrostbiteContent.death;
    }

    @Override
    protected SoundEvent getStepSound() {
        return FrostbiteContent.step;
    }

    /** A melee hit gives Freezing for 60 ticks. */
    @Override
    public boolean attackEntityAsMob(Entity target) {
        boolean hit = super.attackEntityAsMob(target);
        if (hit && target instanceof EntityLivingBase && FrostbiteContent.freezing != null) {
            ((EntityLivingBase) target).addPotionEffect(new PotionEffect(FrostbiteContent.freezing, FREEZING_TICKS_ON_MELEE));
        }
        return hit;
    }

    /** {@code RangedZombie.performRangedAttack}: velocity 1.6, the difficulty's uncertainty, the ice ball's arc. */
    @Override
    public void attackEntityWithRangedAttack(EntityLivingBase target, float distanceFactor) {
        EntityIceBall ball = new EntityIceBall(this.world, this);
        double dx = target.posX - this.posX;
        double dy = target.getEntityBoundingBox().minY + target.height / 3.0F - ball.posY;
        double dz = target.posZ - this.posZ;
        double horizontal = MathHelper.sqrt(dx * dx + dz * dz);
        // trajectoryCorrectionFactor = gravity x 4 = 0.12.
        ball.shoot(dx, dy + horizontal * 0.12D, dz, 1.6F, 14 - this.world.getDifficulty().getId() * 4);
        playSound(FrostbiteContent.throwSound, 1.0F, 0.4F / (this.rand.nextFloat() * 0.4F + 0.8F));
        this.world.spawnEntity(ball);
        swingArm(EnumHand.MAIN_HAND);
    }

    @Override
    public void setSwingingArms(boolean swinging) {
    }

    /** {@code RangedZombie.canHoldRangedWeapon}: an ice ball in the main hand. */
    public boolean holdsIceBall() {
        return FrostbiteContent.iceBall != null && getHeldItemMainhand().getItem() == FrostbiteContent.iceBall;
    }

    /** {@code getAttackIntervalMin/Max}, in ticks: Easy 60/60, Normal 20/40, Hard 15/30. */
    public int intervalMin() {
        EnumDifficulty d = this.world.getDifficulty();
        return (int) (20.0F * (d == EnumDifficulty.EASY ? 3.0F : d == EnumDifficulty.HARD ? 0.75F : 1.0F));
    }

    public int intervalMax() {
        EnumDifficulty d = this.world.getDifficulty();
        return (int) (20.0F * (d == EnumDifficulty.EASY ? 3.0F : d == EnumDifficulty.HARD ? 1.5F : 2.0F));
    }

    // ---- drowning into a zombie ----------------------------------------------------------------------------------

    @Override
    public void onUpdate() {
        if (!this.world.isRemote && isEntityAlive() && !isAIDisabled()) {
            if (this.conversionTime >= 0) {
                if (--this.conversionTime < 0) {
                    convertToZombie();
                    return;
                }
            } else if (isInsideOfMaterial(Material.WATER)) {
                if (++this.inWaterTime >= WATER_TICKS) {
                    this.conversionTime = CONVERSION_TICKS;
                }
            } else {
                this.inWaterTime = -1;
            }
        }
        super.onUpdate();
    }

    private void convertToZombie() {
        EntityZombie zombie = new EntityZombie(this.world);
        zombie.copyLocationAndAnglesFrom(this);
        zombie.setChild(isChild());
        zombie.setNoAI(isAIDisabled());
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            zombie.setItemStackToSlot(slot, getItemStackFromSlot(slot).copy());
        }
        zombie.setCanPickUpLoot(canPickUpLoot());
        if (hasCustomName()) {
            zombie.setCustomNameTag(getCustomNameTag());
            zombie.setAlwaysRenderNameTag(getAlwaysRenderNameTag());
        }
        if (isNoDespawnRequired()) {
            zombie.enablePersistence();
        }
        zombie.setInvisible(isInvisible());
        this.world.removeEntity(this);
        this.world.spawnEntity(zombie);
        // Level event 1057: volume 2, pitch 0.9-1.0.
        this.world.playSound(null, new BlockPos(this), FrostbiteContent.convertedToZombie, SoundCategory.HOSTILE, 2.0F,
                this.rand.nextFloat() * 0.1F + 0.9F);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("InWaterTime", isInsideOfMaterial(Material.WATER) ? this.inWaterTime : -1);
        tag.setInteger("DrownedConversionTime", this.conversionTime);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        this.inWaterTime = tag.hasKey("InWaterTime") ? tag.getInteger("InWaterTime") : -1;
        this.conversionTime = tag.hasKey("DrownedConversionTime") ? tag.getInteger("DrownedConversionTime") : -1;
    }

    // ---- spawning --------------------------------------------------------------------------------------------------

    /** {@code Monster.checkColdVariantSpawnRules}. */
    @Override
    public boolean getCanSpawnHere() {
        return super.getCanSpawnHere() && coldVariantSky(this.world, new BlockPos(this));
    }

    /**
     * The cold variants' extra rule (frostbites and, in 26.4, strays): in ice caves nothing more; elsewhere the sky in
     * view from the top of the powder snow column the mob stands in.
     */
    public static boolean coldVariantSky(World world, BlockPos pos) {
        Biome biome = UndergroundBiomes.biomeAt(world, pos);
        if (biome != null && ICE_CAVES.equals(biome.getRegistryName())) {
            return true;
        }
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(pos);
        do {
            m.move(net.minecraft.util.EnumFacing.UP);
        } while (isPowderSnow(world.getBlockState(m)));
        return world.canSeeSky(m);
    }

    static boolean isPowderSnow(IBlockState state) {
        ResourceLocation id = state.getBlock().getRegistryName();
        return id != null && "powder_snow".equals(id.getPath());
    }
}
