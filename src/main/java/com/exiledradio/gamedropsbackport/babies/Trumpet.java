package com.exiledradio.gamedropsbackport.babies;

import com.exiledradio.backportcore.copper.Oxidation;
import com.exiledradio.gamedropsbackport.GameDropsBackport;

import net.minecraft.block.Block;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.world.NoteBlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 26.1's trumpet: a note block on a block of copper, cut copper, chiseled copper, or cut copper stairs or slabs
 * (waxed or not, any family mod's copper in Backport Core's oxidation table) plays the trumpet of that copper's
 * stage: {@code trumpet}, {@code trumpet_exposed}, {@code trumpet_weathered}, {@code trumpet_oxidized}.
 *
 * <p>Same pitch rule, volume (3) and note particle as every instrument. 1.12.2's instruments are a fixed enum,
 * so the note is cancelled and played here, on the server, which sends both the sound and the particle.
 */
@Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID)
public final class Trumpet {

    private static final String[] SOUNDS = {"block.note_block.trumpet", "block.note_block.trumpet_exposed",
            "block.note_block.trumpet_weathered", "block.note_block.trumpet_oxidized"};

    private Trumpet() {
    }

    /** @return 0-3, the copper stage of a block that gives the trumpet, or -1 */
    public static int stage(Block below) {
        if (!Oxidation.isCopper(below)) {
            return -1;
        }
        Block plain = Oxidation.unwaxed(below);
        if (plain == null) {
            plain = below;
        }
        int stage = Oxidation.stage(plain);
        if (stage < 0) {
            return -1;
        }
        ResourceLocation first = Oxidation.first(plain).getRegistryName();
        return first != null && trumpetShape(first.getPath()) ? stage : -1;
    }

    /** 26.3's trumpet blocks, by the unaffected form's name: every mod in the family names them as vanilla does. */
    static boolean trumpetShape(String path) {
        return path.equals("copper_block") || path.equals("copper") || path.equals("cut_copper")
                || path.equals("chiseled_copper") || path.endsWith("cut_copper_slab") || path.equals("cut_copper_stairs");
    }

    @SubscribeEvent
    public static void onNote(NoteBlockEvent.Play event) {
        if (!BabiesContent.on()) {
            return;
        }
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        int stage = stage(world.getBlockState(pos.down()).getBlock());
        if (stage < 0) {
            return;
        }
        event.setCanceled(true);
        if (!(world instanceof WorldServer)) {
            return;
        }
        int note = event.getVanillaNoteId();
        SoundEvent sound = BabiesContent.sound(SOUNDS[stage]);
        if (sound != null) {
            float pitch = (float) Math.pow(2.0D, (note - 12) / 12.0D);
            world.playSound(null, pos, sound, SoundCategory.RECORDS, 3.0F, pitch);
        }
        // BlockNote.eventReceived's particle; count 0 sends the offset as its speed, the note's color.
        ((WorldServer) world).spawnParticle(EnumParticleTypes.NOTE, pos.getX() + 0.5D, pos.getY() + 1.2D,
                pos.getZ() + 0.5D, 0, note / 24.0D, 0.0D, 0.0D, 1.0D);
    }
}
