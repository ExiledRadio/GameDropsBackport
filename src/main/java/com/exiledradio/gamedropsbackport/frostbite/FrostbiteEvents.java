package com.exiledradio.gamedropsbackport.frostbite;

import com.exiledradio.backportcore.freeze.FreezeTicks;
import com.exiledradio.backportcore.terrain.ModernGeneration;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.monster.EntityHusk;
import net.minecraft.entity.monster.EntityStray;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.event.entity.living.ZombieEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

/**
 * The frostbite stream's work outside its classes:
 * <ul>
 *   <li>26.4's freezing conversions ({@code Zombie}'s new tracker): a zombie in powder snow, wearing no leather, for
 *       140 ticks starts a 300-tick conversion into a frostbite (level event 1056); a husk becomes a zombie (1041) and
 *       Update Aquatic's drowned a zombie (1058) the same way. Leaving the snow resets it. Powder snow is Caves &amp;
 *       Cliffs' (any mod's {@code powder_snow}); equipment and the ability to pick up loot carry over.</li>
 *   <li>Frostbites call frostbites as reinforcements ({@code this.getType()}).</li>
 *   <li>{@code canBeAffected}: Freezing never lands on what can't freeze ({@code #immune_to_freezing}); when it does it
 *       plays {@code event.mob_effect.freezing} ({@code withSoundOnAdded}).</li>
 *   <li>In Caves &amp; Cliffs' 1.18 terrain, its frozen peaks, jagged peaks and snowy slopes spawn frostbites (25, 10,
 *       10) and strays (95), by the generator's biome.</li>
 * </ul>
 */
public final class FrostbiteEvents {

    public static final int FREEZING_TICKS = 140;
    public static final int CONVERSION_TICKS = 300;
    static final String TAG = "gamedropsbackport:Freezing";
    private static final ResourceLocation DROWNED = new ResourceLocation("updateaquaticbackport", "drowned");

    @SubscribeEvent
    public void livingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase e = event.getEntityLiving();
        if (e.world.isRemote || !(e instanceof EntityZombie) || e instanceof EntityFrostbite || !e.isEntityAlive()) {
            return;
        }
        ResourceLocation id = EntityList.getKey(e);
        boolean zombie = e.getClass() == EntityZombie.class, husk = e instanceof EntityHusk, drowned = DROWNED.equals(id);
        if (!zombie && !husk && !drowned) {
            return;
        }
        if (((EntityLiving) e).isAIDisabled()) {
            return;
        }
        NBTTagCompound data = e.getEntityData();
        boolean afflicted = inPowderSnow(e) && !wearsLeather(e);
        if (!afflicted) {
            data.removeTag(TAG);
            return;
        }
        NBTTagCompound f = data.getCompoundTag(TAG);
        if (f.hasKey("ConversionTime")) {
            int left = f.getInteger("ConversionTime") - 1;
            if (left < 0) {
                data.removeTag(TAG);
                convert((EntityZombie) e, zombie);
                return;
            }
            f.setInteger("ConversionTime", left);
        } else {
            int time = f.getInteger("FreezingTime") + 1;
            f.setInteger("FreezingTime", time);
            if (time >= FREEZING_TICKS) {
                f.setInteger("ConversionTime", CONVERSION_TICKS);
            }
        }
        data.setTag(TAG, f);
    }

    /** {@code isInPowderSnow}: a powder snow block inside the entity's box. */
    static boolean inPowderSnow(EntityLivingBase e) {
        AxisAlignedBB box = e.getEntityBoundingBox().shrink(0.001D);
        for (BlockPos p : BlockPos.getAllInBoxMutable(new BlockPos(MathHelper.floor(box.minX), MathHelper.floor(box.minY), MathHelper.floor(box.minZ)),
                new BlockPos(MathHelper.floor(box.maxX), MathHelper.floor(box.maxY), MathHelper.floor(box.maxZ)))) {
            IBlockState s = e.world.getBlockState(p);
            if (EntityFrostbite.isPowderSnow(s)) {
                return true;
            }
        }
        return false;
    }

    static boolean wearsLeather(EntityLivingBase e) {
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            if (slot.getSlotType() == EntityEquipmentSlot.Type.ARMOR && FreezeTicks.warm(e.getItemStackFromSlot(slot))) {
                return true;
            }
        }
        return false;
    }

    /** The zombie to a frostbite; a husk or drowned to a zombie. */
    static void convert(EntityZombie from, boolean toFrostbite) {
        World world = from.world;
        EntityZombie to = toFrostbite ? new EntityFrostbite(world) : new EntityZombie(world);
        to.copyLocationAndAnglesFrom(from);
        to.setChild(from.isChild());
        to.setNoAI(from.isAIDisabled());
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            to.setItemStackToSlot(slot, from.getItemStackFromSlot(slot).copy());
        }
        to.setCanPickUpLoot(from.canPickUpLoot());
        if (from.hasCustomName()) {
            to.setCustomNameTag(from.getCustomNameTag());
            to.setAlwaysRenderNameTag(from.getAlwaysRenderNameTag());
        }
        if (from.isNoDespawnRequired()) {
            to.enablePersistence();
        }
        world.removeEntity(from);
        world.spawnEntity(to);
        SoundEvent sound;
        float pitch;
        if (toFrostbite) {
            sound = FrostbiteContent.zombieConverted;
            pitch = world.rand.nextFloat() * 0.1F + 0.9F;
        } else if (from instanceof EntityHusk) {
            sound = FrostbiteContent.huskConverted;
            pitch = (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F + 1.0F;
        } else {
            sound = FrostbiteContent.drownedConverted;
            pitch = (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F + 1.0F;
        }
        world.playSound(null, new BlockPos(to), sound, SoundCategory.HOSTILE, 2.0F, pitch);
    }

    @SubscribeEvent
    public void summonAid(ZombieEvent.SummonAidEvent event) {
        if (event.getEntity() instanceof EntityFrostbite) {
            event.setCustomSummonedAid(new EntityFrostbite(event.getWorld()));
            event.setResult(Event.Result.ALLOW);
        }
    }

    @SubscribeEvent
    public void applicable(PotionEvent.PotionApplicableEvent event) {
        if (event.getPotionEffect().getPotion() == FrostbiteContent.freezing && FreezeTicks.immune(event.getEntityLiving())) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public void added(PotionEvent.PotionAddedEvent event) {
        EntityLivingBase e = event.getEntityLiving();
        if (event.getPotionEffect().getPotion() == FrostbiteContent.freezing && event.getOldPotionEffect() == null && !e.world.isRemote
                && !e.isSilent()) {
            e.world.playSound(null, e.posX, e.posY, e.posZ, FrostbiteContent.applyFreezing, e.getSoundCategory(), 1.0F, 1.0F);
        }
    }

    @SubscribeEvent
    public void potentialSpawns(WorldEvent.PotentialSpawns event) {
        if (event.getType() != EnumCreatureType.MONSTER) {
            return;
        }
        String modern = ModernGeneration.biomeId(event.getWorld(), event.getPos());
        if (modern == null) {
            return;
        }
        String path = modern.substring(modern.indexOf(':') + 1);
        int frostbite = "frozen_peaks".equals(path) ? 25 : "jagged_peaks".equals(path) || "snowy_slopes".equals(path) ? 10 : 0;
        if (frostbite == 0) {
            return;
        }
        List<Biome.SpawnListEntry> list = event.getList();
        if (!has(list, EntityFrostbite.class)) {
            list.add(new Biome.SpawnListEntry(EntityFrostbite.class, frostbite, FrostbiteContent.GROUP, FrostbiteContent.GROUP));
        }
        if (!has(list, EntityStray.class)) {
            list.add(new Biome.SpawnListEntry(EntityStray.class, 95, FrostbiteContent.GROUP, FrostbiteContent.GROUP));
        }
    }

    private static boolean has(List<Biome.SpawnListEntry> list, Class<?> type) {
        for (Biome.SpawnListEntry e : list) {
            if (e.entityClass == type) {
                return true;
            }
        }
        return false;
    }
}
