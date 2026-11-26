"""26.4 stream 1 assets: icicles, ice crystals and brittle ice, with their sounds and names, from the 26.4 jar.

    python -I tools/icecaves/generate.py

Reads 26.4 Pre-Release 1 until 26.4 releases (GD_ASSETS_VERSION; tools/assets.py).
"""
import os
import sys

os.environ.setdefault("GD_ASSETS_VERSION", "26.4-pre-1")
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID


def bs(model):
    return model.replace(":block/", ":")


def variants_of(name, drop=("waterlogged",)):
    """26.4's variants under our models; 1.12.2 rotates only about x and y, so z=180 becomes x=180 y=180."""
    data = assets.jar_json("assets/minecraft/blockstates/%s.json" % name)["variants"]
    out = {}
    for key, value in data.items():
        props = dict(p.split("=") for p in key.split(",")) if key else {}
        for d in drop:
            props.pop(d, None)
        k = ",".join("%s=%s" % kv for kv in sorted(props.items())) or "normal"
        v = dict(value)
        v["model"] = bs(assets.port_model(value["model"].rpartition(":")[2]))
        if v.pop("z", 0) == 180:
            v["x"] = (v.get("x", 0) + 180) % 360
            v["y"] = (v.get("y", 0) + 180) % 360
            for axis in ("x", "y"):
                if v[axis] == 0:
                    del v[axis]
        out[k] = v
    return out


names = {}
for block in ("icicle", "ice_crystal", "brittle_ice"):
    assets.write_json("blockstates/%s.json" % block, {"variants": variants_of(block)})
    names["tile.%s.%s.name" % (M, block)] = assets.name("block.minecraft." + block)
assets.port_model("item/icicle")
assets.port_model("item/ice_crystal")
assets.item_from_block("brittle_ice", "brittle_ice_0")

events = {}
for sound in ("icicle", "ice_crystal", "brittle_ice"):
    for kind in ("break", "step", "place", "hit", "fall"):
        event = "block.%s.%s" % (sound, kind)
        events[event] = assets.copy_sound(event)
for event in ("block.icicle.land", "block.ice_crystal.ambient", "block.brittle_ice.crack"):
    events[event] = assets.copy_sound(event)
assets.merge_sounds("icecaves", events)

lines = sorted("%s=%s" % kv for kv in names.items())
lines += assets.subtitle_lines(events)
lines.append("config.%s.iceCaves=Ice Caves" % M)
lines.append("config.%s.iceCaves.enabled=Enabled" % M)
assets.merge_lang("icecaves", lines)
print("icecaves: %d names, %d sound events" % (len(names), len(events)))
