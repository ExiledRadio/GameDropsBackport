package com.exiledradio.gamedropsbackport.jukebox;

import com.exiledradio.backportcore.gameevent.GameEvent;
import com.exiledradio.backportcore.gameevent.GameEventContext;
import com.exiledradio.backportcore.gameevent.GameEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockJukebox;
import net.minecraft.block.state.IBlockState;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemRecord;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 26.4's jukebox playlists ({@code JukeboxBlockEntity}): a jukebox under a single chest, trapped chest or copper chest
 * ({@code #jukebox_containers}; a double chest turns it off) plays the discs in that chest.
 *
 * <ul>
 *   <li>When a song ends, the jukebox swaps its disc for the next disc in the chest: the first slot after the one it
 *       last took ({@code playlist_position}), wrapping round, skipping anything that isn't a disc. With no other disc
 *       the same one plays again: the playlist loops forever.</li>
 *   <li>An empty jukebox in playlist mode takes the next disc at once (every tick), so ejecting one skips to the
 *       next.</li>
 *   <li>Taking a disc out in playlist mode is a {@code block_change} game event (26.4 Pre-Release 2; putting one
 *       in is not).</li>
 * </ul>
 * 1.12.2's jukebox doesn't know when a song ends; 26.4's lengths ({@code jukebox_song/*.json}) tell it, by the disc's
 * song name ({@code record_13}'s 13, {@code music_disc_otherside}'s otherside). A disc with no known length (another
 * mod's) never ends, as in 1.12.2. Checked every tick, for the jukeboxes in loaded chunks.
 */
public final class Playlists {

    /** 26.4's {@code length_in_seconds} by song. */
    static final Map<String, Float> LENGTHS = new HashMap<String, Float>();

    static {
        Object[] t = {"11", 71.0F, "13", 178.0F, "5", 178.0F, "blocks", 345.0F, "bounce", 234.0F, "cat", 185.0F,
                "chirp", 185.0F, "creator", 176.0F, "creator_music_box", 73.0F, "far", 174.0F, "flurry", 154.0F,
                "lava_chicken", 134.0F, "mall", 197.0F, "mellohi", 96.0F, "otherside", 195.0F, "pigstep", 149.0F,
                "precipice", 299.0F, "relic", 218.0F, "stal", 150.0F, "strad", 188.0F, "tears", 175.0F, "wait", 238.0F,
                "ward", 251.0F};
        for (int i = 0; i < t.length; i += 2) {
            LENGTHS.put((String) t[i], (Float) t[i + 1]);
        }
    }

    static final String POSITION = "gamedropsbackport:playlist_position";
    static final String STARTED = "gamedropsbackport:song_started";

    /** @return the song a disc plays, by its registry name, or null */
    @Nullable
    static String song(Item item) {
        ResourceLocation id = item.getRegistryName();
        if (id == null || !(item instanceof ItemRecord)) {
            return null;
        }
        String p = id.getPath();
        return p.startsWith("record_") ? p.substring(7) : p.startsWith("music_disc_") ? p.substring(11) : null;
    }

    /** @return the song's length in ticks, or -1 when unknown */
    public static int lengthTicks(ItemStack disc) {
        String song = disc.isEmpty() ? null : song(disc.getItem());
        Float s = song == null ? null : LENGTHS.get(song);
        return s == null ? -1 : (int) (s * 20.0F);
    }

    /** {@code #jukebox_containers}: chests, trapped chests and any mod's copper chests. */
    static boolean container(Block block) {
        if (block instanceof BlockChest) {
            return true;
        }
        ResourceLocation id = block.getRegistryName();
        return id != null && id.getPath().endsWith("copper_chest");
    }

    /** {@code isInPlaylistMode}: a single container above. */
    @Nullable
    public static IInventory playlistChest(World world, BlockPos jukebox) {
        BlockPos up = jukebox.up();
        IBlockState state = world.getBlockState(up);
        if (!container(state.getBlock())) {
            return null;
        }
        for (EnumFacing side : EnumFacing.Plane.HORIZONTAL) {
            if (world.getBlockState(up.offset(side)).getBlock() == state.getBlock() && state.getBlock() instanceof BlockChest) {
                return null;
            }
        }
        TileEntity te = world.getTileEntity(up);
        return te instanceof IInventory ? (IInventory) te : null;
    }

    /** {@code findNextPlaylistPosition}: the first disc slot after {@code start}, wrapping; -1 when none. */
    public static int nextPosition(IInventory chest, int start) {
        int size = chest.getSizeInventory();
        int from = Math.max(-1, Math.min(start, size));
        for (int offset = 1; offset <= size; offset++) {
            int slot = (from + offset) % size;
            ItemStack s = chest.getStackInSlot(slot);
            if (!s.isEmpty() && s.getItem() instanceof ItemRecord) {
                return slot;
            }
        }
        return -1;
    }

    /**
     * {@code tryToAdvancePlaylist}: swaps the jukebox's disc (or nothing) into the next disc's slot and plays that one;
     * with no next disc and {@code replay}, the same disc again.
     */
    public static boolean advance(World world, BlockPos pos, BlockJukebox.TileEntityJukebox jukebox, IInventory chest, boolean replay) {
        NBTTagCompound data = jukebox.getTileData();
        int at = data.hasKey(POSITION) ? data.getInteger(POSITION) : -1;
        int next = nextPosition(chest, at);
        ItemStack current = jukebox.getRecord();
        if (next < 0) {
            if (replay && !current.isEmpty()) {
                play(world, pos, jukebox, current);
                return true;
            }
            return false;
        }
        ItemStack disc = chest.getStackInSlot(next);
        chest.setInventorySlotContents(next, current.isEmpty() ? ItemStack.EMPTY : current.copy());
        data.setInteger(POSITION, next);
        play(world, pos, jukebox, disc.copy());
        chest.markDirty();
        return true;
    }

    private static void play(World world, BlockPos pos, BlockJukebox.TileEntityJukebox jukebox, ItemStack disc) {
        jukebox.setRecord(disc);
        IBlockState state = world.getBlockState(pos);
        if (!state.getValue(BlockJukebox.HAS_RECORD)) {
            world.setBlockState(pos, state.withProperty(BlockJukebox.HAS_RECORD, true), 2);
        }
        jukebox.getTileData().setLong(STARTED, world.getTotalWorldTime());
        jukebox.markDirty();
        // Level event 1010 starts the disc on clients (stopping what played before).
        world.playEvent(null, 1010, pos, Item.getIdFromItem(disc.getItem()));
    }

    /** Every jukebox in loaded chunks, every tick, on the server. */
    public static final class Events {
        private final Map<World, List<BlockPos>> scratch = new HashMap<World, List<BlockPos>>();

        @SubscribeEvent
        public void tick(TickEvent.WorldTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.world.isRemote) {
                return;
            }
            World world = event.world;
            List<BlockPos> jukeboxes = this.scratch.computeIfAbsent(world, w -> new ArrayList<BlockPos>());
            jukeboxes.clear();
            for (TileEntity te : world.loadedTileEntityList) {
                if (te instanceof BlockJukebox.TileEntityJukebox && !te.isInvalid()) {
                    jukeboxes.add(te.getPos());
                }
            }
            for (BlockPos pos : jukeboxes) {
                tick(world, pos);
            }
        }

        static void tick(World world, BlockPos pos) {
            TileEntity te = world.getTileEntity(pos);
            if (!(te instanceof BlockJukebox.TileEntityJukebox)) {
                return;
            }
            BlockJukebox.TileEntityJukebox jukebox = (BlockJukebox.TileEntityJukebox) te;
            NBTTagCompound data = jukebox.getTileData();
            ItemStack record = jukebox.getRecord();
            boolean taken = record.isEmpty() && data.hasKey(STARTED);
            if (record.isEmpty()) {
                data.removeTag(STARTED);
            } else if (!data.hasKey(STARTED)) {
                // A disc put in by hand or a hopper: its song starts now.
                data.setLong(STARTED, world.getTotalWorldTime());
            }
            IInventory chest = playlistChest(world, pos);
            if (chest == null) {
                return;
            }
            if (record.isEmpty()) {
                if (taken) {
                    GameEvents.emit(world, GameEvent.BLOCK_CHANGE, pos, GameEventContext.of(world.getBlockState(pos)));
                }
                advance(world, pos, jukebox, chest, false);
                return;
            }
            int length = lengthTicks(record);
            if (length > 0 && world.getTotalWorldTime() - data.getLong(STARTED) >= length) {
                advance(world, pos, jukebox, chest, true);
            }
        }
    }
}
