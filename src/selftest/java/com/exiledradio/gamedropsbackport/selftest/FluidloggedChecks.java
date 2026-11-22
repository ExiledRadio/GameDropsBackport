package com.exiledradio.gamedropsbackport.selftest;

import git.jbredwards.fluidlogged_api.api.util.FluidState;
import git.jbredwards.fluidlogged_api.api.util.FluidloggedUtils;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidRegistry;

import java.util.List;

/**
 * The waterlog test's questions only Fluidlogged API can answer. Its types are named only here, and {@link
 * WaterlogSelfTest} calls this class only when Fluidlogged API is installed, so the test loads without it.
 */
final class FluidloggedChecks {

    private FluidloggedChecks() {
    }

    /** Adds to {@code wrong} if this state would take flowing water or lava (26.3: a water source only). */
    static void refused(IBlockState state, World world, BlockPos pos, List<String> wrong) {
        FluidState flowing = FluidState.of(FluidRegistry.WATER).withLevel(3);
        FluidState lava = FluidState.of(FluidRegistry.LAVA);
        if (FluidloggedUtils.isStateFluidloggable(state, world, pos, flowing)) {
            wrong.add(state + " takes flowing water");
        }
        if (FluidloggedUtils.isStateFluidloggable(state, world, pos, lava)) {
            wrong.add(state + " takes lava");
        }
    }

    /** @return true if no fluid at all is kept beside the block at pos */
    static boolean empty(World world, BlockPos pos) {
        return FluidloggedUtils.getFluidState(world, pos).isEmpty();
    }
}
