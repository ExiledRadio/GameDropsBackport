package com.exiledradio.gamedropsbackport.sulfurcaves.client;

import com.exiledradio.backportcore.presentation.client.GameMusic;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.sulfurcaves.SulfurCaves;
import com.exiledradio.gamedropsbackport.sulfurcaves.SulfurCavesContent;
import com.exiledradio.gamedropsbackport.sulfurcaves.SulfurNet;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The sulfur caves on the client: 26.2's music ({@code music.overworld.sulfur_caves}, 12000-24000 ticks apart, as
 * every biome's) where 1.12.2 would play the game music, and its fog color {@code #8cb831} (under water
 * {@code #17543c}) at the brightness 1.12.2 gives its own fog there, eased in and out over a second.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID, value = Side.CLIENT)
public final class SulfurCavesClient {

    private static final float[] FOG = {0x8C / 255F, 0xB8 / 255F, 0x31 / 255F};
    private static final float[] WATER_FOG = {0x17 / 255F, 0x54 / 255F, 0x3C / 255F};
    /** 1.12.2's overworld fog at full daylight (WorldProvider.getFogColor), and its underwater fog. */
    private static final float DEFAULT_LUMA = 0.7529412F + 0.84705883F + 1.0F, WATER_LUMA = 0.02F + 0.02F + 0.2F;

    private static float blend, lastBlend;

    private SulfurCavesClient() {
    }

    /** From pre-init. */
    public static void init() {
        GameMusic.add("26.2", "sulfur caves music", new GameMusic.Step() {
            @Override
            public void choose(GameMusic.Situation s) {
                if (SulfurNet.inside && SulfurCavesContent.music != null && s.vanilla == MusicTicker.MusicType.GAME) {
                    s.choose(new GameMusic.Track(SulfurCavesContent.music, 12000, 24000, false));
                }
            }
        });
    }

    @SubscribeEvent
    public static void models(ModelRegistryEvent event) {
        if (SulfurCavesContent.tuffCopy != null) {
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(SulfurCavesContent.tuffCopy), 0,
                    new ModelResourceLocation(GameDropsBackport.MOD_ID + ":tuff", "inventory"));
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        lastBlend = blend;
        blend = Math.max(0F, Math.min(1F, blend + (SulfurNet.inside ? 0.05F : -0.05F)));
    }

    @SubscribeEvent
    public static void disconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        SulfurNet.inside = false;
    }

    @SubscribeEvent
    public static void fog(EntityViewRenderEvent.FogColors event) {
        if (!SulfurCaves.enabled()) {
            return;
        }
        float b = lastBlend + (blend - lastBlend) * (float) event.getRenderPartialTicks();
        if (b <= 0F || Minecraft.getMinecraft().world == null || Minecraft.getMinecraft().world.provider.getDimension() != 0) {
            return;
        }
        boolean water = event.getState().getMaterial() == Material.WATER;
        float[] target = water ? WATER_FOG : FOG;
        float luma = (event.getRed() + event.getGreen() + event.getBlue()) / (water ? WATER_LUMA : DEFAULT_LUMA);
        luma = Math.min(1F, luma);
        event.setRed(event.getRed() + (target[0] * luma - event.getRed()) * b);
        event.setGreen(event.getGreen() + (target[1] * luma - event.getGreen()) * b);
        event.setBlue(event.getBlue() + (target[2] * luma - event.getBlue()) * b);
    }
}
