package com.exiledradio.gamedropsbackport.selftest;

import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.backportcore.presentation.client.MenuScreens;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.lookfeel.client.WorldOptionsScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

/**
 * {@code runClient -Pselftest=client}: the look and feel on a real client. The title screen (26.3's panorama), then
 * a superflat creative world: the pause menu (26.2's icon row, World Options...), the in-world Options screen
 * (no Difficulty), and World Options itself (allow commands, a personal game mode, difficulty, open to LAN on a
 * chosen port). Photos go to {@code eclipse/gdclient/screenshots}; the result is logged as
 * {@code CLIENT SELFTEST DONE pass= fail=}.
 *
 * <p>Unattended, as Tricky Trials' is: {@code -Dfml.noGrab=true} and {@code inGameHasFocus} kept false, so the user's
 * mouse never turns the player.
 */
public final class ClientSelfTest {

    private static final String WORLD = "gamedrops-client-test";
    private static final String FLAT = "3;minecraft:bedrock,2*minecraft:dirt,minecraft:grass;1;";

    private final List<IntSupplier> steps = new ArrayList<IntSupplier>();
    private int step;
    private int wait;
    private int ticks;
    private int pass;
    private int fail;
    private int phase;
    private File shots;
    private int lanPort;

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ClientSelfTest());
        GameDropsBackport.LOGGER.info("CLIENT SELFTEST registered");
    }

    void ok(boolean condition, String what) {
        if (condition) {
            this.pass++;
            GameDropsBackport.LOGGER.info("CLIENT SELFTEST ok {}", what);
        } else {
            this.fail++;
            GameDropsBackport.LOGGER.error("CLIENT SELFTEST FAIL {}", what);
        }
    }

    private static void unfocus() {
        Minecraft.getMinecraft().inGameHasFocus = false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.getGui() != null) {
            unfocus();
        }
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent event) {
        unfocus();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        unfocus();
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        try {
            if (this.phase == 0 && mc.currentScreen instanceof GuiMainMenu && ++this.ticks >= 100) {
                this.shots = new File(mc.gameDir, "gdclient");
                FileUtils.deleteQuietly(new File(this.shots, "screenshots"));
                this.shots.mkdirs();
                ok(Presentation.panoramas().get(Presentation.panoramas().size() - 1).faces[0].getNamespace().equals(GameDropsBackport.MOD_ID),
                        "title screen: our panorama is the newest");
                shot("title.png");
                this.phase = 1;
                this.ticks = 0;
                FileUtils.deleteQuietly(new File(new File(mc.gameDir, "saves"), WORLD));
                mc.gameSettings.pauseOnLostFocus = false;
                System.clearProperty(GameDropsBackport.MOD_ID + ".selftest");
                WorldSettings settings = new WorldSettings(1L, GameType.CREATIVE, false, false, WorldType.FLAT);
                settings.setGeneratorOptions(FLAT);
                mc.launchIntegratedServer(WORLD, WORLD, settings);
            } else if (this.phase == 1 && ++this.ticks > 2400) {
                ok(false, "the test world never opened");
                finish(mc);
            } else if (this.phase == 1 && mc.world != null && mc.player != null && mc.getIntegratedServer() != null && this.ticks >= 60) {
                this.phase = 2;
                scenes(mc);
            } else if (this.phase == 2) {
                if (this.wait > 0) {
                    this.wait--;
                } else if (this.step < this.steps.size()) {
                    this.wait = this.steps.get(this.step++).getAsInt();
                } else {
                    finish(mc);
                }
            }
        } catch (Throwable t) {
            GameDropsBackport.LOGGER.error("CLIENT SELFTEST threw", t);
            this.fail++;
            finish(mc);
        }
    }

    private void scenes(Minecraft mc) {
        // Blocks, items, mobs and their screens first: World Options below switches the player to adventure.
        new ClientScenes(this, mc).add(this.steps);
        // 26.3's off-map player marker (Core's P43): a map 2000 blocks east, held, shows the pointed marker facing west.
        this.steps.add(() -> {
            net.minecraft.entity.player.EntityPlayerMP sp = mc.getIntegratedServer().getPlayerList().getPlayerByUUID(mc.player.getUniqueID());
            mc.getIntegratedServer().addScheduledTask(() -> {
                net.minecraft.item.ItemStack map = net.minecraft.item.ItemMap.setupNewMap(sp.world, sp.posX + 2000, sp.posZ, (byte) 0, true, true);
                sp.inventory.setInventorySlotContents(sp.inventory.currentItem, map);
                sp.inventory.setInventorySlotContents(40, net.minecraft.item.ItemStack.EMPTY);
            });
            mc.player.rotationYaw = 90.0F;
            mc.player.rotationPitch = 70.0F;
            return 60;
        });
        // A loaded server can be seconds behind: wait (up to 30 s) for the map and its marker to reach the client.
        this.steps.add(() -> {
            if (!offLimitsShown(mc)) {
                this.step--; // this step again next tick
                return 1;
            }
            return 20;
        });
        this.steps.add(() -> {
            mc.player.rotationYaw = 90.0F;
            mc.player.rotationPitch = 70.0F;
            net.minecraft.item.ItemStack held = mc.player.getHeldItemMainhand();
            net.minecraft.world.storage.MapData data = held.getItem() instanceof net.minecraft.item.ItemMap
                    ? ((net.minecraft.item.ItemMap) held.getItem()).getMapData(held, mc.world) : null;
            net.minecraft.world.storage.MapDecoration me = null;
            if (data != null) {
                for (net.minecraft.world.storage.MapDecoration d : data.mapDecorations.values()) {
                    me = d.getType() == net.minecraft.world.storage.MapDecoration.Type.PLAYER_OFF_LIMITS ? d : me;
                }
            }
            // Yaw 90 (west): (90 + 8) * 16 / 360 = 4; 1.12.2 gives 0.
            ok(me != null && (me.getRotation() & 15) == 4, "map: the off-limits marker faces west on the client, " + me
                    + (me == null ? "" : " rotation " + me.getRotation()));
            ok(com.exiledradio.backportcore.map.MapFacing.spriteOf(net.minecraft.world.storage.MapDecoration.Type.PLAYER_OFF_LIMITS) != null,
                    "map: 26.3's pointed off-limits sprite");
            shot("map_off_limits.png");
            ok(mc.getTextureManager().getTexture(new net.minecraft.util.ResourceLocation("backportcore",
                    "textures/map/decorations/player_off_limits.png")) != null, "map: the pointed sprite was drawn (texture loaded)");
            return 5;
        });
        this.steps.add(() -> {
            mc.player.rotationPitch = 0.0F;
            net.minecraft.entity.player.EntityPlayerMP sp = mc.getIntegratedServer().getPlayerList().getPlayerByUUID(mc.player.getUniqueID());
            mc.getIntegratedServer().addScheduledTask(() -> sp.inventory.setInventorySlotContents(sp.inventory.currentItem, net.minecraft.item.ItemStack.EMPTY));
            return 5;
        });
        // The pause menu: World Options... in Open to LAN's place, the 26.2 icon row under Advancements and Statistics.
        this.steps.add(() -> {
            mc.displayGuiScreen(new GuiIngameMenu());
            return 20;
        });
        this.steps.add(() -> {
            GuiButton lan = button(mc.currentScreen, 7);
            GuiButton back = button(mc.currentScreen, 4);
            ok(lan != null && "World Options...".equals(lan.displayString) && lan.enabled, "pause: World Options... in Open to LAN's place");
            ok(MenuScreens.ICONS.size() == 2, "pause: two icon buttons, " + MenuScreens.ICONS.size());
            ok(!MenuScreens.ICONS.isEmpty() && back != null && MenuScreens.ICONS.get(0).y == back.y + 48
                    && MenuScreens.ICONS.get(0).width == 20, "pause: the icon row is the third row, 20 wide");
            GuiButton options = button(mc.currentScreen, 0);
            ok(options != null && lan != null && options.y == lan.y && lan.x == mc.currentScreen.width / 2 + 4, "pause: Options... and World Options... share a row");
            shot("pause.png");
            mc.displayGuiScreen(new GuiOptions(mc.currentScreen, mc.gameSettings));
            return 20;
        });
        this.steps.add(() -> {
            ok(button(mc.currentScreen, 108) == null && button(mc.currentScreen, 109) == null, "options: no Difficulty or lock in a world");
            shot("options.png");
            mc.displayGuiScreen(new WorldOptionsScreen(new GuiIngameMenu()));
            return 20;
        });
        this.steps.add(() -> {
            WorldOptionsScreen s = (WorldOptionsScreen) mc.currentScreen;
            List<String> labels = s.labels();
            GameDropsBackport.LOGGER.info("CLIENT SELFTEST world options: {}", labels);
            ok(labels.contains("Default Game Mode: Creative (off)") && labels.contains("Personal Game Mode: Creative (off)"),
                    "world options: game modes off without commands");
            ok(labels.contains("Allow Cheats: OFF") && labels.contains("LAN: OFF"), "world options: commands and LAN off");
            ok(labels.contains("Command Access: OFF (off)") && labels.contains("Force Game Mode: ON (off)"), "world options: guests' rows off without LAN");
            ok(labels.contains("Apply Changes (off)"), "world options: nothing to apply yet");
            shot("world_options.png");
            s.press("Allow Cheats");
            s.press("Personal Game Mode");
            s.press("Difficulty");
            return 3;
        });
        this.steps.add(() -> {
            WorldOptionsScreen s = (WorldOptionsScreen) mc.currentScreen;
            List<String> labels = s.labels();
            ok(labels.contains("Personal Game Mode: Adventure") && labels.contains("Allow Cheats: ON"), "world options: game modes on with commands");
            ok(labels.contains("Apply Changes"), "world options: changes to apply");
            shot("world_options_changed.png");
            s.press("Apply Changes");
            return 40;
        });
        this.steps.add(() -> {
            ok(WorldOptionsScreen.applied == 1, "world options: applied once");
            ok(mc.playerController.getCurrentGameType() == GameType.ADVENTURE, "world options: personal game mode now adventure, "
                    + mc.playerController.getCurrentGameType());
            ok(mc.getIntegratedServer().worlds[0].getWorldInfo().areCommandsAllowed(), "world options: commands allowed");
            ok(mc.getIntegratedServer().worlds[0].getWorldInfo().getDifficulty() == EnumDifficulty.HARD, "world options: difficulty hard, "
                    + mc.getIntegratedServer().worlds[0].getWorldInfo().getDifficulty());
            ok(mc.getIntegratedServer().worlds[0].getWorldInfo().getGameType() == GameType.CREATIVE, "world options: default game mode kept");
            mc.displayGuiScreen(new WorldOptionsScreen(new GuiIngameMenu()));
            return 5;
        });
        this.steps.add(() -> {
            WorldOptionsScreen s = (WorldOptionsScreen) mc.currentScreen;
            s.press("LAN");
            s.typePort("80");
            List<String> labels = s.labels();
            ok(labels.contains("Apply Changes (off)"), "world options: port 80 is refused");
            this.lanPort = freePort();
            s.typePort(String.valueOf(this.lanPort));
            labels = s.labels();
            ok(labels.contains("Command Access: OFF") && labels.contains("Apply Changes"), "world options: LAN on, guests' rows on");
            s.press("Command Access");
            s.scrollToEnd();
            return 3;
        });
        this.steps.add(() -> {
            shot("world_options_lan.png");
            ((WorldOptionsScreen) mc.currentScreen).press("Apply Changes");
            return 40;
        });
        this.steps.add(() -> {
            ok(mc.getIntegratedServer().getPublic(), "world options: open to LAN");
            boolean everyone = ReflectionHelper.getPrivateValue(net.minecraft.server.management.PlayerList.class,
                    mc.getIntegratedServer().getPlayerList(), "commandsAllowedForAll", "field_72407_n");
            ok(everyone, "world options: guests can use commands");
            mc.displayGuiScreen(new WorldOptionsScreen(new GuiIngameMenu()));
            return 5;
        });
        this.steps.add(() -> {
            List<String> labels = ((WorldOptionsScreen) mc.currentScreen).labels();
            ok(labels.contains("LAN: ON (off)") && labels.contains("Command Access: ON"), "world options: LAN stays on once open");
            mc.displayGuiScreen(null);
            return 5;
        });
    }

    private int mapWaits;

    /** True once the held map shows an off-limits marker, or after 600 tries. */
    private boolean offLimitsShown(Minecraft mc) {
        mc.player.rotationYaw = 90.0F;
        mc.player.rotationPitch = 70.0F;
        if (++this.mapWaits > 600) {
            return true;
        }
        net.minecraft.item.ItemStack held = mc.player.getHeldItemMainhand();
        net.minecraft.world.storage.MapData data = held.getItem() instanceof net.minecraft.item.ItemMap
                ? ((net.minecraft.item.ItemMap) held.getItem()).getMapData(held, mc.world) : null;
        if (data != null) {
            for (net.minecraft.world.storage.MapDecoration d : data.mapDecorations.values()) {
                if (d.getType() == net.minecraft.world.storage.MapDecoration.Type.PLAYER_OFF_LIMITS) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int freePort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            int port = socket.getLocalPort();
            return port >= 1024 ? port : 25929;
        } catch (java.io.IOException e) {
            return 25929;
        }
    }

    private static GuiButton button(GuiScreen screen, int id) {
        List<GuiButton> list = ReflectionHelper.getPrivateValue(GuiScreen.class, screen, "buttonList", "field_146292_n");
        for (GuiButton b : list) {
            if (b.id == id) {
                return b;
            }
        }
        return null;
    }

    void shot(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(this.shots, name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        File f = new File(new File(this.shots, "screenshots"), name);
        ok(f.isFile() && f.length() > 1000, "saved " + name);
    }

    private void finish(Minecraft mc) {
        this.phase = 3;
        GameDropsBackport.LOGGER.info("CLIENT SELFTEST DONE pass={} fail={}", this.pass, this.fail);
        mc.shutdown();
    }
}
