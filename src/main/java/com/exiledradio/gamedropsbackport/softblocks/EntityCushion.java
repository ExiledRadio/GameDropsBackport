package com.exiledradio.gamedropsbackport.softblocks;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fluids.IFluidBlock;

/**
 * 26.3's cushion ({@code Cushion}, a {@code BlockAttachedEntity}): a 1 x 0.25 seat with no collision, resting on
 * the block below. Right-click to sit (sneaking does not), any hit breaks it into its item, and every 100 ticks it
 * checks its support and breaks if the block under it is gone or blocks now fill its space. Fire and lava break it;
 * so does being moved (a piston). The rider sits as on 26.3: the cushion's top is its seat.
 */
public class EntityCushion extends Entity {

    private static final DataParameter<Byte> COLOR = EntityDataManager.createKey(EntityCushion.class, DataSerializers.BYTE);
    private static final int CHECK_INTERVAL = 100;
    private static final double ANCHOR = 0.015625;

    private int ticksSinceCheck;

    public EntityCushion(World world) {
        super(world);
        setSize(1.0F, 0.25F);
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(COLOR, (byte) 0);
    }

    public EnumDyeColor getColor() {
        return EnumDyeColor.byMetadata(this.dataManager.get(COLOR));
    }

    public void setColor(EnumDyeColor color) {
        this.dataManager.set(COLOR, (byte) color.getMetadata());
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        if (this.world.isRemote) {
            return;
        }
        if (this.posY < -64.0D) {
            setDead();
            return;
        }
        if (this.ticksSinceCheck++ >= CHECK_INTERVAL) {
            this.ticksSinceCheck = 0;
            breakIfBurning();
            if (!this.isDead && !survives()) {
                setDead();
                dropItem(null);
            }
        }
    }

    /** 26.3: fire in its space hurts it ({@code destroyIfInFire}), and lava does through its fluid. */
    void breakIfBurning() {
        if (this.isDead) {
            return;
        }
        AxisAlignedBB box = getEntityBoundingBox().shrink(1.0E-7D);
        for (BlockPos p : BlockPos.getAllInBoxMutable(new BlockPos(box.minX, box.minY, box.minZ),
                new BlockPos(box.maxX, box.maxY, box.maxZ))) {
            IBlockState state = this.world.getBlockState(p);
            if (state.getBlock() == Blocks.FIRE) {
                attackEntityFrom(DamageSource.IN_FIRE, 1.0F);
                return;
            }
            if (state.getMaterial() == Material.LAVA) {
                attackEntityFrom(DamageSource.LAVA, 4.0F);
                return;
            }
        }
    }

    public boolean survives() {
        return wouldSurviveAt(this.world, getEntityBoundingBox());
    }

    static boolean canBePlacedAt(World world, AxisAlignedBB box) {
        return wouldSurviveAt(world, box) && !isAnchorBuried(world, box);
    }

    static boolean wouldSurviveAt(World world, AxisAlignedBB box) {
        return hasAnchorBelow(world, box) && !isCoveredBySuffocatingBlocks(world, box);
    }

    /** A block whose outline reaches into the 1/64 under the cushion (fluids have no outline in 26.x). */
    private static boolean hasAnchorBelow(World world, AxisAlignedBB box) {
        AxisAlignedBB anchor = new AxisAlignedBB(box.minX, box.minY - ANCHOR, box.minZ,
                Math.nextDown(box.maxX), box.minY, Math.nextDown(box.maxZ));
        AxisAlignedBB search = anchor.expand(0.0D, -0.125D, 0.0D);
        for (BlockPos p : BlockPos.getAllInBoxMutable(new BlockPos(search.minX, search.minY, search.minZ),
                new BlockPos(search.maxX, search.maxY, search.maxZ))) {
            IBlockState state = world.getBlockState(p);
            if (state.getMaterial() == Material.AIR || state.getBlock() instanceof BlockLiquid
                    || state.getBlock() instanceof IFluidBlock) {
                continue;
            }
            AxisAlignedBB shape = state.getBoundingBox(world, p);
            if (shape != null && shape.maxX > shape.minX && shape.offset(p).intersects(anchor)) {
                return true;
            }
        }
        return false;
    }

    /** The bottom 1/64 of its space is fully covered by collision boxes: it would be inside a block. */
    private static boolean isAnchorBuried(World world, AxisAlignedBB box) {
        AxisAlignedBB slice = new AxisAlignedBB(box.minX, box.minY, box.minZ, box.maxX, box.minY + ANCHOR, box.maxZ)
                .shrink(1.0E-7D);
        java.util.List<AxisAlignedBB> colliders = world.getCollisionBoxes(null, slice);
        if (colliders.isEmpty()) {
            return false;
        }
        double y = (slice.minY + slice.maxY) / 2.0D;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                double x = slice.minX + (slice.maxX - slice.minX) * (i + 0.5D) / 8.0D;
                double z = slice.minZ + (slice.maxZ - slice.minZ) * (j + 0.5D) / 8.0D;
                boolean covered = false;
                for (AxisAlignedBB c : colliders) {
                    if (x > c.minX && x < c.maxX && y > c.minY && y < c.maxY && z > c.minZ && z < c.maxZ) {
                        covered = true;
                        break;
                    }
                }
                if (!covered) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isCoveredBySuffocatingBlocks(World world, AxisAlignedBB box) {
        AxisAlignedBB in = box.shrink(1.0E-7D);
        for (BlockPos p : BlockPos.getAllInBoxMutable(new BlockPos(in.minX, in.minY, in.minZ),
                new BlockPos(in.maxX, in.maxY, in.maxZ))) {
            if (!world.getBlockState(p).causesSuffocation()) {
                return false;
            }
        }
        return true;
    }

    /** Breaks into its item (none for a creative player), with 26.3's sound and wool particles. */
    void dropItem(Entity causedBy) {
        playSound(SoftBlocks.cushionBreak, 1.0F, 1.0F);
        if (this.world instanceof WorldServer) {
            ((WorldServer) this.world).spawnParticle(EnumParticleTypes.BLOCK_CRACK, this.posX,
                    this.posY + this.height * 0.6666666666666666D, this.posZ, 10, this.width / 4.0F, this.height / 4.0F,
                    this.width / 4.0F, 0.05D, net.minecraft.block.Block.getStateId(
                            Blocks.WOOL.getStateFromMeta(getColor().getMetadata())));
        }
        if (!this.world.getGameRules().getBoolean("doEntityDrops")) {
            return;
        }
        if (causedBy instanceof EntityPlayer && ((EntityPlayer) causedBy).capabilities.isCreativeMode) {
            return;
        }
        ItemStack stack = item();
        if (hasCustomName()) {
            stack.setStackDisplayName(getCustomNameTag());
        }
        EntityItem dropped = entityDropItem(stack, 0.0F);
        if (dropped != null && causedBy instanceof EntityLightningBolt) {
            // 26.3: the item survives the strike that dropped it (20 ticks); 1.12.2 can only make it fireproof.
            dropped.setEntityInvulnerable(true);
            SoftBlocks.unprotectLater(dropped);
        }
    }

    ItemStack item() {
        return new ItemStack(SoftBlocks.cushion, 1, getColor().getMetadata());
    }

    private boolean denied(DamageSource source) {
        Entity by = source.getTrueSource();
        if (by instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) by;
            return !player.capabilities.allowEdit || !this.world.isBlockModifiable(player, new BlockPos(this));
        }
        return false;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (isEntityInvulnerable(source) || denied(source)) {
            return false;
        }
        if (source.isExplosion() && source.getImmediateSource() != null && source.getImmediateSource().isInWater()) {
            return false;
        }
        if (this.world.isRemote) {
            return true;
        }
        if (!this.world.getGameRules().getBoolean("mobGriefing") && source.getTrueSource() instanceof EntityLiving) {
            return false;
        }
        if (!this.isDead) {
            setDead();
            markVelocityChanged();
            dropItem(source.getTrueSource());
        }
        return true;
    }

    @Override
    public void onStruckByLightning(EntityLightningBolt bolt) {
        if (!this.isDead && !this.world.isRemote) {
            setDead();
            dropItem(bolt);
        }
    }

    @Override
    public void move(MoverType type, double x, double y, double z) {
        if (!this.world.isRemote && !this.isDead && x * x + y * y + z * z > 0.0D) {
            setDead();
            dropItem(null);
        }
    }

    @Override
    public void addVelocity(double x, double y, double z) {
        if (!this.world.isRemote && !this.isDead && x * x + y * y + z * z > 0.0D) {
            setDead();
            dropItem(null);
        }
    }

    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        if (player.isSneaking() || isBeingRidden()) {
            return false;
        }
        if (!this.world.isRemote && player.startRiding(this)) {
            playSound(SoftBlocks.cushionSit, 1.0F, 1.0F);
        }
        return true;
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!this.world.isRemote && !this.isDead) {
            playSound(SoftBlocks.cushionGetUp, 1.0F, 1.0F);
        }
    }

    /** 26.3's attachment points: the seat at the cushion's top (0.25), the player's at 0.6 above its feet. */
    @Override
    public double getMountedYOffset() {
        return 0.0D;
    }

    @Override
    public boolean canBeCollidedWith() {
        return !this.isDead;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox() {
        return null;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public ItemStack getPickedResult(RayTraceResult target) {
        return item();
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        String name = tag.getString("color");
        for (EnumDyeColor c : EnumDyeColor.values()) {
            if (SoftColors.modern(c).equals(name)) {
                setColor(c);
            }
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        tag.setString("color", SoftColors.modern(getColor()));
    }
}
