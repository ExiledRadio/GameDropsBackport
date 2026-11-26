"""26.4's changes to existing things: the ice blocks' own sound sets and the ice's ambient sound, from the 26.4 jar.

    python -I tools/changes264/generate.py

Packed ice's set is tools/iceblocks' (the packed ice bricks share it). Reads 26.4 Pre-Release 1 until 26.4 releases.
"""
import os
import sys

os.environ.setdefault("GD_ASSETS_VERSION", "26.4-pre-1")
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID
events = {}
for block in ("ice", "frosted_ice", "blue_ice"):
    for kind in ("break", "step", "place", "hit", "fall"):
        event = "block.%s.%s" % (block, kind)
        events[event] = assets.copy_sound(event)
events["block.ice.ambient"] = assets.copy_sound("block.ice.ambient")
assets.merge_sounds("changes264", events)
lines = assets.subtitle_lines(events)
for key, text in (("snowOnPackedIce", "Snow on Packed Ice"), ("mushroomsAnyLight", "Mushrooms in Any Light"),
                  ("iceSounds", "Ice Sounds"), ("iceFaces", "Ice Faces"), ("frostedIceVibrations", "Frosted Ice Vibrations"),
                  ("snowballKnockback", "Snowball Knockback"), ("steeringBoost", "Steering Boost"),
                  ("dragonPerchThrown", "Perching Dragon Ignores Thrown Things")):
    lines.append("config.%s.changes.%s=%s" % (M, key, text))
assets.merge_lang("changes264", lines)
print("changes 26.4: %d sound events" % len(events))
