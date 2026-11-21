"""Sulfur stream assets: blockstates, models, textures, particles, sounds and names, from the 26.3 jar.

    python -I tools/sulfur/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID


def bs(model):
    """Blockstate model names drop the block/ folder; 1.12.2 adds it back."""
    return model.replace(":block/", ":")


def full(name):
    model = assets.port_model("block/" + name)
    assets.write_json("blockstates/%s.json" % name, {"variants": {"normal": {"model": bs(model)}}})
    assets.item_from_block(name)


def stairs(name):
    """1.12.2's stairs states (facing, half, shape) are named as 26.3's: copy its variants, our models."""
    for suffix in ("", "_inner", "_outer"):
        assets.port_model("block/%s%s" % (name, suffix))
    data = assets.jar_json("assets/minecraft/blockstates/%s.json" % name)
    for variant in data["variants"].values():
        variant["model"] = "%s:%s" % (M, variant["model"].split("/", 1)[1])
    assets.write_json("blockstates/%s.json" % name, data)
    assets.item_from_block(name)


def slab(name, double_model):
    """The family's two-block slab: half=bottom/top on the slab, the full block's model on the double."""
    bottom = assets.port_model("block/" + name)
    top = assets.port_model("block/%s_top" % name)
    assets.write_json("blockstates/%s.json" % name, {"variants": {
        "half=bottom": {"model": bs(bottom)}, "half=top": {"model": bs(top)}}})
    double = name.replace("_slab", "_double_slab")
    assets.write_json("blockstates/%s.json" % double, {"variants": {"normal": {"model": "%s:%s" % (M, double_model)}}})
    assets.item_from_block(name)


def wall(name):
    """Backport Core's ModernWall has 1.16+'s states (up, and none/low/tall per side): 26.3's own multipart."""
    data = assets.jar_json("assets/minecraft/blockstates/%s.json" % name)
    for part in data["multipart"]:
        model = part["apply"]["model"].rpartition(":")[2]
        part["apply"]["model"] = bs(assets.port_model(model))
    # A wall with no sides always stands up as a post (ModernWall.getActualState); the OR also covers the stored
    # state, which 1.12.2 never draws but lists among the valid ones.
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


for stone in ("sulfur", "cinnabar"):
    for base, prefix in ((stone, stone), ("polished_" + stone, "polished_" + stone), (stone + "_bricks", stone + "_brick")):
        full(base)
        named(base)
        stairs(prefix + "_stairs")
        slab(prefix + "_slab", base)
        wall(prefix + "_wall")
        named(prefix + "_stairs")
        named(prefix + "_slab")
        named(prefix + "_double_slab", prefix + "_slab")
        named(prefix + "_wall")
    full("chiseled_" + stone)
    named("chiseled_" + stone)

# Potent sulfur: one model for its five states.
potent = bs(assets.port_model("block/potent_sulfur"))
assets.write_json("blockstates/potent_sulfur.json", {"variants": {
    "potent_sulfur_state=%s" % s: {"model": potent} for s in ("dry", "wet", "dormant", "erupting", "continuous")}})
assets.item_from_block("potent_sulfur")
named("potent_sulfur")

# Sulfur spike: 26.3's ten variants (its waterlogged is Backport Core's water, not a property here).
spike = assets.jar_json("assets/minecraft/blockstates/sulfur_spike.json")
variants = {}
for key, value in spike["variants"].items():
    props = dict(p.split("=") for p in key.split(","))
    props.pop("waterlogged", None)
    k = ",".join("%s=%s" % kv for kv in sorted(props.items()))
    variants[k] = {"model": bs(assets.port_model(value["model"].rpartition(":")[2]))}
assets.write_json("blockstates/sulfur_spike.json", {"variants": variants})
assets.port_model("item/sulfur_spike")
named("sulfur_spike")

# Music disc Bounce.
assets.item_generated("music_disc_bounce", assets.texture("item/music_disc_bounce"))
names["item.%s.music_disc_bounce.name" % M] = assets.name("item.minecraft.music_disc_bounce")
names["item.record.bounce.desc"] = assets.name("jukebox_song.minecraft.bounce")

# Particles: sprites stitched into the block atlas by the client.
for particle in ("bubble_white",):
    assets.texture("particle/" + particle)
for kind in ("noxious_gas", "geyser_base", "geyser_plume", "geyser_poof"):
    for i in range(1, 9):
        assets.texture("particle/%s_%02d" % (kind, i))

events = {}
for sound in ("sulfur", "cinnabar", "potent_sulfur", "sulfur_spike"):
    for kind in ("break", "step", "place", "hit", "fall"):
        event = "block.%s.%s" % (sound, kind)
        events[event] = assets.copy_sound(event)
for event in ("block.sulfur_spike.land", "block.potent_sulfur.noxious_gas", "block.potent_sulfur.geyser_eruption",
              "block.potent_sulfur.geyser_eruption_active", "block.potent_sulfur.geyser_continuous_eruption",
              "block.potent_sulfur.geyser_continuous_eruption_active", "music_disc.bounce"):
    events[event] = assets.copy_sound(event)
assets.merge_sounds("sulfur", events)

lines = sorted("%s=%s" % kv for kv in names.items())
lines += assets.subtitle_lines(events)
lines.append("death.attack.%s.fallingStalactite=%s" % (M, assets.name("death.attack.fallingStalactite")))
lines.append("death.attack.%s.fallingStalactite.player=%s" % (M, assets.name("death.attack.fallingStalactite.player")))
assets.merge_lang("sulfur", lines)
print("sulfur: %d names, %d sound events" % (len(names), len(events)))
