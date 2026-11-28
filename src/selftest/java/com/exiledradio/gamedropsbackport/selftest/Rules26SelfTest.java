package com.exiledradio.gamedropsbackport.selftest;

import com.exiledradio.backportcore.waterlog.Waterlogging;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.ModConfig;
import com.exiledradio.gamedropsbackport.changes.ChangeHooks;
import com.exiledradio.gamedropsbackport.changes.EndermanRules;
import com.exiledradio.gamedropsbackport.changes.TeleportTrails;
import com.exiledradio.gamedropsbackport.changes.SmallRules;
import com.exiledradio.gamedropsbackport.softblocks.SoftBlocks;
import com.exiledradio.gamedropsbackport.softblocks.StrawBedSleep;
import com.exiledradio.gamedropsbackport.sulfur.TilePotentSulfur;
import com.exiledradio.gamedropsbackport.sulfurcube.EntitySulfurCube;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.BlockStairs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntitySkeletonHorse;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.stats.StatList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;
import net.minecraftforge.event.world.BlockEvent;

import java.util.UUID;

/**
 * The xisumavoid audit's smaller 26.x rows: 26.3's enderman rules ({@link EndermanRules}), the sapling, trampling and
 * trap horse rules ({@link SmallRules}) for the changes stream; the sulfur cube's boats, the geysers' dragon and the
 * straw bed's hoe and statistic for their own streams.
 */
final class Rules26SelfTest {

    private Rules26SelfTest() {
    }

    // --- changes stream --------------------------------------------------------------------------

    static void changes(Check check, WorldServer world, BlockPos origin) throws Exception {
        signs(check, world, origin.add(0, 0, 6));
        waterlogged(check, world, origin.add(2, 0, 6));
        arrows(check, world, origin.add(-4, 0, 6));
        placing(check, world, origin.add(4, 0, -6));
        saplings(check, world, origin.add(-4, 0, -6));
        trampling(check, world, origin.add(0, 0, -8));
        trapHorses(check, world, origin.add(6, 0, 6));
        leashed(check, world, origin.add(-6, 0, 0));
        trails(check, world, origin.add(0, 0, 3));
    }

    private static void leashed(Check check, WorldServer world, BlockPos p) {
        net.minecraft.entity.passive.EntitySquid squid = new net.minecraft.entity.passive.EntitySquid(world);
        squid.setPosition(p.getX() + 0.5D, p.getY(), p.getZ() + 0.5D);
        world.spawnEntity(squid);
        check.ok(!ChangeHooks.persistentDespawn(squid), "a free squid despawns as before");
        EntityZombie holder = new EntityZombie(world);
        holder.setPosition(squid.posX + 1, squid.posY, squid.posZ);
        world.spawnEntity(holder);
        squid.setLeashHolder(holder, false);
        check.ok(ChangeHooks.customPersistence(squid), "a leashed squid is kept (26.1)");
        check.ok(ChangeHooks.persistentDespawn(squid), "its despawn check is skipped");
        ModConfig.changes.persistentMobsIdle = false;
        check.ok(ChangeHooks.persistentDespawn(squid), "also with persistentMobsIdle off");
        ModConfig.changes.persistentMobsIdle = true;
        squid.clearLeashed(false, false);
        check.ok(!ChangeHooks.customPersistence(squid), "let go, it may despawn again");
        EntityBoat boat = new EntityBoat(world, p.getX() + 0.5D, p.getY(), p.getZ() + 3.5D);
        world.spawnEntity(boat);
        EntityZombie rider = new EntityZombie(world);
        rider.setPosition(boat.posX, boat.posY, boat.posZ);
        world.spawnEntity(rider);
        rider.startRiding(boat, true);
        check.ok(ChangeHooks.customPersistence(rider), "a zombie in a boat is kept (requiresCustomPersistence)");
        ModConfig.changes.leashedMobsStay = false;
        check.ok(!ChangeHooks.customPersistence(rider), "setting off: 1.12.2's despawning");
        ModConfig.changes.leashedMobsStay = true;
        for (Entity e : new Entity[]{squid, holder, boat, rider}) {
            e.setDead();
        }
    }

    private static void trails(Check check, WorldServer world, BlockPos p) throws Exception {
        EntityEnderman enderman = new EntityEnderman(world);
        enderman.setPosition(10.5D, 70D, 10.5D);
        check.ok(TeleportTrails.landedFor(enderman, 0.5D, 70D, 0.5D, 10.5D, 75D, 10.5D),
                "an enderman at the target's x and z, at or below its y, landed");
        check.ok(!TeleportTrails.landedFor(enderman, 0.5D, 70D, 0.5D, 20.5D, 75D, 10.5D), "elsewhere it did not");
        check.ok(!TeleportTrails.landedFor(enderman, 10.5D, 70D, 10.5D, 10.5D, 75D, 10.5D), "nor where it started");
        world.setBlockState(p.down(), Blocks.STONE.getDefaultState());
        BlockPos to = p.add(5, 0, 0);
        world.setBlockState(to.down(), Blocks.STONE.getDefaultState());
        enderman.setPosition(p.getX() + 0.5D, p.getY(), p.getZ() + 0.5D);
        world.spawnEntity(enderman);
        int before = TeleportTrails.sent;
        // 1.12.2's teleportTo: the event, then attemptTeleport at its target.
        EnderTeleportEvent tp = new EnderTeleportEvent(enderman, to.getX() + 0.5D, to.getY() + 3, to.getZ() + 0.5D, 0);
        check.ok(!MinecraftForge.EVENT_BUS.post(tp), "the test teleport is allowed");
        check.ok(enderman.attemptTeleport(tp.getTargetX(), tp.getTargetY(), tp.getTargetZ()), "and lands on the stone");
        TeleportTrails.onWorldTick(new net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent(
                net.minecraftforge.fml.relauncher.Side.SERVER, net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END, world));
        check.eq(before + 1, TeleportTrails.sent, "an enderman's teleport sends one trail (26.3)");
        EnderTeleportEvent failed = new EnderTeleportEvent(enderman, to.getX() + 40.5D, to.getY() + 3, to.getZ() + 0.5D, 0);
        MinecraftForge.EVENT_BUS.post(failed);
        TeleportTrails.onWorldTick(new net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent(
                net.minecraftforge.fml.relauncher.Side.SERVER, net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END, world));
        check.eq(before + 1, TeleportTrails.sent, "a teleport that did not happen sends none");
        world.setBlockToAir(to.down());
        enderman.setDead();
        world.setBlockToAir(p.down());
    }

    private static EnderTeleportEvent teleport(EntityEnderman e, BlockPos ground) {
        EnderTeleportEvent tp = new EnderTeleportEvent(e, ground.getX() + 0.5D, ground.getY() + 4, ground.getZ() + 0.5D, 0);
        MinecraftForge.EVENT_BUS.post(tp);
        return tp;
    }

    private static void signs(Check check, WorldServer world, BlockPos ground) {
        EntityEnderman enderman = new EntityEnderman(world);
        world.setBlockState(ground, Blocks.STONE.getDefaultState());
        world.setBlockState(ground.up(), Blocks.STANDING_SIGN.getDefaultState(), 2);
        check.ok(EndermanRules.isSign(world.getBlockState(ground.up())), "a standing sign is a sign");
        EnderTeleportEvent tp = teleport(enderman, ground.up());
        check.ok(!tp.isCanceled(), "over a sign the teleport still happens");
        check.eq((double) ground.up().getY(), tp.getTargetY(),
                "MC-58061: not onto the sign: the search goes on below it, to the ground it stands on");
        world.setBlockToAir(ground.up());
        tp = teleport(enderman, ground);
        check.eq(ground.getY() + 4.0D, tp.getTargetY(), "with no sign the target is left alone");
        world.setBlockToAir(ground);
    }

    private static void waterlogged(Check check, WorldServer world, BlockPos ground) {
        world.setBlockState(ground, Blocks.STONE_STAIRS.getDefaultState().withProperty(BlockStairs.HALF, BlockStairs.EnumHalf.TOP));
        boolean wet = Waterlogging.setWater(world, ground, true, 2) && Waterlogging.hasWater(world, ground);
        if (!wet) {
            GameDropsBackport.LOGGER.info("SELFTEST note: no waterlogging in this run (Fluidlogged API absent); MC-220319 not checked");
        } else {
            EnderTeleportEvent tp = teleport(new EntityEnderman(world), ground);
            check.ok(tp.isCanceled(), "MC-220319: never onto a waterlogged block (26.3)");
            Waterlogging.setWater(world, ground, false, 2);
            tp = teleport(new EntityEnderman(world), ground);
            check.ok(!tp.isCanceled(), "onto the same block dry");
        }
        world.setBlockToAir(ground);
    }

    private static void arrows(Check check, WorldServer world, BlockPos ground) {
        world.setBlockState(ground, Blocks.STONE.getDefaultState());
        EntityEnderman enderman = new EntityEnderman(world);
        enderman.setPosition(ground.getX() + 0.5D, ground.getY() + 1, ground.getZ() + 0.5D);
        world.spawnEntity(enderman);
        EntityTippedArrow arrow = new EntityTippedArrow(world, enderman.posX + 3.0D, enderman.posY + 1.0D, enderman.posZ);
        arrow.motionX = -2.0D;
        world.spawnEntity(arrow);
        ProjectileImpactEvent.Arrow hit = new ProjectileImpactEvent.Arrow(arrow, new RayTraceResult(enderman));
        check.ok(MinecraftForge.EVENT_BUS.post(hit), "an arrow hitting an enderman is not stopped there: it dodges (26.3)");
        check.ok(arrow.isEntityAlive() && arrow.motionX == -2.0D, "the arrow flies on, not sent back: " + arrow.motionX);
        check.eq(0, enderman.getArrowCountInEntity(), "and is not counted stuck in it");
        EntityBoat boat = new EntityBoat(world, ground.getX() + 0.5D, ground.getY() + 1, ground.getZ() + 0.5D);
        world.spawnEntity(boat);
        EntityEnderman rider = new EntityEnderman(world);
        rider.setPosition(boat.posX, boat.posY, boat.posZ);
        world.spawnEntity(rider);
        rider.startRiding(boat, true);
        hit = new ProjectileImpactEvent.Arrow(arrow, new RayTraceResult(rider));
        check.ok(!MinecraftForge.EVENT_BUS.post(hit), "a riding enderman takes the arrow (endermenRiding)");
        ModConfig.changes.endermenArrows = false;
        hit = new ProjectileImpactEvent.Arrow(arrow, new RayTraceResult(enderman));
        check.ok(!MinecraftForge.EVENT_BUS.post(hit), "setting off: 1.12.2's arrow");
        ModConfig.changes.endermenArrows = true;
        for (Entity e : new Entity[]{enderman, arrow, rider, boat}) {
            e.setDead();
        }
        world.setBlockToAir(ground);
    }

    private static void placing(Check check, WorldServer world, BlockPos p) {
        EntityEnderman enderman = new EntityEnderman(world);
        enderman.setPosition(p.getX() + 3.5D, p.getY(), p.getZ() + 0.5D);
        world.spawnEntity(enderman);
        EndermanRules.PlaceBlock goal = null;
        boolean vanilla = false;
        for (EntityAITasks.EntityAITaskEntry entry : enderman.tasks.taskEntries) {
            if (entry.action instanceof EndermanRules.PlaceBlock) {
                goal = (EndermanRules.PlaceBlock) entry.action;
                check.eq(10, entry.priority, "26.3's place goal at 1.12.2's priority");
            }
            vanilla |= entry.action.getClass().getName().endsWith("$AIPlaceBlock");
        }
        check.ok(goal != null && !vanilla, "an enderman joining the world gets 26.3's place-block goal");
        if (goal == null) {
            enderman.setDead();
            return;
        }
        enderman.setHeldBlockState(Blocks.DIRT.getDefaultState());
        world.setBlockState(p.down(), Blocks.BEDROCK.getDefaultState());
        check.ok(!goal.place(world, p), "never onto bedrock (26.3)");
        world.setBlockState(p.down(), Blocks.STONE.getDefaultState());
        EntityItem item = new EntityItem(world, p.getX() + 0.5D, p.getY() + 0.1D, p.getZ() + 0.5D, new ItemStack(Items.STICK));
        item.setNoPickupDelay();
        world.spawnEntity(item);
        check.ok(!goal.place(world, p), "never where an entity is (an item frame, a dropped item)");
        item.setDead();
        world.removeEntityDangerously(item);
        check.ok(goal.place(world, p), "onto stone in the open it puts the block down");
        check.eq(Blocks.DIRT, world.getBlockState(p).getBlock(), "the carried dirt is there");
        check.ok(enderman.getHeldBlockState() == null, "and no longer carried");
        world.setBlockToAir(p);
        world.setBlockToAir(p.down());
        enderman.setDead();
    }

    private static void saplings(Check check, WorldServer world, BlockPos p) {
        FakePlayer player = FakePlayerFactory.getMinecraft(world);
        for (BlockPos g : BlockPos.getAllInBox(p.add(-1, -1, -1), p.add(1, -1, 1))) {
            world.setBlockState(g, Blocks.DIRT.getDefaultState());
        }
        world.setBlockState(p, Blocks.SAPLING.getDefaultState().withProperty(BlockSapling.TYPE, BlockPlanks.EnumType.DARK_OAK));
        ItemStack meal = new ItemStack(Items.DYE, 4, 15);
        check.ok(!ItemDye.applyBonemeal(meal, world, p, player, net.minecraft.util.EnumHand.MAIN_HAND), "a lone dark oak sapling takes no bone meal (26.3)");
        check.eq(4, meal.getCount(), "the bone meal stays in hand");
        check.ok(SmallRules.canGrow(world, p, world.getBlockState(p)) == false, "canGrow: a lone dark oak cannot");
        for (BlockPos s : new BlockPos[]{p.east(), p.south(), p.east().south()}) {
            world.setBlockState(s, Blocks.SAPLING.getDefaultState().withProperty(BlockSapling.TYPE, BlockPlanks.EnumType.DARK_OAK));
        }
        check.ok(SmallRules.canGrow(world, p, world.getBlockState(p)), "in its 2x2 it can");
        check.ok(SmallRules.canGrow(world, p.east().south(), world.getBlockState(p.east().south())), "from any corner");
        world.setBlockState(p.east(), Blocks.SAPLING.getDefaultState());
        check.ok(SmallRules.canGrow(world, p.east(), world.getBlockState(p.east())), "an oak sapling alone can");
        for (BlockPos q : BlockPos.getAllInBox(p.add(-1, -1, -1), p.add(1, 0, 1))) {
            world.setBlockToAir(q);
        }
    }

    private static void trampling(Check check, WorldServer world, BlockPos p) {
        MinecraftServer server = world.getMinecraftServer();
        if (!(server instanceof DedicatedServer)) {
            return;
        }
        DedicatedServer ds = (DedicatedServer) server;
        BlockPos spawn = world.getSpawnPoint();
        GameProfile op = new GameProfile(UUID.nameUUIDFromBytes("GDTestOp".getBytes()), "GDTestOp");
        int was = ds.getIntProperty("spawn-protection", 0);
        ds.setProperty("spawn-protection", 16);
        ds.getPlayerList().addOp(op);
        FakePlayer player = FakePlayerFactory.getMinecraft(world);
        try {
            check.ok(SmallRules.underSpawnProtection(world, spawn, player), "a non-op at spawn is under spawn protection");
            BlockEvent.FarmlandTrampleEvent near = new BlockEvent.FarmlandTrampleEvent(world, spawn,
                    Blocks.FARMLAND.getDefaultState(), 1.5F, player);
            check.ok(MinecraftForge.EVENT_BUS.post(near), "a player cannot trample farmland there (26.3)");
            EntityZombie zombie = new EntityZombie(world);
            BlockEvent.FarmlandTrampleEvent mob = new BlockEvent.FarmlandTrampleEvent(world, spawn,
                    Blocks.FARMLAND.getDefaultState(), 1.5F, zombie);
            check.ok(!MinecraftForge.EVENT_BUS.post(mob), "a mob still can (mob griefing decides)");
            BlockPos far = spawn.add(200, 0, 200);
            BlockEvent.FarmlandTrampleEvent away = new BlockEvent.FarmlandTrampleEvent(world, far,
                    Blocks.FARMLAND.getDefaultState(), 1.5F, player);
            check.ok(!MinecraftForge.EVENT_BUS.post(away), "outside spawn protection a player can");
        } finally {
            ds.getPlayerList().removeOp(op);
            ds.setProperty("spawn-protection", was);
        }
    }

    private static void trapHorses(Check check, WorldServer world, BlockPos p) throws Exception {
        world.setBlockState(p.down(), Blocks.STONE.getDefaultState());
        EntitySkeletonHorse kept = new EntitySkeletonHorse(world);
        EntitySkeletonHorse plain = new EntitySkeletonHorse(world);
        for (EntitySkeletonHorse h : new EntitySkeletonHorse[]{kept, plain}) {
            h.setPosition(p.getX() + 0.5D, p.getY(), p.getZ() + 0.5D);
            h.setTrap(true);
            NBTSet.trapTime(h, 17998);
            world.spawnEntity(h);
        }
        kept.enablePersistence();
        for (int i = 0; i < 5; i++) {
            kept.onUpdate();
            plain.onUpdate();
        }
        check.ok(kept.isEntityAlive(), "a persistent trap horse past 18000 ticks stays (26.1): " + SmallRules.trapTime(kept));
        check.ok(!plain.isEntityAlive(), "an ordinary one still goes, as in 1.12.2");
        kept.setDead();
        world.setBlockToAir(p.down());
    }

    /** Writes a skeleton trap horse's count through its own NBT. */
    private static final class NBTSet {
        static void trapTime(EntitySkeletonHorse horse, int ticks) {
            net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
            horse.writeEntityToNBT(tag);
            tag.setInteger("SkeletonTrapTime", ticks);
            tag.setBoolean("SkeletonTrap", true);
            horse.readEntityFromNBT(tag);
        }
    }

    // --- other streams ---------------------------------------------------------------------------

    /** Sulfur cube stream: 26.2's {@code #cannot_be_pushed_onto_boats}. */
    static void cubeBoats(Check check, WorldServer world, BlockPos at) {
        EntitySulfurCube cube = new EntitySulfurCube(world);
        cube.setPosition(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D);
        world.spawnEntity(cube);
        EntityBoat boat = new EntityBoat(world, at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D);
        world.spawnEntity(boat);
        boat.applyEntityCollision(cube);
        check.ok(!cube.isRiding(), "a boat bumping into a sulfur cube does not take it aboard (26.2)");
        EntityZombie zombie = new EntityZombie(world);
        zombie.setPosition(boat.posX, boat.posY, boat.posZ);
        world.spawnEntity(zombie);
        check.ok(zombie.startRiding(boat), "a zombie it still can");
        cube.setDead();
        boat.setDead();
        zombie.setDead();
    }

    /** Sulfur cube stream: 26.2's dispensers shear a cube and never put armor on one. */
    static void cubeDispensers(Check check, WorldServer world, BlockPos d) {
        world.setBlockState(d, Blocks.DISPENSER.getDefaultState().withProperty(net.minecraft.block.BlockDispenser.FACING, EnumFacing.EAST));
        net.minecraft.dispenser.IBlockSource source = new net.minecraft.block.BlockSourceImpl(world, d);
        EntitySulfurCube cube = new EntitySulfurCube(world);
        cube.setLocationAndAngles(d.getX() + 1.5D, d.getY(), d.getZ() + 0.5D, 0.0F, 0.0F);
        cube.onInitialSpawn(world.getDifficultyForLocation(d), null);
        world.spawnEntity(cube);
        cube.equipItem(new ItemStack(Blocks.PLANKS));
        check.ok(cube.hasBodyItem(), "the cube holds planks");
        ItemStack shears = new ItemStack(Items.SHEARS);
        net.minecraft.block.BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(Items.SHEARS).dispense(source, shears);
        check.ok(!cube.hasBodyItem(), "a dispenser's shears cut the block out of a cube (26.2)");
        check.eq(1, shears.getItemDamage(), "and lose a point");
        ItemStack helmet = new ItemStack(Items.IRON_HELMET);
        check.ok(cube.canPickUpLoot(), "the empty adult cube still picks up blocks");
        net.minecraft.block.BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(Items.IRON_HELMET).dispense(source, helmet);
        check.ok(cube.getItemStackFromSlot(net.minecraft.inventory.EntityEquipmentSlot.HEAD).isEmpty(),
                "a dispenser never puts armor on a cube (26.2: body slot only)");
        check.ok(helmet.isEmpty(), "the helmet is dispensed as an item instead");
        cube.setDead();
        world.removeEntityDangerously(cube);
        EntityZombie zombie = new EntityZombie(world);
        zombie.setCanPickUpLoot(true);
        zombie.setPosition(d.getX() + 1.5D, d.getY(), d.getZ() + 0.5D);
        world.spawnEntity(zombie);
        ItemStack helmet2 = new ItemStack(Items.IRON_HELMET);
        net.minecraft.block.BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(Items.IRON_HELMET).dispense(source, helmet2);
        check.eq(Items.IRON_HELMET, zombie.getItemStackFromSlot(net.minecraft.inventory.EntityEquipmentSlot.HEAD).getItem(),
                "a zombie in front still gets the helmet");
        zombie.setDead();
        for (EntityItem item : world.getEntitiesWithinAABB(EntityItem.class, new net.minecraft.util.math.AxisAlignedBB(d).grow(3))) {
            item.setDead();
        }
        world.setBlockToAir(d);
    }

    /** Sulfur stream: 26.2's {@code #not_affected_by_geysers}. */
    static void geysers(Check check, WorldServer world) {
        check.ok(TilePotentSulfur.notAffectedByGeysers(new EntityDragon(world)), "geysers leave the ender dragon alone (26.2)");
        check.ok(!TilePotentSulfur.notAffectedByGeysers(new EntityZombie(world)), "they push a zombie");
    }

    /** Soft blocks stream: 26.3's {@code #mineable/hoe} and {@code sleep_in_straw_bed}. */
    static void strawBed(Check check, WorldServer world) {
        check.eq(8.0F, StrawBedSleep.hoeSpeed(new ItemStack(Items.DIAMOND_HOE)), "a diamond hoe cuts a straw bed at its tier speed");
        check.eq(2.0F, StrawBedSleep.hoeSpeed(new ItemStack(Items.WOODEN_HOE)), "a wooden hoe at 2");
        check.eq(1.0F, StrawBedSleep.hoeSpeed(new ItemStack(Items.STICK)), "a stick is no tool");
        FakePlayer fake = FakePlayerFactory.getMinecraft(world);
        fake.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND, new ItemStack(Items.IRON_HOE));
        net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed speed = new net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed(
                fake, SoftBlocks.strawBed.getDefaultState(), 1.0F, BlockPos.ORIGIN);
        MinecraftForge.EVENT_BUS.post(speed);
        check.eq(6.0F, speed.getNewSpeed(), "breaking a straw bed with an iron hoe: 6 times as fast (26.3)");
        fake.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND, ItemStack.EMPTY);
        check.ok(SoftBlocks.sleepInStrawBed != null && StatList.getOneShotStat(SoftBlocks.SLEEP_STAT) == SoftBlocks.sleepInStrawBed,
                "the straw bed statistic is registered: " + SoftBlocks.SLEEP_STAT);
        check.ok(StatList.BASIC_STATS.contains(SoftBlocks.sleepInStrawBed), "and listed with the general statistics");
        MinecraftServer server = world.getMinecraftServer();
        EntityPlayerMP p = new EntityPlayerMP(server, world, new GameProfile(UUID.nameUUIDFromBytes("GDStraw".getBytes()), "GDStraw"),
                new PlayerInteractionManager(world));
        p.addStat(StatList.SLEEP_IN_BED);
        int beds = p.getStatFile().readStat(StatList.SLEEP_IN_BED);
        try {
            java.lang.reflect.Method count = com.exiledradio.gamedropsbackport.softblocks.BlockStrawBed.class.getDeclaredMethod("countSleep",
                    net.minecraft.entity.player.EntityPlayer.class);
            count.setAccessible(true);
            count.invoke(null, p);
        } catch (ReflectiveOperationException e) {
            check.ok(false, "countSleep: " + e);
            return;
        }
        check.eq(beds - 1, p.getStatFile().readStat(StatList.SLEEP_IN_BED), "a night in a straw bed is not a night in a bed");
        check.eq(1, p.getStatFile().readStat(SoftBlocks.sleepInStrawBed), "it is a night in a straw bed");
    }
}
