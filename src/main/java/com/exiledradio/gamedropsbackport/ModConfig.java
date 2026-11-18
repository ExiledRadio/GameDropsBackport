package com.exiledradio.gamedropsbackport;

import com.exiledradio.backportcore.settings.Setting;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Configuration: one category per stream (docs/PLAN.md), each with an {@code enabled} switch.
 *
 * <h2>Rules for streams</h2>
 * <ul>
 *   <li>A stream edits only inside its own category class, between its markers.</li>
 *   <li>Category classes extend {@code Object} directly. Forge 1.12 crashes at startup ("Unknown type")
 *       on a category class that extends another one.</li>
 *   <li>{@code enabled} is read once, during registration, and needs a restart: switching a stream off
 *       means its blocks and items are never registered, so a world that has them loses them.</li>
 *   <li>A changed default never reaches an installed game (Forge keeps the value in the file), so a
 *       default that must change gets a new key.</li>
 *   <li>Every field has an explicit {@code @Config.LangKey}; its name and {@code .tooltip} lines are in
 *       en_us.lang (family Rule 4; tooltips from {@code tools/lang/build_langs.py config}).</li>
 *   <li>Each category's icon names the stream's own item first and a vanilla stand-in after the {@code |}, so
 *       the settings screen shows something before the stream is built.</li>
 * </ul>
 */
@Config(modid = GameDropsBackport.MOD_ID, name = GameDropsBackport.MOD_ID)
public final class ModConfig {

    @Setting.Feature(icon = "gamedropsbackport:golden_dandelion|minecraft:yellow_flower")
    @Config.LangKey("config.gamedropsbackport.babies")
    @Config.Comment("The golden dandelion, the new babies, name tag recipes and the adult sound variants.")
    public static final Babies babies = new Babies();

    @Setting.Feature(icon = "gamedropsbackport:sulfur|minecraft:gunpowder")
    @Config.LangKey("config.gamedropsbackport.sulfur")
    @Config.Comment("Sulfur and cinnabar blocks, potent sulfur, sulfur spikes and the music disc Bounce.")
    public static final Sulfur sulfur = new Sulfur();

    @Setting.Feature(icon = "gamedropsbackport:sulfur_cube_spawn_egg|minecraft:slime_ball")
    @Config.LangKey("config.gamedropsbackport.sulfurCube")
    @Config.Comment("The sulfur cube.")
    public static final SulfurCube sulfurCube = new SulfurCube();

    @Setting.Feature(icon = "gamedropsbackport:potent_sulfur|minecraft:magma")
    @Config.LangKey("config.gamedropsbackport.sulfurCaves")
    @Config.Comment("The sulfur caves and sulfur springs.")
    public static final SulfurCaves sulfurCaves = new SulfurCaves();

    @Setting.Feature(icon = "gamedropsbackport:poplar_sapling|minecraft:sapling@2")
    @Config.LangKey("config.gamedropsbackport.poplar")
    @Config.Comment("Poplar wood, the red shrub, shelf mushrooms and the dappled forest.")
    public static final Poplar poplar = new Poplar();

    @Setting.Feature(icon = "gamedropsbackport:straw_bed|minecraft:hay_block")
    @Config.LangKey("config.gamedropsbackport.softBlocks")
    @Config.Comment("Wool and concrete stairs and slabs, cushions and straw beds.")
    public static final SoftBlocks softBlocks = new SoftBlocks();

    @Setting.Feature(icon = "minecraft:map")
    @Config.LangKey("config.gamedropsbackport.camps")
    @Config.Comment("Abandoned camps and explorer maps.")
    public static final Camps camps = new Camps();

    // ------------------------------------------------------------------------------------------

    public static final class Babies {
        @Config.RequiresMcRestart
        @Config.Comment("Off, nothing is registered, and a world that has them loses them.")
        @Config.LangKey("config.gamedropsbackport.babies.enabled")
        public boolean enabled = true;

        // --- Babies fields ---
        // --- end Babies ---
    }

    public static final class Sulfur {
        @Config.RequiresMcRestart
        @Config.Comment("Off, nothing is registered, and a world that has them loses them.")
        @Config.LangKey("config.gamedropsbackport.sulfur.enabled")
        public boolean enabled = true;

        // --- Sulfur fields ---
        // --- end Sulfur ---
    }

    public static final class SulfurCube {
        @Config.RequiresMcRestart
        @Config.Comment("Off, nothing is registered, and a world that has them loses them.")
        @Config.LangKey("config.gamedropsbackport.sulfurCube.enabled")
        public boolean enabled = true;

        // --- SulfurCube fields ---
        // --- end SulfurCube ---
    }

    public static final class SulfurCaves {
        @Config.RequiresMcRestart
        @Setting.NewChunks
        @Config.Comment("Sulfur caves and sulfur springs generate in newly generated chunks.")
        @Config.LangKey("config.gamedropsbackport.sulfurCaves.enabled")
        public boolean enabled = true;

        // --- SulfurCaves fields ---
        // --- end SulfurCaves ---
    }

    public static final class Poplar {
        @Config.RequiresMcRestart
        @Config.Comment("Off, nothing is registered, and a world that has them loses them.")
        @Config.LangKey("config.gamedropsbackport.poplar.enabled")
        public boolean enabled = true;

        // --- Poplar fields ---
        // --- end Poplar ---
    }

    public static final class SoftBlocks {
        @Config.RequiresMcRestart
        @Config.Comment("Off, nothing is registered, and a world that has them loses them.")
        @Config.LangKey("config.gamedropsbackport.softBlocks.enabled")
        public boolean enabled = true;

        // --- SoftBlocks fields ---
        // --- end SoftBlocks ---
    }

    public static final class Camps {
        @Config.RequiresMcRestart
        @Setting.NewChunks
        @Config.Comment("Abandoned camps generate in newly generated chunks.")
        @Config.LangKey("config.gamedropsbackport.camps.enabled")
        public boolean enabled = true;

        // --- Camps fields ---
        // --- end Camps ---
    }

    // ------------------------------------------------------------------------------------------

    /** Saves edits made in the in-game config screen. */
    @Mod.EventBusSubscriber(modid = GameDropsBackport.MOD_ID)
    public static final class EventHandler {
        private EventHandler() {
        }

        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (GameDropsBackport.MOD_ID.equals(event.getModID())) {
                ConfigManager.sync(GameDropsBackport.MOD_ID, Config.Type.INSTANCE);
            }
        }
    }

    private ModConfig() {
    }
}
