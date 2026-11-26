package com.exiledradio.gamedropsbackport.selftest;

import com.exiledradio.gamedropsbackport.jukebox.JukeboxContent;
import com.exiledradio.gamedropsbackport.jukebox.Playlists;

import net.minecraft.block.BlockJukebox;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.LootTableList;

/**
 * 26.4 stream 6: playlist mode (single chest above, not a double), the next-disc order (after the last slot taken,
 * wrapping, skipping non-discs), the swap at a song's end and the loop, an empty jukebox taking a disc at once,
 * Flurry's length, signal and igloo pool.
 */
final class JukeboxSelfTest implements SelfTest {

    @Override
    public String name() {
        return "jukebox";
    }

    @Override
    public void run(Check check) throws Exception {
        if (!check.ok(JukeboxContent.enabled(), "jukebox stream enabled")) {
            return;
        }
        check.eq(154 * 20, Playlists.lengthTicks(new ItemStack(JukeboxContent.flurry)), "Flurry: 154 s");
        check.eq(178 * 20, Playlists.lengthTicks(new ItemStack(Items.RECORD_13)), "13: 178 s");
        check.eq(7, JukeboxContent.flurry.comparatorSignal(), "Flurry: comparator 7");
        LootTable igloo = check.world.getLootTableManager().getLootTableFromLocation(LootTableList.CHESTS_IGLOO_CHEST);
        LootPool pool = igloo.getPool("gamedropsbackport:flurry");
        check.ok(pool != null, "igloo chests have Flurry's 30% pool");

        WorldServer w = check.world;
        BlockPos at = w.getSpawnPoint().add(150, 0, -150);
        at = new BlockPos(at.getX(), 200, at.getZ());
        w.getChunk(at);
        w.setBlockState(at, Blocks.JUKEBOX.getDefaultState(), 2);
        check.ok(Playlists.playlistChest(w, at) == null, "no chest: no playlist");
        w.setBlockState(at.up(), Blocks.CHEST.getDefaultState(), 2);
        IInventory chest = Playlists.playlistChest(w, at);
        check.ok(chest instanceof TileEntityChest, "a chest on top: playlist mode");
        w.setBlockState(at.up().east(), Blocks.CHEST.getDefaultState(), 2);
        check.ok(Playlists.playlistChest(w, at) == null, "a double chest: no playlist");
        w.setBlockToAir(at.up().east());
        chest = Playlists.playlistChest(w, at);

        chest.setInventorySlotContents(2, new ItemStack(Items.RECORD_CAT));
        chest.setInventorySlotContents(5, new ItemStack(Items.DIAMOND));
        chest.setInventorySlotContents(9, new ItemStack(JukeboxContent.flurry));
        check.eq(2, Playlists.nextPosition(chest, -1), "first disc slot from the start");
        check.eq(9, Playlists.nextPosition(chest, 2), "after slot 2 the next disc, skipping the diamond");
        check.eq(2, Playlists.nextPosition(chest, 9), "after the last it wraps");

        BlockJukebox.TileEntityJukebox jukebox = (BlockJukebox.TileEntityJukebox) w.getTileEntity(at);
        Playlists.Events.class.getName();
        tick(w, at);
        check.ok(jukebox.getRecord().getItem() == Items.RECORD_CAT, "empty jukebox takes the next disc at once");
        check.ok(chest.getStackInSlot(2).isEmpty(), "its slot is empty");
        check.ok(w.getBlockState(at).getValue(BlockJukebox.HAS_RECORD), "the jukebox shows a disc");
        // The song ends: the cat disc goes into Flurry's slot and Flurry plays.
        jukebox.getTileData().setLong("gamedropsbackport:song_started", w.getTotalWorldTime() - 185 * 20);
        tick(w, at);
        check.ok(jukebox.getRecord().getItem() == JukeboxContent.flurry, "at the song's end the next disc plays");
        check.ok(chest.getStackInSlot(9).getItem() == Items.RECORD_CAT, "the played disc takes its place");
        jukebox.getTileData().setLong("gamedropsbackport:song_started", w.getTotalWorldTime() - 100);
        tick(w, at);
        check.ok(jukebox.getRecord().getItem() == JukeboxContent.flurry, "mid-song nothing changes");
        jukebox.getTileData().setLong("gamedropsbackport:song_started", w.getTotalWorldTime() - 154 * 20);
        tick(w, at);
        check.ok(jukebox.getRecord().getItem() == Items.RECORD_CAT, "it loops back to the cat disc");
        check.ok(chest.getStackInSlot(9).getItem() == JukeboxContent.flurry, "Flurry back in slot 9");
        for (int i = 0; i < 27; i++) {
            chest.setInventorySlotContents(i, ItemStack.EMPTY);
        }
        jukebox.getTileData().setLong("gamedropsbackport:song_started", w.getTotalWorldTime() - 185 * 20);
        tick(w, at);
        check.ok(jukebox.getRecord().getItem() == Items.RECORD_CAT, "with no other disc the same one plays again");
        check.eq(w.getTotalWorldTime(), jukebox.getTileData().getLong("gamedropsbackport:song_started"), "and starts over");
        w.setBlockToAir(at.up());
        w.setBlockToAir(at);
    }

    private static void tick(WorldServer w, BlockPos at) throws Exception {
        java.lang.reflect.Method m = Playlists.Events.class.getDeclaredMethod("tick", net.minecraft.world.World.class, BlockPos.class);
        m.setAccessible(true);
        m.invoke(null, w, at);
    }
}
