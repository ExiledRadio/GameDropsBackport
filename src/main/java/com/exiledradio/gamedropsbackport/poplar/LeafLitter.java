package com.exiledradio.gamedropsbackport.poplar;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * Leaf litter is Tricky Trials' block (1.21.5). The dappled forest's poplars drop it around their foot when Tricky
 * Trials is installed; without it they drop none (no copy of it is registered here yet: see
 * {@code docs/CORE_REQUESTS.md}).
 */
final class LeafLitter {

    private static final ResourceLocation OWNER = new ResourceLocation(Owners.TRICKY_TRIALS, "leaf_litter");

    private LeafLitter() {
    }

    /** @return Tricky Trials' leaf litter, or null when it is not in this game */
    @Nullable
    static Block block() {
        if (!Loader.isModLoaded(Owners.TRICKY_TRIALS)) {
            return null;
        }
        Block b = ForgeRegistries.BLOCKS.getValue(OWNER);
        return b == null || b == Blocks.AIR || !OWNER.equals(b.getRegistryName()) ? null : b;
    }
}
