package com.exiledradio.gamedropsbackport.changes;

import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.ModConfig;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.state.IBlockState;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityShulker;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityEndGateway;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraftforge.registries.IForgeRegistryModifiable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The "changes" stream: 26.1 to 26.3's changes to things 1.12.2 already has, one setting each
 * ({@code docs/streams/changes.md}). This class holds the ones done with events; {@link ChangeHooks} the ones the
 * patches call, {@link TimeCommand} and {@link GameRuleCommand} the commands.
 */
@Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID)
public final class Changes {

    private Changes() {
    }

    /** @return the change's own setting, while the stream itself is on */
    public static boolean on(boolean setting) {
        return ModConfig.changes.enabled && setting;
    }

    /** Registers the two commands (both fall back to 1.12.2's own while their setting is off). */
    public static void serverStarting(net.minecraftforge.fml.common.event.FMLServerStartingEvent event) {
        event.registerServerCommand(new TimeCommand());
        event.registerServerCommand(new GameRuleCommand());
    }

    public static void postInit() {
        wrapSpawnEggDispensing();
    }

    // ------------------------------------------------------------------------------------------
    // Mushroom stew (26.3)
    // ------------------------------------------------------------------------------------------

    /** 26.3's {@code #mushrooms}: brown, red and (with the poplar stream) the shelf mushroom. */
    static Ingredient mushrooms() {
        NonNullList<ItemStack> stacks = NonNullList.create();
        stacks.add(new ItemStack(Blocks.BROWN_MUSHROOM));
        stacks.add(new ItemStack(Blocks.RED_MUSHROOM));
        Item shelf = ForgeRegistries.ITEMS.getValue(new ResourceLocation(GameDropsBackport.MOD_ID, "shelf_mushroom"));
        if (shelf != null && shelf != Items.AIR) {
            stacks.add(new ItemStack(shelf));
        }
        return Ingredient.fromStacks(stacks.toArray(new ItemStack[0]));
    }

    /**
     * 26.3's {@code mushroom_stew}: shapeless, two of {@code #mushrooms} and a bowl. Replaces 1.12.2's
     * {@code minecraft:mushroom_stew} (one brown, one red), as Tricky Trials replaces the lead recipe. Low priority, so
     * the poplar stream's shelf mushroom item is registered first. Ours keeps the id {@code minecraft:mushroom_stew}, so
     * vanilla's recipe-book advancement for it still loads and unlocks it.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!on(ModConfig.changes.mushroomStew)) {
            return;
        }
        if (event.getRegistry() instanceof IForgeRegistryModifiable) {
            ((IForgeRegistryModifiable<IRecipe>) event.getRegistry()).remove(new ResourceLocation("mushroom_stew"));
        }
        ResourceLocation id = new ResourceLocation("mushroom_stew");
        Ingredient mushroom = mushrooms();
        event.getRegistry().register(new ShapelessOreRecipe(new ResourceLocation("mushroom_stew"),
                new ItemStack(Items.MUSHROOM_STEW), mushroom, mushroom, Items.BOWL).setRegistryName(id));
    }

    // ------------------------------------------------------------------------------------------
    // Team colors (26.2)
    // ------------------------------------------------------------------------------------------

    /** @return the names 26.2 accepts for a team color: each color's own (dark_purple), and reset */
    static Set<String> teamColorNames() {
        Set<String> names = new HashSet<String>();
        for (TextFormatting f : TextFormatting.values()) {
            if (f.isColor() || f == TextFormatting.RESET) {
                names.add(f.getFriendlyName());
            }
        }
        return names;
    }

    /**
     * {@code /scoreboard teams option <team> color <value>}: 1.12.2 reads the value loosely ({@code darkpurple},
     * {@code DarkPurple}); 26.2's {@code /team modify} takes only {@code dark_purple}. Other values get 1.12.2's own
     * "valid values" error.
     */
    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        String[] args = event.getParameters();
        if (!"scoreboard".equals(event.getCommand().getName()) || args.length < 5 || !"teams".equalsIgnoreCase(args[0])
                || !"option".equalsIgnoreCase(args[1]) || !"color".equalsIgnoreCase(args[3])
                || !on(ModConfig.changes.teamColorNames)) {
            return;
        }
        Set<String> names = teamColorNames();
        if (!names.contains(args[4])) {
            event.setCanceled(true);
            TextComponentTranslation error = new TextComponentTranslation("commands.scoreboard.teams.option.noValue",
                    "color", net.minecraft.command.CommandBase.joinNiceStringFromCollection(
                    TextFormatting.getValidValues(true, false)));
            error.getStyle().setColor(TextFormatting.RED);
            event.getSender().sendMessage(error);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Shield before hoes and shovels (26.3)
    // ------------------------------------------------------------------------------------------

    /**
     * 26.3's {@code BlockTransformer.playerHasBlockingItemUseIntent}: main hand, a shield in the off hand, not sneaking.
     * The hoe or shovel is not used on the block (the block itself still is, as a chest opens), so the use goes on to
     * the off hand and raises the shield. Axes are Tricky Trials' (1.21's same rule).
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != EnumHand.MAIN_HAND || !on(ModConfig.changes.shieldBeforeTools)) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        if (shieldFirst(player.getHeldItemMainhand(), player.getHeldItemOffhand(), player.isSneaking(), player)) {
            event.setUseItem(Event.Result.DENY);
        }
    }

    public static boolean shieldFirst(ItemStack main, ItemStack off, boolean sneaking, EntityPlayer player) {
        if (main.isEmpty() || sneaking || off.isEmpty() || !off.getItem().isShield(off, player)) {
            return false;
        }
        Item item = main.getItem();
        Set<String> tools = item.getToolClasses(main);
        return item instanceof ItemHoe || item instanceof ItemSpade || tools.contains("shovel") || tools.contains("hoe");
    }

    // ------------------------------------------------------------------------------------------
    // Enderman and shulker teleports (26.3)
    // ------------------------------------------------------------------------------------------

    /** 26.3's {@code #dangerous_for_teleportation}, by registry path (1.12.2's and the family's blocks). */
    static final Set<String> DANGEROUS = new HashSet<String>(Arrays.asList(
            "fire", "soul_fire", "lava_cauldron", "campfire", "soul_campfire", "cactus", "magma", "magma_block",
            "sweet_berry_bush", "wither_rose", "pointed_dripstone", "powder_snow"));

    static boolean dangerous(IBlockState state) {
        ResourceLocation id = state.getBlock().getRegistryName();
        return id != null && DANGEROUS.contains(id.getPath());
    }

    /**
     * Endermen: none while riding (26.3's {@code teleport} returns false for a passenger), and not onto
     * {@code #enderman_does_not_teleport_to} (bedrock and the dangerous blocks). 1.12.2's {@code attemptTeleport}
     * lands on the first block below the target that blocks movement; the same search is made here, and the
     * enderman's box there must not touch a dangerous block. A refused try is just a failed one: 1.12.2 tries again,
     * as 26.3 does. Shulkers: not onto bedrock ({@code #shulker_does_not_teleport_to}), the block they would attach to.
     */
    @SubscribeEvent
    public static void onEnderTeleport(EnderTeleportEvent event) {
        Entity entity = event.getEntity();
        if (entity.world.isRemote) {
            return;
        }
        if (entity instanceof EntityEnderman) {
            if (on(ModConfig.changes.endermenRiding) && entity.isRiding()) {
                event.setCanceled(true);
            } else if (on(ModConfig.changes.teleportTargets)
                    && !endermanMayLand(entity, event.getTargetX(), event.getTargetY(), event.getTargetZ())) {
                event.setCanceled(true);
            }
        } else if (entity instanceof EntityShulker && on(ModConfig.changes.teleportTargets)) {
            BlockPos attach = new BlockPos(event.getTargetX(), event.getTargetY(), event.getTargetZ())
                    .offset(((EntityShulker) entity).getAttachmentFacing());
            if (entity.world.getBlockState(attach).getBlock() == Blocks.BEDROCK) {
                event.setCanceled(true);
            }
        }
    }

    public static boolean endermanMayLand(Entity entity, double x, double y, double z) {
        World world = entity.world;
        BlockPos pos = new BlockPos(x, y, z);
        if (!world.isBlockLoaded(pos)) {
            return true;
        }
        while (pos.getY() > 0) {
            BlockPos below = pos.down();
            IBlockState state = world.getBlockState(below);
            if (state.getMaterial().blocksMovement()) {
                if (state.getBlock() == Blocks.BEDROCK || dangerous(state)) {
                    return false;
                }
                break;
            }
            y--;
            pos = below;
        }
        double w = entity.width / 2.0D;
        AxisAlignedBB box = new AxisAlignedBB(x - w, y, z - w, x + w, y + entity.height, z + w);
        for (BlockPos p : BlockPos.getAllInBoxMutable(new BlockPos(box.minX, box.minY, box.minZ),
                new BlockPos(box.maxX, box.maxY, box.maxZ))) {
            if (dangerous(world.getBlockState(p))) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------
    // Leader zombies (26.1)
    // ------------------------------------------------------------------------------------------

    static final String LEADER_CHECKED = GameDropsBackport.MOD_ID + ":LeaderHealth";
    /** 1.12.2's modifier name ({@code EntityZombie.onInitialSpawn}; its UUID is random). */
    static final String LEADER_BONUS = "Leader zombie bonus";

    /**
     * 26.1's {@code Zombie.finalizeSpawn} sets a leader's health to its raised maximum; 1.12.2 raises the maximum and
     * leaves the health at 20. Checked once per zombie, when it first joins a world after its spawn set it up.
     */
    @SubscribeEvent
    public static void onJoin(EntityJoinWorldEvent event) {
        if (!(event.getEntity() instanceof EntityZombie) || event.getWorld().isRemote
                || !on(ModConfig.changes.leaderZombieHealth)) {
            return;
        }
        EntityZombie zombie = (EntityZombie) event.getEntity();
        NBTTagCompound data = zombie.getEntityData();
        if (data.getBoolean(LEADER_CHECKED)) {
            return;
        }
        data.setBoolean(LEADER_CHECKED, true);
        if (isLeader(zombie) && zombie.getHealth() == 20.0F && zombie.getMaxHealth() > 20.0F) {
            zombie.setHealth(zombie.getMaxHealth());
        }
    }

    static boolean isLeader(EntityZombie zombie) {
        IAttributeInstance health = zombie.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        for (AttributeModifier m : health.getModifiers()) {
            if (LEADER_BONUS.equals(m.getName())) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------------------------
    // Spawn eggs in dispensers on Peaceful (26.2)
    // ------------------------------------------------------------------------------------------

    /**
     * 26.2's {@code SpawnEggItemBehavior}: when the mob cannot spawn ({@code EntityType.canSpawn}, false for a monster
     * on Peaceful) the egg is given back unused. 1.12.2 spawns the monster, which vanishes the same tick, and uses the
     * egg up. Wraps whatever behavior the spawn egg has (vanilla's or another mod's).
     */
    static void wrapSpawnEggDispensing() {
        final IBehaviorDispenseItem inner = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(Items.SPAWN_EGG);
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(Items.SPAWN_EGG, new IBehaviorDispenseItem() {
            @Override
            public ItemStack dispense(IBlockSource source, ItemStack stack) {
                if (on(ModConfig.changes.peacefulSpawnEggs) && blockedOnPeaceful(source.getWorld(), stack)) {
                    // 26.2 returns the stack unchanged; DefaultDispenseItemBehavior still clicks.
                    source.getWorld().playEvent(1000, source.getBlockPos(), 0);
                    return stack;
                }
                return inner.dispense(source, stack);
            }
        });
    }

    public static boolean blockedOnPeaceful(World world, ItemStack egg) {
        if (world.getDifficulty() != EnumDifficulty.PEACEFUL) {
            return false;
        }
        ResourceLocation id = ItemMonsterPlacer.getNamedIdFrom(egg);
        Class<? extends Entity> type = id == null ? null : EntityList.getClass(id);
        return type != null && IMob.class.isAssignableFrom(type);
    }

    // ------------------------------------------------------------------------------------------
    // Spectators and portals (26.3)
    // ------------------------------------------------------------------------------------------

    /** Players whose last spectator portal trip was this many ticks ago or less wait (1.12.2's player cooldown, 10). */
    private static final Map<EntityPlayerMP, Long> LAST_TRIP = new WeakHashMap<EntityPlayerMP, Long>();

    /**
     * 26.3 lets spectators use nether portals, end portals and end gateways. 1.12.2's spectators never collide with
     * blocks ({@code noClip}), so their portal blocks are found here, each tick: a nether portal sends them across
     * at once (1.12.2's portal time for a player who takes no damage is 1 tick) to an existing portal (26.3 builds none
     * for a spectator); the end portal runs its own collision; a gateway its own teleport.
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof EntityPlayerMP)
                || !event.player.isSpectator() || !on(ModConfig.changes.spectatorPortals)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        World world = player.world;
        Long last = LAST_TRIP.get(player);
        if (last != null && world.getTotalWorldTime() - last >= 0 && world.getTotalWorldTime() - last <= 10) {
            return;
        }
        AxisAlignedBB box = player.getEntityBoundingBox().shrink(0.001D);
        for (BlockPos.MutableBlockPos p : BlockPos.getAllInBoxMutable(new BlockPos(box.minX, box.minY, box.minZ),
                new BlockPos(box.maxX, box.maxY, box.maxZ))) {
            IBlockState state = world.getBlockState(p);
            Block block = state.getBlock();
            if (block == Blocks.PORTAL) {
                LAST_TRIP.put(player, world.getTotalWorldTime());
                netherTrip(player);
                return;
            }
            if (block == Blocks.END_PORTAL) {
                LAST_TRIP.put(player, world.getTotalWorldTime());
                block.onEntityCollision(world, p.toImmutable(), state, player);
                return;
            }
            if (block == Blocks.END_GATEWAY) {
                TileEntity te = world.getTileEntity(p);
                if (te instanceof TileEntityEndGateway && !((TileEntityEndGateway) te).isCoolingDown()) {
                    LAST_TRIP.put(player, world.getTotalWorldTime());
                    ((TileEntityEndGateway) te).teleportEntity(player);
                }
                return;
            }
        }
    }

    static void netherTrip(EntityPlayerMP player) {
        if (!player.server.getAllowNether() || player.isRiding()) {
            return;
        }
        int to = player.world.provider.getDimensionType().getId() == -1 ? 0 : -1;
        player.changeDimension(to, new ITeleporter() {
            @Override
            public void placeEntity(World world, Entity entity, float yaw) {
                // Only an existing portal: no frame is built for a spectator. Without one, the scaled position.
                ((WorldServer) world).getDefaultTeleporter().placeInExistingPortal(entity, yaw);
            }
        });
    }
}
