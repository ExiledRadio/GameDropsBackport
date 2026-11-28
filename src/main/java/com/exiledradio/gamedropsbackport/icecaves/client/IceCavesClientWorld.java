package com.exiledradio.gamedropsbackport.icecaves.client;

import com.exiledradio.backportcore.presentation.client.GameMusic;
import com.exiledradio.backportcore.worldgen.UndergroundBiomes;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.icecaves.BiomeIceCaves;
import com.exiledradio.gamedropsbackport.icecaves.IceCavesContent;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * The ice caves on the client, read from Backport Core's underground biomes at the player: 26.4's music (the sulfur
 * caves' track, {@code music.overworld.sulfur_caves}, 12000-24000 ticks apart) where 1.12.2 would play the game music,
 * and its fog {@code #14559b} (under water too) at the brightness 1.12.2 gives its own fog, eased over a second.
 */
public final class IceCavesClientWorld {

    private static final float[] FOG = {(BiomeIceCaves.FOG >> 16 & 255) / 255F, (BiomeIceCaves.FOG >> 8 & 255) / 255F, (BiomeIceCaves.FOG & 255) / 255F};
    private static final float DEFAULT_LUMA = 0.7529412F + 0.84705883F + 1.0F, WATER_LUMA = 0.02F + 0.02F + 0.2F;
    private static float blend, lastBlend;
    static boolean inside;

    private IceCavesClientWorld() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new IceCavesClientWorld());
        GameMusic.add("26.4", "ice caves music", new GameMusic.Step() {
            @Override
            public void choose(GameMusic.Situation s) {
                SoundEvent music = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(GameDropsBackport.MOD_ID, "music.overworld.sulfur_caves"));
                if (inside && music != null && s.vanilla == MusicTicker.MusicType.GAME) {
                    s.choose(new GameMusic.Track(music, 12000, 24000, false));
                }
            }
        });
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null || mc.world.provider.getDimension() != 0) {
            inside = false;
        } else if (mc.player.ticksExisted % 10 == 0) {
            Biome b = UndergroundBiomes.biomeAt(mc.world, new BlockPos(mc.player.posX, mc.player.posY + mc.player.getEyeHeight(), mc.player.posZ));
            inside = b != null && b == IceCavesContent.biome;
        }
        lastBlend = blend;
        blend = Math.max(0F, Math.min(1F, blend + (inside ? 0.05F : -0.05F)));
    }

    @SubscribeEvent
    public void fog(EntityViewRenderEvent.FogColors event) {
        float b = lastBlend + (blend - lastBlend) * (float) event.getRenderPartialTicks();
        if (b <= 0F || Minecraft.getMinecraft().world == null) {
            return;
        }
        boolean water = event.getState().getMaterial() == Material.WATER;
        float luma = Math.min(1F, (event.getRed() + event.getGreen() + event.getBlue()) / (water ? WATER_LUMA : DEFAULT_LUMA));
        event.setRed(event.getRed() + (FOG[0] * luma - event.getRed()) * b);
        event.setGreen(event.getGreen() + (FOG[1] * luma - event.getGreen()) * b);
        event.setBlue(event.getBlue() + (FOG[2] * luma - event.getBlue()) * b);
    }
}
