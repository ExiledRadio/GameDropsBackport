package com.exiledradio.gamedropsbackport.selftest;

import com.exiledradio.backportcore.waterlog.Waterloggable;
import com.exiledradio.backportcore.waterlog.Waterlogging;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.poplar.BlockPoplarLeaves;
import com.exiledradio.gamedropsbackport.poplar.PoplarContent;
import com.exiledradio.gamedropsbackport.poplar.PoplarFeatures;
import com.mojang.authlib.GameProfile;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Waterlogging (26.3's rules over Backport Core and Fluidlogged API): which of our blocks and states hold water, placing
 * into water sources, flowing water and lava, buckets in and out, waterlogged blocks broken, and generation (poplar
 * leaves grown into water, sulfur spikes placed in water).
 *
 * <p>Without Fluidlogged API ({@code -PnoFluidlogged}) the same blocks are still {@link Waterloggable} but nothing holds
 * water: every check is then that they stay dry and act as 1.12.2's. Fluidlogged API's own types are only reached
 * through {@link FluidloggedChecks}, which is loaded only when it is installed.
 */
public final class WaterlogSelfTest implements SelfTest {

    private int nextArea;

    @Override
    public String name() {
        return "waterlog";
    }

    /** @return whether 26.3 makes this block of ours waterloggable (it has {@code waterlogged}; any state but a double slab) */
    static boolean waterloggable(String path) {
        if (path.endsWith("_double_slab")) {
            return false;
        }
        return path.endsWith("_slab") || path.endsWith("_stairs") || path.endsWith("_wall")
                || path.equals("poplar_fence") || path.equals("poplar_trapdoor")
                || path.equals("poplar_sign") || path.equals("poplar_wall_sign")
                || path.endsWith("_poplar_leaves") || path.equals("sulfur_spike");
    }

    @Override
    public void run(Check c) throws Exception {
        WorldServer world = c.world;
        boolean wet = Waterlogging.isAvailable();
        c.eq(Loader.isModLoaded(Waterlogging.FLUIDLOGGED_API), wet, "waterlogging is available exactly when Fluidlogged API is loaded");
        GameDropsBackport.LOGGER.info("SELFTEST waterlog: Fluidlogged API {}", wet ? "installed" : "absent: everything must stay dry");

        // 1. Which blocks and states hold water ---------------------------------------------------------------------
        BlockPos probe = area(c).add(16, 1, 16);
        int blocks = 0;
        int states = 0;
        int holding = 0;
        List<String> wrong = new ArrayList<String>();
        for (Block block : ForgeRegistries.BLOCKS) {
            ResourceLocation name = block.getRegistryName();
            if (name == null || !GameDropsBackport.MOD_ID.equals(name.getNamespace())) {
                continue;
            }
            blocks++;
            boolean expected = waterloggable(name.getPath());
            String path = name.getPath();
            boolean waterloggableClass = block instanceof Waterloggable;
            // A double slab shares its half's Waterloggable class and answers canHoldWater false.
            if (!path.endsWith("_double_slab") && waterloggableClass != expected) {
                wrong.add(name + (waterloggableClass ? " is Waterloggable, 26.3's is not" : " is not Waterloggable, 26.3's is"));
            }
            for (IBlockState state : block.getBlockState().getValidStates()) {
                states++;
                boolean can = Waterlogging.canHoldWater(state, world, probe);
                if (can) {
                    holding++;
                }
                if (can != (expected && wet)) {
                    wrong.add(state + " holds water: " + can);
                }
                if (wet) {
                    FluidloggedChecks.refused(state, world, probe, wrong);
                }
            }
        }
        c.ok(wrong.isEmpty(), blocks + " blocks, " + states + " states of ours checked against 26.3 (" + holding
                + " hold water), wrong: " + (wrong.size() > 20 ? wrong.subList(0, 20) + " and " + (wrong.size() - 20) + " more" : wrong));
        if (wet) {
            c.ok(holding > 500, "a fair number of states hold water: " + holding);
        } else {
            c.eq(0, holding, "without Fluidlogged API no state holds water");
        }
        GameDropsBackport.LOGGER.info("SELFTEST waterlog: {} blocks, {} states of ours, {} hold a water source{}", blocks, states,
                holding, wet ? ", none flowing water or lava" : "");

        if (PoplarContent.set == null || block("sulfur_stairs") == null || block("white_wool_slab") == null) {
            c.fail("the poplar, sulfur and soft block streams must be on for this test");
            return;
        }
        Block stairs = PoplarContent.set.stairs;
        Block slab = PoplarContent.set.slab;
        Block doubleSlab = PoplarContent.set.doubleSlab;
        Block fence = PoplarContent.set.fence;
        Block trapdoor = PoplarContent.set.trapdoor;
        Block sign = PoplarContent.set.sign;
        Block wallSign = PoplarContent.set.wallSign;
        Block gate = PoplarContent.set.fenceGate;
        Block door = PoplarContent.set.door;
        Block button = PoplarContent.set.button;
        Block leaves = PoplarContent.redLeaves;
        Block wall = block("cinnabar_brick_wall");
        Block sulfurSlab = block("polished_sulfur_slab");
        Block woolStairs = block("white_wool_stairs");
        Block spike = block("sulfur_spike");
        Block potent = block("potent_sulfur");
        Block shelf = PoplarContent.shelfMushroom;
        Block strawBed = block("straw_bed");
        if (!c.ok(wall != null && sulfurSlab != null && woolStairs != null && spike != null && potent != null && shelf != null
                && strawBed != null, "every block this test uses exists")) {
            return;
        }

        // 2. Placed into a water source: kept. Into flowing water: replaced. ---------------------------------------
        BlockPos pool = pool(c);
        Block[] keeps = {stairs, slab, fence, trapdoor, sign, wallSign, leaves, wall, sulfurSlab, woolStairs, spike};
        for (int i = 0; i < keeps.length; i++) {
            BlockPos p = pool.add(1 + i, 1, 1);
            IBlockState state = keeps[i] == slab ? slab.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP)
                    : keeps[i].getDefaultState();
            world.setBlockState(p, state, 2);
            c.ok(world.getBlockState(p).getBlock() == keeps[i] && Waterlogging.hasWaterSource(world, p) == wet,
                    keeps[i].getRegistryName() + (wet ? " placed into a water source keeps it" : " placed into water replaces it"));
        }
        Block[] dries = {doubleSlab, gate, door, button, potent, shelf, strawBed, PoplarContent.set.planks, PoplarContent.sapling};
        for (int i = 0; i < dries.length; i++) {
            BlockPos p = pool.add(1 + i, 1, 3);
            world.setBlockState(p, dries[i].getDefaultState(), 2);
            c.ok(world.getBlockState(p).getBlock() == dries[i] && !Waterlogging.hasWater(world, p),
                    dries[i].getRegistryName() + " placed into water replaces it (26.3's holds none)");
        }

        BlockPos runs = area(c);
        Block[] onFlowing = {stairs, fence, trapdoor, leaves, sign, wall};
        for (int i = 0; i < onFlowing.length; i++) {
            BlockPos p = runs.add(2 + 2 * i, 0, 2);
            world.setBlockState(p, Blocks.FLOWING_WATER.getDefaultState().withProperty(BlockLiquid.LEVEL, 3), 2);
            world.setBlockState(p, onFlowing[i].getDefaultState(), 2);
            c.ok(world.getBlockState(p).getBlock() == onFlowing[i] && !Waterlogging.hasWater(world, p),
                    onFlowing[i].getRegistryName() + " placed into flowing water replaces it");
        }
        if (wet) {
            // Lava next to a waterloggable block never goes into it (checked per state above); a placed block replaces it.
            BlockPos lava = runs.add(2, 0, 6);
            world.setBlockState(lava, Blocks.LAVA.getDefaultState(), 2);
            world.setBlockState(lava, fence.getDefaultState(), 2);
            c.ok(world.getBlockState(lava).getBlock() == fence && FluidloggedChecks.empty(world, lava), "a poplar fence placed into lava holds none");
        }

        // 3. Flowing water does not run into a dry block; water flows out of a waterlogged one --------------------
        BlockPos flat = area(c);
        BlockPos spring = flat.add(10, 0, 10);
        world.setBlockState(spring.east(), fence.getDefaultState(), 3);
        world.setBlockState(spring.west(), leaves.getDefaultState(), 3);
        world.setBlockState(spring.north(), trapdoor.getDefaultState(), 3);
        world.setBlockState(spring, Blocks.FLOWING_WATER.getDefaultState(), 3);
        settle(world);
        c.ok(Waterlogging.hasWater(world, spring.south()), "the spring's water spreads");
        c.ok(!Waterlogging.hasWater(world, spring.east()), "but not into a dry poplar fence beside it");
        c.ok(!Waterlogging.hasWater(world, spring.west()), "nor into dry poplar leaves");
        c.ok(!Waterlogging.hasWater(world, spring.north()), "nor into a dry poplar trapdoor");
        // Each alone in the middle of its own area. Which sides the water then takes is Fluidlogged API's flow (it runs
        // toward the cheapest way down, as 1.12.2's water, and in a full run often takes one side only), so the test asks
        // only that it leaves the block, and logs how many sides. 26.3's water never leaves waterlogged leaves (a full
        // collision box); Fluidlogged API's does: logged, not failed (docs/CORE_REQUESTS.md, W-1).
        Block[] outflow = {slab, fence, wall, sign, trapdoor, leaves};
        for (int i = 0; i < outflow.length; i++) {
            BlockPos logged = area(c).add(16, 0, 16);
            String n = outflow[i].getRegistryName().getPath();
            c.eq(wet, Waterlogging.placeWithWater(world, logged, outflow[i].getDefaultState(), 3), "placeWithWater gives " + n + " a source");
            settle(world);
            c.eq(wet, Waterlogging.hasWaterSource(world, logged), "waterlogged " + n + " keeps its source");
            int sides = 0;
            for (EnumFacing side : EnumFacing.HORIZONTALS) {
                if (Waterlogging.hasWater(world, logged.offset(side)) && !Waterlogging.hasWaterSource(world, logged.offset(side))) {
                    sides++;
                }
            }
            if (outflow[i] == leaves) {
                GameDropsBackport.LOGGER.info("SELFTEST waterlog: water runs out of {} sides of waterlogged {} (26.3: none)", sides, n);
            } else {
                c.ok(wet ? sides >= 1 : sides == 0, (wet ? "water flows out of a waterlogged " : "no water flows out of ") + n + " (" + sides + " sides)");
            }
        }

        // 4. Buckets -------------------------------------------------------------------------------------------------
        BlockPos cups = area(c);
        EntityPlayerMP player = player(world, "waterbucket");
        player.interactionManager.setGameType(GameType.SURVIVAL);
        Block[] bucketed = {stairs, trapdoor, leaves, sign, fence};
        for (int i = 0; i < bucketed.length; i++) {
            BlockPos s = cup(world, cups.add(2 + 4 * i, 0, 2));
            Block b = bucketed[i];
            world.setBlockState(s, b.getDefaultState(), 3);
            String n = b.getRegistryName().getPath();
            ItemStack after = bucket(player, s, Items.WATER_BUCKET);
            if (wet) {
                c.eq(Items.BUCKET, after.getItem(), "a water bucket on dry " + n + " comes back empty");
                c.ok(world.getBlockState(s).getBlock() == b && Waterlogging.hasWaterSource(world, s), "and waterlogs it");
                c.ok(!Waterlogging.hasWater(world, s.up()), "putting no water above it");
                c.eq(Items.WATER_BUCKET, bucket(player, s, Items.BUCKET).getItem(), "an empty bucket takes the water back from " + n);
                c.ok(world.getBlockState(s).getBlock() == b && !Waterlogging.hasWater(world, s), "leaving the " + n + " dry");
            } else {
                // 1.12.2's bucket: water goes beside the block (above it), or nowhere for a sign, which a full bucket's aim
                // passes through to the stone under it.
                c.ok(world.getBlockState(s).getBlock() == b && !Waterlogging.hasWater(world, s), "without Fluidlogged API a water bucket leaves the " + n + " dry");
                world.setBlockState(s.up(), Blocks.AIR.getDefaultState(), 2 | 16);
            }
        }
        BlockPos d = cup(world, cups.add(2, 0, 8));
        world.setBlockState(d, doubleSlab.getDefaultState(), 3);
        bucket(player, d, Items.WATER_BUCKET);
        c.ok(!Waterlogging.hasWater(world, d) && Waterlogging.hasWater(world, d.up()), "a water bucket on a double poplar slab pours the water on top of it");

        // 5. A slab placed onto a waterlogged slab: a double slab, and the water is gone ---------------------------
        if (wet) {
            BlockPos m = cup(world, cups.add(10, 0, 8));
            Waterlogging.placeWithWater(world, m, slab.getDefaultState(), 3);
            c.ok(Waterlogging.hasWaterSource(world, m), "a waterlogged bottom poplar slab");
            ItemStack slabs = new ItemStack(Item.getItemFromBlock(slab), 4);
            player.setHeldItem(EnumHand.MAIN_HAND, slabs);
            player.setLocationAndAngles(m.getX() + 0.5D, m.getY() + 1.6D, m.getZ() + 0.5D, 0.0F, 90.0F);
            slabs.onItemUse(player, world, m, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 0.5F, 0.5F);
            c.eq(doubleSlab, world.getBlockState(m).getBlock(), "a second slab makes it a double slab");
            c.ok(!Waterlogging.hasWater(world, m), "with no water left in it");
            settle(world);
            c.ok(!Waterlogging.hasWater(world, m.up()), "and none put above it");

            // 6. A trapdoor keeps its water opening and closing --------------------------------------------------
            BlockPos t = cup(world, cups.add(14, 0, 8));
            Waterlogging.placeWithWater(world, t, trapdoor.getDefaultState(), 3);
            world.setBlockState(t, world.getBlockState(t).withProperty(BlockTrapDoor.OPEN, true), 3);
            c.ok(world.getBlockState(t).getValue(BlockTrapDoor.OPEN) && Waterlogging.hasWaterSource(world, t), "an opened waterlogged trapdoor keeps its water");
            world.setBlockState(t, world.getBlockState(t).withProperty(BlockTrapDoor.OPEN, false), 3);
            c.ok(Waterlogging.hasWaterSource(world, t), "and closed again too");
        }

        // 7. Broken while waterlogged: the block drops, the water stays ------------------------------------------
        BlockPos br = area(c);
        Block[] broken = {fence, sign, trapdoor, stairs, wall};
        for (int i = 0; i < broken.length; i++) {
            BlockPos p = cup(world, br.add(2 + 4 * i, 0, 2));
            Waterlogging.placeWithWater(world, p, broken[i].getDefaultState(), 2);
            Item item = broken[i] == sign ? PoplarContent.set.signItem : Item.getItemFromBlock(broken[i]);
            world.destroyBlock(p, true);
            boolean dropped = !world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(p).grow(1.5D),
                    e -> e.getItem().getItem() == item).isEmpty();
            String n = broken[i].getRegistryName().getPath();
            c.ok(dropped, (wet ? "waterlogged " : "") + n + " broken drops itself");
            c.ok(world.getBlockState(p).getBlock() != broken[i] && Waterlogging.hasWaterSource(world, p) == wet,
                    wet ? n + " broken leaves its water source behind" : n + " broken leaves no water (it had none)");
        }

        // 8. Generation: a poplar grown into water keeps the sources in its leaves; flowing water it replaces ---------
        BlockPos lake = area(c).add(16, 0, 16);
        for (int dx = -10; dx <= 10; dx++) {
            for (int dz = -10; dz <= 10; dz++) {
                for (int dy = 0; dy < 20; dy++) {
                    IBlockState water = dx <= 0 ? Blocks.WATER.getDefaultState()
                            : Blocks.FLOWING_WATER.getDefaultState().withProperty(BlockLiquid.LEVEL, 2);
                    world.setBlockState(lake.add(dx, dy, dz), water, 2 | 16);
                }
            }
        }
        boolean grew = PoplarFeatures.tree(world, new Random(7L), lake, BlockPoplarLeaves.Color.YELLOW, false);
        c.ok(grew, "a poplar grows in a lake");
        int sourceLeaves = 0;
        int flowingLeaves = 0;
        List<String> badLeaves = new ArrayList<String>();
        for (int dx = -10; dx <= 10; dx++) {
            for (int dz = -10; dz <= 10; dz++) {
                for (int dy = 0; dy < 20; dy++) {
                    BlockPos p = lake.add(dx, dy, dz);
                    if (!(world.getBlockState(p).getBlock() instanceof BlockPoplarLeaves)) {
                        continue;
                    }
                    boolean water = Waterlogging.hasWaterSource(world, p);
                    if (dx <= 0) {
                        sourceLeaves++;
                        if (water != wet) {
                            badLeaves.add(p + " in a source: " + water);
                        }
                    } else {
                        flowingLeaves++;
                        if (Waterlogging.hasWater(world, p)) {
                            badLeaves.add(p + " in flowing water holds some");
                        }
                    }
                }
            }
        }
        c.ok(sourceLeaves > 10 && flowingLeaves > 10, "leaves grew into both halves: " + sourceLeaves + " and " + flowingLeaves);
        c.ok(badLeaves.isEmpty(), (wet ? "leaves in source water are waterlogged, in flowing water dry: " : "leaves in water are dry: ") + badLeaves);
        // On dry land none hold water.
        BlockPos land = area(c).add(16, 0, 16);
        c.ok(PoplarFeatures.tree(world, new Random(7L), land, BlockPoplarLeaves.Color.RED, false), "a poplar grows on land");
        int dryLeaves = 0;
        boolean anyWet = false;
        for (BlockPos p : BlockPos.getAllInBox(land.add(-10, 0, -10), land.add(10, 20, 10))) {
            if (world.getBlockState(p).getBlock() instanceof BlockPoplarLeaves) {
                dryLeaves++;
                anyWet |= Waterlogging.hasWater(world, p);
            }
        }
        c.ok(dryLeaves > 10 && !anyWet, "its " + dryLeaves + " leaves hold no water");

        // Sulfur spikes placed by the sulfur caves' features: waterlogged in any water (26.3's isWaterAt), dry in air.
        BlockPos spikes = area(c);
        com.exiledradio.gamedropsbackport.sulfurcaves.SpikeWaterChecks.run(c, world, spikes, wet);
    }

    private static Block block(String path) {
        Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(GameDropsBackport.MOD_ID, path));
        return b == Blocks.AIR ? null : b;
    }

    /** A real server player with a connection that goes nowhere. */
    private static EntityPlayerMP player(WorldServer world, String name) {
        EntityPlayerMP p = new EntityPlayerMP(world.getMinecraftServer(), world,
                new GameProfile(UUID.nameUUIDFromBytes(("selftest:" + name).getBytes()), "Test" + name), new PlayerInteractionManager(world));
        p.connection = new NetHandlerPlayServer(world.getMinecraftServer(), new NetworkManager(EnumPacketDirection.SERVERBOUND), p);
        return p;
    }

    /** Uses a bucket on the block at pos, looking straight down at it from above. @return the bucket after */
    private static ItemStack bucket(EntityPlayerMP player, BlockPos pos, Item bucket) {
        player.setLocationAndAngles(pos.getX() + 0.5D, pos.getY() + 1.4D, pos.getZ() + 0.5D, 0.0F, 90.0F);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(bucket));
        ActionResult<ItemStack> result = bucket.onItemRightClick(player.world, player, EnumHand.MAIN_HAND);
        return result.getResult();
    }

    /** A stone cup: stone below and on the four sides of pos, air above. @return pos */
    private static BlockPos cup(WorldServer world, BlockPos pos) {
        world.setBlockState(pos.down(), Blocks.STONE.getDefaultState(), 2);
        for (EnumFacing side : EnumFacing.HORIZONTALS) {
            world.setBlockState(pos.offset(side), Blocks.STONE.getDefaultState(), 2);
        }
        return pos;
    }

    /** A 14x14 pool of still water one deep, walled with stone, on a fresh area's floor. @return its floor corner */
    private BlockPos pool(Check c) {
        BlockPos corner = area(c);
        for (int x = 0; x < 14; x++) {
            for (int z = 0; z < 14; z++) {
                boolean edge = x == 0 || z == 0 || x == 13 || z == 13;
                c.world.setBlockState(corner.add(x, 0, z), (edge ? Blocks.STONE : Blocks.WATER).getDefaultState(), 2 | 16);
            }
        }
        return corner.add(0, -1, 0);
    }

    /**
     * A fresh 32x32 area far from everything: stone at y 99, air from y 100 to 159. @return its corner at y 100
     */
    private BlockPos area(Check c) {
        int x = 26000 + 48 * this.nextArea++;
        int z = 26000;
        for (int dx = 0; dx < 32; dx++) {
            for (int dz = 0; dz < 32; dz++) {
                for (int y = 60; y < 160; y++) {
                    BlockPos p = new BlockPos(x + dx, y, z + dz);
                    IBlockState want = y == 99 ? Blocks.STONE.getDefaultState() : Blocks.AIR.getDefaultState();
                    if (c.world.getBlockState(p) != want) {
                        Waterlogging.placeDry(c.world, p, want, 2 | 16);
                    }
                }
            }
        }
        return new BlockPos(x, 100, z);
    }

    /** Runs every pending block tick now, a few rounds, so water finishes flowing. */
    private static void settle(WorldServer world) {
        for (int i = 0; i < 40; i++) {
            world.tickUpdates(true);
        }
    }
}
