package com.exiledradio.gamedropsbackport.selftest;

import com.exiledradio.gamedropsbackport.GameDropsBackport;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Runs the headless self-tests on a dedicated dev server, then stops it.
 *
 * <pre>    ./gradlew runServer -Pselftest=all      (or a stream name: scaffold, ...)</pre>
 *
 * <p>Waits a second after the server starts, runs each requested test, logs one line per failure and
 * {@code SELFTEST DONE pass=N fail=M}, and writes {@code eclipse/selftest-report.txt}, and for
 * {@code tools/check_assets.py} {@code eclipse/selftest-states.txt} (every valid state of every block of this mod,
 * {@code registry<TAB>variant<TAB>variants}) and {@code eclipse/selftest-items.txt} (every item, its model, which
 * is its registry name, and its translation key).
 *
 * <p>This class and its tests are compiled in only with {@code -Pselftest} and never reach a release jar.
 */
public final class SelfTestRunner {

    /** Every stream's test. A stream adds its own line. */
    private static final SelfTest[] TESTS = {
            new ScaffoldSelfTest(),
            new SulfurSelfTest(),
            new SoftBlocksSelfTest(),
            new BabiesSelfTest(),
            new PoplarSelfTest(),
            new CampsSelfTest(),
            new JigsawDumpSelfTest(),
            new SulfurCubeSelfTest(),
            new ChangesSelfTest(),
            new com.exiledradio.gamedropsbackport.sulfurcaves.SulfurCavesSelfTest(),
            new LookFeelSelfTest(),
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
        STATES.clear();
        ITEMS.clear();
        for (SelfTest test : TESTS) {
            // The jigsaw dump builds structures far out and writes large files: only when asked for by name.
            boolean inAll = !(test instanceof JigsawDumpSelfTest);
            if ((inAll && this.wanted.contains("all")) || this.wanted.contains(test.name())) {
                check.section(test.name());
                try {
                    test.run(check);
                } catch (Throwable t) {
                    GameDropsBackport.LOGGER.error("SELFTEST {} threw", test.name(), t);
                    check.fail(test.name() + " threw " + t);
                }
            }
        }
        try {
            dumpStates(server.getFile("selftest-states.txt"));
            dumpItems(server.getFile("selftest-items.txt"));
        } catch (Exception e) {
            check.fail("dump: " + e);
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

    /** Lines the tests give for selftest-states.txt and selftest-items.txt, in the order given. */
    private static final List<String> STATES = new java.util.ArrayList<String>();
    private static final List<String> ITEMS = new java.util.ArrayList<String>();

    /**
     * A writer whose lines go into selftest-states.txt when it is closed. A test that knows its blocks better than
     * {@link #variant} does (multipart files, ignored properties) writes them here; the runner writes the file once,
     * after every test, adding plain lines for the blocks no test listed. One file, so no test overwrites another.
     */
    static PrintWriter statesOut() {
        return collector(STATES);
    }

    /** {@link #statesOut()} for selftest-items.txt: a stream's items with their real model names and metadata. */
    static PrintWriter itemsOut() {
        return collector(ITEMS);
    }

    private static PrintWriter collector(final List<String> into) {
        final java.io.StringWriter buffer = new java.io.StringWriter();
        return new PrintWriter(buffer) {
            @Override
            public void close() {
                super.close();
                for (String line : buffer.toString().split("\\r?\\n")) {
                    if (!line.isEmpty()) {
                        into.add(line);
                    }
                }
            }
        };
    }

    private static java.util.Set<String> firstColumn(List<String> lines) {
        java.util.Set<String> out = new java.util.HashSet<String>();
        for (String line : lines) {
            out.add(line.split("\t", 2)[0]);
        }
        return out;
    }

    /**
     * For tools/check_assets.py: the states the tests gave, then every valid state of every other block of this mod,
     * as its variant string.
     */
    private static void dumpStates(java.io.File file) throws Exception {
        PrintWriter out = new PrintWriter(file, "UTF-8");
        java.util.Set<String> listed = firstColumn(STATES);
        for (String line : STATES) {
            out.println(line);
        }
        for (net.minecraft.block.Block block : net.minecraftforge.fml.common.registry.ForgeRegistries.BLOCKS) {
            if (!GameDropsBackport.MOD_ID.equals(block.getRegistryName().getNamespace())
                    || listed.contains(block.getRegistryName().toString())) {
                continue;
            }
            for (net.minecraft.block.state.IBlockState state : block.getBlockState().getValidStates()) {
                out.println(block.getRegistryName() + "	" + variant(state) + "	variants");
            }
        }
        out.close();
    }

    /** Vanilla's DefaultStateMapper string: properties sorted by name, name=value joined by commas. */
    static String variant(net.minecraft.block.state.IBlockState state) {
        java.util.Map<String, String> sorted = new java.util.TreeMap<String, String>();
        for (java.util.Map.Entry<net.minecraft.block.properties.IProperty<?>, Comparable<?>> e : state.getProperties().entrySet()) {
            net.minecraft.block.properties.IProperty p = e.getKey();
            sorted.put(p.getName(), p.getName((Comparable) e.getValue()));
        }
        if (sorted.isEmpty()) {
            return "normal";
        }
        StringBuilder b = new StringBuilder();
        for (java.util.Map.Entry<String, String> e : sorted.entrySet()) {
            if (b.length() > 0) {
                b.append(',');
            }
            b.append(e.getKey()).append('=').append(e.getValue());
        }
        return b.toString();
    }

    /**
     * For tools/check_assets.py: the items the tests gave, then every other item of this mod with its registry path
     * as its model, and its translation key.
     */
    private static void dumpItems(java.io.File file) throws Exception {
        PrintWriter out = new PrintWriter(file, "UTF-8");
        java.util.Set<String> listed = firstColumn(ITEMS);
        for (String line : ITEMS) {
            out.println(line);
        }
        for (net.minecraft.item.Item item : net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS) {
            if (GameDropsBackport.MOD_ID.equals(item.getRegistryName().getNamespace())
                    && !listed.contains(item.getRegistryName().toString())) {
                net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(item);
                out.println(item.getRegistryName() + "	0	" + item.getRegistryName().getPath() + "	" + item.getTranslationKey(stack) + ".name");
            }
        }
        out.close();
    }
}
