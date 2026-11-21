package com.exiledradio.gamedropsbackport.sulfurcube;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.stats.StatList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import com.exiledradio.backportcore.gameevent.GameEvent;
import com.exiledradio.backportcore.gameevent.GameEventContext;
import com.exiledradio.backportcore.gameevent.GameEvents;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * The sulfur cube (26.2; 26.3's {@code SulfurCube} and {@code AbstractCubeMob}).
 *
 * <p>An ageable cube mob: adults are size 2 (8 health, 0.98 blocks), babies size 1 (4 health). It hops like a slime,
 * follows a player holding something it can swallow (a baby: a slimeball), and hops to dropped blocks it can swallow.
 * A swallowed block (26.3's body slot; here a synced stack) stops its AI until it is sheared off, gives it its
 * archetype's physics, knockback and sounds, and makes it immune to the damage of
 * {@code #sulfur_cube_with_block_immune_to} (falls, players, mobs, projectiles, explosions...). A cube that holds TNT
 * is primed by fire, flint and steel, fire charges, redstone and explosions. Adults split into two babies on death;
 * babies grow up on slimeballs. Adults go into a bucket.
 */
public class EntitySulfurCube extends EntityAgeable {

    private static final DataParameter<Integer> SIZE = EntityDataManager.createKey(EntitySulfurCube.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> MAX_FUSE = EntityDataManager.createKey(EntitySulfurCube.class, DataSerializers.VARINT);
    private static final DataParameter<Boolean> FROM_BUCKET = EntityDataManager.createKey(EntitySulfurCube.class, DataSerializers.BOOLEAN);
    private static final DataParameter<ItemStack> BODY = EntityDataManager.createKey(EntitySulfurCube.class, DataSerializers.ITEM_STACK);

    /** 26.3's {@code EntityType} size, scaled by the cube's size. */
    public static final float BASE_SIZE = 0.49F;
    public static final int PICKUP_TIMER = 100;
    private static final float GRAVITY = 0.08F;

    /**
     * {@code #sulfur_cube_with_block_immune_to} as 1.12.2 and the family name their damage types (plus every explosion).
     */
    private static final Set<String> IMMUNE = new HashSet<String>(Arrays.asList("arrow", "cactus", "fall", "anvil",
            "fallingBlock", "fallingStalactite", "stalagmite", "freeze", "maceSmash", "hotFloor", "mob", "player",
            "thrown", "spear", "spit", "sting", "sulfurCubeHot", "sweetBerryBush", "trident", "windCharge", "dryout"));
    /** {@code #no_knockback} among those. */
    private static final Set<String> NO_KNOCKBACK = new HashSet<String>(Arrays.asList("cactus", "fall", "hotFloor",
            "sulfurCubeHot", "sweetBerryBush", "freeze", "stalagmite", "spear", "dryout"));

    public float squish;
    public float prevSquish;
    private float targetSquish;
    private boolean wasOnGround;

    private int pickupTimer;
    private int pushSoundCooldown;
    private int fuse = -1;
    /** The archetype of the held block, null while it holds nothing. */
    @Nullable
    private Archetype archetype;
    /** A damage amount handed from {@link #attackEntityFrom} to {@link #knockBack}, -1 outside it. */
    private float hurtAmount = -1;

    public EntitySulfurCube(World world) {
        super(world);
        this.moveHelper = new CubeMoveHelper(this);
        this.setSize(BASE_SIZE, BASE_SIZE);
        this.experienceValue = 0;
    }

    // --- data, size, age ------------------------------------------------------------------------

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(SIZE, 1);
        this.dataManager.register(MAX_FUSE, -1);
        this.dataManager.register(FROM_BUCKET, false);
        this.dataManager.register(BODY, ItemStack.EMPTY);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(4.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3D);
    }

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(1, new FloatGoal(this));
        this.tasks.addTask(2, new TemptGoal(this));
        this.tasks.addTask(3, new SearchForItemsGoal(this));
        this.tasks.addTask(4, new RandomDirectionGoal(this));
        this.tasks.addTask(5, new KeepOnJumpingGoal(this));
    }

    public int getCubeSize() {
        return this.dataManager.get(SIZE);
    }

    public boolean isTiny() {
        return this.getCubeSize() <= 1;
    }

    /** 26.3's {@code setSize}: 4 health per size, speed 0.2 + 0.1 per size; size 1 makes an adult a baby. */
    public void setCubeSize(int size, boolean resetHealth) {
        int s = MathHelper.clamp(size, 1, 127);
        this.dataManager.set(SIZE, s);
        this.refreshSize();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(4.0D * s);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.2F + 0.1F * s);
        if (resetHealth) {
            this.setHealth(this.getMaxHealth());
        }
        if (resetHealth && s == 1 && !this.isChild()) {
            this.setGrowingAge(-24000);
        }
    }

    private void refreshSize() {
        double x = this.posX;
        double y = this.posY;
        double z = this.posZ;
        float w = BASE_SIZE * this.getCubeSize();
        this.setSize(w, w);
        this.setScale(1.0F);
        this.setPosition(x, y, z);
    }

    /** Babies are smaller through their size, not 1.12.2's half scale. */
    @Override
    public void setScaleForAge(boolean child) {
        this.setScale(1.0F);
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        if (SIZE.equals(key)) {
            this.refreshSize();
            this.rotationYaw = this.rotationYawHead;
            this.renderYawOffset = this.rotationYawHead;
        } else if (MAX_FUSE.equals(key)) {
            this.fuse = this.dataManager.get(MAX_FUSE);
        } else if (BODY.equals(key) && this.world.isRemote) {
            this.archetype = ArchetypeItems.of(this.getBodyItem());
        }
        super.notifyDataManagerChange(key);
    }

    @Override
    public float getEyeHeight() {
        return 0.175F * this.getCubeSize();
    }

    @Override
    protected void onGrowingAdult() {
        // 1.12.2 calls this only on growing up, and from ageUp before the new age is set: no isChild() check.
        super.onGrowingAdult();
        this.setCubeSize(2, true);
    }

    @Nullable
    @Override
    public EntityAgeable createChild(EntityAgeable other) {
        EntitySulfurCube child = new EntitySulfurCube(this.world);
        child.setCubeSize(1, true);
        return child;
    }

    @Nullable
    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        data = super.onInitialSpawn(difficulty, data);
        // 26.3's AgeableMob: the first of a group is an adult, the rest are babies 5% of the time.
        if (data instanceof GroupData) {
            GroupData group = (GroupData) data;
            if (group.count > 0 && this.rand.nextFloat() <= 0.05F) {
                this.setGrowingAge(-24000);
            }
            group.count++;
        } else {
            data = new GroupData();
            ((GroupData) data).count = 1;
        }
        this.setCubeSize(this.isChild() ? 1 : 2, true);
        return data;
    }

    private static final class GroupData implements IEntityLivingData {
        int count;
    }

    // --- the held block -------------------------------------------------------------------------

    public ItemStack getBodyItem() {
        return this.dataManager.get(BODY);
    }

    public boolean hasBodyItem() {
        return !this.getBodyItem().isEmpty();
    }

    @Nullable
    public Archetype archetype() {
        return this.archetype;
    }

    /** Sets the held block and everything its archetype changes (26.3's {@code collectEquipmentChanges}). */
    public void setBodyItem(ItemStack stack) {
        ItemStack held = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        if (!held.isEmpty()) {
            held.setCount(1);
        }
        this.dataManager.set(BODY, held);
        this.archetype = ArchetypeItems.of(held);
        if (!held.isEmpty()) {
            this.setAIMoveSpeed(0.0F);
            this.setMoveForward(0.0F);
            this.setMoveStrafing(0.0F);
            this.setJumping(false);
            this.enablePersistence();
        }
    }

    public boolean canExplode() {
        return this.archetype != null && this.archetype.explodes() && this.isEntityAlive() && !this.isPrimed();
    }

    public int getFuse() {
        return this.fuse;
    }

    public boolean isPrimed() {
        return this.fuse >= 0;
    }

    public boolean fromBucket() {
        return this.dataManager.get(FROM_BUCKET);
    }

    public void setFromBucket(boolean b) {
        this.dataManager.set(FROM_BUCKET, b);
    }

    /** 26.3's {@code primeTime}: a 6 s fuse, or TNT's short random fuse when an explosion set it off. */
    public boolean primeTime(boolean imminent) {
        if (this.archetype == null || !this.archetype.explodes() || !this.isEntityAlive() || this.world.isRemote
                || this.isPrimed()) {
            return false;
        }
        int f = imminent ? this.rand.nextInt(Math.max(1, Archetype.FUSE / 4)) + Archetype.FUSE / 8 : Archetype.FUSE;
        this.setEntityInvulnerable(true);
        this.fuse = f;
        this.dataManager.set(MAX_FUSE, f);
        this.playSound(SoundEvents.ENTITY_TNT_PRIMED, 1.0F, 1.0F);
        vibration(GameEvent.PRIME_FUSE, this);
        return true;
    }

    /**
     * 26.3's {@code equipItem}: swallows one of the stack, spitting out a different block it held. Babies swallow
     * nothing.
     */
    public boolean equipItem(ItemStack stack) {
        if (this.isChild() || !ArchetypeItems.swallowable(stack)) {
            return false;
        }
        if (this.hasBodyItem()) {
            ItemStack held = this.getBodyItem();
            if (held.getItem() == stack.getItem() && held.getMetadata() == stack.getMetadata()) {
                return false;
            }
            if (!this.world.isRemote) {
                this.entityDropItem(held.copy(), this.passengerOffset());
            }
        }
        if (!this.world.isRemote) {
            this.setBodyItem(stack);
        }
        this.playSound(SulfurCubes.absorb, this.getSoundVolume(), 1.0F);
        return true;
    }

    /** A game event for Wild Update's sculk, through Backport Core (nothing without a listener). */
    void vibration(GameEvent event, Entity source) {
        GameEvents.emit(this.world, event, new Vec3d(this.posX, this.posY, this.posZ), GameEventContext.of(source));
    }

    /** 26.3's passenger attachment: the top of the cube less a sixty-fourth per size. */
    private float passengerOffset() {
        return this.height - 0.015625F * this.getCubeSize();
    }

    public void shear() {
        ItemStack held = this.getBodyItem();
        this.setBodyItem(ItemStack.EMPTY);
        this.entityDropItem(held, this.passengerOffset());
        this.playSound(SulfurCubes.eject, this.getSoundVolume(), 1.0F);
        this.pickupTimer = PICKUP_TIMER;
    }

    // --- ticking -------------------------------------------------------------------------------

    @Override
    public void onUpdate() {
        this.tickFuse();
        if (this.isDead) {
            return;
        }
        this.primeWhenPowered();
        this.prevSquish = this.squish;
        this.squish += (this.targetSquish - this.squish) * 0.5F;
        super.onUpdate();
        if (this.onGround && !this.wasOnGround) {
            if (this.world.isRemote) {
                SulfurCubes.landingParticles.accept(this);
            }
            this.playSound(this.squishSound(), this.getSoundVolume(),
                    ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F + 1.0F) / 0.8F);
            this.targetSquish = -0.5F;
        } else if (!this.onGround && this.wasOnGround) {
            this.targetSquish = 1.0F;
        }
        this.wasOnGround = this.onGround;
        this.targetSquish *= 0.6F;
    }

    private void tickFuse() {
        if (this.fuse > 0) {
            this.fuse--;
        }
        if (this.fuse == 0 && this.archetype != null && this.archetype.explodes()) {
            this.clearLeashed(true, false);
            if (!this.world.isRemote) {
                boolean grief = net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.world, this);
                this.world.newExplosion(this, this.posX, this.posY + this.height * 0.0625D, this.posZ,
                        Archetype.EXPLOSION_POWER, false, grief);
            }
            this.setDead();
        }
    }

    /** 26.3: a TNT cube on or beside a powered position primes. */
    private void primeWhenPowered() {
        if (this.world.isRemote || !this.canExplode()) {
            return;
        }
        BlockPos here = new BlockPos(this);
        boolean powered = this.world.getRedstonePowerFromNeighbors(here) > 0;
        if (!powered) {
            IBlockState own = this.world.getBlockState(here);
            if (own.canProvidePower()) {
                for (EnumFacing f : EnumFacing.values()) {
                    if (own.getWeakPower(this.world, here, f) > 0) {
                        powered = true;
                        break;
                    }
                }
            }
        }
        if (powered) {
            this.primeTime(false);
        }
    }

    @Override
    protected void updateAITasks() {
        super.updateAITasks();
        if (this.hasBodyItem()) {
            // 26.3's SulfurCubeLookControl: holding a block, it squares itself to the nearest quarter turn.
            float a = this.rotationYaw % 90.0F;
            if (a >= 45.0F) {
                a -= 90.0F;
            }
            if (a < -45.0F) {
                a += 90.0F;
            }
            this.rotationYaw -= a;
            this.rotationYawHead = this.rotationYaw;
            this.renderYawOffset = this.rotationYaw;
        }
        if (this.pickupTimer > 0) {
            this.pickupTimer--;
        }
        if (this.pushSoundCooldown > 0) {
            this.pushSoundCooldown--;
        }
    }

    @Override
    protected boolean canDespawn() {
        return !this.hasBodyItem() && !this.fromBucket() && super.canDespawn();
    }

    /** 26.3's spawn rule is always true: no light or block check beyond 1.12.2's own. */
    @Override
    public boolean getCanSpawnHere() {
        IBlockState below = this.world.getBlockState(new BlockPos(this).down());
        return below.canEntitySpawn(this);
    }

    /** Counted as a monster for spawning (26.3's {@code MobCategory.MONSTER}), without being an {@code IMob}. */
    @Override
    public boolean isCreatureType(EnumCreatureType type, boolean forSpawnCount) {
        return type == EnumCreatureType.MONSTER;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return this.hasBodyItem() || super.canBreatheUnderwater();
    }

    @Override
    public boolean canBeLeashedTo(EntityPlayer player) {
        return this.hasBodyItem() && !this.getLeashed();
    }

    @Override
    public int getVerticalFaceSpeed() {
        return 0;
    }

    @Override
    protected void jump() {
        this.motionY = 0.42F;
        this.isAirBorne = true;
        ForgeHooks.onLivingJump(this);
    }

    // --- movement ------------------------------------------------------------------------------

    @Override
    public void travel(float strafe, float vertical, float forward) {
        Archetype a = this.archetype;
        if (a == null || !this.isServerWorld()) {
            super.travel(strafe, vertical, forward);
            return;
        }
        if (this.isInWater() || this.isInLava()) {
            super.travel(strafe, vertical, forward);
            if (a.buoyant) {
                // 26.3's travelInFluid: a buoyant cube bobs up toward the surface.
                float vibe = 0.2F * MathHelper.sin(this.ticksExisted * 0.4F);
                double immersion = this.fluidHeight() - this.height * 0.2D + vibe;
                if (immersion > 0.0D) {
                    this.motionY += Math.min(1.0D, immersion) * 0.04F;
                }
            }
            return;
        }
        // 26.3's travelInAir with the archetype's friction and air drag; holding a block makes it an
        // omnidirectional air mover (air drag slows it vertically too).
        float blockFriction = 1.0F;
        if (this.onGround) {
            BlockPos below = new BlockPos(MathHelper.floor(this.posX), MathHelper.floor(this.getEntityBoundingBox().minY) - 1,
                    MathHelper.floor(this.posZ));
            IBlockState state = this.world.getBlockState(below);
            blockFriction = Archetype.modifiedFriction(state.getBlock().getSlipperiness(state, this.world, below, this),
                    a.frictionModifier);
        }
        float speed = this.onGround ? this.getAIMoveSpeed() * (0.21600002F / (blockFriction * blockFriction * blockFriction))
                : this.jumpMovementFactor;
        this.moveRelative(strafe, vertical, forward, speed);
        this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
        if (!this.hasNoGravity()) {
            this.motionY -= GRAVITY;
        }
        float airDrag = Archetype.modifiedFriction(0.91F, a.airDragModifier);
        float friction = blockFriction * airDrag;
        this.motionX *= friction;
        this.motionY *= airDrag;
        this.motionZ *= friction;
        this.prevLimbSwingAmount = this.limbSwingAmount;
        this.limbSwingAmount += (0.0F - this.limbSwingAmount) * 0.4F;
    }

    /** @return how far above the cube's feet the liquid it is in reaches */
    private double fluidHeight() {
        AxisAlignedBB box = this.getEntityBoundingBox();
        double top = box.minY;
        for (int x = MathHelper.floor(box.minX); x < MathHelper.ceil(box.maxX); x++) {
            for (int z = MathHelper.floor(box.minZ); z < MathHelper.ceil(box.maxZ); z++) {
                for (int y = MathHelper.floor(box.minY); y < MathHelper.ceil(box.maxY) + 1; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    IBlockState s = this.world.getBlockState(p);
                    if (s.getMaterial().isLiquid()) {
                        float h = s.getBlock() instanceof net.minecraft.block.BlockLiquid
                                ? 1.0F - net.minecraft.block.BlockLiquid.getLiquidHeightPercent(s.getValue(net.minecraft.block.BlockLiquid.LEVEL))
                                : 1.0F;
                        top = Math.max(top, y + h);
                    }
                }
            }
        }
        return top - box.minY;
    }

    /**
     * 26.3's {@code restituteMovementAfterCollisions}: a cube that holds a block bounces off what it hits by its
     * bounciness (and off slime blocks and beds by theirs); honey blocks and sneaking stop the bounce. Each bounce
     * emits the {@code bounce} game event.
     */
    @Override
    public void move(MoverType type, double x, double y, double z) {
        if (this.archetype == null || this.noClip || this.world.isRemote) {
            super.move(type, x, y, z);
            return;
        }
        double mx = this.motionX;
        double my = this.motionY;
        double mz = this.motionZ;
        double ox = this.posX;
        double oy = this.posY;
        double oz = this.posZ;
        super.move(type, x, y, z);
        double dy = this.posY - oy;
        boolean xCollision = Math.abs((this.posX - ox) - x) > 1.0E-5D;
        boolean zCollision = Math.abs((this.posZ - oz) - z) > 1.0E-5D;
        boolean vertical = Math.abs(dy - y) > 1.0E-5D;
        boolean below = vertical && y < 0.0D;
        if (!(vertical && y != 0.0D) && !xCollision && !zCollision) {
            return;
        }
        double r = this.archetype.bounciness;
        double nx = this.motionX;
        double ny = this.motionY;
        double nz = this.motionZ;
        if (xCollision) {
            nx = -mx * r;
        }
        if (zCollision) {
            nz = -mz * r;
        }
        boolean bounced = r > 0.0D && (xCollision || zCollision);
        if (vertical) {
            if (below) {
                BlockPos p = new BlockPos(MathHelper.floor(this.posX), MathHelper.floor(this.posY - 0.2D), MathHelper.floor(this.posZ));
                Block block = this.world.getBlockState(p).getBlock();
                boolean honey = "honey_block".equals(block.getRegistryName() == null ? "" : block.getRegistryName().getPath());
                r = -my <= GRAVITY || honey ? 0.0D : Math.max(r, blockBounciness(block));
            }
            double gravity = 0.0D;
            double drag = 1.0D;
            if (r > 0.0D) {
                double portion = my == 0.0D ? 0.0D : dy / my;
                gravity = portion * GRAVITY;
                drag = 1.0D + (Archetype.modifiedFriction(0.91F, this.archetype.airDragModifier) - 1.0D) * portion;
                bounced = true;
            }
            ny = (gravity - my) * drag * r;
        }
        this.motionX = nx;
        this.motionY = ny;
        this.motionZ = nz;
        if (bounced) {
            // 26.2's bounce game event (Backport Core 1.4.4's GameEvent.BOUNCE): sculk hears a bouncing cube.
            vibration(GameEvent.BOUNCE, this);
            this.velocityChanged = true;
        }
    }

    private static double blockBounciness(Block block) {
        if (block == Blocks.SLIME_BLOCK) {
            return 1.0D;
        }
        return block instanceof BlockBed ? 0.66D : 0.0D;
    }

    // --- pushing and contact ---------------------------------------------------------------------

    @Override
    protected void collideWithEntity(Entity entity) {
        super.collideWithEntity(entity);
        this.applyContactDamage(entity);
    }

    @Override
    public void onCollideWithPlayer(EntityPlayer player) {
        super.onCollideWithPlayer(player);
        if (this.world.isRemote || this.archetype == null) {
            return;
        }
        // 26.3's playerPush: a player walking into a cube that holds a block shoves it.
        Entity pusher = player.isRiding() ? player.getLowestRidingEntity() : player;
        double dx = this.posX - pusher.posX;
        double dz = this.posZ - pusher.posZ;
        double top = this.posY + this.height;
        if (Math.sqrt(dx * dx + dz * dz) < 1.3D && pusher.posY <= top && pusher.posY + pusher.height > this.posY) {
            double kb = Math.max(0.0D, 1.0D - this.archetype.knockbackResistance);
            double len = Math.sqrt(dx * dx + dz * dz);
            double px = len < 1.0E-4D ? 0.0D : dx / len * kb;
            double pz = len < 1.0E-4D ? 0.0D : dz / len * kb;
            float scale = player.isRiding() ? 0.16F : 0.3F;
            double mx = player.posX - player.lastTickPosX;
            double my = player.posY - player.lastTickPosY;
            double mz = player.posZ - player.lastTickPosZ;
            double speed = MathHelper.clamp(Math.sqrt(mx * mx + my * my + mz * mz) * 2.0D * scale, 0.0D, 0.5D);
            double vx = px * speed;
            double vy = (this.onGround ? kb * 0.3F : 0.0D) * speed;
            double vz = pz * speed;
            this.velocityChanged = true;
            float threshold = this.archetype.pushSoundThreshold;
            if (vx * vx + vy * vy + vz * vz > threshold * threshold && this.pushSoundCooldown <= 0) {
                this.pushSoundCooldown = (int) (this.archetype.pushSoundCooldown * 20.0F);
                this.playSound(SulfurCubes.push(this.archetype), this.getSoundVolume(), 1.0F);
            }
            this.motionX += vx;
            this.motionY += vy;
            this.motionZ += vz;
            this.applyContactDamage(player);
        }
    }

    /** The hot archetype: 1 damage of {@code sulfurCubeHot}, from the cube only when the target is a player. */
    private void applyContactDamage(Entity entity) {
        if (this.world.isRemote || this.archetype == null || !this.archetype.hot()) {
            return;
        }
        DamageSource source = entity instanceof EntityPlayer ? new EntityDamageSource(SulfurCubes.HOT_DAMAGE, this)
                : new DamageSource(SulfurCubes.HOT_DAMAGE);
        entity.attackEntityFrom(source, 1.0F);
    }

    // --- damage and knockback --------------------------------------------------------------------

    public static boolean immuneWithBlock(DamageSource source) {
        return source.isExplosion() || IMMUNE.contains(source.getDamageType());
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (this.hasBodyItem() && !this.world.isRemote) {
            if (this.canExplode()) {
                Entity direct = source.getImmediateSource();
                if (source.isFireDamage() || direct instanceof EntityArrow && direct.isBurning()) {
                    this.primeTime(false);
                } else if (source.isExplosion()) {
                    this.primeTime(true);
                }
            }
            if (immuneWithBlock(source)) {
                if (!source.isExplosion() && !NO_KNOCKBACK.contains(source.getDamageType())) {
                    this.defaultKnockback(source, amount);
                }
                return true;
            }
        }
        this.hurtAmount = amount;
        try {
            return super.attackEntityFrom(source, amount);
        } finally {
            this.hurtAmount = -1;
        }
    }

    /** 26.3's {@code dealDefaultKnockback}: away from the attacker, or along a projectile's flight. */
    private void defaultKnockback(DamageSource source, float amount) {
        Entity direct = source.getImmediateSource();
        Entity attacker = source.getTrueSource();
        double xd;
        double zd;
        if (direct != null && direct != attacker) {
            xd = -direct.motionX;
            zd = -direct.motionZ;
        } else if (attacker != null) {
            xd = attacker.posX - this.posX;
            zd = attacker.posZ - this.posZ;
        } else {
            return;
        }
        this.hurtAmount = amount;
        try {
            this.knockBack(attacker != null ? attacker : direct, 0.4F, xd, zd);
        } finally {
            this.hurtAmount = -1;
        }
    }

    /**
     * 26.3's sulfur cube knockback while it holds a block: the archetype's horizontal and vertical power, turned by
     * where the attacker aims and stands, scaled by the square root of the damage, and the archetype's hit sound.
     */
    @Override
    public void knockBack(Entity attacker, float strength, double xRatio, double zRatio) {
        Archetype a = this.archetype;
        if (attacker == null || a == null) {
            super.knockBack(attacker, strength, xRatio, zRatio);
            return;
        }
        boolean fromEffect = this.hurtAmount < 0;
        float damage = fromEffect ? 1.0F : this.hurtAmount;
        Vec3d eye = attacker.getPositionEyes(1.0F);
        Vec3d look = attacker.getLookVec().normalize();
        Vec3d center = new Vec3d(this.posX, this.posY + this.height / 2.0D, this.posZ);
        float hp = a.horizontalPower;
        float vp = a.verticalPower;
        // Horizontal hit angle (scale 1.6): hitting off-center turns the push.
        Vec3d toTarget = center.subtract(eye).normalize();
        float angle = (float) Math.atan2(look.x * toTarget.z - look.z * toTarget.x, look.x * toTarget.x + look.z * toTarget.z);
        double rot = angle * 1.6F;
        double cos = Math.cos(rot);
        double sin = Math.sin(rot);
        double nx = xRatio * cos - zRatio * sin;
        double nz = xRatio * sin + zRatio * cos;
        // Vertical hit angle (scale 0.5): hitting low lifts more, hitting high pushes flatter.
        float half = this.height * 0.5F;
        Vec3d toTop = center.add(0, half, 0).subtract(eye).normalize();
        Vec3d toBottom = center.add(0, -half, 0).subtract(eye).normalize();
        float factor = (float) clampedMap(look.y, toTop.y, toBottom.y, -1.0D, 1.0D);
        float ratio = Math.abs(factor * 0.5F);
        if (factor < 0.0F) {
            ratio = -ratio;
        }
        float px = hp * (1.0F - ratio);
        float py = vp * (1.0F + ratio);
        // Vertical position angle (scale 0.8): an attacker below lifts it, above presses it down.
        double fx = this.posX - attacker.posX;
        double fy = this.posY - attacker.posY;
        double fz = this.posZ - attacker.posZ;
        float posAngle = (float) Math.atan2(-fy, Math.sqrt(fx * fx + fz * fz));
        double r = -posAngle * 0.8F;
        float rx = (float) (px * Math.cos(r) - py * Math.sin(r));
        float ry = (float) (px * Math.sin(r) + py * Math.cos(r));
        float hr = hp > 0.0F ? Math.abs(rx) / hp : 0.0F;
        float vr = vp > 0.0F ? Math.abs(ry) / vp : 0.0F;
        float max = Math.max(hr, vr);
        if (max > 1.0F) {
            rx /= max;
            ry /= max;
        }
        float mult = MathHelper.sqrt(damage) * (fromEffect ? strength * 0.25F : 1.0F);
        float kb = (float) (1.0D - a.knockbackResistance);
        float h = MathHelper.clamp(rx * mult * kb * 0.4F, -128.0F, 128.0F);
        float v = MathHelper.clamp(ry * mult * kb, -128.0F, 128.0F);
        double len = Math.sqrt(nx * nx + nz * nz);
        if (len > 1.0E-4D) {
            this.motionX -= nx / len * h;
            this.motionZ -= nz / len * h;
        }
        this.motionY += v * 1.2D;
        this.velocityChanged = true;
        this.isAirBorne = true;
        this.playSound(SulfurCubes.hit(a), this.getSoundVolume(), 1.0F);
    }

    private static double clampedMap(double v, double fromMin, double fromMax, double toMin, double toMax) {
        double t = fromMax == fromMin ? 0.0D : (v - fromMin) / (fromMax - fromMin);
        t = MathHelper.clamp(t, 0.0D, 1.0D);
        return toMin + (toMax - toMin) * t;
    }

    /** Explosions push a cube that holds a block by the archetype's explosion knockback resistance. */
    public double explosionKnockbackResistance() {
        return this.archetype == null ? 0.0D : this.archetype.explosionKnockbackResistance;
    }

    // --- interaction ------------------------------------------------------------------------------

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        if (this.isChild()) {
            if (held.getItem() == Items.SLIME_BALL) {
                if (!this.world.isRemote) {
                    int age = this.getGrowingAge();
                    if (!player.capabilities.isCreativeMode) {
                        held.shrink(1);
                    }
                    this.ageUp((int) ((-age / 20) * 0.1F), true);
                    this.playSound(SulfurCubes.smallEat, this.getSoundVolume(), this.getSoundPitch());
                }
                return true;
            }
            return super.processInteract(player, hand);
        }
        if (this.isPrimed()) {
            return false;
        }
        if (this.canExplode() && (held.getItem() == Items.FLINT_AND_STEEL || held.getItem() == Items.FIRE_CHARGE)) {
            if (!this.world.isRemote) {
                this.primeTime(false);
                if (held.getItem() == Items.FLINT_AND_STEEL) {
                    held.damageItem(1, player);
                } else if (!player.capabilities.isCreativeMode) {
                    held.shrink(1);
                }
                player.addStat(StatList.getObjectUseStats(held.getItem()));
            }
            return true;
        }
        if (held.getItem() == Items.SHEARS && this.hasBodyItem()) {
            if (!this.world.isRemote) {
                this.shear();
                vibration(GameEvent.SHEAR, player);
                held.damageItem(1, player);
            }
            return true;
        }
        if (ArchetypeItems.swallowable(held)) {
            boolean tnt = ArchetypeItems.of(held) == Archetype.EXPLOSIVE;
            boolean worked = this.equipItem(held);
            if (worked && !this.world.isRemote) {
                if (!player.capabilities.isCreativeMode) {
                    held.shrink(1);
                }
                vibration(GameEvent.ENTITY_INTERACT, player);
                if (tnt && player instanceof EntityPlayerMP) {
                    SulfurCubes.grantUhOh((EntityPlayerMP) player, "give_tnt_directly");
                }
            }
            return worked;
        }
        if (held.getItem() == Items.BUCKET && this.isEntityAlive()) {
            if (!this.world.isRemote) {
                this.playSound(SulfurCubes.bucketFill, 1.0F, 1.0F);
                ItemStack bucket = ItemSulfurCubeBucket.of(this);
                if (player.capabilities.isCreativeMode) {
                    if (!player.inventory.addItemStackToInventory(bucket)) {
                        player.dropItem(bucket, false);
                    }
                } else {
                    held.shrink(1);
                    if (held.isEmpty()) {
                        player.setHeldItem(hand, bucket);
                    } else if (!player.inventory.addItemStackToInventory(bucket)) {
                        player.dropItem(bucket, false);
                    }
                }
                this.setDead();
            }
            return true;
        }
        return super.processInteract(player, hand);
    }

    @Override
    public boolean canPickUpLoot() {
        return !this.hasBodyItem() && !this.isChild();
    }

    @Override
    protected void updateEquipmentIfNeeded(EntityItem item) {
        ItemStack stack = item.getItem();
        if (this.hasBodyItem() || this.isChild() || this.pickupTimer > 0 || !ArchetypeItems.swallowable(stack)) {
            return;
        }
        String thrower = item.getThrower();
        this.onItemPickup(item, 1);
        this.setBodyItem(stack.splitStack(1));
        this.playSound(SulfurCubes.absorb, this.getSoundVolume(), 1.0F);
        if (stack.isEmpty()) {
            item.setDead();
        }
        if (ArchetypeItems.of(this.getBodyItem()) == Archetype.EXPLOSIVE && thrower != null) {
            EntityPlayer p = this.world.getPlayerEntityByName(thrower);
            if (p instanceof EntityPlayerMP) {
                SulfurCubes.grantUhOh((EntityPlayerMP) p, "pick_up_dropped_tnt");
            }
        }
    }

    // --- death -------------------------------------------------------------------------------------

    @Override
    protected void dropEquipment(boolean wasRecentlyHit, int lootingModifier) {
        super.dropEquipment(wasRecentlyHit, lootingModifier);
        if (this.hasBodyItem()) {
            this.entityDropItem(this.getBodyItem().copy(), this.passengerOffset());
            this.dataManager.set(BODY, ItemStack.EMPTY);
        }
    }

    @Override
    protected int getExperiencePoints(EntityPlayer player) {
        return this.isChild() ? 0 : 1 + this.rand.nextInt(2);
    }

    @Nullable
    @Override
    protected ResourceLocation getLootTable() {
        return null;
    }

    /** 26.3's {@code AbstractCubeMob.remove}: an adult that dies splits into two babies, unless it was primed. */
    @Override
    public void setDead() {
        int size = this.getCubeSize();
        if (!this.world.isRemote && size > 1 && this.getHealth() <= 0.0F && !this.isPrimed() && !this.isDead) {
            float offset = this.width / 2.0F;
            for (int i = 0; i < 2; i++) {
                float xd = ((i % 2) - 0.5F) * offset;
                float zd = ((i / 2) - 0.5F) * offset;
                EntitySulfurCube child = new EntitySulfurCube(this.world);
                if (this.hasCustomName()) {
                    child.setCustomNameTag(this.getCustomNameTag());
                }
                if (this.isNoDespawnRequired()) {
                    child.enablePersistence();
                }
                child.setNoAI(this.isAIDisabled());
                child.setInvisible(this.isInvisible());
                child.setCubeSize(size / 2, true);
                child.setGrowingAge(-24000);
                child.setLocationAndAngles(this.posX + xd, this.posY + 0.5D, this.posZ + zd, this.rand.nextFloat() * 360.0F, 0.0F);
                this.world.spawnEntity(child);
            }
        }
        super.setDead();
    }

    // --- sounds -------------------------------------------------------------------------------------

    @Override
    public SoundCategory getSoundCategory() {
        return SoundCategory.NEUTRAL;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F * this.getCubeSize();
    }

    public float getSoundPitch() {
        float adjust = this.isTiny() ? 1.4F : 0.8F;
        return ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F + 1.0F) * adjust;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return this.isTiny() ? SulfurCubes.smallHurt : SulfurCubes.hurt;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.isTiny() ? SulfurCubes.smallDeath : SulfurCubes.death;
    }

    private SoundEvent squishSound() {
        if (this.isTiny()) {
            return SulfurCubes.smallSquish;
        }
        return this.hasBodyItem() ? SulfurCubes.bounce : SulfurCubes.squish;
    }

    SoundEvent jumpSound() {
        return this.isTiny() ? SulfurCubes.smallJump : SulfurCubes.jump;
    }

    @Override
    protected void playStepSound(BlockPos pos, Block block) {
        if (!this.hasBodyItem()) {
            super.playStepSound(pos, block);
        }
    }

    // --- saving ---------------------------------------------------------------------------------------

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("Size", this.getCubeSize() - 1);
        tag.setBoolean("wasOnGround", this.wasOnGround);
        tag.setInteger("pickup_timer", this.pickupTimer);
        tag.setBoolean("from_bucket", this.fromBucket());
        tag.setInteger("fuse", this.fuse);
        if (this.hasBodyItem()) {
            tag.setTag("body_item", this.getBodyItem().writeToNBT(new NBTTagCompound()));
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        this.setCubeSize(tag.getInteger("Size") + 1, false);
        super.readEntityFromNBT(tag);
        this.wasOnGround = tag.getBoolean("wasOnGround");
        this.pickupTimer = tag.getInteger("pickup_timer");
        this.setFromBucket(tag.getBoolean("from_bucket"));
        this.setBodyItem(tag.hasKey("body_item") ? new ItemStack(tag.getCompoundTag("body_item")) : ItemStack.EMPTY);
        this.fuse = tag.hasKey("fuse") ? tag.getInteger("fuse") : -1;
        this.dataManager.set(MAX_FUSE, this.fuse);
    }

    // --- AI ---------------------------------------------------------------------------------------------

    CubeMoveHelper cubeMove() {
        return (CubeMoveHelper) this.moveHelper;
    }

    boolean aiActive() {
        return !this.hasBodyItem();
    }

    /** 26.3's {@code CubeMobMoveControl}: hops toward a direction, more often when it wants something. */
    static final class CubeMoveHelper extends EntityMoveHelper {
        private final EntitySulfurCube cube;
        private float yRot;
        private int jumpDelay;
        private boolean aggressive;

        CubeMoveHelper(EntitySulfurCube cube) {
            super(cube);
            this.cube = cube;
            this.yRot = 180.0F * cube.rotationYaw / (float) Math.PI;
        }

        void setDirection(float yRot, boolean aggressive) {
            this.yRot = yRot;
            this.aggressive = aggressive;
        }

        void setWantedMovement(double speed) {
            this.speed = speed;
            this.action = Action.MOVE_TO;
        }

        @Override
        public void onUpdateMoveHelper() {
            if (this.cube.hasBodyItem()) {
                return;
            }
            this.entity.rotationYaw = this.limitAngle(this.entity.rotationYaw, this.yRot, 90.0F);
            this.entity.rotationYawHead = this.entity.rotationYaw;
            this.entity.renderYawOffset = this.entity.rotationYaw;
            if (this.action != Action.MOVE_TO) {
                this.entity.setMoveForward(0.0F);
                return;
            }
            this.action = Action.WAIT;
            float speed = (float) (this.speed * this.entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue());
            if (this.entity.onGround) {
                this.entity.setAIMoveSpeed(speed);
                if (this.jumpDelay-- <= 0) {
                    this.jumpDelay = this.cube.rand.nextInt(20) + 10;
                    if (this.aggressive) {
                        this.jumpDelay /= 3;
                    }
                    this.cube.getJumpHelper().setJumping();
                    this.cube.playSound(this.cube.jumpSound(), this.cube.getSoundVolume(), this.cube.getSoundPitch());
                } else {
                    this.cube.moveStrafing = 0.0F;
                    this.cube.moveForward = 0.0F;
                    this.entity.setAIMoveSpeed(0.0F);
                }
            } else {
                this.entity.setAIMoveSpeed(speed);
            }
        }
    }

    private static final class FloatGoal extends EntityAIBase {
        private final EntitySulfurCube cube;

        FloatGoal(EntitySulfurCube cube) {
            this.cube = cube;
            this.setMutexBits(5);
            if (cube.getNavigator() instanceof net.minecraft.pathfinding.PathNavigateGround) {
                ((net.minecraft.pathfinding.PathNavigateGround) cube.getNavigator()).setCanSwim(true);
            }
        }

        @Override
        public boolean shouldExecute() {
            return this.cube.aiActive() && (this.cube.isInWater() || this.cube.isInLava());
        }

        @Override
        public void updateTask() {
            if (this.cube.getRNG().nextFloat() < 0.8F) {
                this.cube.getJumpHelper().setJumping();
            }
            this.cube.cubeMove().setWantedMovement(1.2D);
        }
    }

    /** 26.3's {@code TemptGoal.ForNonPathfinders}: 8 blocks, stops 1 block away, 5 s calm-down after. */
    private static final class TemptGoal extends EntityAIBase {
        private final EntitySulfurCube cube;
        private EntityPlayer player;
        private int calmDown;

        TemptGoal(EntitySulfurCube cube) {
            this.cube = cube;
            this.setMutexBits(2);
        }

        private boolean tempts(ItemStack stack) {
            return this.cube.isChild() ? stack.getItem() == Items.SLIME_BALL : ArchetypeItems.swallowable(stack);
        }

        @Override
        public boolean shouldExecute() {
            if (this.calmDown > 0) {
                this.calmDown--;
                return false;
            }
            if (!this.cube.aiActive()) {
                return false;
            }
            this.player = null;
            double best = 64.0D;
            for (EntityPlayer p : this.cube.world.playerEntities) {
                if (p.isSpectator() || !p.isEntityAlive()) {
                    continue;
                }
                double d = p.getDistanceSq(this.cube);
                if (d < best && (this.tempts(p.getHeldItemMainhand()) || this.tempts(p.getHeldItemOffhand()))) {
                    best = d;
                    this.player = p;
                }
            }
            return this.player != null;
        }

        @Override
        public void resetTask() {
            this.player = null;
            this.calmDown = 100;
            this.cube.cubeMove().setWantedMovement(0.0D);
        }

        @Override
        public void updateTask() {
            this.cube.faceEntity(this.player, 10.0F, 10.0F);
            if (this.cube.getDistanceSq(this.player) < 1.0D) {
                this.cube.cubeMove().setWantedMovement(0.0D);
            } else {
                this.cube.cubeMove().setDirection(this.cube.rotationYaw, true);
            }
        }
    }

    /** 26.3's {@code SulfurCubeSearchForItemsGoal}: an adult hops to a swallowable item within 8 blocks. */
    private static final class SearchForItemsGoal extends EntityAIBase {
        private final EntitySulfurCube cube;
        private EntityItem target;

        SearchForItemsGoal(EntitySulfurCube cube) {
            this.cube = cube;
            this.setMutexBits(2);
        }

        @Override
        public boolean shouldExecute() {
            if (this.cube.isChild() || this.cube.pickupTimer > 0 || !this.cube.aiActive()) {
                return false;
            }
            List<EntityItem> items = this.cube.world.getEntitiesWithinAABB(EntityItem.class,
                    this.cube.getEntityBoundingBox().grow(8.0D, 8.0D, 8.0D),
                    e -> e != null && !e.cannotPickup() && e.isEntityAlive() && ArchetypeItems.swallowable(e.getItem()));
            this.target = null;
            double best = Double.MAX_VALUE;
            for (EntityItem e : items) {
                double d = e.getDistanceSq(this.cube);
                if (d < best) {
                    best = d;
                    this.target = e;
                }
            }
            return this.target != null;
        }

        @Override
        public void updateTask() {
            this.cube.faceEntity(this.target, 10.0F, 10.0F);
            this.cube.cubeMove().setDirection(this.cube.rotationYaw, true);
        }
    }

    private static final class RandomDirectionGoal extends EntityAIBase {
        private final EntitySulfurCube cube;
        private float chosen;
        private int next;

        RandomDirectionGoal(EntitySulfurCube cube) {
            this.cube = cube;
            this.setMutexBits(2);
        }

        @Override
        public boolean shouldExecute() {
            return this.cube.aiActive() && this.cube.getAttackTarget() == null && (this.cube.onGround || this.cube.isInWater()
                    || this.cube.isInLava() || this.cube.isPotionActive(MobEffects.LEVITATION));
        }

        @Override
        public void updateTask() {
            if (--this.next <= 0) {
                this.next = 40 + this.cube.getRNG().nextInt(60);
                this.chosen = this.cube.getRNG().nextInt(360);
            }
            this.cube.cubeMove().setDirection(this.chosen, false);
        }
    }

    private static final class KeepOnJumpingGoal extends EntityAIBase {
        private final EntitySulfurCube cube;

        KeepOnJumpingGoal(EntitySulfurCube cube) {
            this.cube = cube;
            this.setMutexBits(5);
        }

        @Override
        public boolean shouldExecute() {
            return this.cube.aiActive() && !this.cube.isRiding();
        }

        @Override
        public void updateTask() {
            this.cube.cubeMove().setWantedMovement(1.0D);
        }
    }
}
