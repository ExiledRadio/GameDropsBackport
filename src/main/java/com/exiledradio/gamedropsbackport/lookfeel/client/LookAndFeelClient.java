package com.exiledradio.gamedropsbackport.lookfeel.client;

import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.ModConfig;
import com.exiledradio.gamedropsbackport.lookfeel.WorldOptionsRules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Iterator;

/**
 * 26.3's menus, on Forge's GUI events (no patch): the pause menu's Open to LAN slot (button 7, which Backport Core
 * places) becomes World Options..., and the in-world Options screen loses Difficulty and its lock (26.1 moved them to
 * World Options).
 */
@SideOnly(Side.CLIENT)
public final class LookAndFeelClient {

    /** 1.12.2's ids: GuiIngameMenu's Open to LAN, GuiOptions' Difficulty and its lock. */
    static final int OPEN_TO_LAN = 7;
    static final int DIFFICULTY = 108;
    static final int LOCK = 109;

    private LookAndFeelClient() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new LookAndFeelClient());
        // 26.2's /unpublish closes the integrated server's LAN as World Options' LAN switch does.
        com.exiledradio.gamedropsbackport.changes.UnpublishCommand.unpublisher = server ->
                server instanceof net.minecraft.server.integrated.IntegratedServer
                        && Lan.unpublish((net.minecraft.server.integrated.IntegratedServer) server);
        // 26.2's in-game music: fingerspit's tracks join music.game.
        if (ModConfig.lookAndFeel.enabled && ModConfig.lookAndFeel.gameMusic) {
            com.exiledradio.gamedropsbackport.music.client.GameMusicPack.install();
        }
        if ("client".equals(System.getProperty(GameDropsBackport.MOD_ID + ".selftest"))) {
            try {
                Class.forName("com.exiledradio.gamedropsbackport.selftest.ClientSelfTest").getMethod("register").invoke(null);
            } catch (ReflectiveOperationException e) {
                GameDropsBackport.LOGGER.error("CLIENT SELFTEST DONE pass=0 fail=1: the test classes are not in this build", e);
            }
        }
    }

    @SubscribeEvent
    public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!WorldOptionsRules.active() || Minecraft.getMinecraft().world == null) {
            return;
        }
        if (event.getGui().getClass() == GuiIngameMenu.class) {
            for (GuiButton b : event.getButtonList()) {
                if (b.id == OPEN_TO_LAN) {
                    b.displayString = I18n.format("options.worldOptions.button");
                    b.enabled = true;
                }
            }
        } else if (event.getGui().getClass() == GuiOptions.class) {
            for (Iterator<GuiButton> it = event.getButtonList().iterator(); it.hasNext(); ) {
                int id = it.next().id;
                if (id == DIFFICULTY || id == LOCK) {
                    it.remove();
                }
            }
        }
    }

    @SubscribeEvent
    public void onAction(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (event.getGui().getClass() == GuiIngameMenu.class && event.getButton().id == OPEN_TO_LAN && WorldOptionsRules.active()
                && Minecraft.getMinecraft().world != null) {
            event.setCanceled(true);
            Minecraft.getMinecraft().displayGuiScreen(new WorldOptionsScreen(event.getGui()));
        }
    }
}
