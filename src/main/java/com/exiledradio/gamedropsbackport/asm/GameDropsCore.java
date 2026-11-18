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
 * <p>No patches yet: the plugin is here from the start so the jar's manifest, the dev run's property and
 * {@code PatchCheck} never change when the first one is added.
 */
@IFMLLoadingPlugin.Name("Game Drops Backport Core")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions("com.exiledradio.gamedropsbackport.asm.")
@IFMLLoadingPlugin.SortingIndex(1001)
public final class GameDropsCore implements IFMLLoadingPlugin {

    /** Every transformer, in the order they run. */
    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    /** The classes the transformers patch on both sides, for {@code PatchCheck}: each transformer's TARGETS. */
    public static String[] serverTargets() {
        return new String[0];
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
