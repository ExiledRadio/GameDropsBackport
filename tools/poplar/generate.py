"""Poplar stream assets: the poplar wood set, the three leaves, sapling, pot, boats, red shrub and shelf mushroom,
their sounds, particles and names, all from the 26.3 jar.

    python -I tools/poplar/generate.py

Blockstates are 26.3's wherever 1.12.2's properties carry the same names (stairs, fences, gates, doors, pressure
plates, the shelf mushroom); trapdoors and buttons are mapped from 1.12.2's states; slabs are the family's two-block
form. Signs: 26.3 draws them as block models from a 32x32 texture, 1.12.2 as an entity from the old 64x32 layout, so
the sign texture is rearranged into that layout ({@code sign_texture}) and the blockstates name a particle-only model.
"""
import io
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402
from PIL import Image  # noqa: E402

M = assets.MODID
HORIZONTAL = ["north", "south", "west", "east"]
BOOL = ["true", "false"]


def bs_model(model):
    """1.12.2 blockstates add the block/ folder: 'gamedropsbackport:block/x' -> 'gamedropsbackport:x'."""
    return model.replace(":block/", ":")


def port_variant(v):
    if isinstance(v, list):
        return [port_variant(x) for x in v]
    out = dict(v)
    ns, _, path = out["model"].rpartition(":")
    out["model"] = bs_model(assets.port_model(path))
    return out


def states(props):
    combos = [{}]
    for key in sorted(props):
        combos = [dict(c, **{key: v}) for c in combos for v in props[key]]
    return combos


def variant_string(state):
    if not state:
        return "normal"
    return ",".join("%s=%s" % (k, state[k]) for k in sorted(state))


def port_blockstate(vanilla, ours, props, convert=None):
    data = assets.jar_json("assets/minecraft/blockstates/%s.json" % vanilla)
    if "multipart" in data:
        parts = []
        for part in data["multipart"]:
            p = dict(part)
            p["apply"] = port_variant(part["apply"])
            parts.append(p)
        assets.write_json("blockstates/%s.json" % ours, {"multipart": parts})
        return
    table = []
    for key, v in data["variants"].items():
        want = dict(kv.split("=", 1) for kv in key.split(",")) if key else {}
        table.append((want, v))
    out = {}
    for state in states(props):
        modern = convert(dict(state)) if convert else state
        modern = {k: str(v) for k, v in modern.items()}
        for want, v in table:
            if all(modern.get(k) == val for k, val in want.items()):
                out[variant_string(state)] = port_variant(v)
                break
        else:
            raise KeyError("%s: no 26.3 variant for %s (as %s)" % (vanilla, state, modern))
    assets.write_json("blockstates/%s.json" % ours, {"variants": out})


def port_item_model(name):
    if ("assets/minecraft/models/item/%s.json" % name) in assets.JAR.namelist():
        return assets.port_model("item/" + name)
    ref = assets.jar_json("assets/minecraft/items/%s.json" % name)["model"]["model"]
    ns, _, path = ref.rpartition(":")
    assets.write_json("models/item/%s.json" % name, {"parent": assets.port_model(path)})


def _button(state):
    f = state.pop("facing")
    if f == "up":
        state.update(face="floor", facing="north")
    elif f == "down":
        state.update(face="ceiling", facing="north")
    else:
        state.update(face="wall", facing=f)
    return state


def _slab_half(state):
    state["type"] = state.pop("half")
    state.pop("variant")
    return state


def _slab_double(state):
    state.pop("variant")
    state["type"] = "double"
    return state


def _trapdoor(state):
    state["powered"] = "false"
    state["waterlogged"] = "false"
    return state


def sign_texture(wood):
    """26.3's 32x32 block/<wood>_sign (template_sign's UVs, in pixels) laid out as 1.12.2's 64x32 entity sign."""
    src = Image.open(io.BytesIO(assets.jar_bytes("assets/minecraft/textures/block/%s_sign.png" % wood))).convert("RGBA")
    out = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    def put(box, at, turn=False):
        part = src.crop(box)
        if turn:
            part = part.rotate(180)
        out.paste(part, at)

    # Board, a 24x12x2 box at (0, 0): top, bottom, -x side, front, +x side, back.
    put((0, 0, 24, 2), (2, 0), True)
    put((0, 28, 24, 30), (26, 0), True)
    put((24, 2, 26, 14), (0, 2))
    put((0, 2, 24, 14), (2, 2))
    put((24, 16, 26, 28), (26, 2))
    put((0, 16, 24, 28), (28, 2))
    # Post, a 2x14x2 box at (0, 14): top (unseen, the bottom again), bottom, then its four sides.
    put((28, 30, 30, 32), (2, 14))
    put((28, 30, 30, 32), (4, 14))
    put((30, 0, 32, 14), (0, 16))
    put((28, 0, 30, 14), (2, 16))
    put((30, 16, 32, 30), (4, 16))
    put((28, 16, 30, 30), (6, 16))
    buf = io.BytesIO()
    out.save(buf, "PNG")
    assets.write("textures/entity/signs/%s.png" % wood, buf.getvalue())


lang = []


def named(ours, vanilla=None, kind="tile"):
    key = ("block.minecraft." if kind == "tile" else "item.minecraft.") + (vanilla or ours)
    if key not in assets.VANILLA_LANG:
        key = "block.minecraft." + (vanilla or ours)
    lang.append("%s.%s.%s.name=%s" % (kind, M, ours, assets.name(key)))


def drop_mcmeta(texture):
    """26.3's leaves carry only 'mipmap_strategy', which 1.12.2 does not know: left out."""
    path = os.path.join(assets.ASSETS, "textures", texture + ".png.mcmeta")
    if os.path.exists(path):
        os.remove(path)


# --- the wood set ---------------------------------------------------------------------------------
W = "poplar"
pillar = {"axis": ["x", "y", "z"]}
for s in ("log", "wood"):
    for name in (W + "_" + s, "stripped_%s_%s" % (W, s)):
        port_blockstate(name, name, pillar)
        port_item_model(name)
        named(name)
port_blockstate(W + "_planks", W + "_planks", {})
port_item_model(W + "_planks")
named(W + "_planks")
port_blockstate(W + "_stairs", W + "_stairs", {"facing": HORIZONTAL, "half": ["top", "bottom"],
                                                "shape": ["straight", "inner_left", "inner_right", "outer_left", "outer_right"]})
port_item_model(W + "_stairs")
named(W + "_stairs")
port_blockstate(W + "_slab", W + "_slab", {"half": ["top", "bottom"], "variant": ["default"]}, _slab_half)
port_blockstate(W + "_slab", W + "_double_slab", {"variant": ["default"]}, _slab_double)
port_item_model(W + "_slab")
named(W + "_slab")
named(W + "_double_slab", W + "_slab")
port_blockstate(W + "_fence", W + "_fence", None)
port_item_model(W + "_fence")
named(W + "_fence")
port_blockstate(W + "_fence_gate", W + "_fence_gate", {"facing": HORIZONTAL, "in_wall": BOOL, "open": BOOL, "powered": BOOL})
port_item_model(W + "_fence_gate")
named(W + "_fence_gate")
port_blockstate(W + "_door", W + "_door", {"facing": HORIZONTAL, "half": ["upper", "lower"], "hinge": ["left", "right"],
                                          "open": BOOL, "powered": BOOL})
named(W + "_door")
named(W + "_door", kind="item")
port_item_model(W + "_door")
port_blockstate(W + "_trapdoor", W + "_trapdoor", {"facing": HORIZONTAL, "half": ["top", "bottom"], "open": BOOL}, _trapdoor)
port_item_model(W + "_trapdoor")
named(W + "_trapdoor")
port_blockstate(W + "_pressure_plate", W + "_pressure_plate", {"powered": BOOL})
port_item_model(W + "_pressure_plate")
named(W + "_pressure_plate")
port_blockstate(W + "_button", W + "_button", {"facing": ["down", "up"] + HORIZONTAL, "powered": BOOL}, _button)
port_item_model(W + "_button")
named(W + "_button")
# Signs: drawn by our renderer, so the blockstates name a model with the planks' particle only.
assets.write_json("models/block/%s_sign.json" % W, {"textures": {"particle": assets.texture("block/%s_planks" % W)}})
assets.write_json("blockstates/%s_sign.json" % W,
                  {"variants": {"rotation=%d" % i: {"model": "%s:%s_sign" % (M, W)} for i in range(16)}})
assets.write_json("blockstates/%s_wall_sign.json" % W,
                  {"variants": {"facing=%s" % f: {"model": "%s:%s_sign" % (M, W)} for f in HORIZONTAL}})
named(W + "_sign")
named(W + "_wall_sign", W + "_sign")
named(W + "_sign", kind="item")
port_item_model(W + "_sign")
sign_texture(W)
# 26.2's flat sign for Trails & Tales' sign editing screen (it reads gui/signs/<wood> in the sign's namespace).
assets.texture("gui/signs/" + W)

# --- leaves, sapling, pot, boats -------------------------------------------------------------------
for color in ("red", "orange", "yellow"):
    name = color + "_poplar_leaves"
    port_blockstate(name, name, {})
    port_item_model(name)
    named(name)
    drop_mcmeta("block/" + name)
    for i in range(1, 5):
        assets.texture("particle/%s_poplar_%d" % (color, i))
port_blockstate("poplar_sapling", "poplar_sapling", {})
port_item_model("poplar_sapling")
named("poplar_sapling")
port_blockstate("potted_poplar_sapling", "potted_poplar_sapling", {})
named("potted_poplar_sapling")
for chest in (False, True):
    item = W + ("_chest_boat" if chest else "_boat")
    port_item_model(item)
    named(item, kind="item")
    assets.texture("entity/%s/%s" % ("chest_boat" if chest else "boat", W))
lang.append("entity.%s.boat.name=%s" % (M, assets.name("entity.minecraft.poplar_boat")))
lang.append("entity.%s.chest_boat.name=%s" % (M, assets.name("entity.minecraft.poplar_chest_boat")))

# --- red shrub, shelf mushroom ---------------------------------------------------------------------
port_blockstate("red_shrub", "red_shrub", {})
port_item_model("red_shrub")
named("red_shrub")
port_blockstate("shelf_mushroom", "shelf_mushroom", {"age": ["0", "1"], "facing": HORIZONTAL})
port_item_model("shelf_mushroom")
named("shelf_mushroom")

# --- sounds -----------------------------------------------------------------------------------------
events = {}
for part in ("break", "step", "place", "hit", "fall", "ambient"):
    events["block.poplar_leaves." + part] = assets.copy_sound("block.poplar_leaves." + part)
for part in ("break", "place"):
    events["block.red_shrub." + part] = assets.copy_sound("block.red_shrub." + part)
for part in ("break", "step", "place", "fall", "bounce"):
    events["block.shelf_mushroom." + part] = assets.copy_sound("block.shelf_mushroom." + part)
events["item.axe.strip"] = assets.copy_sound("item.axe.strip")
assets.merge_sounds("poplar", events)

lang.append("biome.%s.dappled_forest=%s" % (M, assets.name("biome.minecraft.dappled_forest")))
lang.append("config.%s.poplar.generateBiome=Generate Dappled Forests" % M)
assets.merge_lang("poplar", sorted(set(lang)) + assets.subtitle_lines(events))
print("poplar: %d lang lines, %d sound events" % (len(set(lang)), len(events)))
