package com.exiledradio.gamedropsbackport.camps;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Patch P6's hooks: the container whose loot is being rolled ({@code TileEntityLockableLoot.fillWithLoot}), per
 * thread, so the camp loot's {@code location_check} and {@code exploration_map} start at the container, as 26.3's
 * {@code ORIGIN} does. Public only because patched vanilla code calls it.
 */
public final class LootOrigin {

    private static final ThreadLocal<TileEntity> CURRENT = new ThreadLocal<TileEntity>();

    private LootOrigin() {
    }

    public static void enter(TileEntity container) {
        CURRENT.set(container);
    }

    public static void exit() {
        CURRENT.remove();
    }

    /** @return the block of the container being filled in this world, or null outside {@code fillWithLoot} */
    @Nullable
    static BlockPos of(@Nullable World world) {
        TileEntity te = CURRENT.get();
        return te == null || world == null || te.getWorld() != world ? null : te.getPos();
    }
}
