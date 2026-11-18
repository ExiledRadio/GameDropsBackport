package com.exiledradio.gamedropsbackport;

import com.exiledradio.gamedropsbackport.asm.GameDropsCore;

import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The release-jar check behind {@code tools/release/boot-server.sh}: with {@code -Dgamedropsbackport.loadPatched=true}
 * it loads and initializes every class this mod's coremod patches, once the server has started, so the JVM verifies
 * each one, and logs {@code PATCHCHECK ok <n>} or one {@code PATCHCHECK FAIL} line per class.
 *
 * <p>Why: Mojang's classes that Forge does not patch keep ProGuard's stack-map frames, which mark a local TOP once
 * vanilla stops reading it. A patch that loads such a local is a VerifyError only in the real game, and only once the
 * class loads: the dev runs recompile Minecraft with javac and never show it, and some targets (a command, an item)
 * may never load during a short boot. Without the property this does nothing.
 *
 * <p>Each transformer lists its targets in a {@code TARGETS} array and adds it to {@link GameDropsCore#serverTargets}
 * or {@link GameDropsCore#clientTargets}. With no patches yet, the check passes with 0 classes.
 */
public final class PatchCheck {

    static final String PROPERTY = GameDropsBackport.MOD_ID + ".loadPatched";

    private PatchCheck() {
    }

    /** Every class a transformer of this mod patches on the given side (OptiFine's classes left out). */
    public static List<String> targets(boolean client) {
        List<String> out = new ArrayList<String>(Arrays.asList(GameDropsCore.serverTargets()));
        if (client) {
            out.addAll(Arrays.asList(GameDropsCore.clientTargets()));
        }
        return out;
    }

    /** Runs the check when the property asks for it. */
    static void runIfAsked() {
        if (!Boolean.getBoolean(PROPERTY)) {
            return;
        }
        boolean client = FMLCommonHandler.instance().getSide().isClient();
        List<String> targets = targets(client);
        int failed = 0;
        for (String target : targets) {
            try {
                Class.forName(target, true, PatchCheck.class.getClassLoader());
            } catch (Throwable t) {
                failed++;
                GameDropsBackport.LOGGER.error("PATCHCHECK FAIL {}: {}", target, t.toString().split("\n")[0], t);
            }
        }
        if (failed == 0) {
            GameDropsBackport.LOGGER.info("PATCHCHECK ok {} patched classes loaded and verified ({}).", targets.size(),
                    client ? "client" : "server");
        }
    }
}
