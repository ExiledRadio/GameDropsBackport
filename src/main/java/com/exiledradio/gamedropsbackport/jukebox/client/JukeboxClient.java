package com.exiledradio.gamedropsbackport.jukebox.client;

import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.jukebox.JukeboxContent;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/** The Flurry disc's item model. */
@Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID, value = Side.CLIENT)
public final class JukeboxClient {

    private JukeboxClient() {
    }

    @SubscribeEvent
    public static void models(ModelRegistryEvent event) {
        if (JukeboxContent.enabled() && JukeboxContent.flurry != null) {
            ModelLoader.setCustomModelResourceLocation(JukeboxContent.flurry, 0,
                    new ModelResourceLocation(GameDropsBackport.MOD_ID + ":music_disc_flurry", "inventory"));
        }
    }
}
