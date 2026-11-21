package com.exiledradio.gamedropsbackport.softblocks.client;

import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.softblocks.EntityCushion;
import com.exiledradio.gamedropsbackport.softblocks.SoftBlocks;
import com.exiledradio.gamedropsbackport.softblocks.SoftColors;

import net.minecraft.block.BlockBed;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/** Item models per color, the straw bed's state mapper (occupied is not drawn) and the cushion's renderer. */
@Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID, value = Side.CLIENT)
public final class SoftBlocksClient {

    private SoftBlocksClient() {
    }

    @SubscribeEvent
    public static void onModels(ModelRegistryEvent event) {
        if (!SoftBlocks.enabled()) {
            return;
        }
        for (String mat : SoftBlocks.MATERIALS) {
            for (EnumDyeColor c : EnumDyeColor.values()) {
                String n = SoftColors.modern(c) + "_" + mat;
                model(Item.getItemFromBlock(SoftBlocks.stairs(mat, c)), c.getMetadata(), n + "_stairs");
                model(Item.getItemFromBlock(SoftBlocks.slab(mat, c)), c.getMetadata(), n + "_slab");
            }
        }
        for (EnumDyeColor c : EnumDyeColor.values()) {
            model(SoftBlocks.cushion, c.getMetadata(), SoftColors.modern(c) + "_cushion");
        }
        model(Item.getItemFromBlock(SoftBlocks.strawBed), 0, "straw_bed");
        ModelLoader.setCustomStateMapper(SoftBlocks.strawBed, new StateMap.Builder().ignore(BlockBed.OCCUPIED).build());
        RenderingRegistry.registerEntityRenderingHandler(EntityCushion.class, RenderCushion::new);
    }

    private static void model(Item item, int meta, String name) {
        ModelLoader.setCustomModelResourceLocation(item, meta,
                new ModelResourceLocation(GameDropsBackport.MOD_ID + ":" + name, "inventory"));
    }
}
