package com.exiledradio.gamedropsbackport.selftest;

import com.exiledradio.gamedropsbackport.GameDropsBackport;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/**
 * Runs the headless self-tests on a dedicated dev server, then stops it.
 *
 * <pre>    ./gradlew runServer -Pselftest=all      (or a stream name: scaffold, ...)</pre>
 *
 * <p>Waits a second after the server starts, runs each requested test, logs one line per failure and
 * {@code SELFTEST DONE pass=N fail=M}, and writes {@code eclipse/selftest-report.txt}.
 *
 * <p>This class and its tests are compiled in only with {@code -Pselftest} and never reach a release jar.
 */
public final class SelfTestRunner {

    /** Every stream's test. A stream adds its own line. */
    private static final SelfTest[] TESTS = {
            new ScaffoldSelfTest(),
            new SoftBlocksSelfTest(),
    };

    private final List<String> wanted;
    private int ticks;

    private SelfTestRunner(List<String> wanted) {
        this.wanted = wanted;
    }

    public static void start(String which) {
        GameDropsBackport.LOGGER.info("SELFTEST requested: {}", which);
        MinecraftForge.EVENT_BUS.register(new SelfTestRunner(Arrays.asList(which.split(","))));
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++this.ticks != 20) {
            return;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        WorldServer world = server.getWorld(0);
        Check check = new Check(world);
        for (SelfTest test : TESTS) {
            if (this.wanted.contains("all") || this.wanted.contains(test.name())) {
                check.section(test.name());
                try {
                    test.run(check);
                } catch (Throwable t) {
                    GameDropsBackport.LOGGER.error("SELFTEST {} threw", test.name(), t);
                    check.fail(test.name() + " threw " + t);
                }
            }
        }
        GameDropsBackport.LOGGER.info("SELFTEST DONE pass={} fail={}", check.passed(), check.failed());
        try {
            PrintWriter out = new PrintWriter(server.getFile("selftest-report.txt"), "UTF-8");
            out.println("pass=" + check.passed() + " fail=" + check.failed());
            for (String line : check.failures()) {
                out.println(line);
            }
            out.close();
        } catch (Exception ignored) {
        }
        server.initiateShutdown();
    }
}
