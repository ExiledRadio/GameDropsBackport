"""26.4's look and feel pieces of Game Drops: the Deep Freeze title panorama, from the 26.4 asset index.

    python -I tools/lookfeel264/generate.py

Written to textures/gui/title/background/deep_freeze/ (26.3's panorama stays where it is, for Core's Newest Look).
Reads 26.4 Pre-Release 1 until 26.4 releases (GD_ASSETS_VERSION; tools/assets.py).
"""
import os
import sys

os.environ.setdefault("GD_ASSETS_VERSION", "26.4-pre-1")
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

for i in range(6):
    data = assets._object("minecraft/textures/gui/title/background/panorama_%d.png" % i)
    assets.write("textures/gui/title/background/deep_freeze/panorama_%d.png" % i, data)
print("lookfeel 26.4: the Deep Freeze panorama")
