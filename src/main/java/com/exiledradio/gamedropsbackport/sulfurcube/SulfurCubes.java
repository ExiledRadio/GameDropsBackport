package com.exiledradio.gamedropsbackport.sulfurcube;

import com.exiledradio.backportcore.creative.CreativePlacement;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.ModConfig;

import net.minecraft.advancements.Advancement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityRegistry;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Stream 3 (26.2): the sulfur cube, its bucket, sounds, spawning and the advancement <i>Uh Oh</i>. Everything is
 * registered from here, behind {@code sulfurCube.enabled}.
 *
 * <h2>Spawning</h2>
 * 26.3 spawns sulfur cubes only in the sulfur caves (monster category, weight 100, groups of 2-4, at any light). The
 * caves are stream 4's; a cube may spawn where the biome is {@code gamedropsbackport:sulfur_caves}, or underground
 * on sulfur, cinnabar or potent sulfur, which only the sulfur caves generate.
 *
 * <h2>Blocks it swallows</h2>
 * {@link ArchetypeItems}: 1.12.2's blocks, stream 2's sulfur and cinnabar, and the blocks of other family mods
 * by name when they are installed (Rule 2).
 */
@Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID)
public final class SulfurCubes {

    public static final ResourceLocation ID = new ResourceLocation(GameDropsBackport.MOD_ID, "sulfur_cube");
    public static final ResourceLocation UH_OH = new ResourceLocation(GameDropsBackport.MOD_ID, "husbandry/uh_oh");
    /** 26.3's damage type {@code sulfur_cube_hot} (message id {@code sulfurCubeHot}). */
    public static final String HOT_DAMAGE = "sulfurCubeHot";
    /** 26.3's spawn egg colors are a texture; these are its two main colors. */
    public static final int EGG_PRIMARY = 0xE3E290;
    public static final int EGG_SECONDARY = 0x8F654F;

    /** Before the sound fields: they add themselves to it. */
    private static final List<SoundEvent> SOUNDS = new ArrayList<SoundEvent>();

    public static final SoundEvent absorb = sound("entity.sulfur_cube.absorb");
    public static final SoundEvent bounce = sound("entity.sulfur_cube.bounce");
    public static final SoundEvent death = sound("entity.sulfur_cube.death");
    public static final SoundEvent eject = sound("entity.sulfur_cube.eject");
    public static final SoundEvent hurt = sound("entity.sulfur_cube.hurt");
    public static final SoundEvent jump = sound("entity.sulfur_cube.jump");
    public static final SoundEvent squish = sound("entity.sulfur_cube.squish");
    public static final SoundEvent smallDeath = sound("entity.small_sulfur_cube.death");
    public static final SoundEvent smallEat = sound("entity.small_sulfur_cube.eat");
    public static final SoundEvent smallHurt = sound("entity.small_sulfur_cube.hurt");
    public static final SoundEvent smallJump = sound("entity.small_sulfur_cube.jump");
    public static final SoundEvent smallSquish = sound("entity.small_sulfur_cube.squish");
    public static final SoundEvent bucketFill = sound("item.bucket.fill_sulfur_cube");
    public static final SoundEvent bucketEmpty = sound("item.bucket.empty_sulfur_cube");
    private static final Map<Archetype, SoundEvent> HIT = new EnumMap<Archetype, SoundEvent>(Archetype.class);
    private static final Map<Archetype, SoundEvent> PUSH = new EnumMap<Archetype, SoundEvent>(Archetype.class);

    static {
        for (Archetype a : Archetype.values()) {
            if (a == Archetype.NONE) {
                continue;
            }
            HIT.put(a, sound("entity.sulfur_cube." + a.id + ".hit"));
            PUSH.put(a, sound("entity.sulfur_cube." + a.id + ".push"));
        }
    }

    public static ItemSulfurCubeBucket bucket;

    /** Set by the client: the goo a landing cube splashes. */
    public static Consumer<EntitySulfurCube> landingParticles = c -> {
    };

    private SulfurCubes() {
    }

    public static boolean enabled() {
        return ModConfig.sulfurCube.enabled;
    }

    private static SoundEvent sound(String name) {
        ResourceLocation id = new ResourceLocation(GameDropsBackport.MOD_ID, name);
        SoundEvent e = new SoundEvent(id).setRegistryName(id);
        SOUNDS.add(e);
        return e;
    }

    public static SoundEvent hit(Archetype a) {
        return HIT.get(a == Archetype.NONE ? Archetype.REGULAR : a);
    }

    public static SoundEvent push(Archetype a) {
        return PUSH.get(a == Archetype.NONE ? Archetype.REGULAR : a);
    }

    public static List<SoundEvent> sounds() {
        return java.util.Collections.unmodifiableList(SOUNDS);
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (enabled()) {
            event.getRegistry().registerAll(SOUNDS.toArray(new SoundEvent[0]));
        }
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        bucket = new ItemSulfurCubeBucket();
        bucket.setRegistryName(new ResourceLocation(GameDropsBackport.MOD_ID, "sulfur_cube_bucket"));
        event.getRegistry().register(bucket);
        CreativePlacement.placeAfter(bucket, CreativePlacement.ANY, net.minecraft.creativetab.CreativeTabs.MISC,
                "minecraft:tadpole_bucket");
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!enabled()) {
            return;
        }
        // Entity id 1 (the stream's 1-5). 26.3: tracked 10 chunks out, like the slime.
        EntityRegistry.registerModEntity(ID, EntitySulfurCube.class, GameDropsBackport.MOD_ID + ".sulfur_cube", 1,
                GameDropsBackport.instance, 160, 3, true, EGG_PRIMARY, EGG_SECONDARY);
        CreativePlacement.placeSpawnEgg(ID);
    }

    /** After every mod has registered: the swallowable items and the dispenser behavior for them. */
    public static void postInit() {
        if (!enabled()) {
            return;
        }
        ArchetypeItems.build();
        dispenserShears();
        dispenserArmor();
        for (Item item : ArchetypeItems.items()) {
            final IBehaviorDispenseItem previous = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(item);
            BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(item, new IBehaviorDispenseItem() {
                @Override
                public ItemStack dispense(IBlockSource source, ItemStack stack) {
                    return dispenseOrElse(source, stack, previous);
                }
            });
        }
    }

    /**
     * 26.3's {@code SulfurCubeBlockDispenseItemBehavior}: a dispenser feeds a swallowable block to a cube in front of
     * it; with no cube there, the item's own behavior runs (TNT still primes).
     */
    static ItemStack dispenseOrElse(IBlockSource source, ItemStack stack, IBehaviorDispenseItem previous) {
        if (ArchetypeItems.swallowable(stack)) {
            EnumFacing facing = source.getBlockState().getValue(BlockDispenser.FACING);
            BlockPos pos = source.getBlockPos().offset(facing);
            for (EntitySulfurCube cube : source.getWorld().getEntitiesWithinAABB(EntitySulfurCube.class, new AxisAlignedBB(pos))) {
                if (cube.equipItem(stack)) {
                    stack.shrink(1);
                    return stack;
                }
            }
        }
        return previous.dispense(source, stack);
    }

    /**
     * 26.2's {@code ShearDispenseBehavior} for the cube ({@code Shearable}, ready while it holds a block): a dispenser's
     * shears cut the block out of a cube in front and lose a point; with none, the shears' earlier behavior runs.
     */
    static void dispenserShears() {
        final IBehaviorDispenseItem previous = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(net.minecraft.init.Items.SHEARS);
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(net.minecraft.init.Items.SHEARS, new IBehaviorDispenseItem() {
            @Override
            public ItemStack dispense(IBlockSource source, ItemStack stack) {
                return shearOrElse(source, stack, previous);
            }
        });
    }

    static ItemStack shearOrElse(IBlockSource source, ItemStack stack, IBehaviorDispenseItem previous) {
        net.minecraft.world.World world = source.getWorld();
        BlockPos pos = source.getBlockPos().offset(source.getBlockState().getValue(BlockDispenser.FACING));
        if (!world.isRemote) {
            for (EntitySulfurCube cube : world.getEntitiesWithinAABB(EntitySulfurCube.class, new AxisAlignedBB(pos))) {
                if (cube.isEntityAlive() && cube.hasBodyItem()) {
                    cube.shear();
                    world.playEvent(1000, source.getBlockPos(), 0);
                    if (stack.attemptDamageItem(1, world.rand, null)) {
                        stack.setCount(0);
                    }
                    return stack;
                }
            }
        }
        return previous.dispense(source, stack);
    }

    /**
     * 26.2's dispenser equips a cube's body slot only ({@code canDispenserEquipIntoSlot}): 1.12.2's armor behavior took
     * any mob that can pick up loot, empty adult cubes too. Wrapped for every item using it (armor, elytra, other
     * mods' armor), it equips the first other mob in front, as 1.12.2 does, and drops the item when only cubes are
     * there.
     */
    static void dispenserArmor() {
        final IBehaviorDispenseItem armor = new net.minecraft.dispenser.BehaviorDefaultDispenseItem() {
            @Override
            protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
                ItemStack out = dispenseArmorSkippingCubes(source, stack);
                return out.isEmpty() ? super.dispenseStack(source, stack) : out;
            }
        };
        for (Item item : Item.REGISTRY) {
            if (BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(item) == net.minecraft.item.ItemArmor.DISPENSER_BEHAVIOR) {
                BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(item, armor);
            }
        }
    }

    /** 1.12.2's {@code ItemArmor.dispenseArmor}, without sulfur cubes. @return the rest, or empty when nothing took it */
    public static ItemStack dispenseArmorSkippingCubes(IBlockSource source, ItemStack stack) {
        BlockPos pos = source.getBlockPos().offset(source.getBlockState().getValue(BlockDispenser.FACING));
        for (net.minecraft.entity.EntityLivingBase e : source.getWorld().getEntitiesWithinAABB(net.minecraft.entity.EntityLivingBase.class,
                new AxisAlignedBB(pos), com.google.common.base.Predicates.and(net.minecraft.util.EntitySelectors.NOT_SPECTATING,
                        new net.minecraft.util.EntitySelectors.ArmoredMob(stack)))) {
            if (e instanceof EntitySulfurCube) {
                continue;
            }
            net.minecraft.inventory.EntityEquipmentSlot slot = net.minecraft.entity.EntityLiving.getSlotForItemStack(stack);
            e.setItemStackToSlot(slot, stack.splitStack(1));
            if (e instanceof net.minecraft.entity.EntityLiving) {
                ((net.minecraft.entity.EntityLiving) e).setDropChance(slot, 2.0F);
            }
            return stack;
        }
        return ItemStack.EMPTY;
    }

    /** Grants one of <i>Uh Oh</i>'s criteria (either completes it). */
    static void grantUhOh(EntityPlayerMP player, String criterion) {
        Advancement adv = player.getServer() == null ? null : player.getServer().getAdvancementManager().getAdvancement(UH_OH);
        if (adv != null) {
            player.getAdvancements().grantCriterion(adv, criterion);
        }
    }

    /** @return whether a sulfur cube may spawn here: in the sulfur caves (see the class comment) */
    public static boolean inSulfurCaves(World world, BlockPos pos) {
        Biome biome = world.getBiome(pos);
        if (biome.getRegistryName() != null && biome.getRegistryName().equals(new ResourceLocation(GameDropsBackport.MOD_ID, "sulfur_caves"))) {
            return true;
        }
        Block below = world.getBlockState(pos.down()).getBlock();
        ResourceLocation name = below.getRegistryName();
        if (name == null || !GameDropsBackport.MOD_ID.equals(name.getNamespace())) {
            return false;
        }
        String path = name.getPath();
        return (path.equals("sulfur") || path.equals("cinnabar") || path.equals("potent_sulfur")) && !world.canSeeSky(pos);
    }

    @SubscribeEvent
    public static void onPotentialSpawns(WorldEvent.PotentialSpawns event) {
        if (enabled() && event.getType() == EnumCreatureType.MONSTER && inSulfurCaves(event.getWorld(), event.getPos())) {
            event.getList().add(new Biome.SpawnListEntry(EntitySulfurCube.class, 100, 2, 4));
        }
    }

    /**
     * Explosions push a cube that holds a block by its archetype's explosion knockback resistance (26.3's
     * {@code explosion_knockback_resistance}); 1.12.2's explosion pushes every mob fully, so these cubes are taken out
     * of its list and hurt and pushed here, as 1.12.2's {@code Explosion.doExplosionA} would.
     */
    @SubscribeEvent
    public static void onDetonate(ExplosionEvent.Detonate event) {
        if (!enabled()) {
            return;
        }
        Vec3d c = event.getExplosion().getPosition();
        float size = ObfuscationReflectionHelper.getPrivateValue(net.minecraft.world.Explosion.class, event.getExplosion(), "field_77280_f");
        double reach = size * 2.0F;
        for (Iterator<Entity> it = event.getAffectedEntities().iterator(); it.hasNext(); ) {
            Entity e = it.next();
            if (!(e instanceof EntitySulfurCube) || !((EntitySulfurCube) e).hasBodyItem() || e.isImmuneToExplosions()) {
                continue;
            }
            it.remove();
            EntitySulfurCube cube = (EntitySulfurCube) e;
            double dist = cube.getDistance(c.x, c.y, c.z) / reach;
            if (dist > 1.0D) {
                continue;
            }
            double dx = cube.posX - c.x;
            double dy = cube.posY + cube.getEyeHeight() - c.y;
            double dz = cube.posZ - c.z;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len == 0.0D) {
                continue;
            }
            dx /= len;
            dy /= len;
            dz /= len;
            double impact = (1.0D - dist) * event.getWorld().getBlockDensity(c, cube.getEntityBoundingBox());
            cube.attackEntityFrom(DamageSource.causeExplosionDamage(event.getExplosion()),
                    (float) (int) ((impact * impact + impact) / 2.0D * 7.0D * reach + 1.0D));
            double push = impact * (1.0D - cube.explosionKnockbackResistance());
            cube.motionX += dx * push;
            cube.motionY += dy * push;
            cube.motionZ += dz * push;
            cube.velocityChanged = true;
        }
    }
}
