package com.exiledradio.gamedropsbackport.selftest;

import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.sulfurcube.Archetype;
import com.exiledradio.gamedropsbackport.sulfurcube.ArchetypeItems;
import com.exiledradio.gamedropsbackport.sulfurcube.EntitySulfurCube;
import com.exiledradio.gamedropsbackport.sulfurcube.ItemSulfurCubeBucket;
import com.exiledradio.gamedropsbackport.sulfurcube.SulfurCubes;
import com.exiledradio.gamedropsbackport.sulfurcube.client.HeldBlockPose;

import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.block.Block;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Stream 3 (sulfur cube): registration, the archetype lists, sizes and growth, swallowing, swapping and shearing,
 * damage immunity while holding a block, TNT priming (fire, flint and steel, explosions) and exploding, splitting on
 * death, buckets both ways, physics (bounce, friction), knockback, hot contact damage, the dispenser, spawning rules
 * and the advancement. Appends the bucket to {@code selftest-items.txt} for {@code tools/check_assets.py}.
 */
final class SulfurCubeSelfTest implements SelfTest {

    private static final BlockPos ORIGIN = new BlockPos(48, 180, 48);

    @Override
    public String name() {
        return "sulfurcube";
    }

    @Override
    public void run(Check check) throws Exception {
        if (!check.ok(SulfurCubes.enabled() && SulfurCubes.bucket != null, "sulfur cube is registered")) {
            return;
        }
        WorldServer world = check.world;
        check.eq(EntitySulfurCube.class, EntityList.getClass(SulfurCubes.ID), "entity class");
        check.ok(EntityList.ENTITY_EGGS.containsKey(SulfurCubes.ID), "spawn egg");
        check.eq(38, SulfurCubes.sounds().size(), "sound events (7 + 5 small + 24 archetype + 2 bucket)");
        for (net.minecraft.util.SoundEvent e : SulfurCubes.sounds()) {
            check.ok(ForgeRegistries.SOUND_EVENTS.containsValue(e), "sound registered: " + e.getRegistryName());
        }
        check.ok(world.getMinecraftServer().getAdvancementManager().getAdvancement(SulfurCubes.UH_OH) != null,
                "advancement Uh Oh is loaded");

        heldBlockPose(check);
        archetypes(check);
        floor(world);
        sizes(check, world);
        swallowing(check, world);
        skulls(check, world);
        immunity(check, world);
        tnt(check, world);
        splitting(check, world);
        buckets(check, world);
        physics(check, world);
        contact(check, world);
        dispenser(check, world);
        spawning(check, world);
        dump();
    }

    /** The held block sits centered in the gel, as 26.3 draws it (1.12.2's renderModelBrightness adds a yaw). */
    private static void heldBlockPose(Check check) {
        double[][] corners = {
            HeldBlockPose.place(0, 0, 0), HeldBlockPose.place(1, 0, 0), HeldBlockPose.place(0, 0, 1),
            HeldBlockPose.place(1, 1, 1)};
        double[][] want = {{-0.5, -0.518, -0.5}, {0.5, -0.518, -0.5}, {-0.5, -0.518, 0.5}, {0.5, 0.482, 0.5}};
        for (int i = 0; i < corners.length; i++) {
            boolean near = true;
            for (int a = 0; a < 3; a++) {
                near &= Math.abs(corners[i][a] - want[i][a]) < 1.0E-4;
            }
            check.ok(near, "held block corner " + i + " at 26.3's spot: " + java.util.Arrays.toString(corners[i]));
        }
    }

    private static void archetypes(Check check) {
        check.eq(Archetype.EXPLOSIVE, ArchetypeItems.of(new ItemStack(Blocks.TNT)), "TNT");
        check.eq(Archetype.HOT, ArchetypeItems.of(new ItemStack(Blocks.MAGMA)), "magma block");
        check.eq(Archetype.LIGHT, ArchetypeItems.of(new ItemStack(Blocks.WOOL, 1, 5)), "lime wool");
        check.eq(Archetype.BOUNCY, ArchetypeItems.of(new ItemStack(Blocks.PLANKS, 1, 3)), "jungle planks");
        check.eq(Archetype.BOUNCY, ArchetypeItems.of(new ItemStack(Blocks.LOG2, 1, 1)), "dark oak log");
        check.eq(Archetype.SLOW_BOUNCY, ArchetypeItems.of(new ItemStack(Blocks.STONE, 1, 1)), "granite");
        check.eq(Archetype.SLOW_BOUNCY, ArchetypeItems.of(new ItemStack(Blocks.STAINED_HARDENED_CLAY, 1, 9)), "cyan terracotta");
        check.eq(Archetype.FAST_FLAT, ArchetypeItems.of(new ItemStack(Blocks.SPONGE, 1, 1)), "wet sponge");
        check.eq(Archetype.FAST_FLAT, ArchetypeItems.of(new ItemStack(Blocks.LIT_PUMPKIN)), "jack o'lantern");
        check.eq(Archetype.REGULAR, ArchetypeItems.of(new ItemStack(Blocks.DIRT, 1, 1)), "coarse dirt");
        check.eq(Archetype.REGULAR, ArchetypeItems.of(new ItemStack(Blocks.CONCRETE_POWDER, 1, 14)), "red concrete powder");
        check.eq(Archetype.SLOW_FLAT, ArchetypeItems.of(new ItemStack(Blocks.IRON_BLOCK)), "iron block");
        check.eq(Archetype.FAST_SLIDING, ArchetypeItems.of(new ItemStack(Blocks.PACKED_ICE)), "packed ice");
        check.eq(Archetype.HIGH_RESISTANCE, ArchetypeItems.of(new ItemStack(Blocks.SOUL_SAND)), "soul sand");
        check.eq(Archetype.SLOW_SLIDING, ArchetypeItems.of(new ItemStack(Blocks.RED_MUSHROOM_BLOCK)), "red mushroom block");
        check.eq(Archetype.SLOW_SLIDING, ArchetypeItems.of(new ItemStack(Blocks.NETHER_WART_BLOCK)), "nether wart block");
        check.eq(null, ArchetypeItems.of(new ItemStack(Blocks.STONE, 1, 7)), "no such stone");
        check.eq(null, ArchetypeItems.of(new ItemStack(Blocks.GLASS)), "glass");
        check.eq(null, ArchetypeItems.of(new ItemStack(Items.DIAMOND_SWORD)), "a sword");
        check.eq(null, ArchetypeItems.of(new ItemStack(Items.MELON)), "a melon slice");
        Item sulfur = ForgeRegistries.ITEMS.getValue(new ResourceLocation(GameDropsBackport.MOD_ID, "sulfur"));
        if (sulfur != null && sulfur != Items.AIR) {
            check.eq(Archetype.SLOW_BOUNCY, ArchetypeItems.of(new ItemStack(sulfur)), "stream 2's sulfur");
        }
    }

    private static void floor(WorldServer world) {
        for (int x = -8; x <= 24; x++) {
            for (int z = -8; z <= 24; z++) {
                world.setBlockState(ORIGIN.add(x, -1, z), Blocks.STONE.getDefaultState(), 2);
                for (int y = 0; y < 12; y++) {
                    world.setBlockState(ORIGIN.add(x, y, z), Blocks.AIR.getDefaultState(), 2);
                }
            }
        }
    }

    private static EntitySulfurCube cube(WorldServer world, BlockPos at, boolean baby) {
        EntitySulfurCube c = new EntitySulfurCube(world);
        c.setLocationAndAngles(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, 0.0F, 0.0F);
        c.onInitialSpawn(world.getDifficultyForLocation(at), null);
        if (baby) {
            c.setCubeSize(1, true);
        }
        world.spawnEntity(c);
        return c;
    }

    private static void sizes(Check check, WorldServer world) {
        EntitySulfurCube adult = cube(world, ORIGIN, false);
        check.eq(2, adult.getCubeSize(), "a spawned cube is an adult, size 2");
        check.eq(8.0F, adult.getMaxHealth(), "adult health");
        check.ok(Math.abs(adult.width - 0.98F) < 1.0E-4F, "adult width 0.98: " + adult.width);
        check.ok(!adult.isChild(), "adult is not a baby");
        EntitySulfurCube baby = cube(world, ORIGIN.east(2), true);
        check.ok(baby.isChild(), "size 1 is a baby");
        check.eq(4.0F, baby.getMaxHealth(), "baby health");
        check.ok(Math.abs(baby.width - 0.49F) < 1.0E-4F, "baby width 0.49: " + baby.width);
        // Slimeballs grow a baby: a tenth of the time it has left.
        FakePlayer player = FakePlayerFactory.getMinecraft(world);
        int age = baby.getGrowingAge();
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.SLIME_BALL, 2));
        check.ok(baby.processInteract(player, EnumHand.MAIN_HAND), "a baby eats a slimeball");
        check.eq(age + 2400, baby.getGrowingAge(), "a slimeball takes 2 minutes off a fresh baby");
        check.eq(1, player.getHeldItemMainhand().getCount(), "the slimeball is used");
        check.ok(!baby.equipItem(new ItemStack(Blocks.DIRT)), "a baby swallows nothing");
        baby.ageUp(10000, false);
        check.ok(!baby.isChild(), "a baby grows up");
        check.eq(2, baby.getCubeSize(), "and becomes size 2");
        check.eq(8.0F, baby.getHealth(), "with full adult health");
        adult.setDead();
        baby.setDead();
    }

    /**
     * 26.3: no archetype tag lists a skull, so none is swallowable; one put in by command (here the saved
     * {@code body_item}) is held with no archetype: AI stopped, the default knockback, and drawn as a skull.
     */
    private static void skulls(Check check, WorldServer world) {
        ItemStack skull = new ItemStack(Items.SKULL, 1, 1);
        check.ok(!ArchetypeItems.swallowable(skull), "a skull is not swallowable (26.3's tags)");
        check.eq(null, ArchetypeItems.of(skull), "no archetype names a skull");
        check.eq(Archetype.NONE, ArchetypeItems.held(skull), "held anyway, it has none");
        EntitySulfurCube c = cube(world, ORIGIN.east(8), false);
        FakePlayer player = FakePlayerFactory.getMinecraft(world);
        player.setHeldItem(EnumHand.MAIN_HAND, skull.copy());
        check.ok(!c.processInteract(player, EnumHand.MAIN_HAND) || !c.hasBodyItem(), "a player cannot feed it a skull");
        check.ok(!c.hasBodyItem(), "so it holds nothing");
        NBTTagCompound tag = new NBTTagCompound();
        c.writeEntityToNBT(tag);
        tag.setTag("body_item", new ItemStack(Items.SKULL, 1, 4).writeToNBT(new NBTTagCompound()));
        c.readEntityFromNBT(tag);
        check.ok(c.hasBodyItem() && c.getBodyItem().getItem() == Items.SKULL, "a command puts a creeper head in");
        check.eq(Archetype.NONE, c.archetype(), "with no archetype");
        check.ok(!c.aiActive(), "its AI stops as with a block");
        check.ok(!c.canExplode(), "it is no TNT cube");
        check.eq(0.33F, Archetype.NONE.horizontalPower, "26.3's default knockback, horizontal");
        check.eq(0.06F, Archetype.NONE.verticalPower, "and vertical");
        check.eq(SulfurCubes.hit(Archetype.REGULAR), SulfurCubes.hit(Archetype.NONE), "26.3's default sounds are regular's");
        NBTTagCompound saved = new NBTTagCompound();
        c.writeEntityToNBT(saved);
        check.eq(4, new ItemStack(saved.getCompoundTag("body_item")).getMetadata(), "the head is saved");
        c.setDead();
        player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
    }

    private static void swallowing(Check check, WorldServer world) {
        EntitySulfurCube c = cube(world, ORIGIN.east(4), false);
        FakePlayer player = FakePlayerFactory.getMinecraft(world);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Blocks.DIRT, 3));
        check.ok(c.processInteract(player, EnumHand.MAIN_HAND), "a player feeds dirt");
        check.eq(Archetype.REGULAR, c.archetype(), "it holds dirt");
        check.eq(2, player.getHeldItemMainhand().getCount(), "one dirt is used");
        check.ok(c.isNoDespawnRequired(), "a cube holding a block stays");
        check.ok(!c.processInteract(player, EnumHand.MAIN_HAND), "the same block again does nothing");
        int before = items(world, c).size();
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Blocks.WOOL, 1, 2));
        check.ok(c.processInteract(player, EnumHand.MAIN_HAND), "a different block swaps");
        check.eq(Archetype.LIGHT, c.archetype(), "it holds wool");
        check.eq(before + 1, items(world, c).size(), "the dirt is spat out");
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        check.ok(c.processInteract(player, EnumHand.MAIN_HAND), "shears");
        check.ok(!c.hasBodyItem(), "shearing removes the block");
        check.eq(1, player.getHeldItemMainhand().getItemDamage(), "the shears wear");
        // A dropped block is picked up by walking into it, not for 5 s after shearing.
        for (EntityItem e : items(world, c)) {
            e.setDead();
        }
        EntityItem stone = new EntityItem(world, c.posX, c.posY, c.posZ, new ItemStack(Blocks.STONE));
        world.spawnEntity(stone);
        c.onLivingUpdate();
        check.ok(!c.hasBodyItem(), "no pickup while the 5 s timer runs");
        for (int i = 0; i < EntitySulfurCube.PICKUP_TIMER + 1; i++) {
            invoke(c, "updateAITasks");
        }
        c.onLivingUpdate();
        check.eq(Archetype.SLOW_BOUNCY, c.archetype(), "it picks up stone it touches");
        check.ok(stone.isDead, "the item is taken");
        c.setDead();
    }

    private static List<EntityItem> items(WorldServer world, Entity near) {
        return world.getEntitiesWithinAABB(EntityItem.class, near.getEntityBoundingBox().grow(3.0D));
    }

    private static void immunity(Check check, WorldServer world) {
        EntitySulfurCube c = cube(world, ORIGIN.east(6), false);
        FakePlayer player = FakePlayerFactory.getMinecraft(world);
        c.equipItem(new ItemStack(Blocks.IRON_BLOCK));
        check.ok(c.attackEntityFrom(DamageSource.causePlayerDamage(player), 5.0F), "a player hit counts as a hit");
        check.eq(8.0F, c.getHealth(), "but does no damage while it holds a block");
        c.attackEntityFrom(DamageSource.FALL, 5.0F);
        c.attackEntityFrom(DamageSource.ANVIL, 5.0F);
        c.attackEntityFrom(DamageSource.CACTUS, 5.0F);
        check.eq(8.0F, c.getHealth(), "nor falls, anvils or cacti");
        c.attackEntityFrom(DamageSource.LAVA, 2.0F);
        check.eq(6.0F, c.getHealth(), "lava still hurts");
        c.shear();
        c.hurtResistantTime = 0;
        c.attackEntityFrom(DamageSource.FALL, 1.0F);
        check.eq(5.0F, c.getHealth(), "without a block, falls hurt");
        c.setDead();
    }

    private static void tnt(Check check, WorldServer world) {
        FakePlayer player = FakePlayerFactory.getMinecraft(world);
        EntitySulfurCube a = cube(world, ORIGIN.add(0, 0, 6), false);
        a.equipItem(new ItemStack(Blocks.TNT));
        check.ok(a.canExplode(), "a TNT cube can explode");
        a.attackEntityFrom(DamageSource.IN_FIRE, 1.0F);
        check.eq(Archetype.FUSE, a.getFuse(), "fire primes it with a 6 s fuse");
        check.ok(a.getIsInvulnerable(), "a primed cube cannot be hurt");
        check.eq(8.0F, a.getHealth(), "fire did no damage once primed");
        EntitySulfurCube b = cube(world, ORIGIN.add(2, 0, 6), false);
        b.equipItem(new ItemStack(Blocks.TNT));
        b.attackEntityFrom(DamageSource.causeExplosionDamage((net.minecraft.entity.EntityLivingBase) null), 1.0F);
        check.ok(b.getFuse() >= 15 && b.getFuse() < 45, "an explosion primes it short: " + b.getFuse());
        EntitySulfurCube d = cube(world, ORIGIN.add(4, 0, 6), false);
        d.equipItem(new ItemStack(Blocks.TNT));
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
        check.ok(d.processInteract(player, EnumHand.MAIN_HAND), "flint and steel");
        check.ok(d.isPrimed(), "lights it");
        check.eq(1, player.getHeldItemMainhand().getItemDamage(), "the flint and steel wears");
        EntitySulfurCube e = cube(world, ORIGIN.add(6, 0, 6), false);
        e.equipItem(new ItemStack(Blocks.TNT));
        world.setBlockState(ORIGIN.add(7, 0, 6), Blocks.REDSTONE_BLOCK.getDefaultState());
        e.onUpdate();
        check.ok(e.isPrimed(), "redstone beside it primes it");
        world.setBlockState(ORIGIN.add(7, 0, 6), Blocks.AIR.getDefaultState());
        // An explosion at the end of the fuse; no babies, no block left.
        NBTTagCompound tag = new NBTTagCompound();
        a.writeEntityToNBT(tag);
        tag.setInteger("fuse", 1);
        a.readEntityFromNBT(tag);
        int cubes = cubes(world, a).size();
        a.onUpdate();
        check.ok(a.isDead, "the fuse runs out and it explodes");
        check.eq(cubes - 1, cubes(world, a).size(), "a primed cube does not split");
        for (EntitySulfurCube c : cubes(world, a)) {
            c.setDead();
        }
    }

    private static List<EntitySulfurCube> cubes(WorldServer world, Entity near) {
        return world.getEntitiesWithinAABB(EntitySulfurCube.class, near.getEntityBoundingBox().grow(8.0D),
                c -> c != null && !c.isDead);
    }

    private static void splitting(Check check, WorldServer world) {
        EntitySulfurCube c = cube(world, ORIGIN.add(0, 0, 10), false);
        c.setCustomNameTag("Cubey");
        int before = cubes(world, c).size();
        c.setHealth(0.0F);
        c.setDead();
        List<EntitySulfurCube> after = cubes(world, c);
        check.eq(before + 1, after.size(), "an adult splits into two");
        int babies = 0;
        for (EntitySulfurCube b : after) {
            if (b.isChild() && b.getCubeSize() == 1 && "Cubey".equals(b.getCustomNameTag())) {
                babies++;
            }
            b.setDead();
        }
        check.eq(2, babies, "named babies of size 1");
        death(check, world);
    }

    /**
     * 26.3's SulfurCube extends AbstractCubeMob extends AgeableMob, not Slime: an adult without a block dies to
     * ordinary damage (8 health) and its remove() makes two baby sulfur cubes, never slimes; a baby splits into
     * nothing; the loot table is empty, so the only drop is the held block.
     */
    private static void death(Check check, WorldServer world) {
        FakePlayer player = FakePlayerFactory.getMinecraft(world);
        AxisAlignedBB area = new AxisAlignedBB(ORIGIN.add(-8, -1, 0), ORIGIN.add(24, 12, 24));
        int slimes = world.getEntitiesWithinAABB(net.minecraft.entity.monster.EntitySlime.class, area).size();

        EntitySulfurCube adult = cube(world, ORIGIN.add(10, 0, 12), false);
        adult.attackEntityFrom(DamageSource.causePlayerDamage(player), 8.0F);
        check.ok(adult.getHealth() <= 0.0F, "a player kills an adult holding nothing (8 health)");
        adult.setDead();
        List<EntitySulfurCube> children = cubes(world, adult);
        check.ok(!net.minecraft.entity.monster.EntitySlime.class.isAssignableFrom(EntitySulfurCube.class), "a sulfur cube is not a slime");
        check.eq(2, children.size(), "it splits into two sulfur cubes");
        for (EntitySulfurCube b : children) {
            check.ok(b.isChild() && b.getCubeSize() == 1, "each a size 1 baby");
            int around = cubes(world, b).size();
            b.setHealth(0.0F);
            b.setDead();
            check.eq(around - 1, cubes(world, b).size(), "a baby splits into nothing");
        }
        check.eq(slimes, world.getEntitiesWithinAABB(net.minecraft.entity.monster.EntitySlime.class, area).size(),
                "no slimes appear");

        // Drops: the held block and nothing else (26.3's loot table is empty).
        EntitySulfurCube holder = cube(world, ORIGIN.add(16, 0, 12), false);
        holder.equipItem(new ItemStack(Blocks.DIRT, 1, 1));
        for (EntityItem e : items(world, holder)) {
            e.setDead();
        }
        holder.attackEntityFrom(DamageSource.OUT_OF_WORLD, 100.0F);
        check.ok(holder.getHealth() <= 0.0F, "the void kills a cube holding a block");
        List<EntityItem> drops = items(world, holder);
        check.eq(1, drops.size(), "one drop");
        if (drops.size() == 1) {
            ItemStack d = drops.get(0).getItem();
            check.ok(d.getItem() == Item.getItemFromBlock(Blocks.DIRT) && d.getMetadata() == 1 && d.getCount() == 1,
                    "the held coarse dirt: " + d);
        }
        holder.setDead();
        for (EntitySulfurCube b : cubes(world, holder)) {
            b.setDead();
        }
        for (EntityItem e : items(world, holder)) {
            e.setDead();
        }
    }

    private static void buckets(Check check, WorldServer world) {
        FakePlayer player = FakePlayerFactory.getMinecraft(world);
        EntitySulfurCube c = cube(world, ORIGIN.add(4, 0, 10), false);
        c.equipItem(new ItemStack(Blocks.MAGMA));
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        check.ok(c.processInteract(player, EnumHand.MAIN_HAND), "a bucket scoops an adult");
        check.ok(c.isDead, "the cube is taken");
        ItemStack bucket = player.getHeldItemMainhand();
        check.eq(SulfurCubes.bucket, bucket.getItem(), "the player holds a bucket of sulfur cube");
        check.eq(Blocks.MAGMA, Block.getBlockFromItem(ItemSulfurCubeBucket.content(bucket).getItem()), "it holds the magma block");
        BlockPos at = ORIGIN.add(8, 0, 10);
        bucket.getItem().onItemUse(player, world, at.down(), EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        List<EntitySulfurCube> placed = world.getEntitiesWithinAABB(EntitySulfurCube.class, new AxisAlignedBB(at).grow(1.0D));
        check.eq(1, placed.size(), "the bucket places the cube");
        if (!placed.isEmpty()) {
            check.eq(Archetype.HOT, placed.get(0).archetype(), "still holding magma");
            check.ok(placed.get(0).fromBucket(), "marked as from a bucket");
            placed.get(0).setDead();
        }
        check.eq(Items.BUCKET, player.getHeldItemMainhand().getItem(), "the empty bucket is left");
        EntitySulfurCube baby = cube(world, ORIGIN.add(4, 0, 12), true);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        baby.processInteract(player, EnumHand.MAIN_HAND);
        check.ok(!baby.isDead, "babies are not bucketed");
        baby.setDead();
    }

    private static void physics(Check check, WorldServer world) {
        // Wool (light, bounciness 1): a falling cube bounces back up, and the bounce is a game event (26.2).
        EntitySulfurCube c = cube(world, ORIGIN.add(12, 0, 0), false);
        final java.util.List<com.exiledradio.backportcore.gameevent.GameEvent> heard = new java.util.ArrayList<com.exiledradio.backportcore.gameevent.GameEvent>();
        com.exiledradio.backportcore.gameevent.GameEvents.require("26.2", () -> true);
        final net.minecraft.util.math.Vec3d ear = new net.minecraft.util.math.Vec3d(c.posX, ORIGIN.getY() + 1.0D, c.posZ);
        com.exiledradio.backportcore.gameevent.GameEventListener listener = new com.exiledradio.backportcore.gameevent.GameEventListener() {
            @Override
            public com.exiledradio.backportcore.gameevent.PositionSource position() {
                return new com.exiledradio.backportcore.gameevent.PositionSource.Block(ORIGIN.add(12, 1, 2));
            }

            @Override
            public int radius() {
                return 16;
            }

            @Override
            public boolean immediate() {
                return true;
            }

            @Override
            public boolean handle(WorldServer w, com.exiledradio.backportcore.gameevent.GameEvent event, net.minecraft.util.math.Vec3d origin,
                                  com.exiledradio.backportcore.gameevent.GameEventContext context) {
                heard.add(event);
                return false;
            }
        };
        com.exiledradio.backportcore.gameevent.GameEvents.add(world, listener);
        c.equipItem(new ItemStack(Blocks.WOOL));
        c.setPosition(c.posX, ORIGIN.getY() + 3.0D, c.posZ);
        c.motionY = -0.8D;
        double up = -1.0D;
        for (int i = 0; i < 12; i++) {
            c.travel(0.0F, 0.0F, 0.0F);
            up = Math.max(up, c.motionY);
        }
        check.ok(up > 0.3D, "a wool cube bounces off the floor: " + up);
        com.exiledradio.backportcore.gameevent.GameEvents.flush(world);
        com.exiledradio.backportcore.gameevent.GameEvents.remove(world, listener);
        check.ok(heard.contains(com.exiledradio.backportcore.gameevent.GameEvent.BOUNCE), "its bounce is the bounce game event: " + heard);
        // A falling cube without a block does not bounce.
        EntitySulfurCube plain = cube(world, ORIGIN.add(14, 0, 0), false);
        plain.setPosition(plain.posX, ORIGIN.getY() + 3.0D, plain.posZ);
        plain.motionY = -0.8D;
        double plainUp = -1.0D;
        for (int i = 0; i < 12; i++) {
            plain.travel(0.0F, 0.0F, 0.0F);
            plainUp = Math.max(plainUp, plain.motionY);
        }
        check.ok(plainUp <= 0.0D, "a cube without a block does not bounce: " + plainUp);
        // Friction on the ground, by the archetype's friction modifier.
        EntitySulfurCube regular = cube(world, ORIGIN.add(12, 0, 4), false);
        regular.equipItem(new ItemStack(Blocks.DIRT));
        EntitySulfurCube slide = cube(world, ORIGIN.add(12, 0, 7), false);
        slide.equipItem(new ItemStack(Blocks.PACKED_ICE));
        for (EntitySulfurCube e : new EntitySulfurCube[]{regular, slide}) {
            e.onGround = true;
            e.motionY = 0.0D;
            e.motionX = 0.5D;
            e.travel(0.0F, 0.0F, 0.0F);
        }
        check.ok(slide.motionX > regular.motionX, "packed ice slides further than dirt: " + slide.motionX + " vs " + regular.motionX);
        float dirt = Archetype.modifiedFriction(0.6F, Archetype.REGULAR.frictionModifier)
                * Archetype.modifiedFriction(0.91F, Archetype.REGULAR.airDragModifier);
        check.ok(Math.abs(regular.motionX - 0.5D * dirt) < 0.02D, "dirt: 26.3's friction " + 0.5D * dirt + ", got " + regular.motionX);
        // Knockback from a player, by the archetype.
        FakePlayer player = FakePlayerFactory.getMinecraft(world);
        EntitySulfurCube k = cube(world, ORIGIN.add(16, 0, 4), false);
        k.equipItem(new ItemStack(Blocks.PLANKS));
        player.setLocationAndAngles(k.posX - 2.0D, k.posY, k.posZ, -90.0F, 0.0F);
        k.motionX = 0.0D;
        k.motionY = 0.0D;
        k.attackEntityFrom(DamageSource.causePlayerDamage(player), 4.0F);
        check.ok(k.motionX > 0.2D, "a hit pushes it away: " + k.motionX);
        check.ok(k.motionY > 0.0D, "and up");
        for (EntitySulfurCube e : new EntitySulfurCube[]{c, plain, regular, slide, k}) {
            e.setDead();
        }
    }

    private static void contact(Check check, WorldServer world) throws Exception {
        EntitySulfurCube c = cube(world, ORIGIN.add(0, 0, 16), false);
        c.equipItem(new ItemStack(Blocks.MAGMA));
        EntityZombie z = new EntityZombie(world);
        z.setLocationAndAngles(c.posX + 0.5D, c.posY, c.posZ, 0.0F, 0.0F);
        world.spawnEntity(z);
        float before = z.getHealth();
        invoke(c, "collideWithEntity", Entity.class, z);
        check.ok(z.getHealth() < before && z.getHealth() >= before - 1.0F, "a hot cube burns what it touches for 1 (less armor): " + z.getHealth());
        check.eq(SulfurCubes.HOT_DAMAGE, z.getLastDamageSource() == null ? null : z.getLastDamageSource().getDamageType(),
                "with the hot damage type");
        z.setDead();
        c.setDead();
    }

    private static void dispenser(Check check, WorldServer world) {
        BlockPos d = ORIGIN.add(4, 0, 16);
        world.setBlockState(d, Blocks.DISPENSER.getDefaultState().withProperty(BlockDispenser.FACING, EnumFacing.EAST));
        EntitySulfurCube c = cube(world, d.east(), false);
        IBlockSource source = new net.minecraft.block.BlockSourceImpl(world, d);
        ItemStack stack = new ItemStack(Blocks.SOUL_SAND, 2);
        IBehaviorDispenseItem behavior = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(stack.getItem());
        behavior.dispense(source, stack);
        check.eq(Archetype.HIGH_RESISTANCE, c.archetype(), "a dispenser feeds the cube in front of it");
        check.eq(1, stack.getCount(), "one block is used");
        c.setDead();
        int before = items(world, c).size();
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(stack.getItem()).dispense(source, stack);
        check.ok(items(world, c).size() > before || stack.isEmpty(), "with no cube it drops the block as before");
        world.setBlockState(d, Blocks.AIR.getDefaultState());
    }

    private static void spawning(Check check, WorldServer world) {
        EntitySulfurCube c = new EntitySulfurCube(world);
        check.ok(c.isCreatureType(EnumCreatureType.MONSTER, true), "counted as a monster");
        check.ok(!c.isCreatureType(EnumCreatureType.CREATURE, true), "not as a creature");
        check.ok(!(c instanceof net.minecraft.entity.monster.IMob), "not hostile");
        c.setLocationAndAngles(ORIGIN.getX() + 20.5D, ORIGIN.getY(), ORIGIN.getZ() + 20.5D, 0.0F, 0.0F);
        world.setLightFor(net.minecraft.world.EnumSkyBlock.BLOCK, new BlockPos(c), 15);
        check.ok(c.getCanSpawnHere(), "spawns on stone at any light");
        Block sulfur = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(GameDropsBackport.MOD_ID, "sulfur"));
        if (sulfur != null && sulfur != Blocks.AIR) {
            BlockPos p = ORIGIN.add(20, 0, 20);
            world.setBlockState(p.down(), sulfur.getDefaultState());
            check.ok(!SulfurCubes.inSulfurCaves(world, p), "not on sulfur under the open sky");
            world.setBlockState(p.up(4), Blocks.STONE.getDefaultState());
            check.ok(SulfurCubes.inSulfurCaves(world, p), "on sulfur underground: the sulfur caves");
            world.setBlockState(p.down(), Blocks.STONE.getDefaultState());
            world.setBlockState(p.up(4), Blocks.AIR.getDefaultState());
        }
        check.ok(!SulfurCubes.inSulfurCaves(world, ORIGIN.add(20, 0, 20)), "not on stone");
    }

    private static void invoke(Object target, String name, Object... args) {
        try {
            Class<?>[] types = new Class<?>[args.length / 2];
            Object[] values = new Object[args.length / 2];
            for (int i = 0; i < types.length; i++) {
                types[i] = (Class<?>) args[i * 2];
                values[i] = args[i * 2 + 1];
            }
            Class<?> c = target.getClass();
            Method m = null;
            while (m == null && c != null) {
                try {
                    m = c.getDeclaredMethod(name, types);
                } catch (NoSuchMethodException e) {
                    c = c.getSuperclass();
                }
            }
            m.setAccessible(true);
            m.invoke(target, values);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** The bucket, for the runner's selftest-items.txt. */
    private static void dump() throws Exception {
        PrintWriter items = SelfTestRunner.itemsOut();
        items.println(SulfurCubes.bucket.getRegistryName() + "\t0\tsulfur_cube_bucket\t"
                + new ItemStack(SulfurCubes.bucket).getTranslationKey() + ".name");
        items.close();
    }
}
