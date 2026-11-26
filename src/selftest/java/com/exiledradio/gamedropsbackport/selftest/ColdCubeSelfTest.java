package com.exiledradio.gamedropsbackport.selftest;

import com.exiledradio.gamedropsbackport.iceblocks.IceBlocksContent;
import com.exiledradio.gamedropsbackport.sulfurcube.Archetype;
import com.exiledradio.gamedropsbackport.sulfurcube.ArchetypeItems;
import com.exiledradio.gamedropsbackport.sulfurcube.EntitySulfurCube;
import com.exiledradio.gamedropsbackport.sulfurcube.SulfurCubes;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

/**
 * 26.4 stream 5: the cold sulfur cube (blue ice) and the fast-sliding list's new ice and snow bricks. The dev server has
 * no blue ice, so {@code sulfurCube.extraBlocks}' {@code =cold} stands in for it.
 */
final class ColdCubeSelfTest implements SelfTest {

    @Override
    public String name() {
        return "coldcube";
    }

    @Override
    public void run(Check check) throws Exception {
        if (!check.ok(SulfurCubes.enabled(), "sulfur cube enabled")) {
            return;
        }
        Archetype cold = Archetype.COLD, fast = Archetype.FAST_SLIDING;
        check.eq(fast.knockbackResistance, cold.knockbackResistance, "cold: fast sliding's knockback resistance");
        check.eq(fast.frictionModifier, cold.frictionModifier, "cold: fast sliding's friction");
        check.eq(fast.airDragModifier, cold.airDragModifier, "cold: fast sliding's air drag");
        check.eq(0.6625F, cold.horizontalPower, "cold: horizontal knockback 0.6625");
        check.eq(0.09F, cold.verticalPower, "cold: vertical knockback 0.09");
        check.ok(cold.freezes() && !fast.freezes(), "only cold freezes water");
        if (IceBlocksContent.enabled()) {
            check.eq(fast, ArchetypeItems.of(new ItemStack(IceBlocksContent.packedIceBricks)), "packed ice bricks: fast sliding (26.4)");
            check.eq(fast, ArchetypeItems.of(new ItemStack(IceBlocksContent.snowBricks)), "snow bricks: fast sliding (26.4)");
            check.eq(fast, ArchetypeItems.of(new ItemStack(IceBlocksContent.chiseledPackedIce)), "chiseled packed ice: fast sliding (26.4)");
        }
        check.ok(ArchetypeItems.extra("minecraft:snow_layer=cold"), "extraBlocks accepts =cold");
        check.eq(cold, ArchetypeItems.of(new ItemStack(Blocks.SNOW_LAYER)), "the stand-in is cold");

        WorldServer w = check.world;
        BlockPos at = w.getSpawnPoint().add(-150, 0, 150);
        at = new BlockPos(at.getX(), 200, at.getZ());
        w.getChunk(at);
        for (BlockPos p : BlockPos.getAllInBox(at.add(-5, -1, -5), at.add(5, 2, 5))) {
            w.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
        }
        for (BlockPos p : BlockPos.getAllInBox(at.add(-5, -1, -5), at.add(5, -1, 5))) {
            w.setBlockState(p, Blocks.WATER.getDefaultState(), 2);
        }
        w.setBlockState(at.down(), Blocks.STONE.getDefaultState(), 2);
        EntitySulfurCube cube = new EntitySulfurCube(w);
        cube.setCubeSize(2, true);
        cube.setPosition(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D);
        cube.setBodyItem(new ItemStack(Blocks.SNOW_LAYER));
        cube.onGround = true;
        w.getGameRules().setOrCreateGameRule("mobGriefing", "false");
        cube.freezeWater();
        check.ok(w.getBlockState(at.add(1, -1, 0)).getMaterial() == net.minecraft.block.material.Material.WATER, "cold: no freezing without mobGriefing (26.4 ReplaceDisk)");
        w.getGameRules().setOrCreateGameRule("mobGriefing", "true");
        cube.freezeWater();
        check.ok(w.getBlockState(at.add(1, -1, 0)).getBlock() == Blocks.FROSTED_ICE, "cold: water beside it froze");
        check.ok(w.getBlockState(at.add(2, -1, 2)).getBlock() == Blocks.FROSTED_ICE, "cold: within 3 froze");
        check.ok(w.getBlockState(at.add(3, -1, 0)).getMaterial() == net.minecraft.block.material.Material.WATER, "cold: 3 blocks out stays water (< 9)");
        check.ok(w.getBlockState(at.add(3, -1, 3)).getMaterial() == net.minecraft.block.material.Material.WATER, "cold: the corner stays water");
        check.eq(0, w.getBlockState(at.add(1, -1, 0)).getValue(net.minecraft.block.BlockFrostedIce.AGE), "cold: frosted ice age 0");
        w.setBlockState(at.add(0, -1, 4), Blocks.FLOWING_WATER.getDefaultState().withProperty(net.minecraft.block.BlockLiquid.LEVEL, 3), 2);
        cube.setPosition(at.getX() + 0.5D, at.getY(), at.getZ() + 4.5D);
        cube.freezeWater();
        check.ok(w.getBlockState(at.add(0, -1, 4)).getBlock() == Blocks.FLOWING_WATER, "cold: flowing water stays");
        cube.onGround = false;
        w.setBlockState(at.add(-1, -1, 0), Blocks.WATER.getDefaultState(), 2);
        cube.setPosition(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D);
        cube.freezeWater();
        check.ok(w.getBlockState(at.add(-1, -1, 0)).getMaterial() == net.minecraft.block.material.Material.WATER, "cold: in the air nothing freezes");
        for (BlockPos p : BlockPos.getAllInBox(at.add(-5, -1, -5), at.add(5, 2, 5))) {
            w.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
        }
    }
}
