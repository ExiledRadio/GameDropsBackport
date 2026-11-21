"""Stream 1 (babies) baby model textures from 26.3, for BabyRenderers: copied to textures/entity/... in our namespace.

Run: python -I tools/babies/models.py   (idempotent)
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets as A  # noqa: E402

TEXTURES = [
    "chicken/chicken_temperate_baby",
    "cow/cow_temperate_baby",
    "cow/mooshroom_red_baby",
    "pig/pig_temperate_baby",
    "sheep/sheep_baby",
    "sheep/sheep_wool_baby",
    "wolf/wolf_baby",
    "wolf/wolf_tame_baby",
    "wolf/wolf_angry_baby",
    "wolf/wolf_collar_baby",
    "cat/ocelot_baby",
    "cat/cat_black_baby",
    "cat/cat_red_baby",
    "cat/cat_siamese_baby",
    "bear/polarbear_baby",
    "villager/villager_baby",
    "villager/baby/plains",
    "zombie/zombie_baby",
    "zombie/husk_baby",
    "zombie_villager/zombie_villager_baby",
    "zombie_villager/baby/plains",
    "piglin/zombified_piglin_baby",
    "horse/horse_skeleton_baby",
    "horse/horse_zombie_baby",
    "horse/donkey_baby",
    "horse/mule_baby",
] + ["rabbit/rabbit_%s_baby" % t for t in ("brown", "white", "black", "white_splotched", "gold", "salt", "caerbannog", "toast")] \
  + ["horse/horse_%s_baby" % c for c in ("white", "creamy", "chestnut", "brown", "black", "gray", "darkbrown")] \
  + ["horse/horse_markings_%s_baby" % m for m in ("white", "whitefield", "whitedots", "blackdots")] \
  + ["llama/llama_%s_baby" % c for c in ("creamy", "white", "brown", "gray")] \
  + ["equipment/humanoid_baby/%s" % m for m in ("leather", "leather_overlay", "chainmail", "iron", "gold", "diamond")]


def main():
    for t in TEXTURES:
        A.texture("entity/" + t)


if __name__ == "__main__":
    main()
