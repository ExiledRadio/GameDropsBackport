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
        try {
            dumpStates(server.getFile("selftest-states.txt"));
            dumpItems(server.getFile("selftest-items.txt"));
        } catch (Exception e) {
            check.fail("dump: " + e);
        }
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

    private static void dumpStates(File file) throws Exception {
        PrintWriter out = new PrintWriter(file, "UTF-8");
        for (Block block : ForgeRegistries.BLOCKS) {
            if (!GameDropsBackport.MOD_ID.equals(block.getRegistryName().getNamespace())) {
                continue;
            }
            for (IBlockState state : block.getBlockState().getValidStates()) {
                out.println(block.getRegistryName() + "	" + variant(state) + "	variants");
            }
        }
        out.close();
    }

    /** Vanilla's DefaultStateMapper string: properties sorted by name, name=value joined by commas. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static String variant(IBlockState state) {
        Map<String, String> sorted = new TreeMap<String, String>();
        for (Map.Entry<IProperty<?>, Comparable<?>> e : state.getProperties().entrySet()) {
            IProperty p = e.getKey();
            sorted.put(p.getName(), p.getName((Comparable) e.getValue()));
        }
        if (sorted.isEmpty()) {
            return "normal";
        }
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            if (b.length() > 0) {
                b.append(',');
            }
            b.append(e.getKey()).append('=').append(e.getValue());
        }
        return b.toString();
    }

    private static void dumpItems(File file) throws Exception {
        PrintWriter out = new PrintWriter(file, "UTF-8");
        for (Item item : ForgeRegistries.ITEMS) {
            if (GameDropsBackport.MOD_ID.equals(item.getRegistryName().getNamespace())) {
                out.println(item.getRegistryName() + "	0	" + item.getRegistryName().getPath() + "	"
                        + item.getTranslationKey(new ItemStack(item)) + ".name");
            }
        }
        out.close();
    }
}
