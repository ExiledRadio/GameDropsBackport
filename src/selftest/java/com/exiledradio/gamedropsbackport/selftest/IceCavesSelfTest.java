package com.exiledradio.gamedropsbackport.selftest;

import com.exiledradio.backportcore.speleothem.BlockSpeleothem;
import com.exiledradio.backportcore.speleothem.Thickness;
import com.exiledradio.gamedropsbackport.icecaves.BlockBrittleIce;
import com.exiledradio.gamedropsbackport.icecaves.BlockIceCrystal;
import com.exiledradio.gamedropsbackport.icecaves.BlockIcicle;
import com.exiledradio.gamedropsbackport.icecaves.IceCavesContent;
import com.exiledradio.gamedropsbackport.icecaves.IceCavesEvents;
import com.exiledradio.gamedropsbackport.icecaves.Reg;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.monster.EntityStray;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import java.util.Random;

/**
 * 26.4 stream 1's blocks with Pre-Release 1's numbers: icicles (growth on packed ice, length, no merging, melting
 * by block light and magma), ice crystals (shape, light, support) and brittle ice (cracking, who cracks it, drops).
 */
final class IceCavesSelfTest implements SelfTest {

    @Override
    public String name() {
        return "icecaves";
    }

    @Override
    public void run(Check check) throws Exception {
        if (!check.ok(IceCavesContent.enabled(), "iceCaves stream enabled")) {
            return;
        }
        WorldServer w = check.world;
        BlockPos base = w.getSpawnPoint().add(-80, 0, -80);
        base = new BlockPos(base.getX(), 200, base.getZ());
        w.getChunk(base);
        clear(w, base);

        icicle(check, w, base);
        crystal(check, w, base.add(8, 0, 0));
        brittle(check, w, base.add(16, 0, 0));
        RegDump.dump(Reg.blocks(), Reg.multipartFiles(), Reg.ignoredProperties(), Reg.itemModels());
    }

    private static void clear(WorldServer w, BlockPos base) {
        for (BlockPos p : BlockPos.getAllInBox(base.add(-2, -8, -2), base.add(26, 4, 4))) {
            w.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
        }
    }

    private static void icicle(Check check, WorldServer w, BlockPos at) {
        BlockIcicle icicle = IceCavesContent.icicle;
        check.ok(Math.abs(icicle.getBlockHardness(icicle.getDefaultState(), w, at) - 0.5F) < 1e-4, "icicle hardness 0.5");
        check.ok(Math.abs(icicle.getExplosionResistance(null) - 0.5F) < 1e-4, "icicle resistance 0.5");
        check.ok(icicle.getOffsetType() == net.minecraft.block.Block.EnumOffsetType.NONE, "icicle: no sideways offset");
        w.setBlockState(at.up(), Blocks.PACKED_ICE.getDefaultState(), 2);
        w.setBlockState(at, icicle.placementState(w, at, EnumFacing.DOWN, true), 3);
        check.ok(icicle.getActualState(w.getBlockState(at), w, at).getValue(BlockIcicle.ATTACHED), "icicle: root is attached");
        Random down = new Random(0) {
            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };
        for (int i = 0; i < 8; i++) {
            icicle.growIfPossible(w.getBlockState(at), w, at, down);
        }
        int length = 0;
        while (w.getBlockState(at.down(length)).getBlock() == icicle) {
            length++;
        }
        check.eq(5, length, "icicle grows to MAX_GROWING_LENGTH 4's reach (5 segments)");
        check.ok(!icicle.getActualState(w.getBlockState(at.down()), w, at.down()).getValue(BlockIcicle.ATTACHED),
                "icicle: lower segments not attached");
        check.ok(w.getBlockState(at.down(4)).getValue(BlockSpeleothem.THICKNESS) == Thickness.TIP, "icicle: tip at the end");

        // Down only, no merging: a stalagmite tip below the stalactite stays apart.
        BlockPos p = at.add(3, 0, 0);
        w.setBlockState(p.up(), Blocks.PACKED_ICE.getDefaultState(), 2);
        w.setBlockState(p, icicle.state(EnumFacing.DOWN, Thickness.TIP), 2);
        w.setBlockState(p.down(3), Blocks.PACKED_ICE.getDefaultState(), 2);
        w.setBlockState(p.down(2), icicle.state(EnumFacing.UP, Thickness.TIP), 2);
        icicle.growIfPossible(w.getBlockState(p), w, p, down);
        check.ok(w.getBlockState(p.down()).getBlock() == icicle
                && w.getBlockState(p.down()).getValue(BlockSpeleothem.THICKNESS) == Thickness.TIP, "icicle: grew down to the stalagmite");
        icicle.growIfPossible(w.getBlockState(p), w, p, down);
        check.ok(w.getBlockState(p.down()).getValue(BlockSpeleothem.THICKNESS) != Thickness.TIP_MERGE, "icicle: never merges tips");

        // Melting: block light above 4, or magma one or two below.
        check.ok(!BlockIcicle.wouldMelt(w, at.down(2)), "icicle: dark, no melting");
        w.setBlockState(at.down(2).east(), Blocks.GLOWSTONE.getDefaultState(), 3);
        w.checkLight(at.down(2));
        check.ok(BlockIcicle.wouldMelt(w, at.down(2)), "icicle: block light above 4 melts (" + w.getLightFor(net.minecraft.world.EnumSkyBlock.BLOCK, at.down(2)) + ")");
        w.setBlockToAir(at.down(2).east());
        w.checkLight(at.down(2));
        BlockPos m = at.add(-1, 0, 0);
        w.setBlockState(m.down(2), Blocks.MAGMA.getDefaultState(), 2);
        check.ok(BlockIcicle.wouldMelt(w, m), "icicle: magma two below melts");
        w.setBlockToAir(m.down(2));
        check.ok(!icicle.canPlaceBlockAt(w, at.add(-1, -6, 0)), "icicle: needs something to hang from");
    }

    private static void crystal(Check check, WorldServer w, BlockPos at) {
        BlockIceCrystal c = IceCavesContent.iceCrystal;
        check.eq(4, c.getDefaultState().getLightValue(w, at), "ice crystal light 4");
        check.ok(Math.abs(c.getBlockHardness(c.getDefaultState(), w, at) - 1.5F) < 1e-4, "ice crystal hardness 1.5");
        w.setBlockState(at.down(), Blocks.PACKED_ICE.getDefaultState(), 2);
        w.setBlockState(at, c.getDefaultState().withProperty(BlockIceCrystal.FACING, EnumFacing.UP), 3);
        net.minecraft.util.math.AxisAlignedBB box = w.getBlockState(at).getBoundingBox(w, at);
        check.ok(Math.abs(box.maxY - 7 / 16.0) < 1e-6 && Math.abs(box.minX - 3 / 16.0) < 1e-6, "ice crystal: 7 high, 10 wide (" + box + ")");
        w.setBlockToAir(at.down());
        check.ok(w.isAirBlock(at), "ice crystal: turns to air without support");
        check.eq(0, w.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class, new net.minecraft.util.math.AxisAlignedBB(at).grow(2)).size(),
                "ice crystal: no drop when its support goes");
    }

    private static void brittle(Check check, WorldServer w, BlockPos at) {
        BlockBrittleIce ice = IceCavesContent.brittleIce;
        check.ok(Math.abs(ice.slipperiness - 0.98F) < 1e-6, "brittle ice friction 0.98");
        check.eq(0, ice.quantityDropped(new Random()), "brittle ice drops nothing without Silk Touch");
        check.ok(!ice.getTickRandomly(), "brittle ice never melts (no random ticks)");
        w.setBlockState(at, ice.getDefaultState(), 3);
        w.setBlockState(at.east(), ice.getDefaultState(), 3);
        EntityPig pig = new EntityPig(w);
        pig.setPosition(at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5);
        w.getGameRules().setOrCreateGameRule("mobGriefing", "true");
        ice.stepOn(w, at, w.getBlockState(at), pig);
        check.eq(1, w.getBlockState(at).getValue(BlockBrittleIce.AGE), "brittle ice: a step cracks it once");
        ice.stepOn(w, at, w.getBlockState(at), pig);
        check.eq(1, w.getBlockState(at).getValue(BlockBrittleIce.AGE), "brittle ice: 24-tick wait before the next crack");
        check.ok(w.isUpdateScheduled(at, ice), "brittle ice: the wait is a scheduled tick");
        EntityStray stray = new EntityStray(w);
        BlockPos other = at.east();
        ice.stepOn(w, other, w.getBlockState(other), stray);
        check.eq(0, w.getBlockState(other).getValue(BlockBrittleIce.AGE), "brittle ice: strays never crack it");
        w.getGameRules().setOrCreateGameRule("mobGriefing", "false");
        ice.stepOn(w, other, w.getBlockState(other), pig);
        check.eq(0, w.getBlockState(other).getValue(BlockBrittleIce.AGE), "brittle ice: mobs need mobGriefing");
        w.getGameRules().setOrCreateGameRule("mobGriefing", "true");
        // Age 3 breaks on the next crack, with no drop, and cracks its neighbor.
        w.setBlockState(at, ice.getDefaultState().withProperty(BlockBrittleIce.AGE, 3), 2);
        BlockPos fresh = at.add(0, 0, 2);
        w.setBlockState(fresh, ice.getDefaultState().withProperty(BlockBrittleIce.AGE, 3), 2);
        w.setBlockState(fresh.east(), ice.getDefaultState(), 2);
        ice.stepOn(w, fresh, w.getBlockState(fresh), pig);
        check.ok(w.isAirBlock(fresh), "brittle ice: age 3 breaks");
        check.eq(1, w.getBlockState(fresh.east()).getValue(BlockBrittleIce.AGE), "brittle ice: breaking cracks its neighbor");
        // Faces between ice blocks are hidden only on the client; the conversion needs some mod's blue ice.
        check.ok(!IceCavesEvents.freezeNextToBlueIce(w, at.add(5, 0, 5)), "no water there, no brittle ice");
        IBlockState water = Blocks.WATER.getDefaultState();
        w.setBlockState(at.add(5, 0, 5), water, 2);
        check.ok(!IceCavesEvents.freezeNextToBlueIce(w, at.add(5, 0, 5)), "water without blue ice stays water");
        w.setBlockToAir(at.add(5, 0, 5));
    }
}
