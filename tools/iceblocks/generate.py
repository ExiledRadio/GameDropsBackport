"""26.4 streams 3 and 4 assets: the ice, snow and calcite building blocks and chiseled soulshale, from the 26.4 jar.

    python -I tools/iceblocks/generate.py

Reads 26.4 Pre-Release 2 until 26.4 releases (GD_ASSETS_VERSION; tools/assets.py).
"""
import os
import sys

os.environ.setdefault("GD_ASSETS_VERSION", "26.4-pre-2")
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID


def bs(model):
    """Blockstate model names drop the block/ folder; 1.12.2 adds it back."""
    return model.replace(":block/", ":")


def vanilla_states(name):
    return assets.jar_json("assets/minecraft/blockstates/%s.json" % name)


def model_of(variant):
    return variant["model"].rpartition(":")[2]


def full(name):
    model = assets.port_model(model_of(vanilla_states(name)["variants"][""]))
    assets.write_json("blockstates/%s.json" % name, {"variants": {"normal": {"model": bs(model)}}})
    assets.item_from_block(name, model.rpartition("/")[2])


def pillar(name):
    data = vanilla_states(name)
    variants = {}
    for key, value in data["variants"].items():
        v = dict(value)
        v["model"] = bs(assets.port_model(model_of(value)))
        variants[key] = v
    assets.write_json("blockstates/%s.json" % name, {"variants": variants})
    assets.item_from_block(name)


def stairs(name):
    """1.12.2's stairs states (facing, half, shape) are named as 26.4's: copy its variants, our models."""
    data = vanilla_states(name)
    for variant in data["variants"].values():
        variant["model"] = bs(assets.port_model(model_of(variant)))
    assets.write_json("blockstates/%s.json" % name, data)
    assets.item_from_block(name)


def slab(name):
    """The family's two-block slab: half=bottom/top on the slab, 26.4's type=double model on the double."""
    data = vanilla_states(name)["variants"]
    bottom = assets.port_model(model_of(data["type=bottom"]))
    top = assets.port_model(model_of(data["type=top"]))
    double_model = assets.port_model(model_of(data["type=double"]))
    assets.write_json("blockstates/%s.json" % name, {"variants": {
        "half=bottom": {"model": bs(bottom)}, "half=top": {"model": bs(top)}}})
    double = name.replace("_slab", "_double_slab")
    assets.write_json("blockstates/%s.json" % double, {"variants": {"normal": {"model": bs(double_model)}}})
    assets.item_from_block(name)


def wall(name):
    """Backport Core's ModernWall has 1.16+'s states (up, and none/low/tall per side): 26.4's own multipart."""
    data = vanilla_states(name)
    for part in data["multipart"]:
        part["apply"]["model"] = bs(assets.port_model(model_of(part["apply"])))
    lone = {"north": "none", "east": "none", "south": "none", "west": "none"}
    for part in data["multipart"]:
        if part.get("when") == {"up": "true"}:
            part["when"] = {"OR": [{"up": "true"}, lone]}
    assets.port_model("block/%s_inventory" % name)
    assets.write_json("blockstates/%s.json" % name, data)
    assets.item_from_block(name, name + "_inventory")


names = {}


def named(block, vanilla=None):
    names["tile.%s.%s.name" % (M, block)] = assets.name("block.minecraft." + (vanilla or block))


def shapes(prefix):
    stairs(prefix + "_stairs")
    slab(prefix + "_slab")
    wall(prefix + "_wall")
    named(prefix + "_stairs")
    named(prefix + "_slab")
    named(prefix + "_double_slab", prefix + "_slab")
    named(prefix + "_wall")


for base, prefix in (("packed_ice_bricks", "packed_ice_brick"), ("snow_bricks", "snow_brick"),
                     ("polished_calcite", "polished_calcite"), ("polished_calcite_bricks", "polished_calcite_brick")):
    full(base)
    named(base)
    shapes(prefix)
shapes("calcite")
full("chiseled_packed_ice")
named("chiseled_packed_ice")
pillar("calcite_pillar")
named("calcite_pillar")
full("chiseled_soulshale")
named("chiseled_soulshale")

events = {}
for sound in ("packed_ice", "calcite", "chiseled_soulshale"):
    for kind in ("break", "step", "place", "hit", "fall"):
        event = "block.%s.%s" % (sound, kind)
        events[event] = assets.copy_sound(event)
assets.merge_sounds("iceblocks", events)

lines = sorted("%s=%s" % kv for kv in names.items())
lines += assets.subtitle_lines(events)
lines.append("config.%s.iceBlocks=Ice and Snow Blocks" % M)
lines.append("config.%s.iceBlocks.enabled=Enabled" % M)
assets.merge_lang("iceblocks", lines)
print("iceblocks: %d names, %d sound events" % (len(names), len(events)))
