package com.exiledradio.gamedropsbackport.selftest;

import com.exiledradio.backportcore.waterlog.Waterloggable;
import com.exiledradio.backportcore.waterlog.Waterlogging;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.softblocks.BlockSoftSlab;
import com.exiledradio.gamedropsbackport.softblocks.BlockSoftStairs;
import com.exiledradio.gamedropsbackport.softblocks.EntityCushion;
import com.exiledradio.gamedropsbackport.softblocks.SoftBlocks;
import com.exiledradio.gamedropsbackport.softblocks.SoftColors;
import com.exiledradio.gamedropsbackport.softblocks.StrawBedSleep;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Stream 6 (soft blocks): the block id layout, every color's metadata, name and recipes, slab joining, fuel,
 * flammability, waterlogging, shears, cushions (placement, support, riding, breaking) and straw beds (placement,
 * sleeping without a spawn point, breaking on waking and in the Nether).
 *
 * <p>Also writes {@code selftest-states.txt} and {@code selftest-items.txt} for {@code tools/check_assets.py}.
 */
final class SoftBlocksSelfTest implements SelfTest {

    @Override
    public String name() {
        return "softblocks";
    }

    @Override
    public void run(Check check) throws Exception {
        if (!check.ok(SoftBlocks.enabled() && SoftBlocks.strawBed != null, "soft blocks are registered")) {
            return;
        }
        WorldServer world = check.world;
        Rules26SelfTest.strawBed(check, world);
        Core150SelfTest.softBlocks(check, world);
        check.eq(23, SoftBlocks.blocks().size(), "block ids (8 stairs + 2 slabs + 1 double per material, straw bed)");
        check.eq(22, SoftBlocks.items().size(), "items (stairs and slabs, cushion, straw bed)");

        colors(check);
        recipes(check);
        dumpAssets(check);

        FakePlayer player = FakePlayerFactory.getMinecraft(world);
        new NetHandlerPlayServer(world.getMinecraftServer(), new NetworkManager(EnumPacketDirection.SERVERBOUND), player);
        BlockPos origin = new BlockPos(0, 200, 0);
        clear(world, origin, 12);
        try {
            shapes(check, world, player, origin);
            cushions(check, world, player, origin.add(6, 0, 0));
            strawBed(check, world, player, origin.add(0, 0, 6));
        } finally {
            clear(world, origin, 12);
        }
    }

    private static void colors(Check check) {
        for (String mat : SoftBlocks.MATERIALS) {
            for (EnumDyeColor c : EnumDyeColor.values()) {
                String n = SoftColors.modern(c) + "_" + mat;
                BlockSoftStairs stairs = SoftBlocks.stairs(mat, c);
                BlockSoftSlab.Half slab = SoftBlocks.slab(mat, c);
                check.eq("tile." + GameDropsBackport.MOD_ID + "." + n + "_stairs",
                        new ItemStack(stairs, 1, c.getMetadata()).getTranslationKey(), n + " stairs name");
                check.eq("tile." + GameDropsBackport.MOD_ID + "." + n + "_slab",
                        new ItemStack(slab, 1, c.getMetadata()).getTranslationKey(), n + " slab name");
                IBlockState s = stairs.withColor(c).withProperty(BlockHorizontal.FACING, EnumFacing.WEST)
                        .withProperty(net.minecraft.block.BlockStairs.HALF, net.minecraft.block.BlockStairs.EnumHalf.TOP);
                check.eq(s, stairs.getStateFromMeta(stairs.getMetaFromState(s)), n + " stairs metadata round trip");
                check.eq(c.getMetadata(), stairs.damageDropped(s), n + " stairs drop");
                IBlockState h = slab.withColor(c).withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP);
                check.eq(h, slab.getStateFromMeta(slab.getMetaFromState(h)), n + " slab metadata round trip");
                BlockSoftSlab.Double full = SoftBlocks.DOUBLE_SLABS.get(mat);
                IBlockState d = full.withColor(c);
                check.eq(d, full.getStateFromMeta(full.getMetaFromState(d)), n + " double slab metadata round trip");
                check.eq(Item.getItemFromBlock(slab), full.getItemDropped(d, world().rand, 0), n + " double drops its half");
                check.eq(2, full.quantityDropped(world().rand), n + " double drops two");
                check.eq(Blocks.WOOL.getStateFromMeta(0).getMaterial() == stairs.getDefaultState().getMaterial(),
                        mat.equals("wool"), n + " material");
                int burn = TileEntityFurnace.getItemBurnTime(new ItemStack(stairs, 1, c.getMetadata()));
                check.eq(mat.equals("wool") ? 100 : 0, burn, n + " stairs fuel");
                check.eq(mat.equals("wool") ? 50 : 0, TileEntityFurnace.getItemBurnTime(new ItemStack(slab, 1, c.getMetadata())),
                        n + " slab fuel");
                check.eq(Blocks.CONCRETE.getMapColor(Blocks.CONCRETE.getStateFromMeta(c.getMetadata()), null, null),
                        SoftBlocks.stairs("concrete", c).getMapColor(SoftBlocks.stairs("concrete", c).withColor(c), null, null),
                        n + " map color");
            }
            for (Block b : SoftBlocks.STAIRS.get(mat)) {
                check.ok(b instanceof Waterloggable, b.getRegistryName() + " is waterloggable");
            }
            BlockSoftSlab.Double full = SoftBlocks.DOUBLE_SLABS.get(mat);
            check.ok(!full.canHoldWater(full.getDefaultState(), world(), BlockPos.ORIGIN), mat + " double slab holds no water");
            check.ok(SoftBlocks.slab(mat, EnumDyeColor.RED).canHoldWater(SoftBlocks.slab(mat, EnumDyeColor.RED).getDefaultState(),
                    world(), BlockPos.ORIGIN), mat + " half slab holds water");
        }
        check.eq(50, TileEntityFurnace.getItemBurnTime(new ItemStack(SoftBlocks.cushion, 1, 3)), "cushion fuel");
        check.eq(16, SoftBlocks.cushion.getItemStackLimit(new ItemStack(SoftBlocks.cushion)), "cushion stack size");
        check.eq(16, new ItemStack(SoftBlocks.strawBed).getMaxStackSize(), "straw bed stack size");
        check.eq("item." + GameDropsBackport.MOD_ID + ".light_gray_cushion",
                new ItemStack(SoftBlocks.cushion, 1, EnumDyeColor.SILVER.getMetadata()).getTranslationKey(), "cushion name");
        check.eq(60, Blocks.FIRE.getFlammability(SoftBlocks.stairs("wool", EnumDyeColor.LIME)), "wool stairs flammability");
        check.eq(60, Blocks.FIRE.getFlammability(SoftBlocks.strawBed), "straw bed flammability");
        check.eq(SoftBlocks.strawBedBreakLeave, SoftBlocks.STRAW_BED.getBreakSound(), "straw bed breaks with break_leave (26.4 Pre-Release 2)");
        check.eq(0, Blocks.FIRE.getFlammability(SoftBlocks.stairs("concrete", EnumDyeColor.LIME)), "concrete does not burn");
        // 26.3's #dampens_vibrations: wool stairs and slabs (and their items), concrete not (Backport Core's addDampener).
        check.ok(com.exiledradio.backportcore.gameevent.GameEventTags.dampens(SoftBlocks.stairs("wool", EnumDyeColor.LIME).getDefaultState())
                && com.exiledradio.backportcore.gameevent.GameEventTags.dampens(SoftBlocks.slab("wool", EnumDyeColor.LIME).getDefaultState()),
                "wool stairs and slabs dampen vibrations");
        check.ok(!com.exiledradio.backportcore.gameevent.GameEventTags.dampens(SoftBlocks.stairs("concrete", EnumDyeColor.LIME).getDefaultState()),
                "concrete stairs do not");
    }

    private static WorldServer world() {
        return DimensionManager.getWorld(0);
    }

    private static ItemStack craft(Object... grid) {
        InventoryCrafting inv = new InventoryCrafting(new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer playerIn) {
                return true;
            }
        }, 3, 3);
        for (int i = 0; i < grid.length; i++) {
            if (grid[i] != null) {
                inv.setInventorySlotContents(i, (ItemStack) grid[i]);
            }
        }
        return CraftingManager.findMatchingResult(inv, world());
    }

    private static void recipes(Check check) {
        for (String mat : SoftBlocks.MATERIALS) {
            Block base = mat.equals("wool") ? Blocks.WOOL : Blocks.CONCRETE;
            for (EnumDyeColor c : new EnumDyeColor[]{EnumDyeColor.WHITE, EnumDyeColor.SILVER, EnumDyeColor.BLACK}) {
                ItemStack b = new ItemStack(base, 1, c.getMetadata());
                ItemStack stairs = craft(b, null, null, b, b, null, b, b, b);
                check.ok(stairs.getItem() == Item.getItemFromBlock(SoftBlocks.stairs(mat, c)) && stairs.getMetadata() == c.getMetadata()
                        && stairs.getCount() == 4, mat + " " + c + " stairs recipe: " + stairs);
                ItemStack slab = craft(b, b, b);
                check.ok(slab.getItem() == Item.getItemFromBlock(SoftBlocks.slab(mat, c)) && slab.getMetadata() == c.getMetadata()
                        && slab.getCount() == 6, mat + " " + c + " slab recipe: " + slab);
            }
        }
        ItemStack slab = new ItemStack(SoftBlocks.slab("wool", EnumDyeColor.PINK), 1, EnumDyeColor.PINK.getMetadata());
        ItemStack cushion = craft(slab, slab, slab);
        check.ok(cushion.getItem() == SoftBlocks.cushion && cushion.getMetadata() == EnumDyeColor.PINK.getMetadata()
                && cushion.getCount() == 1, "cushion recipe: " + cushion);
        ItemStack redDye = new ItemStack(Items.DYE, 1, EnumDyeColor.RED.getDyeDamage());
        ItemStack dyed = craft(redDye, new ItemStack(SoftBlocks.cushion, 1, EnumDyeColor.PINK.getMetadata()));
        check.ok(dyed.getItem() == SoftBlocks.cushion && dyed.getMetadata() == EnumDyeColor.RED.getMetadata(), "cushion dye: " + dyed);
        ItemStack stairs = new ItemStack(SoftBlocks.stairs("wool", EnumDyeColor.BLUE), 1, EnumDyeColor.BLUE.getMetadata());
        ItemStack dyedStairs = craft(stairs, redDye);
        check.ok(dyedStairs.getItem() == Item.getItemFromBlock(SoftBlocks.stairs("wool", EnumDyeColor.RED))
                && dyedStairs.getMetadata() == EnumDyeColor.RED.getMetadata(), "wool stairs dye: " + dyedStairs);
        check.ok(craft(new ItemStack(SoftBlocks.stairs("wool", EnumDyeColor.RED), 1, EnumDyeColor.RED.getMetadata()), redDye).isEmpty(),
                "no dyeing to the same color");
        ItemStack hay = new ItemStack(Blocks.HAY_BLOCK);
        ItemStack beds = craft(hay, hay, hay);
        check.ok(beds.getItem() == Item.getItemFromBlock(SoftBlocks.strawBed) && beds.getCount() == 4, "straw bed recipe: " + beds);
    }

    private static void clear(WorldServer world, BlockPos origin, int r) {
        for (BlockPos p : BlockPos.getAllInBox(origin.add(-2, -2, -2), origin.add(r, 4, r))) {
            world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
        }
        for (Entity e : world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(origin.add(-4, -4, -4), origin.add(r + 4, 6, r + 4)))) {
            if (!(e instanceof EntityPlayer)) {
                e.setDead();
            }
        }
    }

    private static void use(FakePlayer player, ItemStack stack, BlockPos pos, EnumFacing face, float hitY) {
        player.setHeldItem(EnumHand.MAIN_HAND, stack);
        stack.getItem().onItemUse(player, player.world, pos, EnumHand.MAIN_HAND, face, 0.5F, hitY, 0.5F);
    }

    private static void shapes(Check check, WorldServer world, FakePlayer player, BlockPos o) {
        world.setBlockState(o, Blocks.STONE.getDefaultState());
        player.setPositionAndRotation(o.getX() + 0.5, o.getY() + 1, o.getZ() - 2.5, 0, 0);
        BlockSoftSlab.Half half = SoftBlocks.slab("wool", EnumDyeColor.CYAN);
        use(player, new ItemStack(half, 4, EnumDyeColor.CYAN.getMetadata()), o, EnumFacing.UP, 1.0F);
        IBlockState placed = world.getBlockState(o.up());
        check.ok(placed.getBlock() == half && half.colorOf(placed) == EnumDyeColor.CYAN
                && placed.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.BOTTOM, "cyan wool slab placed: " + placed);
        use(player, new ItemStack(half, 4, EnumDyeColor.CYAN.getMetadata()), o.up(), EnumFacing.UP, 0.5F);
        IBlockState joined = world.getBlockState(o.up());
        BlockSoftSlab.Double full = SoftBlocks.DOUBLE_SLABS.get("wool");
        check.ok(joined.getBlock() == full && full.colorOf(joined) == EnumDyeColor.CYAN, "two cyan halves join: " + joined);
        NonNullList<ItemStack> drops = NonNullList.create();
        full.getDrops(drops, world, o.up(), joined, 0);
        int halves = 0;
        for (ItemStack d : drops) {
            if (d.getItem() == Item.getItemFromBlock(half) && d.getMetadata() == EnumDyeColor.CYAN.getMetadata()) {
                halves += d.getCount();
            }
        }
        check.eq(2, halves, "double slab drops two cyan halves: " + drops);

        BlockPos s = o.add(2, 0, 0);
        world.setBlockState(s, Blocks.STONE.getDefaultState());
        BlockSoftStairs stairs = SoftBlocks.stairs("concrete", EnumDyeColor.LIGHT_BLUE);
        use(player, new ItemStack(stairs, 1, EnumDyeColor.LIGHT_BLUE.getMetadata()), s, EnumFacing.UP, 1.0F);
        IBlockState st = world.getBlockState(s.up());
        check.ok(st.getBlock() == stairs && stairs.colorOf(st) == EnumDyeColor.LIGHT_BLUE, "light blue concrete stairs placed: " + st);
        check.ok(world.getBlockState(s.up()).getActualState(world, s.up()).getPropertyKeys().size() == 4, "stairs keep shape");

        if (Waterlogging.isAvailable()) {
            check.ok(Waterlogging.setWater(world, s.up(), true, 3) && Waterlogging.hasWater(world, s.up()), "concrete stairs hold water");
            Waterlogging.setWater(world, s.up(), false, 3);
        }

        IBlockState wool = SoftBlocks.stairs("wool", EnumDyeColor.RED).withColor(EnumDyeColor.RED);
        PlayerEvent.BreakSpeed shears = new PlayerEvent.BreakSpeed(player, wool, 1.0F, s);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        StrawBedSleep.onBreakSpeed(shears);
        check.eq(5.0F, shears.getNewSpeed(), "shears cut wool stairs at 5");
        player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
    }

    private static List<EntityCushion> cushionsAt(WorldServer world, BlockPos pos) {
        List<EntityCushion> out = new ArrayList<EntityCushion>();
        for (EntityCushion c : world.getEntitiesWithinAABB(EntityCushion.class, new AxisAlignedBB(pos).grow(0.5))) {
            if (!c.isDead) {
                out.add(c);
            }
        }
        return out;
    }

    private static void cushions(Check check, WorldServer world, FakePlayer player, BlockPos o) {
        world.setBlockState(o, Blocks.STONE.getDefaultState());
        player.setPositionAndRotation(o.getX() + 0.5, o.getY() + 1, o.getZ() - 2.5, 0, 0);
        ItemStack stack = new ItemStack(SoftBlocks.cushion, 2, EnumDyeColor.ORANGE.getMetadata());
        use(player, stack, o, EnumFacing.NORTH, 1.0F);
        check.ok(cushionsAt(world, o.up()).isEmpty(), "a cushion is placed only on a top face");
        use(player, stack, o, EnumFacing.UP, 1.0F);
        List<EntityCushion> placed = cushionsAt(world, o.up());
        if (!check.eq(1, placed.size(), "cushion placed")) {
            return;
        }
        EntityCushion cushion = placed.get(0);
        check.eq(EnumDyeColor.ORANGE, cushion.getColor(), "cushion color");
        check.ok(Math.abs(cushion.posY - (o.getY() + 1)) < 1.0E-6, "cushion rests on the block: " + cushion.posY);
        check.eq(1, stack.getCount(), "placing uses one");
        use(player, stack, o, EnumFacing.UP, 1.0F);
        check.eq(1, cushionsAt(world, o.up()).size(), "one cushion per space");
        check.ok(cushion.getCollisionBoundingBox() == null, "a cushion does not collide");

        player.setPosition(cushion.posX, cushion.posY + 0.3, cushion.posZ);
        check.ok(cushion.processInitialInteract(player, EnumHand.MAIN_HAND) && player.getRidingEntity() == cushion, "sit on it");
        cushion.updatePassenger(player);
        check.ok(Math.abs(player.posY - (cushion.posY - 0.35)) < 1.0E-6, "seated at 26.3's height: " + (player.posY - cushion.posY));

        // Seat to seat: no stop at 1.12.2's dismount spot beside the first cushion on the way.
        BlockPos o2 = o.east(2);
        world.setBlockState(o2, Blocks.STONE.getDefaultState());
        use(player, new ItemStack(SoftBlocks.cushion, 1, EnumDyeColor.LIME.getMetadata()), o2, EnumFacing.UP, 1.0F);
        List<EntityCushion> second = cushionsAt(world, o2.up());
        if (check.eq(1, second.size(), "second cushion placed")) {
            EntityCushion next = second.get(0);
            double x = player.posX, y = player.posY, z = player.posZ;
            check.ok(next.processInitialInteract(player, EnumHand.MAIN_HAND) && player.getRidingEntity() == next
                    && next.getPassengers().contains(player) && !cushion.isBeingRidden(), "sitting on another cushion moves the rider there");
            check.ok(player.posX == x && player.posY == y && player.posZ == z,
                    "switching seats does not stand the rider up beside the old one first: " + player.getPositionVector());
            next.updatePassenger(player);
            check.ok(Math.abs(player.posX - next.posX) < 1.0E-6 && Math.abs(player.posY - (next.posY - 0.35)) < 1.0E-6,
                    "seated on the second cushion");
            player.dismountRidingEntity();
            next.setDead();
            world.setBlockToAir(o2);
        }
        player.dismountRidingEntity();
        player.setSneaking(true);
        check.ok(!cushion.processInitialInteract(player, EnumHand.MAIN_HAND), "sneaking does not sit");
        player.setSneaking(false);

        world.setBlockToAir(o);
        for (int i = 0; i <= 101 && !cushion.isDead; i++) {
            cushion.onUpdate();
        }
        check.ok(cushion.isDead, "breaks with its support");
        List<EntityItem> items = world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(o.up()).grow(1));
        check.ok(items.size() == 1 && items.get(0).getItem().getItem() == SoftBlocks.cushion
                && items.get(0).getItem().getMetadata() == EnumDyeColor.ORANGE.getMetadata(), "drops its item: " + items);
        for (EntityItem e : items) {
            e.setDead();
        }

        world.setBlockState(o, SoftBlocks.slab("wool", EnumDyeColor.BLUE).withColor(EnumDyeColor.BLUE));
        use(player, new ItemStack(SoftBlocks.cushion, 1, EnumDyeColor.BLUE.getMetadata()), o, EnumFacing.UP, 0.5F);
        placed = cushionsAt(world, o.up());
        check.ok(placed.size() == 1 && Math.abs(placed.get(0).posY - (o.getY() + 0.5)) < 1.0E-6, "a cushion sits on a slab top");
        if (!placed.isEmpty()) {
            check.ok(placed.get(0).attackEntityFrom(DamageSource.GENERIC, 1.0F) && placed.get(0).isDead, "any hit breaks it");
        }
    }

    private static void strawBed(Check check, WorldServer world, FakePlayer player, BlockPos o) {
        Item item = Item.getItemFromBlock(SoftBlocks.strawBed);
        world.setBlockState(o, Blocks.STONE.getDefaultState());
        player.setPositionAndRotation(o.getX() + 0.5, o.getY() + 1, o.getZ() - 1.5, 0, 0); // facing south
        use(player, new ItemStack(item), o, EnumFacing.UP, 1.0F);
        BlockPos foot = o.up();
        BlockPos head = foot.south();
        IBlockState f = world.getBlockState(foot);
        IBlockState h = world.getBlockState(head);
        if (!check.ok(f.getBlock() == SoftBlocks.strawBed && h.getBlock() == SoftBlocks.strawBed
                && f.getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT && h.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD,
                "straw bed placed: " + f + " / " + h)) {
            return;
        }
        check.ok(f.getBoundingBox(world, foot).maxY == 0.25 && h.getBoundingBox(world, head).maxY == 0.3125, "4 and 5 pixels high");

        long time = world.getWorldTime();
        world.setWorldTime(6000);
        player.setPosition(foot.getX() + 0.5, foot.getY(), foot.getZ() - 1.5);
        SoftBlocks.strawBed.onBlockActivated(world, foot, f, player, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 0.2F, 0.5F);
        check.ok(!player.isPlayerSleeping() && world.getBlockState(head).getBlock() == SoftBlocks.strawBed, "no sleeping by day");

        world.setWorldTime(18000);
        int light = world.getSkylightSubtracted();
        world.setSkylightSubtracted(world.calculateSkylightSubtracted(1.0F));
        BlockPos spawnBefore = player.getBedLocation(0);
        SoftBlocks.strawBed.onBlockActivated(world, foot, f, player, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 0.2F, 0.5F);
        if (check.ok(player.isPlayerSleeping(), "sleeps at night")) {
            check.ok(world.getBlockState(head).getValue(BlockBed.OCCUPIED), "occupied");
            StrawBedSleep.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
            check.ok(Math.abs(player.posY - (head.getY() + 0.375)) < 1.0E-6, "lies on the straw: " + (player.posY - head.getY()));
            player.wakeUpPlayer(false, true, true);
            check.eq(spawnBefore, player.getBedLocation(0), "the spawn point is not set");
            check.ok(world.getBlockState(head).getBlock() == SoftBlocks.strawBed, "the bed lasts until the tick ends");
            StrawBedSleep.onWorldTick(new TickEvent.WorldTickEvent(Side.SERVER, TickEvent.Phase.END, world));
            check.ok(world.isAirBlock(head) && world.isAirBlock(foot), "getting up breaks it");
            check.ok(world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(foot).grow(2)).isEmpty(), "and drops nothing");
        }
        world.setWorldTime(time);
        world.setSkylightSubtracted(light);

        // Broken by hand: one bed back, from either half.
        use(player, new ItemStack(item), o, EnumFacing.UP, 1.0F);
        world.destroyBlock(foot, true);
        check.ok(world.isAirBlock(head), "breaking the foot takes the head");
        List<EntityItem> drops = world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(foot).grow(2));
        int count = 0;
        for (EntityItem e : drops) {
            if (e.getItem().getItem() == item) {
                count += e.getItem().getCount();
            }
            e.setDead();
        }
        check.eq(1, count, "a broken straw bed drops one");

        WorldServer nether = DimensionManager.getWorld(-1);
        if (nether != null) {
            BlockPos n = new BlockPos(0, 100, 0);
            nether.setBlockState(n, Blocks.NETHERRACK.getDefaultState());
            IBlockState nf = SoftBlocks.strawBed.getDefaultState().withProperty(BlockHorizontal.FACING, EnumFacing.SOUTH);
            nether.setBlockState(n.up(), nf, 10);
            nether.setBlockState(n.up().south(), nf.withProperty(BlockBed.PART, BlockBed.EnumPartType.HEAD), 10);
            FakePlayer np = FakePlayerFactory.getMinecraft(nether);
            SoftBlocks.strawBed.onBlockActivated(nether, n.up(), nf, np, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 0.2F, 0.5F);
            check.ok(nether.isAirBlock(n.up()) && nether.isAirBlock(n.up().south()), "in the Nether using it breaks it");
            check.ok(nether.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(n).grow(3)).isEmpty(), "without exploding or dropping");
            nether.setBlockToAir(n);
        }
    }

    /** States and items for tools/check_assets.py, as the client's default state mapper names them. */
    private static void dumpAssets(Check check) throws Exception {
        File dir = FMLCommonHandler.instance().getMinecraftServerInstance().getDataDirectory();
        PrintWriter states = SelfTestRunner.statesOut();
        for (Block block : SoftBlocks.blocks()) {
            for (IBlockState state : block.getBlockState().getValidStates()) {
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<IProperty<?>, Comparable<?>> e : state.getProperties().entrySet()) {
                    if (block == SoftBlocks.strawBed && e.getKey() == BlockBed.OCCUPIED) {
                        continue;
                    }
                    if (sb.length() > 0) {
                        sb.append(',');
                    }
                    sb.append(e.getKey().getName()).append('=').append(name(e.getKey(), e.getValue()));
                }
                states.println(block.getRegistryName() + "\t" + (sb.length() == 0 ? "normal" : sb) + "\tvariants");
            }
        }
        states.close();
        PrintWriter items = SelfTestRunner.itemsOut();
        List<String> seen = new ArrayList<String>();
        for (Item item : SoftBlocks.items()) {
            NonNullList<ItemStack> subs = NonNullList.create();
            item.getSubItems(net.minecraft.creativetab.CreativeTabs.SEARCH, subs);
            for (ItemStack stack : subs) {
                String model;
                if (item == SoftBlocks.cushion) {
                    model = SoftColors.modern(EnumDyeColor.byMetadata(stack.getMetadata())) + "_cushion";
                } else if (item == Item.getItemFromBlock(SoftBlocks.strawBed)) {
                    model = "straw_bed";
                } else {
                    model = stack.getTranslationKey().substring(("tile." + GameDropsBackport.MOD_ID + ".").length());
                }
                items.println(item.getRegistryName() + "\t" + stack.getMetadata() + "\t" + model + "\t" + stack.getTranslationKey() + ".name");
                seen.add(model);
            }
        }
        items.close();
        check.eq(16 * 4 + 16 + 1, seen.size(), "item models (64 shapes, 16 cushions, straw bed)");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String name(IProperty p, Comparable v) {
        return p.getName(v);
    }
}
