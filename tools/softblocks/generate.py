"""Stream 6 (soft blocks, 26.3): wool and concrete stairs and slabs, cushions, straw beds.

    python -I tools/softblocks/generate.py

Writes blockstates, models, textures, sounds and the stream's en_us.lang section. Wool and concrete shapes use
1.12.2's own wool and concrete textures, so they always match the full block (and any pack that changes it); the
cushion and straw bed textures and models come from the 26.3 jar.

Block layout (see softblocks/SoftColors.java): stairs hold two colors per block, half slabs eight, the double slab
all sixteen. A block is named after its first color.
"""
import json
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
import assets  # noqa: E402

COLORS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
          "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"]
# 1.12.2 calls light gray "silver": its texture and model names, and the color property's value.
OLD = {c: ("silver" if c == "light_gray" else c) for c in COLORS}
MATERIALS = {
    "wool": lambda c: "blocks/wool_colored_" + OLD[c],
    "concrete": lambda c: "blocks/concrete_" + OLD[c],
}
OLD_STAIRS = json.loads(assets.OLD_JAR.read("assets/minecraft/blockstates/oak_stairs.json").decode("utf-8"))["variants"]


def stairs_blocks(mat):
    for i in range(0, 16, 2):
        yield "%s_%s_stairs" % (COLORS[i], mat), COLORS[i:i + 2]


def slab_blocks(mat):
    yield "%s_%s_slab" % (COLORS[0], mat), COLORS[0:8]
    yield "%s_%s_slab" % (COLORS[8], mat), COLORS[8:16]


def main():
    mid = assets.MODID
    lang = []
    for mat, tex in MATERIALS.items():
        for c in COLORS:
            t = "minecraft:" + tex(c)
            stairs = {"bottom": t, "top": t, "side": t}
            name = "%s_%s" % (c, mat)
            assets.write_json("models/block/%s_stairs.json" % name, {"parent": "block/stairs", "textures": stairs})
            assets.write_json("models/block/%s_inner_stairs.json" % name, {"parent": "block/inner_stairs", "textures": stairs})
            assets.write_json("models/block/%s_outer_stairs.json" % name, {"parent": "block/outer_stairs", "textures": stairs})
            assets.write_json("models/block/%s_slab.json" % name, {"parent": "block/half_slab", "textures": stairs})
            assets.write_json("models/block/%s_slab_top.json" % name, {"parent": "block/upper_slab", "textures": stairs})
            assets.item_from_block("%s_stairs" % name)
            assets.item_from_block("%s_slab" % name)
            lang.append("tile.%s.%s_stairs.name=%s" % (mid, name, assets.name("block.minecraft.%s_stairs" % name)))
            lang.append("tile.%s.%s_slab.name=%s" % (mid, name, assets.name("block.minecraft.%s_slab" % name)))
        for block, colors in stairs_blocks(mat):
            variants = {}
            for c in colors:
                for key, spec in OLD_STAIRS.items():
                    spec = dict(spec)
                    spec["model"] = "%s:%s_%s" % (mid, c, spec["model"].replace("oak", mat))
                    variants["color=%s,%s" % (OLD[c], key)] = spec
            assets.write_json("blockstates/%s.json" % block, {"variants": dict(sorted(variants.items()))})
        for block, colors in slab_blocks(mat):
            variants = {}
            for c in colors:
                variants["color=%s,half=bottom" % OLD[c]] = {"model": "%s:%s_%s_slab" % (mid, c, mat)}
                variants["color=%s,half=top" % OLD[c]] = {"model": "%s:%s_%s_slab_top" % (mid, c, mat)}
            assets.write_json("blockstates/%s.json" % block, {"variants": variants})
        double = "white_%s_double_slab" % mat
        assets.write_json("blockstates/%s.json" % double,
                          {"variants": {"color=%s" % OLD[c]: {"model": "minecraft:%s_%s" % (OLD[c], mat)} for c in COLORS}})
        lang.append("tile.%s.%s.name=%s" % (mid, double, assets.name("block.minecraft.white_%s_slab" % mat)))

    # Cushions: an item per color, an entity texture per color.
    for c in COLORS:
        assets.item_generated("%s_cushion" % c, assets.texture("item/%s_cushion" % c))
        assets.texture("entity/cushion/%s_cushion" % c)
        lang.append("item.%s.%s_cushion.name=%s" % (mid, c, assets.name("item.minecraft.%s_cushion" % c)))
    lang.append("entity.%s.cushion.name=%s" % (mid, assets.name("entity.minecraft.cushion")))

    # Straw bed: 26.3's two models; the item shows both halves, as 26.3's composite item model does.
    # 1.12.2 names a blockstate's models relative to models/block/.
    foot = assets.port_model("block/straw_bed_foot").replace(":block/", ":")
    head = assets.port_model("block/straw_bed_head").replace(":block/", ":")
    variants = {}
    for facing, y in (("south", 0), ("west", 90), ("north", 180), ("east", 270)):
        for part, model in (("foot", foot), ("head", head)):
            spec = {"model": model}
            if y:
                spec["y"] = y
            variants["facing=%s,part=%s" % (facing, part)] = spec
    assets.write_json("blockstates/straw_bed.json", {"variants": variants})
    assets.write_json("models/item/straw_bed.json", straw_bed_item())
    lang.append("tile.%s.straw_bed.name=%s" % (mid, assets.name("block.minecraft.straw_bed")))

    events = {}
    for e in ("entity.cushion.place", "entity.cushion.break", "entity.cushion.sit", "entity.cushion.get_up",
              "block.straw_bed.break", "block.straw_bed.step", "block.straw_bed.place", "block.straw_bed.hit",
              "block.straw_bed.fall", "block.straw_bed.break_leave"):
        events[e] = assets.copy_sound(e)
    assets.merge_sounds("softblocks", events)
    assets.merge_lang("softblocks", sorted(lang) + assets.subtitle_lines(events))


def straw_bed_item():
    """26.3's item: both halves turned 180 degrees, head at z 0-16 and foot at 16-32; moved 8 back to fit 1.12.2's
    -16..32 element limit (the foot's frills reach z 35)."""
    flip = {"north": "south", "south": "north", "east": "west", "west": "east", "up": "up", "down": "down"}
    elements = []
    for part, dz in (("head", 0), ("foot", 16)):
        data = assets.jar_json("assets/minecraft/models/block/straw_bed_%s.json" % part)
        for el in data["elements"]:
            f, t = el["from"], el["to"]
            out = {"from": [16 - t[0], f[1], 16 - t[2] + dz - 8], "to": [16 - f[0], t[1], 16 - f[2] + dz - 8], "faces": {}}
            for side, face in el["faces"].items():
                face = dict(face)
                face.pop("cullface", None)
                face["texture"] = "#all"
                if side in ("up", "down"):
                    face["rotation"] = (face.get("rotation", 0) + 180) % 360
                    if face["rotation"] == 0:
                        del face["rotation"]
                out["faces"][flip[side]] = face
            elements.append(out)
    foot = assets.jar_json("assets/minecraft/models/block/straw_bed_foot.json")
    return {"textures": {"all": "%s:block/straw_bed" % assets.MODID, "particle": "%s:block/straw_bed_particle" % assets.MODID},
            "elements": elements, "display": foot.get("display", {})}


if __name__ == "__main__":
    main()
