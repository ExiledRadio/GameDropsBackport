package com.exiledradio.gamedropsbackport;

import com.exiledradio.backportcore.settings.BackportSettings;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Game Drops Backport: Minecraft's 26.x game drops (26.1 Tiny Takeover, 26.2, 26.3 and later 26.x) for 1.12.2.
 *
 * <h2>Layout</h2>
 * One package per stream of content (see {@code docs/PLAN.md}): each registers its own blocks, items,
 * entities and sounds from its own {@code @Mod.EventBusSubscriber} class, behind its own config
 * category's {@code enabled} switch. This class only calls each stream's lifecycle hooks, one line per stream.
 *
 * <h2>Behavior source</h2>
 * Current Java Edition as minecraft.wiki describes it; each 26.x release's client jar for assets and names.
 */
@Mod(
        modid = GameDropsBackport.MOD_ID,
        name = GameDropsBackport.MOD_NAME,
        version = GameDropsBackport.VERSION,
        acceptedMinecraftVersions = "[1.12.2]",
        dependencies = "required-after:backportcore@[1.4.3,)"
)
public final class GameDropsBackport {

    public static final String MOD_ID = "gamedropsbackport";
    /** From gradle.properties' mod_name, the one place the display name is written. */
    public static final String MOD_NAME = "@MOD_NAME@";
    public static final String VERSION = "@VERSION@";

    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Mod.Instance(MOD_ID)
    public static GameDropsBackport instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // Core's settings screen lists the family members it knows by itself; this mod is newer than Core 1.4.0's
        // list, so it registers its page.
        BackportSettings.register(MOD_ID, ModConfig.class);
        com.exiledradio.gamedropsbackport.network.NetworkHandler.init();
        com.exiledradio.gamedropsbackport.sulfur.SulfurContent.preInit();
        com.exiledradio.gamedropsbackport.sulfurcaves.SulfurCavesContent.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        com.exiledradio.gamedropsbackport.softblocks.SoftBlocks.init();
        com.exiledradio.gamedropsbackport.sulfurcaves.SulfurCavesContent.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        com.exiledradio.gamedropsbackport.sulfurcaves.SulfurCavesContent.postInit();
    }

    /**
     * Runs the release-jar patch check when asked ({@code tools/release/boot-server.sh}), then starts the headless
     * self-test when a test build asks for one ({@code -Pselftest=<stream>}).
     *
     * <p>The self-test is reached by name, so a release jar - which has no test classes - never links against it.
     */
    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        PatchCheck.runIfAsked();
        String selftest = System.getProperty(MOD_ID + ".selftest");
        if (selftest == null || selftest.isEmpty()) {
            return;
        }
        try {
            Class.forName("com.exiledradio.gamedropsbackport.selftest.SelfTestRunner")
                    .getMethod("start", String.class).invoke(null, selftest);
        } catch (ReflectiveOperationException e) {
            LOGGER.error("SELFTEST DONE pass=0 fail=1: '{}' requested but the test classes are not in this build.", selftest, e);
            // A test run must always end, or it holds its port and its log files.
            net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance().initiateShutdown();
        }
    }
}
