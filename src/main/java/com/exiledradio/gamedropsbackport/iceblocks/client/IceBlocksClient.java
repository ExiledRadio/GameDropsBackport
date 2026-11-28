package com.exiledradio.gamedropsbackport.iceblocks.client;

import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.iceblocks.Reg;
import com.exiledradio.gamedropsbackport.iceblocks.IceBlocksContent;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Map;

/** The stream's client setup: item models and the slabs' state mapper. */
@Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID, value = Side.CLIENT)
public final class IceBlocksClient {

    private IceBlocksClient() {
    }

    @SubscribeEvent
    public static void models(ModelRegistryEvent event) {
        if (!IceBlocksContent.enabled()) {
            return;
        }
        for (Map.Entry<Item, String> e : Reg.itemModels().entrySet()) {
            ModelLoader.setCustomModelResourceLocation(e.getKey(), 0,
                    new ModelResourceLocation(GameDropsBackport.MOD_ID + ":" + e.getValue(), "inventory"));
        }
        for (Map.Entry<Block, IProperty<?>[]> e : Reg.ignoredProperties().entrySet()) {
            ModelLoader.setCustomStateMapper(e.getKey(), new StateMap.Builder().ignore(e.getValue()).build());
        }
    }
}
