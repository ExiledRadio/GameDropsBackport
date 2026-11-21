package com.exiledradio.gamedropsbackport.camps;

import com.exiledradio.backportcore.structure.ScatteredPlacement;
import com.exiledradio.gamedropsbackport.GameDropsBackport;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 26.3's abandoned camp files as this jar holds them ({@code tools/camps/generate.py}): the structure set
 * {@code abandoned_camp} (its {@code random_spread} grid and its 18 weighted structures) and each structure's start
 * pool, depth, distance, heightmap projection and biome tag. Nothing here is a number written by hand.
 */
public final class CampSettings {

    /** One of the set's structures: {@code minecraft:abandoned_camp_<variant>}. */
    public static final class Variant {
        /** Today's id, {@code minecraft:abandoned_camp_forest}. */
        public final String id;
        /** The part after {@code abandoned_camp_}: {@code forest}. */
        public final String name;
        public final int weight;
        public final ResourceLocation startPool;
        public final int maxDepth;
        public final int maxDistance;
        public final boolean expansionHack;
        public final boolean projectToSurface;
        public final int startHeight;
        /** Today's biome ids, {@code minecraft:forest}. */
        public final Set<String> biomes;

        Variant(String id, int weight, JsonObject s) {
            this.id = id;
            this.name = id.substring(id.indexOf(':') + 1).substring("abandoned_camp_".length());
            this.weight = weight;
            this.startPool = new ResourceLocation(s.get("start_pool").getAsString());
            this.maxDepth = s.get("size").getAsInt();
            JsonElement d = s.get("max_distance_from_center");
            this.maxDistance = d == null ? 80 : d.isJsonPrimitive() ? d.getAsInt() : d.getAsJsonObject().get("horizontal").getAsInt();
            this.expansionHack = s.has("use_expansion_hack") && s.get("use_expansion_hack").getAsBoolean();
            this.projectToSurface = s.has("project_start_to_heightmap");
            JsonElement h = s.get("start_height");
            this.startHeight = h != null && h.isJsonObject() && h.getAsJsonObject().has("absolute") ? h.getAsJsonObject().get("absolute").getAsInt() : 0;
            this.biomes = biomes(s.get("biomes").getAsString());
        }
    }

    private static CampSettings instance;

    public final ScatteredPlacement grid;
    public final int spacing;
    public final int separation;
    public final int salt;
    /** The set's structures in the file's order (today's selection order). */
    public final List<Variant> variants;

    private CampSettings(JsonObject set) {
        JsonObject p = set.getAsJsonObject("placement");
        this.spacing = p.get("spacing").getAsInt();
        this.separation = p.get("separation").getAsInt();
        this.salt = p.get("salt").getAsInt();
        this.grid = new ScatteredPlacement(this.spacing, this.separation, this.salt);
        List<Variant> list = new ArrayList<Variant>();
        for (JsonElement e : set.getAsJsonArray("structures")) {
            JsonObject o = e.getAsJsonObject();
            String id = o.get("structure").getAsString();
            JsonObject s = read("worldgen/structure/" + id.substring(id.indexOf(':') + 1) + ".json");
            if (s == null) {
                GameDropsBackport.LOGGER.error("Abandoned camps: no structure file for {}.", id);
                continue;
            }
            list.add(new Variant(id, o.has("weight") ? o.get("weight").getAsInt() : 1, s));
        }
        this.variants = Collections.unmodifiableList(list);
    }

    public static synchronized CampSettings get() {
        if (instance == null) {
            JsonObject set = read("worldgen/structure_set/abandoned_camp.json");
            if (set == null) {
                throw new IllegalStateException("Game Drops Backport: the abandoned camp structure set is missing from the jar.");
            }
            instance = new CampSettings(set);
        }
        return instance;
    }

    /** @return the variant of this id ({@code minecraft:} optional), or null */
    @Nullable
    public Variant variant(String id) {
        String full = id.indexOf(':') < 0 ? "minecraft:" + id : id;
        for (Variant v : this.variants) {
            if (v.id.equals(full)) {
                return v;
            }
        }
        return null;
    }

    /** @return the ids of a structure tag ({@code #minecraft:on_abandoned_camp_swamp}), or the id itself */
    public static List<String> structureTag(String idOrTag) {
        if (!idOrTag.startsWith("#")) {
            return Collections.singletonList(idOrTag);
        }
        String path = idOrTag.substring(idOrTag.indexOf(':') + 1);
        JsonObject tag = read("tags/worldgen/structure/" + path + ".json");
        List<String> out = new ArrayList<String>();
        if (tag != null) {
            for (JsonElement e : tag.getAsJsonArray("values")) {
                out.add(e.getAsString());
            }
        }
        return out;
    }

    private static Set<String> biomes(String tag) {
        Set<String> out = new HashSet<String>();
        if (!tag.startsWith("#")) {
            out.add(tag);
            return out;
        }
        JsonObject t = read("tags/worldgen/biome/" + tag.substring(tag.indexOf(':') + 1) + ".json");
        if (t != null) {
            for (JsonElement e : t.getAsJsonArray("values")) {
                out.add(e.getAsString());
            }
        }
        return out;
    }

    /** @return a JSON file under this mod's assets, or null */
    @Nullable
    static JsonObject read(String path) {
        String full = "/assets/" + GameDropsBackport.MOD_ID + "/" + path;
        try (InputStream in = CampSettings.class.getResourceAsStream(full)) {
            if (in == null) {
                return null;
            }
            return new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            GameDropsBackport.LOGGER.error("Could not read {}.", full, e);
            return null;
        }
    }
}
