package com.exiledradio.gamedropsbackport.music.client;

import com.exiledradio.gamedropsbackport.GameDropsBackport;

import com.google.common.collect.ImmutableSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

/**
 * 26.2's game music: fingerspit's Ebb, Home, Memories, Nightly and Shores join {@code music.game} (26.3's
 * {@code sounds.json}: weight 2, volume 0.4, streamed), and so the creative music, which lists the game music by
 * reference.
 *
 * <p>A built-in pack serving one file, {@code minecraft:sounds.json}, as Caves &amp; Cliffs' and Trails &amp; Tales'
 * game music do: its {@code music.game} adds five sounds and replaces nothing, so 1.12.2's tracks and every other
 * backport's stay. The file sits outside {@code assets/}. The biomes' lists are their owners' (Trails &amp; Tales'
 * 1.20 lists, Caves &amp; Cliffs' 1.18 ones), which add these tracks when this mod is installed; the sulfur caves'
 * list is this mod's own.
 */
@SideOnly(Side.CLIENT)
public final class GameMusicPack implements IResourcePack {

    public static final String FILE = "/" + GameDropsBackport.MOD_ID + "_packs/game_music/assets/minecraft/sounds.json";

    /** For the self-test: the pack is in the default packs. */
    public static volatile boolean installed;

    /** Client pre-init: before the sound handler first reads the sound lists. */
    public static void install() {
        GameMusicPack pack = new GameMusicPack();
        try {
            if (GameMusicPack.class.getResource(FILE) == null) {
                throw new IllegalStateException(FILE + " is missing from the jar");
            }
            Minecraft mc = Minecraft.getMinecraft();
            // Kept for every later reload (F3+T, a resource pack change), which rebuilds from this list...
            List<IResourcePack> defaults = ReflectionHelper.getPrivateValue(Minecraft.class, mc, "defaultResourcePacks", "field_110449_ao");
            defaults.add(pack);
            // ...and added to the live manager now: the startup reload has happened, the sound handler's has not.
            IReloadableResourceManager manager = (IReloadableResourceManager) mc.getResourceManager();
            if (manager instanceof SimpleReloadableResourceManager) {
                ((SimpleReloadableResourceManager) manager).reloadResourcePack(pack);
            }
            installed = true;
            GameDropsBackport.LOGGER.info("Music: fingerspit's tracks join the game music.");
        } catch (Exception e) {
            GameDropsBackport.LOGGER.warn("Music: could not add fingerspit's tracks to the game music.", e);
        }
    }

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        InputStream in = resourceExists(location) ? GameMusicPack.class.getResourceAsStream(FILE) : null;
        if (in == null) {
            throw new FileNotFoundException(location.toString());
        }
        return in;
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        return "minecraft".equals(location.getNamespace()) && "sounds.json".equals(location.getPath());
    }

    @Override
    public Set<String> getResourceDomains() {
        return ImmutableSet.of("minecraft");
    }

    /** A default pack is never listed or described, so there is no pack.mcmeta to read. */
    @Nullable
    @Override
    public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer serializer, String section) {
        return null;
    }

    @Override
    public BufferedImage getPackImage() throws IOException {
        throw new FileNotFoundException("pack.png");
    }

    @Override
    public String getPackName() {
        return GameDropsBackport.MOD_ID + ":game_music";
    }
}
