package com.exiledradio.gamedropsbackport.babies.client;

import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.babies.AgeLock;
import com.exiledradio.gamedropsbackport.babies.BabiesContent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Client side of stream 1: item models, the age lock's particles, and (with the switch on) the baby models. */
@Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID, value = Side.CLIENT)
public final class BabiesClient {

    static TextureAtlasSprite glint;

    /** Entity id -> ticks of particles left (26.3's {@code ageLockParticleTimer}, 40 ticks, one every 2). */
    private static final Map<Integer, Integer> TIMERS = new HashMap<Integer, Integer>();

    private BabiesClient() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        if (!BabiesContent.on()) {
            return;
        }
        ModelLoader.setCustomModelResourceLocation(BabiesContent.goldenDandelionItem, 0,
                new ModelResourceLocation(BabiesContent.goldenDandelionItem.getRegistryName(), "inventory"));
    }

    @SubscribeEvent
    public static void stitch(TextureStitchEvent.Pre event) {
        if (BabiesContent.on()) {
            glint = event.getMap().registerSprite(new ResourceLocation(GameDropsBackport.MOD_ID, "particle/glint"));
        }
    }

    /** From {@code AgeLockMessage}, on the network thread. */
    public static void ageLock(final int entityId, final boolean locked, final boolean particles) {
        final Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                if (mc.world == null) {
                    return;
                }
                Entity e = mc.world.getEntityByID(entityId);
                if (e != null) {
                    if (locked) {
                        e.getEntityData().setBoolean(AgeLock.LOCKED, true);
                    } else {
                        e.getEntityData().removeTag(AgeLock.LOCKED);
                    }
                }
                if (particles) {
                    TIMERS.put(entityId, AgeLock.COOLDOWN_TICKS);
                }
            }
        });
    }

    /** {@code AgeableMob.makeAgeLockedParticle}: every other tick, one particle above the mob. */
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || TIMERS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.world;
        if (world == null || mc.isGamePaused()) {
            if (world == null) {
                TIMERS.clear();
            }
            return;
        }
        for (Iterator<Map.Entry<Integer, Integer>> it = TIMERS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Integer> t = it.next();
            Entity e = world.getEntityByID(t.getKey());
            int left = t.getValue();
            if (e == null || left <= 0) {
                it.remove();
                continue;
            }
            if (left % 2 == 0 && glint != null) {
                boolean locked = AgeLock.isLocked(e);
                double x = e.posX + e.width * (2.0D * world.rand.nextDouble() - 1.0D);
                double y = e.posY + e.height * 0.2D * world.rand.nextDouble() + e.height + (locked ? 0.2D : 0.0D);
                double z = e.posZ + e.width * (2.0D * world.rand.nextDouble() - 1.0D);
                mc.effectRenderer.addEffect(new GrowthParticle(world, x, y, z, !locked));
            }
            t.setValue(left - 1);
        }
    }
}
