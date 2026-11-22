package com.exiledradio.gamedropsbackport.asm;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * This mod's own small coremod: the bytecode patches 26.x's behavior needs where 1.12.2 has no hook. One transformer
 * per patch, each documented in {@code docs/PATCHES.md}. No MixinBooter.
 *
 * <p>Named in the jar's manifest ({@code FMLCorePlugin}, with {@code FMLCorePluginContainsFMLMod}) and, in the dev
 * environment, by {@code -Dfml.coreMods.load}. A transformer only rewrites a call or adds a call to a hook in the
 * mod's own packages; the hook keeps 1.12.2's behavior while its stream is off.
 *
 * <p>The patches: the changes stream's P1-P5 ({@link ChangesTransformer}); the camps' P6 ({@link LootOriginTransformer}).
 */
@IFMLLoadingPlugin.Name("Game Drops Backport Core")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions("com.exiledradio.gamedropsbackport.asm.")
@IFMLLoadingPlugin.SortingIndex(1001)
public final class GameDropsCore implements IFMLLoadingPlugin {

    /** Every transformer, in the order they run. */
    @Override
    public String[] getASMTransformerClass() {
        return new String[]{"com.exiledradio.gamedropsbackport.asm.ChangesTransformer",
                "com.exiledradio.gamedropsbackport.asm.LootOriginTransformer"};
    }

    /** The classes the transformers patch on both sides, for {@code PatchCheck}: each transformer's TARGETS. */
    public static String[] serverTargets() {
        String[] out = new String[ChangesTransformer.TARGETS.length + LootOriginTransformer.TARGETS.length];
        System.arraycopy(ChangesTransformer.TARGETS, 0, out, 0, ChangesTransformer.TARGETS.length);
        System.arraycopy(LootOriginTransformer.TARGETS, 0, out, ChangesTransformer.TARGETS.length, LootOriginTransformer.TARGETS.length);
        return out;
    }

    /** The classes patched only on the client, for {@code PatchCheck}. */
    public static String[] clientTargets() {
        return new String[0];
    }

    @Nullable
    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Nullable
    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
