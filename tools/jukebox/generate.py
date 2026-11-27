"""26.4 stream 6 assets: the music disc Flurry and the jukebox's 26.4 look, from the 26.4 jar.

    python -I tools/jukebox/generate.py

The jukebox model (26.4: cube_bottom_top with a bottom of its own) goes under vanilla/, which the look and feel
stream hands Backport Core as 26.4's replacement of 1.12.2's models/block/jukebox.json.
Reads 26.4 Pre-Release 2 until 26.4 releases (GD_ASSETS_VERSION; tools/assets.py).
"""
import os
import sys

os.environ.setdefault("GD_ASSETS_VERSION", "26.4-pre-2")
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID

assets.item_generated("music_disc_flurry", assets.texture("item/music_disc_flurry"))
for face in ("bottom", "side", "top"):
    assets.texture("block/jukebox_" + face, "blocks/jukebox_" + face)
assets.write_json("vanilla/models/block/jukebox.json", {
    "parent": "block/cube_bottom_top",
    "textures": {"bottom": "%s:blocks/jukebox_bottom" % M, "side": "%s:blocks/jukebox_side" % M, "top": "%s:blocks/jukebox_top" % M},
})

events = {"music_disc.flurry": assets.copy_sound("music_disc.flurry")}
assets.merge_sounds("jukebox", events)
lines = [
    "item.%s.music_disc_flurry.name=%s" % (M, assets.name("item.minecraft.music_disc_flurry")),
    "item.record.flurry.desc=%s" % assets.name("jukebox_song.minecraft.flurry"),
    "config.%s.jukebox=Jukebox" % M,
    "config.%s.jukebox.enabled=Enabled" % M,
]
lines += assets.subtitle_lines(events)
assets.merge_lang("jukebox", lines)
print("jukebox: Flurry, the 26.4 jukebox")
