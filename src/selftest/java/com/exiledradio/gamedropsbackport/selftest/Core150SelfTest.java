package com.exiledradio.gamedropsbackport.selftest;

import com.exiledradio.backportcore.advancement.AdventuringTime;
import com.exiledradio.backportcore.gameevent.GameEvent;
import com.exiledradio.backportcore.gameevent.GameEventContext;
import com.exiledradio.backportcore.gameevent.GameEventListener;
import com.exiledradio.backportcore.gameevent.GameEvents;
import com.exiledradio.backportcore.gameevent.PositionSource;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.softblocks.SoftBlocks;
import com.exiledradio.gamedropsbackport.sulfurcube.EntitySulfurCube;

import net.minecraft.block.BlockDispenser;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * What Backport Core 1.5.0 does for this mod's rows: Adventuring Time names the sulfur caves and the dappled forest
 * (26.2, 26.3), and, where game events run (Wild Update installed), placing a cushion is an {@code entity_place} that
 * Core's emitters find by themselves (26.3 snapshots 7-8) and a dispenser shearing a cube a {@code shear}.
 */
final class Core150SelfTest {

    private Core150SelfTest() {
    }

    /** Hears everything within 16 blocks of a spot, at once. */
    private static final class Ear implements GameEventListener {
        final Vec3d at;
        final List<GameEvent> heard = new ArrayList<GameEvent>();

        Ear(Vec3d at) {
            this.at = at;
        }

        @Override
        public PositionSource position() {
            return new PositionSource.Block(new BlockPos(this.at));
        }

        @Override
        public int radius() {
            return 16;
        }

        @Override
        public boolean handle(WorldServer world, GameEvent event, Vec3d origin, GameEventContext context) {
            this.heard.add(event);
            return true;
        }

        @Override
        public boolean immediate() {
            return true;
        }
    }

    /** Soft blocks stream: Adventuring Time's biomes and the cushion's vibration. */
    static void softBlocks(Check check, WorldServer world) {
        Set<String> biomes = AdventuringTime.backportBiomes();
        check.ok(biomes.contains("minecraft:sulfur_caves") || biomes.contains("sulfur_caves"),
                "Core 1.5.0's Adventuring Time names the sulfur caves (26.2): " + biomes);
        check.ok(biomes.contains("minecraft:dappled_forest") || biomes.contains("dappled_forest"),
                "and the dappled forest (26.3)");
        if (!GameEvents.active()) {
            GameDropsBackport.LOGGER.info("SELFTEST note: no game events in this run (Wild Update not installed); cushion vibrations not checked");
            return;
        }
        BlockPos floor = new BlockPos(40, 150, -40);
        world.setBlockState(floor, Blocks.STONE.getDefaultState());
        Ear ear = new Ear(new Vec3d(floor.up(3)).add(0.5D, 0.5D, 0.5D));
        GameEvents.add(world, ear);
        try {
            FakePlayer player = FakePlayerFactory.getMinecraft(world);
            player.setPosition(floor.getX() + 0.5D, floor.getY() + 1, floor.getZ() + 2.5D);
            ItemStack cushion = new ItemStack(SoftBlocks.cushion, 2);
            player.setHeldItem(EnumHand.MAIN_HAND, cushion);
            ForgeHooks.onRightClickBlock(player, EnumHand.MAIN_HAND, floor, EnumFacing.UP, new Vec3d(0.5D, 1.0D, 0.5D));
            cushion.onItemUse(player, world, floor, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
            com.exiledradio.backportcore.gameevent.GameEventEmitters.serverTick(
                    new TickEvent.ServerTickEvent(TickEvent.Phase.END));
            GameEvents.flush(world);
            check.ok(ear.heard.contains(GameEvent.ENTITY_PLACE), "placing a cushion is entity_place, through Core 1.5.0: " + ear.heard);
            player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        } finally {
            GameEvents.remove(world, ear);
            for (Entity e : world.getEntitiesWithinAABB(Entity.class, new net.minecraft.util.math.AxisAlignedBB(floor).grow(2))) {
                if (!(e instanceof net.minecraft.entity.player.EntityPlayer)) {
                    e.setDead();
                }
            }
            world.setBlockToAir(floor);
        }
    }

    /** Sulfur cube stream: a dispenser's shears are a shear game event (26.2's {@code ShearDispenseBehavior}). */
    static void cubeShearEvent(Check check, WorldServer world, BlockPos d) {
        if (!GameEvents.active()) {
            return;
        }
        world.setBlockState(d, Blocks.DISPENSER.getDefaultState().withProperty(BlockDispenser.FACING, EnumFacing.EAST));
        EntitySulfurCube cube = new EntitySulfurCube(world);
        cube.setLocationAndAngles(d.getX() + 1.5D, d.getY(), d.getZ() + 0.5D, 0.0F, 0.0F);
        cube.onInitialSpawn(world.getDifficultyForLocation(d), null);
        world.spawnEntity(cube);
        cube.equipItem(new ItemStack(Blocks.PLANKS));
        Ear ear = new Ear(new Vec3d(d.east()).add(0.5D, 2.5D, 0.5D));
        GameEvents.add(world, ear);
        try {
            BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(Items.SHEARS).dispense(
                    new net.minecraft.block.BlockSourceImpl(world, d), new ItemStack(Items.SHEARS));
            GameEvents.flush(world);
            check.ok(ear.heard.contains(GameEvent.SHEAR), "a dispenser shearing a cube is a shear game event: " + ear.heard);
        } finally {
            GameEvents.remove(world, ear);
            cube.setDead();
            world.setBlockToAir(d);
        }
    }
}
