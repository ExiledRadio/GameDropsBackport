"""Stream 1 (babies) assets from 26.3: the golden dandelion and its pot, the growth particle, sounds and names.

Run: python -I tools/babies/generate.py   (idempotent; merges into sounds.json and en_us.lang under 'babies')
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets as A  # noqa: E402

M = A.MODID

SOUNDS = [
    "item.golden_dandelion.use", "item.golden_dandelion.unuse",
    "block.note_block.trumpet", "block.note_block.trumpet_exposed",
    "block.note_block.trumpet_weathered", "block.note_block.trumpet_oxidized",
] + ["entity.cat_royal." + s for s in ("ambient", "beg_for_food", "death", "eat", "hiss", "hurt", "purr", "purreow",
                                       "stray_ambient")] \
  + ["entity.baby_cat." + s for s in ("ambient", "beg_for_food", "death", "eat", "hiss", "hurt", "purr", "purreow",
                                      "stray_ambient")] \
  + ["entity.chicken_picky." + s for s in ("ambient", "death", "hurt")] \
  + ["entity.baby_chicken." + s for s in ("ambient", "death", "hurt", "step")] \
  + ["entity.cow_moody." + s for s in ("ambient", "death", "hurt", "step")] \
  + ["entity.pig.eat"] \
  + ["entity.pig_mini." + s for s in ("ambient", "death", "eat", "hurt")] \
  + ["entity.pig_big." + s for s in ("ambient", "death", "eat", "hurt")] \
  + ["entity.baby_pig." + s for s in ("ambient", "death", "eat", "hurt", "step")] \
  + ["entity.baby_horse." + s for s in ("ambient", "angry", "breathe", "death", "eat", "hurt", "land", "step")] \
  + ["entity.baby_wolf." + s for s in ("ambient", "death", "growl", "hurt", "pant", "step", "whine")]


def main():
    # Golden dandelion and its pot.
    flower = A.port_model("block/golden_dandelion")
    pot = A.port_model("block/potted_golden_dandelion")
    A.write_json("blockstates/golden_dandelion.json", {"variants": {"normal": {"model": flower.replace("block/", "")}}})
    A.write_json("blockstates/potted_golden_dandelion.json", {"variants": {"normal": {"model": pot.replace("block/", "")}}})
    A.item_generated("golden_dandelion", M + ":block/golden_dandelion")
    # pause_mob_growth and reset_mob_growth both draw 'glint'.
    A.texture("particle/glint")

    events = {}
    for e in SOUNDS:
        events[e] = A.copy_sound(e)
        sub = events[e].get("subtitle", "")
        if sub.startswith("subtitles.%s." % M) and sub.replace("%s." % M, "", 1) not in A.VANILLA_LANG:
            # 26.3 names subtitles.entity.baby_chicken.hurt without an English line: the chicken's own is used.
            events[e]["subtitle"] = "subtitles.entity.chicken.hurt"
    A.merge_sounds("babies", events)

    lines = [
        "config.%s.babies.babyModels=Baby Models" % M,
        "tile.%s.golden_dandelion.name=%s" % (M, A.name("block.minecraft.golden_dandelion")),
        "tile.%s.potted_golden_dandelion.name=%s" % (M, A.name("block.minecraft.potted_golden_dandelion")),
    ] + A.subtitle_lines(events)
    A.merge_lang("babies", lines)


if __name__ == "__main__":
    main()
