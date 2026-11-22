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

    @Setting.Feature(icon = "gamedropsbackport:sulfur_cube_bucket|minecraft:slime_ball")
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

    @Setting.Feature(icon = "minecraft:painting")
    @Config.LangKey("config.gamedropsbackport.lookAndFeel")
    @Config.Comment("26.x's menus and screens. Backport Core's Look & Feel page has the same switches.")
    public static final LookAndFeel lookAndFeel = new LookAndFeel();
    @Setting.Feature(icon = "minecraft:clock")
    @Config.LangKey("config.gamedropsbackport.changes")
    @Config.Comment("26.x's changes to commands, mobs, blocks and items 1.12.2 already has.")
    public static final Changes changes = new Changes();

    // ------------------------------------------------------------------------------------------

    public static final class Babies {
        @Config.RequiresMcRestart
        @Config.Comment("Off, nothing is registered, and a world that has them loses them.")
        @Config.LangKey("config.gamedropsbackport.babies.enabled")
        public boolean enabled = true;

        // --- Babies fields ---
        @Setting.Look("backports")
        @Config.Comment("26.1's baby models and textures for 1.12.2's babies and the other backports' (Trails & Tales' camels, snifflets and armadillos). Off, babies are shrunken adults.")
        @Config.LangKey("config.gamedropsbackport.babies.babyModels")
        public boolean babyModels = true;
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
        @Config.RequiresMcRestart
        @Setting.NewChunks
        @Config.Comment("Dappled forests generate in newly generated chunks. Off, the biome is not registered.")
        @Config.LangKey("config.gamedropsbackport.poplar.generateBiome")
        public boolean generateBiome = true;
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

    public static final class LookAndFeel {
        @Config.RequiresMcRestart
        @Config.Comment("Off, this mod adds none of 26.x's menus and screens, and an older backport's stay.")
        @Config.LangKey("config.gamedropsbackport.lookAndFeel.enabled")
        public boolean enabled = true;

        // --- LookAndFeel fields ---
        @Config.RequiresMcRestart
        @Setting.Look("title")
        @Config.Comment("26.3's title screen panorama: the dappled forest with an abandoned camp.")
        @Config.LangKey("config.gamedropsbackport.lookAndFeel.panorama")
        public boolean panorama = true;

        @Config.RequiresMcRestart
        @Setting.Look("title")
        @Config.Comment("26.1 to 26.3's splash text changes.")
        @Config.LangKey("config.gamedropsbackport.lookAndFeel.splashes")
        public boolean splashes = true;

        @Config.RequiresMcRestart
        @Setting.Look("music")
        @Config.Comment("26.2's menu music: fingerspit's Ebb, Home, Memories, Nightly and Shores in place of 1.21.6's five.")
        @Config.LangKey("config.gamedropsbackport.lookAndFeel.menuMusic")
        public boolean menuMusic = true;

        @Config.RequiresMcRestart
        @Setting.Look("menus")
        @Config.Comment("26.2's pause menu: Report Bugs and Send Feedback as one row of icon buttons.")
        @Config.LangKey("config.gamedropsbackport.lookAndFeel.pauseMenu")
        public boolean pauseMenu = true;

        @Setting.Look("menus")
        @Config.Comment("26.3's World Options screen on the pause menu, in place of Open to LAN; Difficulty leaves the Options screen.")
        @Config.LangKey("config.gamedropsbackport.lookAndFeel.worldOptions")
        public boolean worldOptions = true;
        // --- end LookAndFeel ---
    }

    public static final class Changes {
        @Config.Comment("Off, every change below is off and 1.12.2's behavior is back.")
        @Config.LangKey("config.gamedropsbackport.changes.enabled")
        public boolean enabled = true;

        // --- Changes fields ---
        @Config.Comment({"/time runs on world clocks: set day, noon, night and midnight go forward to the next one; pause,",
                "resume and rate; of <clock>; query time and timelines; 26.1's messages (26.1)."})
        @Config.LangKey("config.gamedropsbackport.changes.timeCommand")
        public boolean timeCommand = true;

        @Config.Comment("/gamerule answers with 26.x's messages, and setting a rule to its current value is an error (26.3).")
        @Config.LangKey("config.gamedropsbackport.changes.gameRuleMessages")
        public boolean gameRuleMessages = true;

        @Config.Comment("Team colors take only their lowercase names with underscores, such as dark_purple (26.2).")
        @Config.LangKey("config.gamedropsbackport.changes.teamColorNames")
        public boolean teamColorNames = true;

        @Config.Comment("Beds send what falls on them back up at 75% of its speed instead of 66% (26.2).")
        @Config.LangKey("config.gamedropsbackport.changes.bedBounce")
        public boolean bedBounce = true;

        @Config.RequiresMcRestart
        @Config.Comment("Mushroom stew is made from a bowl and any two mushrooms (26.3).")
        @Config.LangKey("config.gamedropsbackport.changes.mushroomStew")
        public boolean mushroomStew = true;

        @Config.Comment({"With a shield in the off hand, using a hoe or shovel raises the shield; sneak to till or make paths",
                "(26.3)."})
        @Config.LangKey("config.gamedropsbackport.changes.shieldBeforeTools")
        public boolean shieldBeforeTools = true;

        @Config.Comment("Endermen riding something take damage from projectiles and never teleport (26.3).")
        @Config.LangKey("config.gamedropsbackport.changes.endermenRiding")
        public boolean endermenRiding = true;

        @Config.Comment({"Endermen never teleport onto bedrock or into fire, cactus, magma and the like; shulkers never onto",
                "bedrock (26.3)."})
        @Config.LangKey("config.gamedropsbackport.changes.teleportTargets")
        public boolean teleportTargets = true;

        @Config.Comment("Persistent mobs (named ones, for example) stop wandering when no player is within 32 blocks (26.3).")
        @Config.LangKey("config.gamedropsbackport.changes.persistentMobsIdle")
        public boolean persistentMobsIdle = true;

        @Config.Comment("Leader zombies spawn with their raised maximum health filled (26.1).")
        @Config.LangKey("config.gamedropsbackport.changes.leaderZombieHealth")
        public boolean leaderZombieHealth = true;

        @Config.Comment("Witches throw their potions more slowly within 2 blocks, so they no longer miss up close (26.1).")
        @Config.LangKey("config.gamedropsbackport.changes.witchCloseThrows")
        public boolean witchCloseThrows = true;

        @Config.Comment("On Peaceful, dispensers keep the spawn eggs of monsters instead of using them up (26.2).")
        @Config.LangKey("config.gamedropsbackport.changes.peacefulSpawnEggs")
        public boolean peacefulSpawnEggs = true;

        @Config.Comment("Unbreaking works on the helmet a zombie or skeleton wears against the sun (26.3).")
        @Config.LangKey("config.gamedropsbackport.changes.sunDamageEnchantments")
        public boolean sunDamageEnchantments = true;

        @Config.Comment("Spectators go through nether portals, end portals and end gateways (26.3).")
        @Config.LangKey("config.gamedropsbackport.changes.spectatorPortals")
        public boolean spectatorPortals = true;

        @Config.Comment("Ocelots spawn in jungles on Peaceful too: the monster spawn pass runs, its monsters refused (26.2).")
        @Config.LangKey("config.gamedropsbackport.changes.ocelotsPeaceful")
        public boolean ocelotsPeaceful = true;

        @Config.Comment({"Chat and commands have their own spam counters, set by chat-spam-threshold-seconds and",
                "command-spam-threshold-seconds in server.properties (10 each; 0 never kicks). Open to LAN never kicks (26.2)."})
        @Config.LangKey("config.gamedropsbackport.changes.spamThresholds")
        public boolean spamThresholds = true;

        @Config.Comment({"The host changing their own game mode with /gamemode (or F3 + N) also changes the world's default",
                "game mode (26.2; 26.3 took this back out, so it is off by default)."})
        @Config.LangKey("config.gamedropsbackport.changes.hostGameModeDefault")
        public boolean hostGameModeDefault = false;

        @Config.Comment({"/advancement grant and revoke take several players and answer as 26.3 does: how many players",
                "and advancements, and an error only when nothing changed (26.2, 26.3)."})
        @Config.LangKey("config.gamedropsbackport.changes.advancementCounts")
        public boolean advancementCounts = true;

        @Config.Comment({"/scoreboard teams join and leave answer as 26.3's /team does: the members that joined or left,",
                "and no error for those already out of a team (26.3)."})
        @Config.LangKey("config.gamedropsbackport.changes.teamJoinCounts")
        public boolean teamJoinCounts = true;

        @Config.Comment("/unpublish closes a world opened to LAN: other players leave and it stops being listed (26.2).")
        @Config.LangKey("config.gamedropsbackport.changes.unpublish")
        public boolean unpublish = true;
        // --- end Changes ---
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
