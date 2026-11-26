package com.exiledradio.gamedropsbackport.selftest;

import com.exiledradio.backportcore.settings.BackportSettings;
import com.exiledradio.backportcore.settings.SettingsModel;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.exiledradio.gamedropsbackport.ModConfig;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.fml.common.Loader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * The skeleton's own checks: the mod and Backport Core are loaded, the settings screen has this mod's page with one
 * group per config category, and every config lang key (name and tooltip) is in en_us.lang.
 */
final class ScaffoldSelfTest implements SelfTest {

    @Override
    public String name() {
        return "scaffold";
    }

    @Override
    public void run(Check check) throws Exception {
        check.ok(Loader.isModLoaded(GameDropsBackport.MOD_ID), "the mod is loaded");
        check.ok(Loader.isModLoaded("backportcore"), "Backport Core is loaded");
        check.eq("1.1.0", Loader.instance().getIndexedModList().get(GameDropsBackport.MOD_ID).getVersion(), "mod version");

        Set<String> lang = langKeys();
        check.ok(lang.size() > 0, "en_us.lang has keys");

        Set<String> categoryKeys = new HashSet<String>();
        int settings = 0;
        for (Field category : ModConfig.class.getFields()) {
            if (!Modifier.isStatic(category.getModifiers())) {
                continue;
            }
            String key = category.getAnnotation(Config.LangKey.class).value();
            categoryKeys.add(key);
            check.ok(lang.contains(key), "lang key " + key);
            check.ok(lang.contains(key + ".tooltip"), "lang key " + key + ".tooltip");
            for (Field setting : category.getType().getFields()) {
                Config.LangKey k = setting.getAnnotation(Config.LangKey.class);
                if (!check.ok(k != null, "LangKey on " + category.getName() + "." + setting.getName())) {
                    continue;
                }
                settings++;
                check.ok(lang.contains(k.value()), "lang key " + k.value());
                check.ok(lang.contains(k.value() + ".tooltip"), "lang key " + k.value() + ".tooltip");
            }
            Object value = category.get(null);
            check.eq(Boolean.TRUE, value.getClass().getField("enabled").get(value), category.getName() + ".enabled default");
        }
        check.eq(13, categoryKeys.size(), "config categories (one per planned stream)");

        SettingsModel.Page page = BackportSettings.page(GameDropsBackport.MOD_ID);
        if (check.ok(page != null, "Backport Core's settings screen has this mod's page")) {
            Set<String> groups = new HashSet<String>();
            for (SettingsModel.Group g : page.groups) {
                groups.add(g.langKey);
                if (categoryKeys.contains(g.langKey)) {
                    check.ok(!g.icon.isEmpty(), "icon on page group " + g.langKey);
                }
            }
            for (String key : categoryKeys) {
                check.ok(groups.contains(key), "page group " + key);
            }
            check.ok(page.entries.size() >= 7, "page settings (an enabled switch per category, plus streams' own): " + page.entries.size());
            check.eq(settings, page.entries.size(), "page settings (one per config field)");
        }
    }

    private static Set<String> langKeys() throws Exception {
        Set<String> keys = new HashSet<String>();
        InputStream in = ScaffoldSelfTest.class.getResourceAsStream(
                "/assets/" + GameDropsBackport.MOD_ID + "/lang/en_us.lang");
        if (in == null) {
            return keys;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                int eq = line.indexOf('=');
                if (!line.startsWith("#") && eq > 0) {
                    keys.add(line.substring(0, eq));
                }
            }
        } finally {
            reader.close();
        }
        return keys;
    }
}
