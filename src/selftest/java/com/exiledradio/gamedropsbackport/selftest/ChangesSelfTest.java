package com.exiledradio.gamedropsbackport.selftest;

import com.exiledradio.gamedropsbackport.ModConfig;
import com.exiledradio.gamedropsbackport.asm.ChangesTransformer;
import com.exiledradio.gamedropsbackport.changes.ChangeHooks;
import com.exiledradio.gamedropsbackport.changes.Changes;
import com.exiledradio.gamedropsbackport.changes.WorldClocks;

import net.minecraft.block.BlockDispenser;
import net.minecraft.command.CommandResultStats;
import net.minecraft.command.ICommandSender;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityShulker;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntityEndGateway;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * The changes stream: one block of checks per setting ({@code docs/streams/changes.md}), each with the setting on and,
 * where 1.12.2's behavior is cheap to see, off.
 */
public final class ChangesSelfTest implements SelfTest {

    @Override
    public String name() {
        return "changes";
    }

    /** A command sender that keeps what it is told: each message's translation key and arguments. */
    static final class Sender implements ICommandSender {
        final MinecraftServer server;
        final World world;
        final List<ITextComponent> messages = new ArrayList<ITextComponent>();
        int query = Integer.MIN_VALUE;

        Sender(MinecraftServer server, World world) {
            this.server = server;
            this.world = world;
        }

        @Override
        public String getName() {
            return "selftest";
        }

        @Override
        public boolean canUseCommand(int permLevel, String commandName) {
            return true;
        }

        @Override
        public World getEntityWorld() {
            return this.world;
        }

        @Override
        public MinecraftServer getServer() {
            return this.server;
        }

        @Override
        public void sendMessage(ITextComponent component) {
            this.messages.add(component);
        }

        @Override
        public void setCommandStat(CommandResultStats.Type type, int amount) {
            if (type == CommandResultStats.Type.QUERY_RESULT) {
                this.query = amount;
            }
        }

        /** Runs a command; returns the key of the last message it got. */
        String run(String command) {
            this.messages.clear();
            this.server.getCommandManager().executeCommand(this, command);
            return lastKey();
        }

        String lastKey() {
            if (this.messages.isEmpty()) {
                return "";
            }
            ITextComponent last = this.messages.get(this.messages.size() - 1);
            return last instanceof TextComponentTranslation ? ((TextComponentTranslation) last).getKey() : last.getUnformattedText();
        }

        String lastText() {
            return this.messages.isEmpty() ? "" : this.messages.get(this.messages.size() - 1).getUnformattedText();
        }
    }

    @Override
    public void run(Check check) throws Exception {
        WorldServer world = check.world;
        MinecraftServer server = world.getMinecraftServer();
        BlockPos origin = new BlockPos(64, 120, 64);
        clear(world, origin, 10);
        time(check, server, world);
        gameRules(check, server, world);
        teamColors(check, server, world);
        bed(check, world, origin);
        stew(check, world);
        shield(check, world, origin);
        endermen(check, world, origin);
        teleportTargets(check, world, origin);
        persistent(check, world, origin);
        leader(check, world, origin);
        witch(check, world, origin);
        spawnEggs(check, world, origin);
        sunHelmet(check);
        spectators(check, world, origin);
        ChangesGapsSelfTest.run(check, server, world);
        clear(world, origin, 10);
    }

    // timeCommand ----------------------------------------------------------------------------------

    private static void time(Check check, MinecraftServer server, WorldServer world) {
        Sender s = new Sender(server, world);
        s.run("time set 1");
        check.eq("commands.time.set.absolute", s.run("time set 1000"), "time set 1000");
        check.eq(1000L, world.getWorldTime(), "time set 1000 sets the overworld time");
        check.eq("Set minecraft:overworld to 1000 tick(s)", s.lastText().replace("§r", ""), "time set's 26.1 message");
        check.eq("commands.time.set.already_at_time_marker", s.run("time set day"), "time set day at 1000 is already there");
        check.eq("commands.time.set.time_marker", s.run("time set noon"), "time set noon");
        check.eq(6000L, world.getWorldTime(), "noon is 6000");
        s.run("time set day");
        check.eq(25000L, world.getWorldTime(), "time set day goes forward to the next day (26.1), not back to 1000");
        s.run("time set minecraft:midnight");
        check.eq(42000L, world.getWorldTime(), "a marker by its full id");
        check.eq("commands.time.set.absolute", s.run("time add 1d"), "time add with a unit");
        check.eq(66000L, world.getWorldTime(), "1d is 24000 ticks");
        check.eq(66000, s.query, "time add returns the clock's total ticks (26.1)");
        s.run("time add -100000");
        check.eq(0L, world.getWorldTime(), "time add never goes below 0");
        s.run("time set 30000");
        check.eq("commands.time.query.timeline", s.run("time query minecraft:day"), "time query minecraft:day");
        check.eq(6000, s.query, "the day timeline's ticks");
        check.eq("commands.time.query.timeline.repetitions", s.run("time query minecraft:day repetition"), "repetition");
        check.eq(1, s.query, "one day passed");
        s.run("time query daytime");
        check.eq(6000, s.query, "1.12.2's query daytime still answers");
        s.run("time query day");
        check.eq(1, s.query, "1.12.2's query day still answers");
        check.eq("commands.time.query.absolute", s.run("time query time"), "time query time");
        check.eq(30000, s.query, "query time is the total");
        check.eq("commands.time.query.gametime", s.run("time query gametime"), "time query gametime");
        check.eq("commands.time.no_time_marker_found", s.run("time of minecraft:the_end set day"), "the End clock has no markers");
        check.eq("commands.time.set.absolute", s.run("time of the_end set 500"), "the End clock by its short id");
        check.eq(500L, WorldClocks.get(server).totalTicks(server, WorldClocks.THE_END), "the End clock's ticks");
        check.eq(30000L, world.getWorldTime(), "the End clock leaves the overworld alone");
        check.eq("commands.time.wrong_timeline_for_clock", s.run("time of the_end query minecraft:day"), "timeline of another clock");
        check.eq("argument.resource.not_found", s.run("time of minecraft:moon set 1"), "unknown clock");
        WorldServer nether = server.getWorld(-1);
        Sender n = new Sender(server, nether);
        check.eq("commands.time.no_default_clock", n.run("time set day"), "the Nether has no default clock");
        Sender e = new Sender(server, server.getWorld(1));
        check.eq("commands.time.set.absolute", e.run("time set 7"), "in the End, the End's clock");
        check.eq(7L, WorldClocks.get(server).totalTicks(server, WorldClocks.THE_END), "the End clock set from the End");
        check.eq("commands.time.pause", s.run("time pause"), "time pause");
        check.eq("commands.time.pause.already_paused", s.run("time pause"), "pausing twice");
        WorldClocks.Clock ow = WorldClocks.get(server).clock(WorldClocks.OVERWORLD);
        check.ok(ow.paused, "the clock is paused");
        check.eq("commands.time.resume", s.run("time resume"), "time resume");
        check.eq("commands.time.pause.already_running", s.run("time resume"), "resuming twice");
        check.eq("commands.time.rate", s.run("time rate 0.5"), "time rate");
        check.eq(0.5F, ow.rate, "the rate");
        check.eq("commands.time.rate.already_same", s.run("time rate 0.5"), "the same rate");
        check.eq("argument.float.big", s.run("time rate 2000"), "rate above 1000");
        s.run("time rate 1");
        check.eq("commands.time.set.already_at_time", s.run("time set 30000"), "setting the same total");
        check.eq("argument.time.tick_count_too_low", s.run("time set -5"), "a negative time");
        boolean was = ModConfig.changes.timeCommand;
        ModConfig.changes.timeCommand = false;
        check.eq("commands.time.set", s.run("time set day"), "setting off: 1.12.2's /time");
        check.eq(1000L, world.getWorldTime(), "setting off: 1.12.2's day is 1000");
        ModConfig.changes.timeCommand = was;
        s.run("time set 1000");
    }

    // gameRuleMessages -----------------------------------------------------------------------------

    private static void gameRules(Check check, MinecraftServer server, WorldServer world) {
        Sender s = new Sender(server, world);
        world.getGameRules().setOrCreateGameRule("keepInventory", "false");
        check.eq("commands.gamerule.query", s.run("gamerule keepInventory"), "query");
        check.eq("commands.gamerule.not_set", s.run("gamerule keepInventory false"), "the same value is an error (26.3)");
        check.eq("commands.gamerule.set", s.run("gamerule keepInventory true"), "set");
        check.ok(world.getGameRules().getBoolean("keepInventory"), "the rule changed");
        check.eq("commands.generic.boolean.invalid", s.run("gamerule keepInventory maybe"), "1.12.2's boolean check stays");
        s.run("gamerule keepInventory false");
        boolean was = ModConfig.changes.gameRuleMessages;
        ModConfig.changes.gameRuleMessages = false;
        check.eq("commands.gamerule.success", s.run("gamerule keepInventory false"), "setting off: 1.12.2's message");
        ModConfig.changes.gameRuleMessages = was;
    }

    // teamColorNames -------------------------------------------------------------------------------

    private static void teamColors(Check check, MinecraftServer server, WorldServer world) {
        Sender s = new Sender(server, world);
        s.run("scoreboard teams add gdtest");
        ScorePlayerTeam team = world.getScoreboard().getTeam("gdtest");
        if (!check.ok(team != null, "the test team exists")) {
            return;
        }
        s.run("scoreboard teams option gdtest color darkpurple");
        check.ok(team.getColor() != TextFormatting.DARK_PURPLE, "darkpurple is refused (26.2)");
        check.eq("commands.scoreboard.teams.option.noValue", s.lastKey(), "with 1.12.2's valid-values error");
        s.run("scoreboard teams option gdtest color DarkPurple");
        check.ok(team.getColor() != TextFormatting.DARK_PURPLE, "DarkPurple is refused");
        s.run("scoreboard teams option gdtest color dark_purple");
        check.eq(TextFormatting.DARK_PURPLE, team.getColor(), "dark_purple is taken");
        boolean was = ModConfig.changes.teamColorNames;
        ModConfig.changes.teamColorNames = false;
        s.run("scoreboard teams option gdtest color gold");
        s.run("scoreboard teams option gdtest color darkaqua");
        check.eq(TextFormatting.DARK_AQUA, team.getColor(), "setting off: 1.12.2 takes darkaqua");
        ModConfig.changes.teamColorNames = was;
        s.run("scoreboard teams remove gdtest");
    }

    // bedBounce ------------------------------------------------------------------------------------

    private static void bed(Check check, WorldServer world, BlockPos origin) {
        check.ok(ChangesTransformer.appliedP1, "P1 applied (BlockBed.onLanded)");
        EntityPig pig = new EntityPig(world);
        pig.motionY = -1.0D;
        Blocks.BED.onLanded(world, pig);
        check.ok(Math.abs(pig.motionY - 0.75D) < 1.0E-6, "a mob bounces back at 0.75 (26.2), got " + pig.motionY);
        EntityItem item = new EntityItem(world, 0, 0, 0, new ItemStack(Items.APPLE));
        item.motionY = -1.0D;
        Blocks.BED.onLanded(world, item);
        check.ok(Math.abs(item.motionY - 0.6D) < 1.0E-6, "anything else at 0.75 x 0.8, got " + item.motionY);
        boolean was = ModConfig.changes.bedBounce;
        ModConfig.changes.bedBounce = false;
        pig.motionY = -1.0D;
        Blocks.BED.onLanded(world, pig);
        check.ok(Math.abs(pig.motionY - 0.66D) < 1.0E-6, "setting off: 1.12.2's 0.66, got " + pig.motionY);
        ModConfig.changes.bedBounce = was;
    }

    // mushroomStew ---------------------------------------------------------------------------------

    private static void stew(Check check, World world) {
        check.ok(CraftingManager.getRecipe(new ResourceLocation("mushroom_stew")) instanceof net.minecraftforge.oredict.ShapelessOreRecipe,
                "1.12.2's stew recipe is replaced under its own id");
        Item shelf = Item.getByNameOrId("gamedropsbackport:shelf_mushroom");
        ItemStack red = new ItemStack(Blocks.RED_MUSHROOM);
        ItemStack brown = new ItemStack(Blocks.BROWN_MUSHROOM);
        check.ok(stewFrom(world, red, red), "two red mushrooms make stew (26.3)");
        check.ok(stewFrom(world, brown, brown), "two brown mushrooms make stew");
        check.ok(stewFrom(world, brown, red), "a brown and a red, as before");
        if (shelf != null) {
            check.ok(stewFrom(world, new ItemStack(shelf), red), "the shelf mushroom is one of #mushrooms");
        }
        check.ok(!stewFrom(world, red, new ItemStack(Items.APPLE)), "not with something else");
    }

    private static boolean stewFrom(World world, ItemStack a, ItemStack b) {
        InventoryCrafting grid = new InventoryCrafting(new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer playerIn) {
                return false;
            }
        }, 3, 3);
        grid.setInventorySlotContents(0, a.copy());
        grid.setInventorySlotContents(4, b.copy());
        grid.setInventorySlotContents(8, new ItemStack(Items.BOWL));
        ItemStack out = CraftingManager.findMatchingResult(grid, world);
        return out.getItem() == Items.MUSHROOM_STEW;
    }

    // shieldBeforeTools ----------------------------------------------------------------------------

    private static void shield(Check check, WorldServer world, BlockPos origin) {
        FakePlayer player = FakePlayerFactory.getMinecraft(world);
        ItemStack shield = new ItemStack(Items.SHIELD);
        check.ok(Changes.shieldFirst(new ItemStack(Items.IRON_HOE), shield, false, player), "hoe and shield: the shield (26.3)");
        check.ok(Changes.shieldFirst(new ItemStack(Items.IRON_SHOVEL), shield, false, player), "shovel and shield: the shield");
        check.ok(!Changes.shieldFirst(new ItemStack(Items.IRON_HOE), shield, true, player), "sneaking: the hoe");
        check.ok(!Changes.shieldFirst(new ItemStack(Items.IRON_HOE), ItemStack.EMPTY, false, player), "no shield: the hoe");
        check.ok(!Changes.shieldFirst(new ItemStack(Items.IRON_SWORD), shield, false, player), "a sword is not a block tool");
        world.setBlockState(origin, Blocks.GRASS.getDefaultState());
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.IRON_HOE));
        player.setHeldItem(EnumHand.OFF_HAND, shield);
        PlayerInteractEvent.RightClickBlock event = ForgeHooks.onRightClickBlock(player, EnumHand.MAIN_HAND, origin,
                EnumFacing.UP, new Vec3d(0.5D, 1.0D, 0.5D));
        check.eq(Event.Result.DENY, event.getUseItem(), "the hoe is not used on the grass");
        check.ok(!event.isCanceled(), "the block itself is still used");
        player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        player.setHeldItem(EnumHand.OFF_HAND, ItemStack.EMPTY);
    }

    // endermenRiding -------------------------------------------------------------------------------

    private static void endermen(Check check, WorldServer world, BlockPos origin) {
        check.ok(ChangesTransformer.appliedP2, "P2 applied (EntityEnderman.attackEntityFrom)");
        EntityEnderman enderman = new EntityEnderman(world);
        enderman.setPosition(origin.getX() + 0.5D, origin.getY() + 1, origin.getZ() + 0.5D);
        world.spawnEntity(enderman);
        EntityBoat boat = new EntityBoat(world, origin.getX() + 0.5D, origin.getY() + 1, origin.getZ() + 0.5D);
        world.spawnEntity(boat);
        enderman.startRiding(boat, true);
        EntityArrow arrow = new EntityTippedArrow(world);
        float before = enderman.getHealth();
        enderman.attackEntityFrom(new EntityDamageSourceIndirect("arrow", arrow, null).setProjectile(), 4.0F);
        check.ok(enderman.getHealth() < before, "a riding enderman is hurt by a projectile (26.3)");
        check.ok(enderman.isRiding(), "and stays in its boat");
        EnderTeleportEvent tp = new EnderTeleportEvent(enderman, origin.getX(), origin.getY() + 5, origin.getZ(), 0);
        check.ok(MinecraftForge.EVENT_BUS.post(tp), "a riding enderman never teleports");
        enderman.dismountRidingEntity();
        enderman.hurtResistantTime = 0;
        before = enderman.getHealth();
        enderman.attackEntityFrom(new EntityDamageSourceIndirect("arrow", arrow, null).setProjectile(), 4.0F);
        check.eq(before, enderman.getHealth(), "not riding, projectiles still miss it (1.12.2 and 26.3)");
        enderman.setDead();
        boat.setDead();
    }

    // teleportTargets ------------------------------------------------------------------------------

    private static void teleportTargets(Check check, WorldServer world, BlockPos origin) {
        EntityEnderman enderman = new EntityEnderman(world);
        BlockPos a = origin.add(4, 0, 0);
        world.setBlockState(a, Blocks.BEDROCK.getDefaultState());
        check.ok(!Changes.endermanMayLand(enderman, a.getX() + 0.5D, a.getY() + 4, a.getZ() + 0.5D), "not onto bedrock (26.3)");
        world.setBlockState(a, Blocks.STONE.getDefaultState());
        check.ok(Changes.endermanMayLand(enderman, a.getX() + 0.5D, a.getY() + 4, a.getZ() + 0.5D), "onto stone");
        world.setBlockState(a, Blocks.MAGMA.getDefaultState());
        check.ok(!Changes.endermanMayLand(enderman, a.getX() + 0.5D, a.getY() + 4, a.getZ() + 0.5D), "not onto magma");
        world.setBlockState(a, Blocks.STONE.getDefaultState());
        world.setBlockState(a.up(), Blocks.FIRE.getDefaultState(), 2);
        check.ok(!Changes.endermanMayLand(enderman, a.getX() + 0.5D, a.getY() + 4, a.getZ() + 0.5D), "not into fire");
        world.setBlockToAir(a.up());
        EnderTeleportEvent tp = new EnderTeleportEvent(enderman, a.getX() + 0.5D, a.getY() + 4, a.getZ() + 0.5D, 0);
        check.ok(!MinecraftForge.EVENT_BUS.post(tp), "the event lets a good landing through");
        EntityShulker shulker = new EntityShulker(world);
        BlockPos b = origin.add(-4, 0, 0);
        world.setBlockState(b, Blocks.BEDROCK.getDefaultState());
        // A new shulker is attached by its bottom (DOWN).
        EnderTeleportEvent st = new EnderTeleportEvent(shulker, b.getX(), b.getY() + 1, b.getZ(), 0);
        check.ok(MinecraftForge.EVENT_BUS.post(st), "a shulker never attaches to bedrock (26.3)");
        world.setBlockState(b, Blocks.STONE.getDefaultState());
        st = new EnderTeleportEvent(shulker, b.getX(), b.getY() + 1, b.getZ(), 0);
        check.ok(!MinecraftForge.EVENT_BUS.post(st), "to stone it may");
    }

    // persistentMobsIdle ---------------------------------------------------------------------------

    private static void persistent(Check check, WorldServer world, BlockPos origin) throws Exception {
        check.ok(ChangesTransformer.appliedP3, "P3 applied (EntityLiving.despawnEntity)");
        Method despawn = ReflectionHelper.findMethod(EntityLiving.class, "despawnEntity", "func_70623_bb");
        EntityZombie zombie = new EntityZombie(world);
        zombie.setPosition(origin.getX(), origin.getY() + 1, origin.getZ());
        zombie.enablePersistence();
        ChangeHooksAccess.idle(zombie, 150);
        despawn.invoke(zombie);
        check.eq(150, zombie.getIdleTime(), "a persistent mob with no player near keeps counting (26.3)");
        boolean was = ModConfig.changes.persistentMobsIdle;
        ModConfig.changes.persistentMobsIdle = false;
        despawn.invoke(zombie);
        check.eq(0, zombie.getIdleTime(), "setting off: 1.12.2 zeroes it");
        ModConfig.changes.persistentMobsIdle = was;
    }

    /** Sets the protected idle time through the hook class's own accessor. */
    static final class ChangeHooksAccess {
        static void idle(EntityLiving mob, int value) throws Exception {
            java.lang.reflect.Field f = ReflectionHelper.findField(net.minecraft.entity.EntityLivingBase.class, "idleTime", "field_70708_bq");
            f.setInt(mob, value);
        }
    }

    // leaderZombieHealth ---------------------------------------------------------------------------

    private static void leader(Check check, WorldServer world, BlockPos origin) {
        EntityZombie zombie = new EntityZombie(world);
        zombie.setPosition(origin.getX(), origin.getY() + 1, origin.getZ());
        zombie.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH)
                .applyModifier(new AttributeModifier("Leader zombie bonus", 1.0D, 2));
        check.eq(20.0F, zombie.getHealth(), "1.12.2 leaves a new leader at 20");
        world.spawnEntity(zombie);
        check.eq(40.0F, zombie.getHealth(), "26.1 fills its raised maximum");
        zombie.setDead();
    }

    // witchCloseThrows -----------------------------------------------------------------------------

    private static void witch(Check check, WorldServer world, BlockPos origin) {
        check.ok(ChangesTransformer.appliedP4, "P4 applied (EntityWitch.attackEntityWithRangedAttack)");
        EntityPotion close = new EntityPotion(world);
        ChangeHooks.witchShoot(close, 1.5D, 0.3D, 0.0D, 0.75F, 0.0F);
        double v = Math.sqrt(close.motionX * close.motionX + close.motionY * close.motionY + close.motionZ * close.motionZ);
        check.ok(Math.abs(v - 0.45D) < 1.0E-3, "within 2 blocks the witch throws at 0.45 (26.1), got " + v);
        EntityPotion far = new EntityPotion(world);
        ChangeHooks.witchShoot(far, 5.0D, 1.0D, 0.0D, 0.75F, 0.0F);
        v = Math.sqrt(far.motionX * far.motionX + far.motionY * far.motionY + far.motionZ * far.motionZ);
        check.ok(Math.abs(v - 0.75D) < 1.0E-3, "farther, 0.75 as before, got " + v);
    }

    // peacefulSpawnEggs ----------------------------------------------------------------------------

    private static void spawnEggs(Check check, WorldServer world, BlockPos origin) {
        EnumDifficulty old = world.getDifficulty();
        world.getWorldInfo().setDifficulty(EnumDifficulty.PEACEFUL);
        try {
            ItemStack zombieEgg = new ItemStack(Items.SPAWN_EGG);
            ItemMonsterPlacer.applyEntityIdToItemStack(zombieEgg, new ResourceLocation("zombie"));
            ItemStack pigEgg = new ItemStack(Items.SPAWN_EGG);
            ItemMonsterPlacer.applyEntityIdToItemStack(pigEgg, new ResourceLocation("pig"));
            check.ok(Changes.blockedOnPeaceful(world, zombieEgg), "a zombie egg cannot spawn on Peaceful");
            check.ok(!Changes.blockedOnPeaceful(world, pigEgg), "a pig egg can");
            BlockPos d = origin.add(0, 3, 4);
            world.setBlockState(d, Blocks.DISPENSER.getDefaultState().withProperty(BlockDispenser.FACING, EnumFacing.UP));
            IBehaviorDispenseItem behavior = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(Items.SPAWN_EGG);
            ItemStack out = behavior.dispense(new net.minecraft.block.BlockSourceImpl(world, d), zombieEgg);
            check.eq(1, out.getCount(), "the dispenser keeps the zombie egg (26.2)");
            ItemStack pigOut = behavior.dispense(new net.minecraft.block.BlockSourceImpl(world, d), pigEgg.copy());
            check.eq(0, pigOut.getCount(), "and uses the pig egg");
            for (Entity e : world.getEntitiesWithinAABB(EntityPig.class, new net.minecraft.util.math.AxisAlignedBB(d).grow(3))) {
                e.setDead();
            }
            world.setBlockToAir(d);
        } finally {
            world.getWorldInfo().setDifficulty(old);
        }
    }

    // sunDamageEnchantments ------------------------------------------------------------------------

    private static void sunHelmet(Check check) {
        check.eq(2, ChangesTransformer.appliedP5, "P5 applied (EntityZombie and AbstractSkeleton)");
        ItemStack plain = new ItemStack(Items.DIAMOND_HELMET);
        ItemStack enchanted = new ItemStack(Items.DIAMOND_HELMET);
        enchanted.addEnchantment(Enchantments.UNBREAKING, 3);
        for (int i = 0; i < 300; i++) {
            ChangeHooks.sunDamage(plain, plain.getItemDamage() + 1);
            ChangeHooks.sunDamage(enchanted, enchanted.getItemDamage() + 1);
        }
        check.eq(300, plain.getItemDamage(), "without Unbreaking every point counts");
        check.ok(enchanted.getItemDamage() < 250, "Unbreaking III saves some of them (armor keeps 60% of hits) (26.3), took " + enchanted.getItemDamage());
    }

    // spectatorPortals -----------------------------------------------------------------------------

    private static void spectators(Check check, WorldServer world, BlockPos origin) {
        FakePlayer player = FakePlayerFactory.getMinecraft(world);
        new NetHandlerPlayServer(world.getMinecraftServer(), new NetworkManager(EnumPacketDirection.SERVERBOUND), player);
        BlockPos g = origin.add(0, 2, -4);
        BlockPos exit = origin.add(0, 6, 4);
        world.setBlockState(g, Blocks.END_GATEWAY.getDefaultState());
        TileEntityEndGateway gateway = (TileEntityEndGateway) world.getTileEntity(g);
        NBTTagCompound tag = gateway.writeToNBT(new NBTTagCompound());
        NBTTagCompound to = new NBTTagCompound();
        to.setInteger("X", exit.getX());
        to.setInteger("Y", exit.getY());
        to.setInteger("Z", exit.getZ());
        tag.setTag("ExitPortal", to);
        tag.setBoolean("ExactTeleport", true);
        tag.setLong("Age", 300L);
        gateway.readFromNBT(tag);
        player.setGameType(GameType.SURVIVAL);
        player.setPositionAndUpdate(g.getX() + 0.5D, g.getY(), g.getZ() + 0.5D);
        Changes.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
        check.ok(player.getDistanceSq(g) < 2.0D, "a player in survival is left to 1.12.2");
        player.setGameType(GameType.SPECTATOR);
        Changes.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
        check.ok(player.getDistanceSq(exit) < 2.0D, "a spectator goes through an end gateway (26.3), at " + player.getPosition());
        player.setGameType(GameType.SURVIVAL);
        world.setBlockToAir(g);
    }

    // ------------------------------------------------------------------------------------------

    private static void clear(WorldServer world, BlockPos origin, int r) {
        for (BlockPos p : BlockPos.getAllInBox(origin.add(-r, -2, -r), origin.add(r, 10, r))) {
            if (!world.isAirBlock(p)) {
                world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
            }
        }
        for (Entity e : world.getEntitiesWithinAABB(Entity.class, new net.minecraft.util.math.AxisAlignedBB(origin).grow(r + 4))) {
            if (!(e instanceof EntityPlayer)) {
                e.setDead();
            }
        }
    }
}
