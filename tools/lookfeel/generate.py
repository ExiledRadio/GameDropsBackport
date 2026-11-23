"""Look and feel stream assets (26.1-26.3): panorama, pause-menu sprites, menu tracks, splashes, vanilla text, names.

    python -I tools/lookfeel/generate.py

Sources: the 26.3 asset index (panorama faces: the jar holds only 69-byte placeholders; the menu tracks), the 26.3
jar (sprites, English), and the 1.21.11, 26.1, 26.2 and 26.3 jars' splashes.txt (each drop's changes).
"""
import os
import sys
import zipfile

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402
import png_rgba  # noqa: E402

# Panorama and overlay, from the asset index.
for face in range(6):
    assets.write("textures/gui/title/background/panorama_%d.png" % face,
                 assets._object("minecraft/textures/gui/title/background/panorama_%d.png" % face))
assets.write("textures/gui/title/background/panorama_overlay.png",
             png_rgba.keyed_to_rgba(assets._object("minecraft/textures/gui/title/background/panorama_overlay.png")))

# 26.2's pause-menu row sprites (Report Bugs, Give Feedback, Friends, Player Reporting) and the system toast the two
# unavailable ones show (Backport Core draws them when we supply them).
for path in ("pause_menu/bug", "pause_menu/social_interactions", "friends/friends", "pause_menu/player_reporting", "toast/system"):
    path = "textures/gui/sprites/%s.png" % path
    assets.write("vanilla/" + path, png_rgba.keyed_to_rgba(assets.jar_bytes("assets/minecraft/" + path)))

# 26.3's check box sprites (Backport Core draws 1.20.2's check box from them), 26.2's flat sign textures for the
# twelve vanilla woods (Trails & Tales' sign editor), and the End credits (Core reads credits.json as 26.3 does).
for path in ("checkbox", "checkbox_highlighted", "checkbox_selected", "checkbox_selected_highlighted"):
    path = "textures/gui/sprites/widget/%s.png" % path
    assets.write("vanilla/" + path, png_rgba.keyed_to_rgba(assets.jar_bytes("assets/minecraft/" + path)))
for wood in ("acacia", "bamboo", "birch", "cherry", "crimson", "dark_oak", "jungle", "mangrove", "oak", "pale_oak", "spruce",
             "warped"):
    path = "textures/gui/signs/%s.png" % wood
    assets.write("vanilla/" + path, png_rgba.keyed_to_rgba(assets.jar_bytes("assets/minecraft/" + path)))
for path in ("texts/credits.json", "texts/postcredits.txt"):
    assets.write("vanilla/" + path, assets.jar_bytes("assets/minecraft/" + path))

# 26.2's menu tracks (fingerspit). The sulfur caves stream already ships some under the same paths.
for track in ("ebb", "home", "memories", "nightly", "shores"):
    assets.write("sounds/music/game/%s.ogg" % track, assets._object("minecraft/sounds/music/game/%s.ogg" % track))


# Splashes: each drop's additions and removals, from consecutive jars.
def splashes(version):
    z = zipfile.ZipFile("G:/MCDownloads/minecraft-%s/client.jar" % version)
    return z.read("assets/minecraft/texts/splashes.txt").decode("utf-8").splitlines()


previous = splashes("26.1")
# 26.1's one change names a Java version, which Backport Core leaves out on 1.12.2, so it has no file.
for version in ("26.2", "26.3"):
    current = splashes(version)
    added = [s for s in current if s not in previous]
    removed = [s for s in previous if s not in current]
    assets.write("texts/splashes_%s_added.txt" % version, "".join(s + "\n" for s in added))
    if removed:
        assets.write("texts/splashes_%s_removed.txt" % version, "".join(s + "\n" for s in removed))
    previous = current

# Vanilla text: 1.12.2 keys whose English 26.x reworded (between 1.21.11 and 26.3) and that mean the same in 1.12.2.
REWORDED = ["advancements.story.enter_the_nether.description", "advancements.story.iron_tools.title", "build.tooHigh",
            "options.languageWarning", "selectWorld.load_folder_access"]
# And the screens Backport Core lays out as 26.3's: the disconnected screen's button, "Saving world", out of memory.
SCREENS = ["gui.toMenu", "gui.toTitle", "menu.savingLevel", "outOfMemory.title", "outOfMemory.message"]
# One line per key: a line break is written as \n (vanilla_text.py does the same for the other languages).
assets.write("vanilla_text/en_us.lang", "".join("%s=%s\n" % (k, assets.name(k).replace("\n", "\\n"))
                                                for k in sorted(REWORDED + SCREENS)))

# Names: the menu tracks' toast names and the World Options screen, by Mojang's 26.3 keys.
KEYS = ["music.game.%s" % t for t in ("ebb", "home", "memories", "nightly", "shores")] + [
    "options.worldOptions.button", "options.worldOptions.title", "options.worldOptions.general.title",
    "options.worldOptions.multiplayer.title",
    "options.worldOptions.game_mode", "options.worldOptions.game_mode.tooltip",
    "options.worldOptions.personal_game_mode", "options.worldOptions.personal_game_mode.tooltip",
    "options.worldOptions.game_mode.disabled.operator.tooltip", "options.worldOptions.game_mode.disabled.tooltip",
    "options.worldOptions.allow_commands.tooltip", "options.worldOptions.allow_commands.disabled.tooltip",
    "options.worldOptions.difficulty.disabled.hardcore.tooltip", "options.worldOptions.difficulty.disabled.locked.tooltip",
    "options.worldOptions.difficulty.disabled.operator.tooltip",
    "options.worldOptions.guest.command_access", "options.worldOptions.guest.command_access.tooltip",
    "options.worldOptions.guest.command_access.disabled.scope.tooltip",
    "options.worldOptions.guest.command_access.disabled.commands.tooltip",
    "options.worldOptions.guest.force_game_mode", "options.worldOptions.guest.force_game_mode.on.tooltip",
    "options.worldOptions.guest.force_game_mode.off.tooltip", "options.worldOptions.guest.force_game_mode.off.scope.tooltip",
    "options.worldOptions.guest.force_game_mode.off.hardcore.tooltip",
    "options.worldOptions.guest.force_game_mode.off.commands.tooltip",
    "menu.multiplayerOptions.lan", "menu.multiplayerOptions.network.lan.tooltip", "menu.multiplayerOptions.network.off.tooltip",
    "menu.multiplayerOptions.applyChanges", "menu.multiplayerOptions.publish.started.lan",
    "lanServer.port", "lanServer.port.unavailable", "lanServer.port.invalid", "selectWorld.gameMode.spectator",
]
lines = ["%s=%s" % (k, " ".join(assets.name(k).split("\n"))) for k in KEYS if k not in assets.OLD_LANG]
# 26.3's "Allow Commands" under our own key: 1.12.2's selectWorld.allowCommands is the create-world screen's "Allow Cheats:".
lines.append("gamedropsbackport.worldOptions.allowCommands=%s" % assets.name("selectWorld.allowCommands"))
assets.merge_lang("lookfeel", [l.replace("%", "%%") if "%s" not in l and "%%" not in l else l for l in lines])
print("lookfeel: %d lang lines" % len(lines))
