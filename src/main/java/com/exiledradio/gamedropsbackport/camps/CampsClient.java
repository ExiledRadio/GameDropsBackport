package com.exiledradio.gamedropsbackport.camps;

import com.exiledradio.gamedropsbackport.GameDropsBackport;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/** The explorer map items' models. Client only. */
@Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID, value = Side.CLIENT)
public final class CampsClient {

    private CampsClient() {
    }

    @SubscribeEvent
    public static void models(ModelRegistryEvent event) {
        if (!CampsContent.enabled()) {
            return;
        }
        for (Item item : ExplorerMaps.items()) {
            // A map's damage is its map id: every damage draws the one model.
            ModelResourceLocation model = new ModelResourceLocation(item.getRegistryName(), "inventory");
            ModelLoader.setCustomMeshDefinition(item, stack -> model);
            ModelBakery.registerItemVariants(item, model);
        }
    }
}
