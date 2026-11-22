package com.exiledradio.gamedropsbackport.selftest;

import com.exiledradio.backportcore.presentation.Contribution;
import com.exiledradio.backportcore.presentation.GameVersion;
import com.exiledradio.backportcore.presentation.MenuMusic;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.backportcore.presentation.Splashes;
import com.exiledradio.backportcore.presentation.VanillaRevisions;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.ModConfig;
import com.exiledradio.gamedropsbackport.lookfeel.LookAndFeel;
import com.exiledradio.gamedropsbackport.lookfeel.WorldOptionsRules;

import net.minecraft.util.ResourceLocation;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Stream 8: the versions this mod registers, every piece it hands Core (and that Core resolves them as 26.3's), the
 * files in the jar, and {@link WorldOptionsRules}.
 */
final class LookFeelSelfTest implements SelfTest {

    private static final String ID = GameDropsBackport.MOD_ID;

    @Override
    public String name() {
        return "lookfeel";
    }

    @Override
    public void run(Check check) throws Exception {
        ModConfig.LookAndFeel cfg = ModConfig.lookAndFeel;
        check.ok(cfg.enabled && cfg.panorama && cfg.splashes && cfg.menuMusic && cfg.gameMusic && cfg.pauseMenu && cfg.worldOptions, "every switch on by default");
        check.ok(Presentation.installedNewest().equals(GameVersion.parse("26.3")), "newest installed is 26.3, " + Presentation.installedNewest());
        check.ok(Presentation.isAtLeast("26.3"), "a dedicated server's look is the installed newest");
        for (String v : new String[]{"26.1", "26.2", "26.3"}) {
            Contribution c = Presentation.contribution(v);
            check.ok(c != null && c.owners().contains(ID), v + " is ours");
        }

        // Panorama: ours is the newest, six square faces of one size; the overlay is ours.
        List<Presentation.Panorama> panoramas = Presentation.panoramas();
        Presentation.Panorama newest = panoramas.isEmpty() ? null : panoramas.get(panoramas.size() - 1);
        check.ok(newest != null && newest.version.equals(GameVersion.parse("26.3")) && newest.faces.length == 6
                && newest.faces[0].getNamespace().equals(ID), "the 26.3 panorama is the newest");
        int size = -1;
        boolean square = true;
        for (int i = 0; i < 6; i++) {
            BufferedImage img = image("textures/gui/title/background/panorama_" + i + ".png");
            square &= img != null && img.getWidth() == img.getHeight() && (size < 0 || img.getWidth() == size);
            size = img == null ? size : img.getWidth();
        }
        check.ok(square && size >= 256, "six square panorama faces, " + size);
        check.ok(new ResourceLocation(ID, "textures/gui/title/background/panorama_overlay.png").equals(Presentation.panoramaOverlay()),
                "the overlay is ours: " + Presentation.panoramaOverlay());

        // Splashes: Core's pool with our files.
        List<String> pool = Splashes.pool(Arrays.asList("Don’t touch your face!", "Kept!"), Presentation.installed(), LookFeelSelfTest::lines);
        check.ok(pool.containsAll(Arrays.asList("Music by fingerspit!", "Don't touch your face!",
                "Prepare, but don't hoard!", "Kept!")), "the 26.x splashes are added: " + pool);
        check.ok(!pool.contains("Don’t touch your face!"), "and the curly-quoted ones gone");
        check.ok(Splashes.isAboutTheRuntime("Java 16 + 1 + 4 * 2 = 25!") && !pool.contains("Now on Vulkan 1.2!"),
                "26.1's Java splash and 26.2's Vulkan one are ones Core leaves out on 1.12.2");

        // Menu music: 1.21.6's five removed by path, whoever plays them; fingerspit's five at 0.4.
        List<MenuMusic.Track> base = new ArrayList<MenuMusic.Track>();
        base.add(new MenuMusic.Track("minecraft:music/menu/menu1", 1));
        Contribution older = Presentation.detached("1.21.6");
        older.addMenuMusic("trickytrialsbackport:music/game/os_piano", 1, 0.4F);
        List<Contribution> withOlder = new ArrayList<Contribution>();
        withOlder.add(older);
        withOlder.addAll(Presentation.installed());
        List<String> names = new ArrayList<String>();
        boolean volumes = true;
        for (MenuMusic.Track t : MenuMusic.pool(base, withOlder)) {
            names.add(t.soundName);
            volumes &= t.soundName.startsWith("minecraft:") || t.volume == LookAndFeel.MENU_VOLUME && t.weight == LookAndFeel.MENU_WEIGHT;
        }
        List<String> expected = new ArrayList<String>();
        expected.add("minecraft:music/menu/menu1");
        for (String track : LookAndFeel.MENU_26_2) {
            expected.add(ID + ":music/game/" + track);
        }
        Collections.sort(names);
        Collections.sort(expected);
        check.ok(names.equals(expected), "menu pool is menu1 and fingerspit's five: " + names);
        check.ok(volumes, "the new tracks at weight 2, volume 0.4");
        for (String track : LookAndFeel.MENU_26_2) {
            check.ok(resource("sounds/music/game/" + track + ".ogg"), "track " + track + " in the jar");
        }

        // 26.2's game music: the built-in pack's music.game adds the five, weight 2, volume 0.4 (26.3's sounds.json).
        InputStream gameIn = LookFeelSelfTest.class.getResourceAsStream("/" + ID + "_packs/game_music/assets/minecraft/sounds.json");
        if (check.ok(gameIn != null, "the game music pack is in the jar")) {
            com.google.gson.JsonObject pack = new com.google.gson.JsonParser().parse(
                    new InputStreamReader(gameIn, StandardCharsets.UTF_8)).getAsJsonObject();
            check.ok(pack.entrySet().size() == 1 && pack.has("music.game") && !pack.getAsJsonObject("music.game").has("replace"),
                    "the pack adds to music.game only, replacing nothing");
            List<String> game = new ArrayList<String>();
            boolean entries = true;
            for (com.google.gson.JsonElement e : pack.getAsJsonObject("music.game").getAsJsonArray("sounds")) {
                com.google.gson.JsonObject o = e.getAsJsonObject();
                game.add(o.get("name").getAsString());
                entries &= o.get("weight").getAsInt() == 2 && o.get("volume").getAsFloat() == 0.4F && o.get("stream").getAsBoolean();
            }
            List<String> want = new ArrayList<String>();
            for (String track : LookAndFeel.MENU_26_2) {
                want.add(ID + ":music/game/" + track);
            }
            check.ok(game.equals(want), "music.game gains fingerspit's five: " + game);
            check.ok(entries, "each streamed, weight 2, volume 0.4");
        }

        // Pause menu: the two sprites (15 by 15) reach Core; the grid's numbers.
        for (String sprite : new String[]{"bug", "social_interactions"}) {
            String path = "textures/gui/sprites/pause_menu/" + sprite + ".png";
            check.ok(Presentation.asset(path) != null, "Core serves " + path);
            BufferedImage img = image("vanilla/" + path);
            // Drawn at 15 by 15 whatever its size (SpriteIconButton.sprite(id, 15, 15)): bug is 13, social_interactions 20.
            check.ok(img != null && img.getWidth() == img.getHeight(), sprite + " is square");
        }
        check.ok(VanillaRevisions.pauseIconRow(), "the 26.2 pause row is on");
        check.ok(VanillaRevisions.pauseGridTop(240, 5) == 18, "pause grid top at 240 high");

        // Vanilla text: 26.3's English, 18 languages.
        List<String> en = lines(new ResourceLocation(ID, "vanilla_text/en_us.lang"));
        check.ok(en.contains("advancements.story.iron_tools.title=Isn't It Iron Pick?")
                && en.contains("build.tooHigh=The height limit for building is %s"), "vanilla text in 26.3's words");
        check.ok(Presentation.vanillaTexts().contains(new ResourceLocation(ID, "vanilla_text")), "vanilla text registered");
        String[] codes = {"de_de", "es_ar", "es_es", "es_mx", "es_uy", "es_ve", "fr_fr", "it_it", "ja_jp", "ko_kr", "nl_nl",
                "pl_pl", "pt_br", "ru_ru", "tr_tr", "uk_ua", "zh_cn", "zh_tw"};
        int langs = 0;
        for (String code : codes) {
            langs += resource("vanilla_text/" + code + ".lang") ? 1 : 0;
        }
        check.ok(langs == codes.length, "vanilla text languages: " + langs);

        // World Options' rules (26.3's WorldOptionsScreen).
        check.ok(WorldOptionsRules.gameModeActive(false, true) && !WorldOptionsRules.gameModeActive(true, true)
                && !WorldOptionsRules.gameModeActive(false, false), "game modes need commands, never hardcore");
        check.ok(WorldOptionsRules.gameModeTooltip(false, false, "t").endsWith("disabled.operator.tooltip")
                && WorldOptionsRules.gameModeTooltip(true, true, "t").endsWith("game_mode.disabled.tooltip")
                && "t".equals(WorldOptionsRules.gameModeTooltip(false, true, "t")), "game mode tooltips");
        check.ok(WorldOptionsRules.gameRulesActive(false, true, true) && !WorldOptionsRules.gameRulesActive(true, true, true)
                && !WorldOptionsRules.gameRulesActive(false, false, true) && !WorldOptionsRules.gameRulesActive(false, true, false)
                && WorldOptionsRules.gameRulesTooltip(false, true, true) == null
                && "editGamerule.inGame.disabled.hardcore.tooltip".equals(WorldOptionsRules.gameRulesTooltip(true, true, true))
                && "editGamerule.inGame.disabled.tooltip".equals(WorldOptionsRules.gameRulesTooltip(false, false, true)),
                "game rules: commands and Core's server needed, never hardcore");
        check.ok(!WorldOptionsRules.commandAccessActive(false, true) && !WorldOptionsRules.commandAccessActive(true, false)
                && WorldOptionsRules.commandAccessActive(true, true) && !WorldOptionsRules.commandAccessValue(false, true, true),
                "command access needs LAN and commands");
        check.ok(WorldOptionsRules.forceValue(false, false, false, false) && WorldOptionsRules.forceValue(true, true, false, false)
                && !WorldOptionsRules.forceValue(false, true, true, true) && !WorldOptionsRules.forceActive(false, true, true)
                && WorldOptionsRules.forceActive(false, true, false), "force game mode: on without LAN or in hardcore, off with command access");
        check.ok(WorldOptionsRules.forceTooltip(false, true, false, true).endsWith("on.tooltip")
                && WorldOptionsRules.forceTooltip(false, false, false, true).endsWith("off.scope.tooltip"), "force tooltips");
        check.ok(WorldOptionsRules.difficultyBlocked(true, false, false) == null
                && WorldOptionsRules.difficultyBlocked(true, true, false).endsWith("locked.tooltip")
                && WorldOptionsRules.difficultyBlocked(false, false, false).endsWith("operator.tooltip"), "difficulty states");
        check.ok(WorldOptionsRules.portError("", -1, p -> false) == null && "lanServer.port.invalid".equals(WorldOptionsRules.portError("80", -1, p -> true))
                && "lanServer.port.invalid".equals(WorldOptionsRules.portError("abc", -1, p -> true))
                && "lanServer.port.unavailable".equals(WorldOptionsRules.portError("25565", -1, p -> false))
                && WorldOptionsRules.portError("25565", 25565, p -> false) == null
                && WorldOptionsRules.portError("65535", -1, p -> true) == null, "port parsing");
    }

    private static boolean resource(String path) {
        InputStream in = LookFeelSelfTest.class.getResourceAsStream("/assets/" + ID + "/" + path);
        if (in == null) {
            return false;
        }
        try {
            in.close();
        } catch (java.io.IOException ignored) {
            // nothing to do
        }
        return true;
    }

    private static BufferedImage image(String path) {
        try (InputStream in = LookFeelSelfTest.class.getResourceAsStream("/assets/" + ID + "/" + path)) {
            return in == null ? null : ImageIO.read(in);
        } catch (java.io.IOException e) {
            return null;
        }
    }

    static List<String> lines(ResourceLocation file) {
        List<String> out = new ArrayList<String>();
        InputStream in = LookFeelSelfTest.class.getResourceAsStream("/assets/" + file.getNamespace() + "/" + file.getPath());
        if (in == null) {
            return out;
        }
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    out.add(line.trim());
                }
            }
        } catch (java.io.IOException ignored) {
            // an unreadable file reads as empty
        }
        return out;
    }
}
