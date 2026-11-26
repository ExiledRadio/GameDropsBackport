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
# The pause menu's Other Players icon (Backport Core lays out 26.4's icon row from it).
assets.write("vanilla/textures/gui/sprites/pause_menu/other_players.png",
             __import__("png_rgba").keyed_to_rgba(assets.jar_bytes("assets/minecraft/textures/gui/sprites/pause_menu/other_players.png")))
print("lookfeel 26.4: the Deep Freeze panorama, the Other Players icon")

# Vanilla text as 26.4 words it: every 1.12.2 key whose English 26.4 changed from 26.3, and the family's own copies of
# those keys (<prefix>.<modid>.<rest>, the backports' subtitles and advancements), in every language Game Drops has.
import json  # noqa: E402
import glob  # noqa: E402
import re  # noqa: E402

old_index = json.load(open(assets.DOWNLOADS + "/minecraft-26.3/assetindex.json", encoding="utf-8"))["objects"]
new_index = assets.INDEX


def mojang(index, code):
    if code == "en_us":
        return None
    path = "minecraft/lang/%s.json" % code
    if path not in index:
        return {}
    h = index[path]["hash"]
    local = "%s/%s/%s" % (assets.OBJECTS, h[:2], h)
    if not os.path.exists(local):
        import urllib.request
        data = urllib.request.urlopen("https://resources.download.minecraft.net/%s/%s" % (h[:2], h)).read()
        os.makedirs(os.path.dirname(local), exist_ok=True)
        open(local, "wb").write(data)
    return json.load(open(local, encoding="utf-8"))


import zipfile  # noqa: E402
en263 = json.loads(zipfile.ZipFile(assets.DOWNLOADS + "/minecraft-26.3/client.jar").read("assets/minecraft/lang/en_us.json").decode("utf-8"))
en264 = assets.VANILLA_LANG
changed = {k for k in en264 if k in en263 and en264[k] != en263[k]}
keys = {k: k for k in changed if k in assets.OLD_LANG}
# The family's copies: a backport's own key with its mod id after the first segment.
FAMILY = ["netherupdatebackport", "cavesandcliffsbackport", "wildupdatebackport", "trailsandtalesbackport",
          "trickytrialsbackport", "updateaquaticbackport", "buzzybeesbackport", "villagerbackport", "gamedropsbackport"]
for lang in glob.glob(os.path.join(os.path.dirname(assets.ROOT), "*", "src", "main", "resources", "assets", "*", "lang", "en_us.lang")):
    workspace = os.path.relpath(lang, os.path.dirname(assets.ROOT)).replace("\\", "/").split("/")[0]
    if "-" in workspace:  # a worktree: the main checkouts only
        continue
    for line in open(lang, encoding="utf-8"):
        if "=" not in line or line.startswith("#"):
            continue
        k = line.split("=", 1)[0]
        if k in changed and line.split("=", 1)[1].rstrip("\n") == en263[k].replace("\n", "\\n"):
            # A backport's copy under vanilla's own key (Nether Update's "Oh Shiny", the sulfur cube's "Uh Oh").
            keys[k] = k
            continue
        parts = k.split(".")
        if len(parts) > 2 and parts[1] in FAMILY:
            vanilla = ".".join([parts[0]] + parts[2:])
            if vanilla in changed and line.split("=", 1)[1].rstrip("\n") == en263[vanilla].replace("\n", "\n"):
                keys[k] = vanilla
codes = [os.path.basename(p)[:-5] for p in glob.glob(os.path.join(assets.ASSETS, "lang", "*.lang"))]
for code in codes:
    tr = mojang(new_index, code)
    lines = []
    for ours, vanilla in sorted(keys.items()):
        value = en264[vanilla] if tr is None else tr.get(vanilla, en264[vanilla])
        lines.append("%s=%s\n" % (ours, value.replace("\r", "").replace("\n", "\n")))
    assets.write("vanilla_text_264/%s.lang" % code, "".join(lines))
print("lookfeel 26.4: %d keys of reworded vanilla text (%d family copies), %d languages" % (
    len(keys), len([k for k, v in keys.items() if k != v]), len(codes)))
