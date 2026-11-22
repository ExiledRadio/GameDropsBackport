package com.exiledradio.gamedropsbackport.lookfeel;

import com.exiledradio.backportcore.presentation.Contribution;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.ModConfig;

import net.minecraft.util.ResourceLocation;

/**
 * Stream 8: the look and feel of 26.1 to 26.3, the changes those drops made beyond their content (docs/PLAN.md).
 * Backport Core owns every piece only one version can have at a time and shows the newest installed version's; this
 * mod registers 26.1, 26.2 and 26.3, so its pieces win over Tricky Trials' 1.21.11.
 *
 * <ul>
 *   <li>26.1: the version only (its one splash change names a Java version, which Core leaves out);</li>
 *   <li>26.2: two splashes, fingerspit's five menu tracks in place of 1.21.6's five, and the pause menu's icon
 *       row (Core lays it out from the two sprites here);</li>
 *   <li>26.3: the dappled forest panorama and overlay, the curly-quote splash fix, vanilla text as 26.3 words it,
 *       and World Options... on the pause menu ({@code lookfeel.client}).</li>
 * </ul>
 */
public final class LookAndFeel {

    private static final String ID = GameDropsBackport.MOD_ID;

    /** 1.21.6's menu tracks, by vanilla path: 26.2 replaced them on the menu. */
    public static final String[] MENU_1_21_6 = {
            "music/game/below_and_above", "music/game/broken_clocks", "music/game/fireflies", "music/game/lilypad",
            "music/game/os_piano",
    };

    /** 26.2's menu tracks (fingerspit), as 26.3's {@code music.menu} lists them: weight 2, volume 0.4. */
    public static final String[] MENU_26_2 = {"ebb", "home", "memories", "nightly", "shores"};

    public static final int MENU_WEIGHT = 2;
    public static final float MENU_VOLUME = 0.4F;

    private static boolean registered;

    private LookAndFeel() {
    }

    /** Pre-init, both sides: hands Core 26.x's pieces. Also registers the versions, so this mod is the newest. */
    public static void preInit(boolean client) {
        if (registered) {
            return;
        }
        registered = true;
        if (client) {
            // Reached by name, so the dedicated server never loads a client class.
            try {
                Class.forName("com.exiledradio.gamedropsbackport.lookfeel.client.LookAndFeelClient").getMethod("register").invoke(null);
            } catch (ReflectiveOperationException e) {
                GameDropsBackport.LOGGER.error("Look and feel: the client part failed to load", e);
            }
        }
        // The versions are registered whatever the switches say: they tell Core (and its worldgen, like every other
        // backport's) which game this mod brings. The switches decide only the pieces.
        Presentation.version("26.1");
        Contribution v262 = Presentation.version("26.2");
        Contribution v263 = Presentation.version("26.3");
        ModConfig.LookAndFeel cfg = ModConfig.lookAndFeel;
        if (!cfg.enabled) {
            return;
        }
        if (cfg.splashes) {
            // 26.1's one change ("Java 16 + 1 + 4 * 2 = 25!") names a Java version, which Core leaves out on 1.12.2.
            v262.addSplashes(texts("splashes_26.2_added.txt"));
            v263.addSplashes(texts("splashes_26.3_added.txt")).removeSplashes(texts("splashes_26.3_removed.txt"));
        }
        if (cfg.menuMusic) {
            for (String track : MENU_1_21_6) {
                v262.removeMenuMusic(track);
            }
            for (String track : MENU_26_2) {
                v262.addMenuMusic(ID + ":music/game/" + track, MENU_WEIGHT, MENU_VOLUME);
            }
        }
        if (cfg.pauseMenu) {
            v262.replaceAsset("textures/gui/sprites/pause_menu/bug.png", vanilla("textures/gui/sprites/pause_menu/bug.png"))
                    .replaceAsset("textures/gui/sprites/pause_menu/social_interactions.png",
                            vanilla("textures/gui/sprites/pause_menu/social_interactions.png"));
        }
        if (cfg.panorama) {
            v263.panorama(ID + ":textures/gui/title/background/panorama_")
                    .panoramaOverlay(new ResourceLocation(ID, "textures/gui/title/background/panorama_overlay.png"));
        }
        v263.vanillaText(new ResourceLocation(ID, "vanilla_text"));
    }

    private static ResourceLocation texts(String file) {
        return new ResourceLocation(ID, "texts/" + file);
    }

    /** @return our copy of one of vanilla's 26.x assets ({@code assets/<modid>/vanilla/<path>}) */
    private static ResourceLocation vanilla(String path) {
        return new ResourceLocation(ID, "vanilla/" + path);
    }
}
