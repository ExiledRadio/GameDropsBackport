"""26.3's built-in High Contrast resource pack, the parts Backport Core uses on 1.12.2 (its High Contrast option).

    python -I tools/lookfeel/high_contrast.py

Source: the 26.3 asset index's minecraft/resourcepacks/high_contrast.zip. Each file is copied under its own path into
assets/gamedropsbackport/vanilla/high_contrast/ (the pack's assets/minecraft/ folder); Core serves them as
minecraft:<path> while the option is on and puts the button, language and lock sprites into 1.12.2's widgets.png.
"""
import io
import os
import sys
import zipfile

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402
import png_rgba  # noqa: E402

PACK = zipfile.ZipFile(io.BytesIO(assets._object("minecraft/resourcepacks/high_contrast.zip")))

FILES = [
    # The buttons and lock buttons (widgets.png), the language button's icon, the title row's icons.
    "textures/gui/sprites/widget/button.png",
    "textures/gui/sprites/widget/button_disabled.png",
    "textures/gui/sprites/widget/button_highlighted.png",
    "textures/gui/sprites/widget/locked_button.png",
    "textures/gui/sprites/widget/locked_button_disabled.png",
    "textures/gui/sprites/widget/locked_button_highlighted.png",
    "textures/gui/sprites/widget/unlocked_button.png",
    "textures/gui/sprites/widget/unlocked_button_disabled.png",
    "textures/gui/sprites/widget/unlocked_button_highlighted.png",
    "textures/gui/sprites/icon/language.png",
    "textures/gui/sprites/icon/accessibility.png",
    # 1.20.5's menu backgrounds and separators (Core's MenuBackground).
    "textures/gui/menu_background.png",
    "textures/gui/inworld_menu_background.png",
    "textures/gui/menu_list_background.png",
    "textures/gui/inworld_menu_list_background.png",
    "textures/gui/header_separator.png",
    "textures/gui/footer_separator.png",
    "textures/gui/inworld_header_separator.png",
    "textures/gui/inworld_footer_separator.png",
]

for path in FILES:
    assets.write("vanilla/high_contrast/" + path, png_rgba.keyed_to_rgba(PACK.read("assets/minecraft/" + path)))
print("high contrast: %d files" % len(FILES))
