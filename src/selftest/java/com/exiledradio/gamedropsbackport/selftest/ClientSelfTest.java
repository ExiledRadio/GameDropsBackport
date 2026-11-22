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
                gameMusic(mc);
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

    /** 26.2's game music: the sound handler's music.game (and so the creative music) holds fingerspit's five. */
    private void gameMusic(Minecraft mc) throws Exception {
        ok(com.exiledradio.gamedropsbackport.music.client.GameMusicPack.installed, "game music: the built-in pack is installed");
        net.minecraft.client.audio.SoundEventAccessor game = mc.getSoundHandler().getAccessor(new net.minecraft.util.ResourceLocation("music.game"));
        java.util.Map<String, net.minecraft.client.audio.Sound> sounds = new java.util.TreeMap<String, net.minecraft.client.audio.Sound>();
        int count = 0;
        if (game != null) {
            for (java.lang.reflect.Field f : net.minecraft.client.audio.SoundEventAccessor.class.getDeclaredFields()) {
                if (java.util.List.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    for (Object o : (java.util.List<?>) f.get(game)) {
                        count++;
                        if (o instanceof net.minecraft.client.audio.Sound) {
                            net.minecraft.client.audio.Sound s = (net.minecraft.client.audio.Sound) o;
                            sounds.put(s.getSoundLocation().toString(), s);
                        }
                    }
                }
            }
        }
        boolean all = true;
        for (String track : com.exiledradio.gamedropsbackport.lookfeel.LookAndFeel.MENU_26_2) {
            net.minecraft.client.audio.Sound s = sounds.get(GameDropsBackport.MOD_ID + ":music/game/" + track);
            all &= s != null && s.getWeight() == 2 && s.getVolume() == 0.4F && s.isStreaming();
        }
        ok(all, "game music: music.game holds fingerspit's five at weight 2, volume 0.4, streamed");
        if (mc.getSoundHandler().getAccessor(new net.minecraft.util.ResourceLocation("ambient.cave")) != null) {
            ok(sounds.containsKey("minecraft:music/game/calm1"), "game music: 1.12.2's own tracks stay (" + count + " entries)");
        } else {
            // An offline dev run has no 1.12.2 sounds.json (the asset download failed): nothing of vanilla's to keep.
            GameDropsBackport.LOGGER.info("CLIENT SELFTEST note: no 1.12.2 sounds in this run; music.game has {} entries", count);
        }
        // The biome lists are their owners'; with them installed, they hold 26.2's tracks.
        if (net.minecraftforge.fml.common.Loader.isModLoaded("trailsandtalesbackport")) {
            java.util.Set<String> forest = soundNames(mc, "trailsandtalesbackport:music.overworld.forest");
            ok(forest.contains(GameDropsBackport.MOD_ID + ":music/game/memories") && forest.contains(GameDropsBackport.MOD_ID + ":music/game/shores"),
                    "biome music: Trails & Tales' forest list has Memories and Shores: " + forest.size() + " sounds");
            java.util.Set<String> cherry = soundNames(mc, "trailsandtalesbackport:music.overworld.cherry_grove");
            ok(cherry.contains(GameDropsBackport.MOD_ID + ":music/game/home") && cherry.contains(GameDropsBackport.MOD_ID + ":music/game/nightly"),
                    "biome music: the cherry grove's has Home and Nightly");
        }
        if (net.minecraftforge.fml.common.Loader.isModLoaded("cavesandcliffsbackport")) {
            java.util.Set<String> lush = soundNames(mc, "cavesandcliffsbackport:music.overworld.lush_caves");
            ok(lush.contains(GameDropsBackport.MOD_ID + ":music/game/shores") && lush.contains("cavesandcliffsbackport:music/game/left_to_bloom"),
                    "biome music: Caves & Cliffs' lush caves list keeps 1.18's and has Shores: " + lush);
            java.util.Set<String> frozen = soundNames(mc, "cavesandcliffsbackport:music.overworld.frozen_peaks");
            ok(frozen.contains(GameDropsBackport.MOD_ID + ":music/game/ebb") && frozen.contains(GameDropsBackport.MOD_ID + ":music/game/home"),
                    "biome music: the frozen peaks' has Ebb and Home");
        }
    }

    /** @return the sound files an event's accessor lists (one level: event references are not followed) */
    private static java.util.Set<String> soundNames(Minecraft mc, String event) throws Exception {
        java.util.Set<String> out = new java.util.TreeSet<String>();
        net.minecraft.client.audio.SoundEventAccessor accessor = mc.getSoundHandler().getAccessor(new net.minecraft.util.ResourceLocation(event));
        if (accessor == null) {
            return out;
        }
        for (java.lang.reflect.Field f : net.minecraft.client.audio.SoundEventAccessor.class.getDeclaredFields()) {
            if (java.util.List.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                for (Object o : (java.util.List<?>) f.get(accessor)) {
                    if (o instanceof net.minecraft.client.audio.Sound) {
                        out.add(((net.minecraft.client.audio.Sound) o).getSoundLocation().toString());
                    }
                }
            }
        }
        return out;
    }

    private void scenes(Minecraft mc) {
        // Blocks, items, mobs and their screens first: World Options below switches the player to adventure.
        new ClientScenes(this, mc).add(this.steps);
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
