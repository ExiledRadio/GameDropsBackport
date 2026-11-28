"""26.4 stream 2 assets: the frostbite, ice balls and Freezing, from the 26.4 jar.

    python -I tools/frostbite/generate.py

Textures, the Freezing icon and particle frames, sounds, names, the loot table and the advancement. The spawn egg's
texture goes into Backport Core's modern-1.21.5 pack (Core's SpawnEggModels), written there by Core's own tooling.
Reads 26.4 Pre-Release 2 until 26.4 releases (GD_ASSETS_VERSION; tools/assets.py).
"""
import json
import os
import sys

os.environ.setdefault("GD_ASSETS_VERSION", "26.4-pre-2")
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID
ROOT = assets.ROOT

for tex in ("frostbite", "frostbite_baby", "frostbite_outer_layer"):
    assets.texture("entity/zombie/" + tex)
assets.item_generated("ice_ball", assets.texture("item/ice_ball"))
assets.texture("mob_effect/freezing")
frames = assets.jar_json("assets/minecraft/particles/freezing.json")["textures"]
for i, name in enumerate(frames):
    assets.texture("particle/" + name.rpartition(":")[2].replace("particle/", ""), "particle/freezing_%d" % i)

events = {}
for event in ("entity.frostbite.ambient", "entity.frostbite.death", "entity.frostbite.hurt", "entity.frostbite.step",
              "entity.frostbite.throw", "entity.frostbite.converted_to_zombie", "entity.zombie.converted_to_frostbite",
              "entity.drowned.converted_to_zombie", "entity.husk.converted_to_zombie", "entity.parrot.imitate.frostbite",
              "entity.ice_ball.throw", "entity.ice_ball.break", "event.mob_effect.freezing"):
    events[event] = assets.copy_sound(event)
assets.merge_sounds("frostbite", events)

# The loot table, as 26.4's (looting is 1.12.2's looting_enchant).
loot = {"pools": [
    {"name": "rotten_flesh", "rolls": 1, "entries": [{"type": "item", "name": "minecraft:rotten_flesh", "weight": 1, "functions": [
        {"function": "set_count", "count": {"min": 0, "max": 2}},
        {"function": "looting_enchant", "count": {"min": 0, "max": 1}}]}]},
    {"name": "ice_ball", "rolls": 1, "entries": [{"type": "item", "name": "%s:ice_ball" % M, "weight": 1, "functions": [
        {"function": "set_count", "count": {"min": 0, "max": 3}},
        {"function": "looting_enchant", "count": {"min": 0, "max": 1}}]}]},
]}
assets.write_json("loot_tables/entities/frostbite.json", loot)

# What Goes Around Comes Around: a frostbite killed by an ice ball (26.4's criterion, 1.12.2's predicate names).
adv = {
    "parent": "minecraft:adventure/kill_a_mob",
    "display": {
        "icon": {"item": "%s:ice_ball" % M},
        "title": {"translate": "advancements.%s.adventure.what_goes_around_comes_around.title" % M},
        "description": {"translate": "advancements.%s.adventure.what_goes_around_comes_around.description" % M},
        "frame": "task",
    },
    "criteria": {"kill_frostbite_with_ice_ball": {"trigger": "minecraft:player_killed_entity", "conditions": {
        "entity": {"type": "%s:frostbite" % M},
        "killing_blow": {"is_projectile": True, "direct_entity": {"type": "%s:ice_ball" % M}}}}},
}
assets.write_json("advancements/adventure/what_goes_around_comes_around.json", adv)

n = assets.name
lines = [
    "entity.%s.frostbite.name=%s" % (M, n("entity.minecraft.frostbite")),
    "entity.%s.ice_ball.name=%s" % (M, n("entity.minecraft.ice_ball")),
    "item.%s.ice_ball.name=%s" % (M, n("item.minecraft.ice_ball")),
    "effect.%s.freezing=%s" % (M, n("effect.minecraft.freezing")),
    "potion.effect.%s.freezing=%s" % (M, n("item.minecraft.potion.effect.freezing")),
    "splash_potion.effect.%s.freezing=%s" % (M, n("item.minecraft.splash_potion.effect.freezing")),
    "lingering_potion.effect.%s.freezing=%s" % (M, n("item.minecraft.lingering_potion.effect.freezing")),
    "tipped_arrow.effect.%s.freezing=%s" % (M, n("item.minecraft.tipped_arrow.effect.freezing")),
    "advancements.%s.adventure.what_goes_around_comes_around.title=%s" % (M, n("advancements.adventure.what_goes_around_comes_around.title")),
    "advancements.%s.adventure.what_goes_around_comes_around.description=%s" % (M, n("advancements.adventure.what_goes_around_comes_around.description")),
    "config.%s.frostbite=Frostbite" % M,
    "config.%s.frostbite.enabled=Enabled" % M,
]
lines += assets.subtitle_lines(events)
assets.merge_lang("frostbite", lines)

# The spawn egg's own texture for Backport Core's pack (copied into the Core worktree when it is next to us).
core = os.path.join(os.path.dirname(ROOT), "BackportCore-264", "src", "main", "resources", "backportcore_packs", "modern-1.21.5",
                    "assets", "minecraft", "textures", "item")
if os.path.isdir(core):
    import png_rgba  # noqa: E402
    with open(os.path.join(core, "frostbite_spawn_egg.png"), "wb") as f:
        f.write(png_rgba.keyed_to_rgba(assets.jar_bytes("assets/minecraft/textures/item/frostbite_spawn_egg.png")))
print("frostbite: %d sound events" % len(events))
