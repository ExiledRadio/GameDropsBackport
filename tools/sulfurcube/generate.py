"""Stream 3 (sulfur cube, 26.2): the cube's textures, the bucket, the goo particle, sounds, the advancement Uh Oh
and the stream's en_us.lang section.

    python -I tools/sulfurcube/generate.py

Everything comes from the 26.3 jar and asset index. The spawn egg is 1.12.2's colored egg (26.3's is a texture).
"""
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
import assets  # noqa: E402

ARCHETYPES = ["bouncy", "explosive", "fast_flat", "fast_sliding", "high_resistance", "hot", "light", "regular",
              "slow_bouncy", "slow_flat", "slow_sliding", "sticky"]


def main():
    mid = assets.MODID
    lang = []
    for t in ("sulfur_cube_outer", "sulfur_cube_inner", "sulfur_cube_outer_small", "sulfur_cube_inner_small"):
        assets.texture("entity/sulfur_cube/" + t)
    assets.texture("particle/sulfur_cube_goo")
    assets.item_generated("sulfur_cube_bucket", assets.texture("item/sulfur_cube_bucket"))

    lang.append("entity.%s.sulfur_cube.name=%s" % (mid, assets.name("entity.minecraft.sulfur_cube")))
    lang.append("entity.%s.sulfur_cube.content=%s" % (mid, assets.name("entity.minecraft.sulfur_cube.content")))
    lang.append("item.%s.sulfur_cube_bucket.name=%s" % (mid, assets.name("item.minecraft.sulfur_cube_bucket")))
    # Uh Oh's two keys are vanilla's: they are in the look and feel's vanilla text (26.3's, then 26.4's "Uh-Oh").
    for key in ("death.attack.sulfurCubeHot", "death.attack.sulfurCubeHot.item", "death.attack.sulfurCubeHot.player"):
        lang.append("%s=%s" % (key, assets.name(key)))

    # Uh Oh: 26.3's two criteria (TNT given to an adult cube, or dropped TNT it picks up), granted by the mod.
    vanilla = assets.jar_json("data/minecraft/advancement/husbandry/uh_oh.json")
    assets.write_json("advancements/husbandry/uh_oh.json", {
        "parent": "minecraft:husbandry/root",
        "display": {
            "icon": {"item": "minecraft:tnt"},
            "title": {"translate": vanilla["display"]["title"]["translate"]},
            "description": {"translate": vanilla["display"]["description"]["translate"]},
        },
        "criteria": {name: {"trigger": "minecraft:impossible"} for name in vanilla["criteria"]},
        "requirements": vanilla["requirements"],
    })

    events = {}
    names = ["absorb", "bounce", "death", "eject", "hurt", "jump", "squish"]
    for n in names:
        events["entity.sulfur_cube." + n] = assets.copy_sound("entity.sulfur_cube." + n)
    for n in ("death", "eat", "hurt", "jump", "squish"):
        events["entity.small_sulfur_cube." + n] = assets.copy_sound("entity.small_sulfur_cube." + n)
    for a in ARCHETYPES:
        for n in ("hit", "push"):
            e = "entity.sulfur_cube.%s.%s" % (a, n)
            events[e] = assets.copy_sound(e)
    for e in ("item.bucket.fill_sulfur_cube", "item.bucket.empty_sulfur_cube"):
        events[e] = assets.copy_sound(e)
    assets.merge_sounds("sulfurcube", events)
    assets.merge_lang("sulfurcube", sorted(lang) + assets.subtitle_lines(events))


if __name__ == "__main__":
    main()
