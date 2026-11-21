"""Sulfur caves stream assets: spring templates, the biome's music, the tuff copy and names, from 26.3.

    python -I tools/sulfurcaves/generate.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID

# The ten sulfur spring templates, as 26.3 ships them (Springs reads 26.3's palette names).
for n in assets.JAR.namelist():
    if n.startswith("data/minecraft/structure/spring/") and n.endswith(".nbt"):
        assets.write("structures/spring/" + n.rsplit("/", 1)[1], assets.jar_bytes(n))

# Tuff, for worlds with no family mod that has it (Caves & Cliffs owns it, Tricky Trials copies it).
model = assets.port_model("block/tuff")
assets.write_json("blockstates/tuff.json", {"variants": {"normal": {"model": model.replace(":block/", ":")}}})
assets.item_from_block("tuff")
names = {"tile.%s.tuff.name" % M: assets.name("block.minecraft.tuff")}

events = {}
for kind in ("break", "step", "place", "hit", "fall"):
    events["block.tuff." + kind] = assets.copy_sound("block.tuff." + kind)

# music.overworld.sulfur_caves: four of its tracks are 1.12.2's own under their old file names; the rest are copied.
OLD = {"danny": "hal4", "key": "nuance1", "oxygene": "nuance2", "subwoofer_lullaby": "hal1"}
spec = json.loads(json.dumps(assets.vanilla_sound("music.overworld.sulfur_caves")))
sounds = []
for s in spec["sounds"]:
    entry = dict(s) if isinstance(s, dict) else {"name": s}
    track = entry["name"].rsplit("/", 1)[1]
    if track in OLD:
        entry["name"] = "minecraft:music/game/" + OLD[track]
    else:
        assets.write("sounds/music/game/%s.ogg" % track, assets._object("minecraft/sounds/music/game/%s.ogg" % track))
        entry["name"] = "%s:music/game/%s" % (M, track)
    sounds.append(entry)
spec["sounds"] = sounds
events["music.overworld.sulfur_caves"] = spec
assets.merge_sounds("sulfurcaves", events)

lines = sorted("%s=%s" % kv for kv in names.items())
lines += assets.subtitle_lines(events)
assets.merge_lang("sulfurcaves", lines)
print("sulfurcaves: %d names, %d sound events" % (len(names), len(events)))
