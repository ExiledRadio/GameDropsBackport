"""Abandoned camps and explorer maps stream data (26.3).

    python -I tools/camps/generate.py

Everything structural is 26.3's own file, unchanged, so no number is written by hand:
  * structure/abandoned_camp/**.nbt and worldgen/template_pool/abandoned_camp/**.json, read by Backport Core's
    JigsawPools and ModernTemplate (this mod is a JigsawPools source for minecraft: names);
  * worldgen/structure/abandoned_camp_*.json and worldgen/structure_set/abandoned_camp.json, which
    camps.CampSettings reads for the grid, the 18 structures, their start pools and biome tags;
  * tags/worldgen/biome/has_structure/abandoned_camp_*.json and tags/worldgen/structure/on_*.json;
  * the three loot tables, into camps/loot_table/ where camps.CampLoot rolls them. 1.12.2 needs a table file to
    fire its load event, so each also gets an empty loot_tables/camps/<path>.json that CampLoot fills at load.

Explorer maps: 26.3's sixteen map items (textures and names). Their decoration sprites are Backport Core's (1.4.4).
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID

COPY = (
    ("data/minecraft/structure/abandoned_camp/", "structure/abandoned_camp/"),
    ("data/minecraft/worldgen/template_pool/abandoned_camp/", "worldgen/template_pool/abandoned_camp/"),
    ("data/minecraft/worldgen/structure_set/abandoned_camp.json", "worldgen/structure_set/abandoned_camp.json"),
)
PREFIX = (
    ("data/minecraft/worldgen/structure/abandoned_camp_", "worldgen/structure/abandoned_camp_"),
    ("data/minecraft/tags/worldgen/biome/has_structure/abandoned_camp_", "tags/worldgen/biome/has_structure/abandoned_camp_"),
    ("data/minecraft/tags/worldgen/structure/on_", "tags/worldgen/structure/on_"),
)
LOOT = ("barrels/abandoned_camp_barrel", "chests/abandoned_camp_common_chest", "chests/abandoned_camp_secret_chest")

# 26.3's explorer map items (Items: registerItem(..., MapItem::new, mapProperties())).
MAPS = ("abandoned_camp_map", "buried_ancient_city_map", "buried_mineshaft_map", "buried_treasure_map",
        "buried_trial_chambers_map", "desert_pyramid_map", "desert_village_map", "jungle_pyramid_map",
        "ocean_monument_map", "plains_village_map", "savanna_village_map", "snowy_village_map", "swamp_hut_map",
        "taiga_village_map", "warm_ocean_ruins_map", "woodland_mansion_map")

counts = {"structure": 0, "json": 0, "loot": 0}
for name in sorted(assets.JAR.namelist()):
    if name.endswith("/"):
        continue
    rel = None
    for src, dst in COPY + PREFIX:
        if name == src or ((src.endswith("/") or src.endswith("_")) and name.startswith(src)):
            rel = dst + name[len(src):] if name != src else dst
            break
    if rel is None:
        continue
    data = assets.jar_bytes(name)
    if name.endswith(".json"):
        data = data.decode("utf-8").replace("\r\n", "\n")
        counts["json"] += 1
    else:
        counts["structure"] += 1
    assets.write(rel, data)

for path in LOOT:
    text = assets.jar_bytes("data/minecraft/loot_table/%s.json" % path).decode("utf-8").replace("\r\n", "\n")
    assets.write("camps/loot_table/%s.json" % path, text)
    assets.write_json("loot_tables/camps/%s.json" % path, {"pools": []})
    counts["loot"] += 1

names = {}
for m in MAPS:
    assets.item_generated(m, assets.texture("item/" + m))
    names["item.%s.%s.name" % (M, m)] = assets.name("item.minecraft." + m)
# The camp maps' item names (the common chest's set_name), vanilla's keys.
for key in sorted(assets.VANILLA_LANG):
    if key.startswith("filled_map.") and key.endswith("_camp_map"):
        names[key] = assets.name(key)

assets.merge_lang("camps", ["%s=%s" % (k, v) for k, v in sorted(names.items())])
print("camps: %(structure)d structure files, %(json)d pool, structure and tag files, %(loot)d loot tables" % counts)
print("maps: %d items" % len(MAPS))
